package com.android.webcrawler;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;


public class WebCrawler {

    private static final long MIN_THREAD_SPAWN_WAIT = 100L;

    private final Context mContext;
    private final SimpleHostThrottler hostThrottler;
    private final DbHelperFactory dbHelperFactory;
    private final LimitThroughPut throttler;
    // For parallel crawling execution using ThreadPoolExecuter
    private final RunnableManager mManager;
    // Callback interface object to notify UI
    private final CrawlingCallback callback;
    private final HttpClientFactory httpClientFactory;

    private static final List<String> defaultStartPages = new ArrayList<>(4);
    {
        defaultStartPages.add("http://www.uroulette.com/");
        defaultStartPages.add("http://linkarena.com/");
        defaultStartPages.add("https://www.dmoz.org/");
        defaultStartPages.add("https://en.wikipedia.org/wiki/Special:Random");
    }
    private final Configuration configuration;

    private volatile String lastFinishedUrl = "Pending...";
    final private AtomicLong crawledUrlCount = new AtomicLong();

    public WebCrawler(Context ctx, int throttle, Configuration configuration, final CrawlingCallback callback) {
        this.mContext = ctx;
        this.callback = new CrawlingCallback() {
            @Override
            public void onPageCrawlingCompleted(String url) {
                lastFinishedUrl = url;
                crawledUrlCount.incrementAndGet();
                callback.onPageCrawlingCompleted(url);
            }

            @Override
            public void onPageCrawlingFailed(String url, int errorCode) {
                callback.onPageCrawlingFailed(url, errorCode);
            }

            @Override
            public void onPageCrawlingFinished() {
                callback.onPageCrawlingFinished();
            }
        };
        this.configuration = configuration;

        throttler = new LimitThroughPut(throttle);
        hostThrottler = new SimpleHostThrottler(configuration);
        dbHelperFactory = new MemDbHelperFactory(configuration, hostThrottler);
        httpClientFactory = new AndroidHttpClientFactory();
        mManager = new RunnableManager(configuration);
        
    }

    public void start(final String url) {
        Log.i(Constant.TAG, "Start crawler");
        try {
            DbHelper dbHelper = dbHelperFactory.buildDbHelper();
            LinkDao linkDao = dbHelper.getLinkDao();

            if (url == null) {
                for (String defaultUrl : defaultStartPages) {
                    // XXX Add to already done list
                    linkDao.saveAndCommit(defaultUrl);
                    linkDao.removeNextAndCommit();
                }
                for (String defaultUrl : defaultStartPages) {
                    enqueueUrl(defaultUrl);
                }
                mHandler.sendEmptyMessageDelayed(Constant.MSG_SPAWN_CRAWLERS, throttler.getStaticWaitTime()+MIN_THREAD_SPAWN_WAIT);
            } else {
                linkDao.saveAndCommit(url);
                mHandler.sendEmptyMessageDelayed(Constant.MSG_SPAWN_CRAWLERS, MIN_THREAD_SPAWN_WAIT);
            }
        } catch (SQLException e) {
            Log.e(Constant.TAG, "Failed to prefill database", e);
            mHandler.sendEmptyMessageDelayed(Constant.MSG_SPAWN_CRAWLERS, MIN_THREAD_SPAWN_WAIT);
        }
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
                private final ReentrantLock lock = new ReentrantLock(false);

                public void handleMessage(android.os.Message msg) {
                    mHandler.removeMessages(Constant.MSG_SPAWN_CRAWLERS);
                    if (!mManager.isShuttingDown() && mManager.hasUnusedThreads() && throttler.hasNext()) {
                        new AsyncTask<Object, Object, Object>() {
                            @Override
                            protected Object doInBackground(Object... params) {
                                lock.lock();
                                try {
                                    final DbHelper dbHelper = dbHelperFactory.buildDbHelper();
                                    final LinkDao linkDao = dbHelper.getLinkDao();

                                    if (!mManager.isShuttingDown() && mManager.hasUnusedThreads() && throttler.hasNext()) {
                                        enqueueNextUrl(linkDao);
                                    }
                                    dbHelper.close();
                                } catch (SQLException e) {
                                    Log.wtf(Constant.TAG, "Failed to access database", e);
                                } finally {
                                    lock.unlock();
                                }

                                if (mManager.isShuttingDown()) {
                                    // Quit if manager is shutting done
                                    mHandler.removeMessages(Constant.MSG_SPAWN_CRAWLERS);
                                    return null;
                                }

                                mHandler.sendEmptyMessageDelayed(Constant.MSG_SPAWN_CRAWLERS, calcWaitTime());
                                return null;
                            }
                        }.execute();
                    } else {
                        long waitTime = throttler.waitTime();
                        if (waitTime < MIN_THREAD_SPAWN_WAIT) {
                            waitTime = MIN_THREAD_SPAWN_WAIT;
                        }
                        mHandler.sendEmptyMessageDelayed(Constant.MSG_SPAWN_CRAWLERS, waitTime);
                    }
                }

               private long calcWaitTime() {
                   long staticWaitTime = throttler.getStaticWaitTime();
                   long waitTime = throttler.waitTime();
                   if (waitTime < staticWaitTime) {
                       waitTime = (waitTime + staticWaitTime)/2L;
                   }
                   if (waitTime < MIN_THREAD_SPAWN_WAIT) {
                       waitTime = MIN_THREAD_SPAWN_WAIT;
                   }

                   return waitTime;
               }



    };

    private void enqueueNextUrl(LinkDao linkDao) {
        String url = linkDao.removeNextAndCommit();
        if (url != null) {
            if (throttler.next()) {
                enqueueUrl(url);
            } else {
                linkDao.saveForced(url);
            }
        }
    }

    private void enqueueUrl(String url) {
        final LinkExtractor extractor = new StreamExtractor(configuration);
        CrawlerImpl crawler = new CrawlerImpl(dbHelperFactory, extractor, httpClientFactory, configuration);
        CrawlerRunner crawlerRunner = new CrawlerRunner(crawler, url, callback, mHandler);
        mManager.addToCrawlingQueue(crawlerRunner);
    }

    public String getLastUrl() {
        return lastFinishedUrl;
    }

    public long getCrawledCount() {
        return crawledUrlCount.get();
    }
}
