package com.android.webcrawler;

import android.util.Log;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Helper class to interact with ThreadPoolExecutor for adding and removing
 * runnable in workQueue
 *
 * @author CLARION
 */

public class RunnableManager {
    // Sets the amount of time an idle thread will wait for a task before
    // terminating
    private static final int KEEP_ALIVE_TIME = 1;

    // Sets the Time Unit to seconds
    private final TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;

    private final int maxPoolSize;

    // A queue of Runnables for crawling url
    private final BlockingQueue<Runnable> mCrawlingQueue;

    // A managed pool of background crawling threads
    private final ThreadPoolExecutor mCrawlingThreadPool;

    public RunnableManager() {
        int cores = Runtime.getRuntime().availableProcessors();
        int startSize = cores/2;
        maxPoolSize = cores;
        Log.i(Constant.TAG, String.format("Setup thread pool [cores=%s, startSize=%s, maxPoolSize=%s]", cores, startSize, maxPoolSize));
        mCrawlingQueue = new LinkedBlockingQueue<>();
        mCrawlingThreadPool = new ThreadPoolExecutor(startSize,
                maxPoolSize, KEEP_ALIVE_TIME, KEEP_ALIVE_TIME_UNIT,
                mCrawlingQueue);
    }

    public void addToCrawlingQueue(Runnable runnable) {
        mCrawlingThreadPool.execute(runnable);
    }

    public void cancelAllRunnable() { mCrawlingThreadPool.shutdownNow(); }

    public int getUnusedPoolSize() {
        return maxPoolSize - mCrawlingThreadPool.getActiveCount();
    }

    public boolean hasUnusedThreads() {
        return getUnusedPoolSize() > 0;
    }

    public boolean hasNoActiveThreads() {
        return mCrawlingThreadPool.getActiveCount() == 0;
    }

    public boolean isShuttingDown() {
        return mCrawlingThreadPool.isShutdown()
                || mCrawlingThreadPool.isTerminating()
                || mCrawlingThreadPool.isTerminated();
    }

}
