package com.android.webcrawler.dao.mem;

import android.os.Debug;
import android.util.Log;

import com.android.webcrawler.Configuration;
import com.android.webcrawler.Constant;
import com.android.webcrawler.dao.DbHelper;
import com.android.webcrawler.dao.DbHelperFactory;
import com.android.webcrawler.throttle.host.HostThrottler;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import static java.lang.String.format;

/**
 * Created by Mike on 18.02.2017.
 */

public class MemDbHelperFactory implements DbHelperFactory {

    private static final long MIN_REMAINING_HEAP = 10*1024*1024;

    private final Configuration configuration;
    volatile HostThrottler hostThrottler;
    volatile SimpleSet<String> queue;
    volatile SimpleSet<String> hashes;

    public MemDbHelperFactory(final Configuration configuration, final HostThrottler hostThrottler) {
        this.configuration = configuration;
        this.hostThrottler = hostThrottler;
        int maxSize = determineMaxSize();
        Log.i(Constant.TAG, format("Queue size determined [size=%s]", maxSize));
        this.queue = new FixedSizeSet<>(maxSize, "queue");
        this.hashes = new FixedSizeSet<>(maxSize, "hashes");
    }

    @Override
    public DbHelper buildDbHelper() throws SQLException {
        return new MemDbHelper(this);
    }

    void shutdown() {
        this.queue = null;
        this.hashes = null;
        this.hostThrottler = null;
    }

    private static int determineMaxSize() {
        long i = 0;
        Runtime rt = Runtime.getRuntime();
        try {
            List<String> hashes = new LinkedList<>();
            List<String> urls = new LinkedList<>();
            for (; i <= Integer.MAX_VALUE ; i++) {
                hashes.add(format("%032d", i));
                urls.add(format("%0256d", i));
                if (getRemainingHeapSize() < MIN_REMAINING_HEAP) {
                    return (int) (i/2);
                }
            }
        } catch (Throwable e) {
            // reached max size
            return (int) (i/2);
        }

        return Integer.MAX_VALUE/2;
    }

    private static long getRemainingHeapSize() {
        long max = Runtime.getRuntime().maxMemory(); //the maximum memory the app can use
        long heapSize = Runtime.getRuntime().totalMemory(); //current heap size
        long heapRemaining = Runtime.getRuntime().freeMemory(); //amount available in heap
        long nativeUsage = Debug.getNativeHeapAllocatedSize(); //is this right? I only want to account for native memory that my app is being "charged" for.  Is this the proper way to account for that?

//heapSize - heapRemaining = heapUsed + nativeUsage = totalUsage
        long remaining = max - (heapSize - heapRemaining + nativeUsage);
        return remaining;
    }


}
