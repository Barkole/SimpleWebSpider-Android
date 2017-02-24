package com.android.webcrawler;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;


public class WebCrawler {

    private static final long MIN_THREAD_SPAWN_WAIT = 10L;

    private final Context mContext;
    private final SimpleHostThrottler hostThrottler;
    private final DbHelperFactory dbHelperFactory;
    private final LimitThroughPut throttler;
    // For parallel crawling execution using ThreadPoolExecuter
    private final RunnableManager mManager;
    // Callback interface object to notify UI
    private final CrawlingCallback callback;
    private final HttpClientFactory httpClientFactory;

    private volatile boolean shutdown;
    private volatile String startUrl;

    private static final List<String> defaultStartPages = new ArrayList<>(4);
    {
        defaultStartPages.add("http://www.uroulette.com/");
        defaultStartPages.add("http://linkarena.com/");
        defaultStartPages.add("https://www.dmoz.org/");
        defaultStartPages.add("https://en.wikipedia.org/wiki/Special:Random");
    }
    private final Configuration configuration;

    public WebCrawler(Context ctx, int throttle, Configuration configuration, final CrawlingCallback callback) {
        this.mContext = ctx;
        this.callback = callback;
        this.configuration = configuration;
        this.shutdown = false;

        throttler = new LimitThroughPut(throttle);
        hostThrottler = new SimpleHostThrottler(configuration);
        dbHelperFactory = new MemDbHelperFactory(configuration, hostThrottler);
        httpClientFactory = new AndroidHttpClientFactory();
        mManager = new RunnableManager(configuration);
        
    }

    public void start(final String url) {
        Log.i(Constant.TAG, "Start crawler");
        long firstWaitTime = MIN_THREAD_SPAWN_WAIT;
        try {
            shutdown = false;
            startUrl = url;
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
                firstWaitTime += throttler.getStaticWaitTime();
            } else {
                linkDao.saveAndCommit(url);
            }
        } catch (SQLException e) {
            Log.e(Constant.TAG, "Failed to prefill database", e);
        } finally {
            run(firstWaitTime);
        }
    }

    /**
     * API to shutdown ThreadPoolExecuter
     */
    public void stopCrawlerTasks() {
        this.shutdown = true;
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

    private void run(long firstWaitTime) {
        try {
            Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);
            TimeUnit.MILLISECONDS.sleep(firstWaitTime);
            while (!shutdown && !mManager.isShuttingDown()) {
                long waitTime;
                if (mManager.hasUnusedThreads() && throttler.hasNext()) {
                    try {
                        final DbHelper dbHelper = dbHelperFactory.buildDbHelper();
                        final LinkDao linkDao = dbHelper.getLinkDao();

                        if (!mManager.isShuttingDown() && mManager.hasUnusedThreads() && throttler.hasNext()) {
                            enqueueNextUrl(linkDao);
                        }
                        dbHelper.close();
                    } catch (SQLException e) {
                        Log.wtf(Constant.TAG, "Failed to access database", e);
                    }
                    waitTime = calcWaitTime();
                } else {
                     waitTime = throttler.waitTime();
                }
                if (waitTime < MIN_THREAD_SPAWN_WAIT) {
                    waitTime = MIN_THREAD_SPAWN_WAIT;
                }
                TimeUnit.MILLISECONDS.sleep(waitTime);
            }
        } catch (InterruptedException e) {
            Log.i(Constant.TAG, "Thread is canceled");
        } finally {
            shutdown = true;
            mManager.cancelAllRunnable();
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

    private void enqueueNextUrl(LinkDao linkDao) {
        String url = linkDao.removeNextAndCommit();
        if (url != null) {
            if (throttler.next()) {
                enqueueUrl(url);
            } else {
                linkDao.saveForced(url);
            }
        } else {
            Log.i(Constant.TAG, "Queue is empty, so refill it again");
            if (startUrl == null) {
                for (String defaultUrl : defaultStartPages) {
                    linkDao.saveAndCommit(defaultUrl);
                }
            } else {
                linkDao.saveAndCommit(startUrl);
            }
        }
    }

    private void enqueueUrl(String url) {
        final LinkExtractor extractor = new StreamExtractor(configuration);
        CrawlerImpl crawler = new CrawlerImpl(dbHelperFactory, extractor, httpClientFactory, configuration);
        CrawlerRunner crawlerRunner = new CrawlerRunner(crawler, url, callback);
        mManager.addToCrawlingQueue(crawlerRunner);
    }
}
