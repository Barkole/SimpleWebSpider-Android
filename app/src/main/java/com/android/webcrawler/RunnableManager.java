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
    public static final String KEY_THREAD_POOL_HARD_LIMIT = "threads.hard-limit";
    private static final int DFLT_THREAD_POOL_HARD_LIMIT = 128;
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

    public RunnableManager(Configuration configuration) {
        maxPoolSize = determineMaxPoolSize(configuration);
        int startSize = maxPoolSize/2;
        mCrawlingQueue = new LinkedBlockingQueue<>(maxPoolSize);
        mCrawlingThreadPool = new ThreadPoolExecutor(startSize,
                maxPoolSize, KEEP_ALIVE_TIME, KEEP_ALIVE_TIME_UNIT,
                mCrawlingQueue);
    }

    private int determineMaxPoolSize(Configuration configuration) {
        int hardLimit = configuration.getInt(KEY_THREAD_POOL_HARD_LIMIT, DFLT_THREAD_POOL_HARD_LIMIT);
        if (hardLimit < 1) {
            hardLimit = 1;
        }
        int cores = Runtime.getRuntime().availableProcessors();
        int maxPoolSize = cores > hardLimit ? hardLimit : cores;
        Log.i(Constant.TAG, String.format("Setup thread pool [cores=%s, hardLimit=%s, maxPoolSize=%s]", cores, hardLimit, maxPoolSize));

        return maxPoolSize;
    }

    public void addToCrawlingQueue(Runnable runnable) {
        mCrawlingThreadPool.execute(runnable);
    }

    public void cancelAllRunnable() { mCrawlingThreadPool.shutdownNow(); }

    public int getUnusedPoolSize() {
        return mCrawlingQueue.remainingCapacity();
    }

    public boolean hasUnusedThreads() {
        return getUnusedPoolSize() > 0;
    }

    public boolean isShuttingDown() {
        return mCrawlingThreadPool.isShutdown()
                || mCrawlingThreadPool.isTerminating()
                || mCrawlingThreadPool.isTerminated();
    }

}
