package com.android.webcrawler;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class WebCrawler {

    /**
     * Interface for crawling callback
     */
    interface CrawlingCallback {
        void onPageCrawlingCompleted(String url);

        void onPageCrawlingFailed(String url, int errorCode);

        void onCrawlingCompleted();
    }

    private static final int SC_OK = 200;
    private static final int MAX_DOCUMENT_SIZE = 10* 1024 * 1024;
    private final Context mContext;
    // SQLiteOpenHelper object for handling crawling database
    // private CrawlerDB mCrawlerDB;
    // Set containing already visited URls
    private volatile FixedSizeSet<String> crawledURL;
    // Queue for unvisited URL
    private volatile FixedSizeSet<String> uncrawledURL;
    private volatile LimitThroughPut throttler;
    // For parallel crawling execution using ThreadPoolExecuter
    private volatile RunnableManager mManager;
    // Callback interface object to notify UI
    private final CrawlingCallback callback;
    private final List<String> defaultStartPages;

    public WebCrawler(Context ctx, CrawlingCallback callback) {
        this.mContext = ctx;
        this.callback = callback;
        // mCrawlerDB = new CrawlerDB(mContext);
        crawledURL = new FixedSizeSet<>(10_000, "crawledURL");
        uncrawledURL = new FixedSizeSet<>(10_000, "uncrawledURL");
        defaultStartPages = new ArrayList<>();
        defaultStartPages.add("http://www.uroulette.com/");
        defaultStartPages.add("https://en.wikipedia.org/wiki/Special:Random");
        defaultStartPages.add("https://de.wikipedia.org/wiki/Spezial:Zuf%C3%A4llige_Seite");
        defaultStartPages.add("https://bar.wikipedia.org/wiki/Spezial:Zuf%C3%A4llige_Seite");

        throttler = new LimitThroughPut(4);
    }

    public void setup(int queueSize, int throttle) {
        if (queueSize < 1000) {
            queueSize = 1000;
        }
        int crawledQueueSize = queueSize / 1;
        if (crawledQueueSize < 1000) {
            crawledQueueSize = 1000;
        }
        crawledURL = new FixedSizeSet<>(crawledQueueSize, "crawledURL");
        uncrawledURL = new FixedSizeSet<>(queueSize, "uncrawledURL");
        throttler = new LimitThroughPut(throttle);
    }


    /**
     * API to add crawler runnable in ThreadPoolExecutor workQueue
     *
     * @param Url       - Url to crawl
     * @param isRootUrl
     */
    public void startCrawlerTask(String Url, boolean isRootUrl) {
        // If it's root URl, we clear previous lists and DB table content
        if (isRootUrl) {
            Log.d("AndroidSRC_Crawler", "Init crawler");
            crawledURL.clear();
            uncrawledURL.clear();
            clearDB();
            if (Url == null) {
                uncrawledURL.addAll(defaultStartPages);
            }
            mManager = new RunnableManager();
        }
        // If ThreadPoolExecuter is not shutting down, add runable to workQueue
        if (!mManager.isShuttingDown()) {
            Log.d("AndroidSRC_Crawler", String.format("Start task [url=%s]", Url));
            CrawlerRunnable mTask = new CrawlerRunnable(callback, Url);
            mManager.addToCrawlingQueue(mTask);
        }
    }

    /**
     * API to shutdown ThreadPoolExecuter
     */
    public void stopCrawlerTasks() {
        mManager.cancelAllRunnable();
    }

    /**
     * Runnable task which performs task of crawling and adding encountered URls
     * to crawling list
     *
     * @author CLARION
     */
    private class CrawlerRunnable implements Runnable {

        CrawlingCallback mCallback;
        String mUrl;

        public CrawlerRunnable(CrawlingCallback callback, String Url) {
            this.mCallback = callback;
            this.mUrl = Url;
        }

        @Override
        public void run() {
            try {
                mCallback.onPageCrawlingCompleted(mUrl);
                String pageContent = retreiveHtmlContent(mUrl);

                if (!TextUtils.isEmpty(pageContent.toString())) {
//                    insertIntoCrawlerDB(mUrl, pageContent);
//                    crawledURL.put(MD5.buildMD5(mUrl));
                } else {
                    mCallback.onPageCrawlingFailed(mUrl, -1);
                }

                if (!TextUtils.isEmpty(pageContent.toString())) {
                    // START
                    // JSoup Library used to filter urls from html body
                    Document doc = Jsoup.parse(pageContent.toString());
                    Elements links = doc.select("a[href]");
                    for (Element link : links) {
                        String extractedLink = link.attr("href");
                        if (!TextUtils.isEmpty(extractedLink)) {
                            if (!crawledURL.contains(MD5.buildMD5(extractedLink))) {
                                uncrawledURL.put(extractedLink);
                            }
                        }
                    }
                    // End JSoup
                }
            } catch (Exception e) {
                Log.wtf("AndroidSRC_Crawler", String.format("Failed to handle url: %s", mUrl), e);
            }
            // Send msg to handler that crawling for this url is finished
            // start more crawling tasks if queue is not empty
            mHandler.sendEmptyMessageDelayed(0, throttler.waitTime()/2);
        }

        private String retreiveHtmlContent(String Url) {
            URL httpUrl = null;
            try {
                httpUrl = new URL(Url);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            int responseCode = SC_OK;
            StringBuilder pageContent = new StringBuilder();
            try {
                if (httpUrl != null) {
                    HttpURLConnection conn = (HttpURLConnection) httpUrl
                            .openConnection();
                    try {
                        conn.setConnectTimeout(5000);
                        conn.setReadTimeout(5000);
                        responseCode = conn.getResponseCode();
                        if (responseCode != SC_OK) {
                            throw new IllegalAccessException(
                                    " http connection failed");
                        }

                        InputStream inputStream = conn.getInputStream();
                        try {
                            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                            try {
                                BufferedReader br = new BufferedReader(inputStreamReader);
                                try {
                                    String line = null;
                                    while ((line = br.readLine()) != null) {
                                        pageContent.append(line);
                                        if (pageContent.length() > MAX_DOCUMENT_SIZE) {
                                            throw new IOException(String.format("Max content size exceeded [url=%s, loadedContentSize=%s]", Url, pageContent.length()));
                                        }
                                    }
                                } finally {
                                    br.close();
                                }
                            } finally {
                                inputStreamReader.close();
                            }
                        } finally {
                            inputStream.close();
                        }
                    } finally {
                        conn.disconnect();
                    }
                }

            } catch (IOException e) {
                Log.wtf("AndroidSRC_Crawler", String.format("Failed to handle url: %s", mUrl), e);
                mCallback.onPageCrawlingFailed(Url, -1);
            } catch (IllegalAccessException e) {
                Log.wtf("AndroidSRC_Crawler", String.format("Failed to handle url: %s", mUrl), e);
                mCallback.onPageCrawlingFailed(Url, responseCode);
            }

            return pageContent.toString();
        }

    }

    /**
     * API to clear previous content of crawler DB table
     */
    public void clearDB() {
        try {
            // SQLiteDatabase db = mCrawlerDB.getWritableDatabase();
            // db.delete(CrawlerDB.TABLE_NAME, null, null);
        } catch (Exception e) {
            Log.wtf("AndroidSRC_Crawler", "Failed to clead db", e);
        }
    }

    /**
     * API to insert crawled url info in database
     *
     * @param mUrl   - crawled url
     * @param result - html body content of url
     */
    public void insertIntoCrawlerDB(String mUrl, String result) {

        if (TextUtils.isEmpty(result))
            return;

        // SQLiteDatabase db = mCrawlerDB.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(CrawlerDB.COLUMNS_NAME.CRAWLED_URL, mUrl);
        values.put(CrawlerDB.COLUMNS_NAME.CRAWLED_PAGE_CONTENT, result);

        // db.insert(CrawlerDB.TABLE_NAME, null, values);
    }

    /**
     * To manage Messages in a Thread
     */
    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(android.os.Message msg) {
            while (throttler.hasNext() && mManager.getUnusedPoolSize() > 0 && !uncrawledURL.isEmpty()) {
                String url = uncrawledURL.remove();
                if (throttler.next()) {
                    crawledURL.put(MD5.buildMD5(url));
                    startCrawlerTask(url, false);
                } else {
                    uncrawledURL.put(url);
                }
            }
            mHandler.sendEmptyMessageDelayed(0, throttler.waitTime()/2);
        }
        ;
    };

    /**
     * Helper class to interact with ThreadPoolExecutor for adding and removing
     * runnable in workQueue
     *
     * @author CLARION
     */
    private static class RunnableManager {

        // Sets the amount of time an idle thread will wait for a task before
        // terminating
        private static final int KEEP_ALIVE_TIME = 1;

        // Sets the Time Unit to seconds
        private final TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;

        // Sets the initial threadpool size to 5
        //private static final int CORE_POOL_SIZE = 5;

        // Sets the maximum threadpool size to 8
        // private static final int MAXIMUM_POOL_SIZE = 8;
        private final int maxPoolSize;

        // A queue of Runnables for crawling url
        private final BlockingQueue<Runnable> mCrawlingQueue;

        // A managed pool of background crawling threads
        private final ThreadPoolExecutor mCrawlingThreadPool;

        public RunnableManager() {
            int cores = Runtime.getRuntime().availableProcessors();
            maxPoolSize = cores * 2;
            Log.i("AndroidSRC_Crawler", String.format("Setup thread pool [cores=%s, maxPoolSize=%s]", cores, maxPoolSize));
            mCrawlingQueue = new LinkedBlockingQueue<>();
            mCrawlingThreadPool = new ThreadPoolExecutor(cores,
                    maxPoolSize, KEEP_ALIVE_TIME, KEEP_ALIVE_TIME_UNIT,
                    mCrawlingQueue);
        }

        private void addToCrawlingQueue(Runnable runnable) {
            mCrawlingThreadPool.execute(runnable);
        }

        private void cancelAllRunnable() {
            mCrawlingThreadPool.shutdownNow();
        }

        private int getUnusedPoolSize() {
            return maxPoolSize - mCrawlingThreadPool.getActiveCount();
        }

        private boolean isShuttingDown() {
            return mCrawlingThreadPool.isShutdown()
                    || mCrawlingThreadPool.isTerminating();
        }

    }

    private static class FixedSizeSet<E>  {
        private final int maxSize;
        private final Object lock = new Object();
        private final Set<E> set = new LinkedHashSet<>();
        private final Random rnd = new Random();
        private final String label;

        public FixedSizeSet(int maxSize, String label) {
            this.maxSize = maxSize;
            this.label = label;
        }

        public void clear() {
            synchronized (lock) {
                set.clear();
            }
        }

        public boolean contains(E e) {
            synchronized (lock) {
                return set.contains(e);
            }
        }

        public boolean isEmpty() {
            synchronized (lock) {
                return set.isEmpty();
            }
        }

        public E remove() {
            synchronized (lock) {
                int size = set.size();
                int toBeDeleted = rnd.nextInt(size);

                int counter = 0;
                for (Iterator<E> iterator = set.iterator(); iterator.hasNext(); counter++) {
                    E element = iterator.next();
                    if (counter == toBeDeleted) {
                        iterator.remove();
                        return element;
                    }
                }

                return null;
            }
        }

        public boolean put(E e) {
            synchronized (lock) {
                if (set.size() >= maxSize) {
                    Log.d("AndroidSRC_Crawler", String.format("Queue has limit exceeded: Remove one random [label=%s, size=%s, maxSize=%s]", label, set.size(), maxSize));
                    remove();
                } else {
                    Log.d("AndroidSRC_Crawler", String.format("Queue added new element [label=%s, size=%s, maxSize=%s]", label, set.size(), maxSize));
                }
                return set.add(e);
            }
        }

        public void addAll(Collection<? extends E> c) {
            synchronized (lock) {
                set.addAll(c);
            }
        }
    }

    private static class LimitThroughPut {

        final private List<Date>	times						= new LinkedList<>();
        final private int maxPerMinute;
        final private Object lock = new Object();

        public LimitThroughPut(final int maxPerMinute) {
            this.maxPerMinute = maxPerMinute;
        }

        public long waitTime() {
            synchronized (lock) {
                final long wait = cleanup();
                return wait;
            }
        }

        public boolean hasNext() {
            synchronized (lock) {
                final long wait = cleanup();
                return wait <= 0;
            }
        }

        public boolean next() {
            synchronized (lock) {
                final long wait = cleanup();
                if (wait > 0) {
                    return false;
                }

                put();
                return true;
            }
        }

        private void put() {
            this.times.add(new Date());
        }

        private long cleanup() {
            final Date beforeOneMinute = getDateBeforeOneMinute();

            Date firstBlocking = null;

            for (final Iterator<Date> iterator = this.times.iterator(); iterator.hasNext();) {
                final Date item = iterator.next();

                if (item.before(beforeOneMinute)) {
                    // Removing all timestamps before the last minute
                    iterator.remove();
                } else if (this.times.size() < maxPerMinute) {
                    // If less than maximum allowed after removing all old timestamp no job to do
                    break;
                } else {
                    firstBlocking = item;
                    break;
                }
            }

            if (firstBlocking == null) {
                // If there is no blocking, return zero for sleeping
                return 0;
            }

            return firstBlocking.getTime() - beforeOneMinute.getTime();
        }

        private Date getDateBeforeOneMinute() {
            final Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MINUTE, -1);
            final Date current = calendar.getTime();
            return current;
        }

    }

    private static class MD5 {
        private static String convertToHex(final byte[] data) {
            final StringBuffer buf = new StringBuffer();
            for (int i = 0; i < data.length; i++) {
                int halfbyte = (data[i] >>> 4) & 0x0F;
                int two_halfs = 0;
                do {
                    if ((0 <= halfbyte) && (halfbyte <= 9)) {
                        buf.append((char) ('0' + halfbyte));
                    } else {
                        buf.append((char) ('a' + (halfbyte - 10)));
                    }
                    halfbyte = data[i] & 0x0F;
                } while (two_halfs++ < 1);
            }
            return buf.toString();
        }

        public static String buildMD5(String text) {
            try {
                final MessageDigest md = MessageDigest.getInstance("MD5");
                // Reduce life time of big string and bytes of string
                {
                    final byte[] bytes = text.getBytes("UTF-8");
                    text = null;
                    md.update(bytes, 0, bytes.length);
                }
                final byte[] md5hash = md.digest();
                return convertToHex(md5hash);
            } catch (final NoSuchAlgorithmException e) {
                // Should never available
                throw new RuntimeException("MD5 is missing", e);
            } catch (final UnsupportedEncodingException e) {
                // Should never available
                throw new RuntimeException("UTF-8 is missing", e);
            }
        }

    }


}
