package com.android.webcrawler;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.android.webcrawler.bot.CrawlerImpl;
import com.android.webcrawler.bot.extractor.LinkExtractor;
import com.android.webcrawler.bot.extractor.html.stream.StreamExtractor;
import com.android.webcrawler.bot.http.HttpClientFactory;
import com.android.webcrawler.bot.http.android.AndroidHttpClientFactory;
import com.android.webcrawler.dao.DbHelper;
import com.android.webcrawler.dao.DbHelperFactory;
import com.android.webcrawler.dao.LinkDao;
import com.android.webcrawler.dao.mem.MemDbHelperFactory;
import com.android.webcrawler.throttle.host.simple.SimpleHostThrottler;
import com.android.webcrawler.throttle.throughput.LimitThroughPut;
import com.android.webcrawler.util.MD5;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;


public class WebCrawler {

    private final Context mContext;
    private final SimpleHostThrottler hostThrottler;
    private final DbHelperFactory dbHelperFactory;
    private final LimitThroughPut throttler;
    // For parallel crawling execution using ThreadPoolExecuter
    private final RunnableManager mManager;
    // Callback interface object to notify UI
    private final CrawlingCallback callback;
    private final HttpClientFactory httpClientFactory;

    private final List<String> defaultStartPages;
    private final Configuration configuration;

    private volatile String lastFinishedUrl = "Pending...";
    final private AtomicLong crawledUrlCount = new AtomicLong();

    public WebCrawler(Context ctx, int throttle, Configuration configuration) {
        this.mContext = ctx;
        this.callback = new CrawlingCallback() {
            @Override
            public void onPageCrawlingCompleted(String url) {
                lastFinishedUrl = url;
                crawledUrlCount.incrementAndGet();
            }

            @Override
            public void onPageCrawlingFailed(String url, int errorCode) {
                // Nothing to do
            }

            @Override
            public void onPageCrawlingFinished() {
                // Nothing to do
            }
        };
        this.configuration = configuration;

        defaultStartPages = new ArrayList<>();
        defaultStartPages.add("http://www.uroulette.com/");
        defaultStartPages.add("https://en.wikipedia.org/wiki/Special:Random");
        defaultStartPages.add("https://de.wikipedia.org/wiki/Spezial:Zuf%C3%A4llige_Seite");
        defaultStartPages.add("https://bar.wikipedia.org/wiki/Spezial:Zuf%C3%A4llige_Seite");
        throttler = new LimitThroughPut(throttle);
        hostThrottler = new SimpleHostThrottler(configuration);
        dbHelperFactory = new MemDbHelperFactory(configuration, hostThrottler);
        httpClientFactory = new AndroidHttpClientFactory();
        mManager = new RunnableManager(configuration);
        
    }

    public void start(final String url) {
        new AsyncTask<Object, Object, Object>() {

            @Override
            protected Object doInBackground(Object... params) {
                Log.i(Constant.TAG, "Start crawler");
                try {
                    DbHelper dbHelper = dbHelperFactory.buildDbHelper();
                    LinkDao linkDao = dbHelper.getLinkDao();

                    if (url == null) {
                        for (String defaultUrl : defaultStartPages) {
                            linkDao.saveAndCommit(defaultUrl);
                        }
                    } else {
                        linkDao.saveAndCommit(url);
                    }
                } catch (SQLException e) {
                    Log.e(Constant.TAG, "Failed to prefill database", e);
                }

                mHandler.sendEmptyMessageDelayed(Constant.MSG_SPAWN_CRAWLERS, 100);
                return null;
            }
        }.execute();
    }

    /**
     * API to shutdown ThreadPoolExecuter
     */
    public void stopCrawlerTasks() {
        new AsyncTask<Object, Object, Object>() {

            @Override
            protected Object doInBackground(Object... params) {
                Log.i(Constant.TAG, "Stop crawler");
                mManager.cancelAllRunnable();
                try {
                    DbHelper dbHelper = dbHelperFactory.buildDbHelper();
                    dbHelper.close();
                    dbHelper.shutdown();
                } catch (SQLException e) {
                    Log.wtf(Constant.TAG, "Failed to close database", e);
                }
                return null;
            }
        }.execute();
    }


           /**
     * To manage Messages in a Thread
     */
    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(android.os.Message msg) {
            mHandler.removeMessages(Constant.MSG_SPAWN_CRAWLERS);
            new AsyncTask<Object, Object, Object>() {
                @Override
                protected Object doInBackground(Object... params) {
                    try {
                        final DbHelper dbHelper = dbHelperFactory.buildDbHelper();
                        final LinkDao linkDao = dbHelper.getLinkDao();

                        if (!mManager.isShuttingDown() && throttler.hasNext() && mManager.hasUnusedThreads() && !linkDao.isQueueEmpty()) {
                            String url = linkDao.removeNextAndCommit();
                            if (throttler.next()) {
                                enqueueTask(url);
                            } else {
                                linkDao.saveForced(url);
                            }
                        }
                        dbHelper.close();
                    } catch (SQLException e) {
                        Log.wtf(Constant.TAG, "Failed to access database", e);
                    }

                    if (mManager.isShuttingDown()) {
                        // Quit if manager is shutting done
                        mHandler.removeMessages(Constant.MSG_SPAWN_CRAWLERS);
                        return null;
                    }

                    mHandler.sendEmptyMessageDelayed(Constant.MSG_SPAWN_CRAWLERS, 100);
                    return null;
                }
            }.execute();
        }

        private void enqueueTask(String url) {
           final LinkExtractor extractor = new StreamExtractor(configuration);
           CrawlerImpl crawler = new CrawlerImpl(dbHelperFactory, extractor, httpClientFactory, configuration);
           CrawlerRunner crawlerRunner = new CrawlerRunner(crawler, url, callback, mHandler);
           mManager.addToCrawlingQueue(crawlerRunner);
        }

    };

    public String getLastUrl() {
        return lastFinishedUrl;
    }

    public long getCrawledCount() {
        return crawledUrlCount.get();
    }
}
