package com.android.webcrawler.throttle.throughput;

import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by Mike on 18.02.2017.
 */

public class LimitThroughPut {
    final private List<Date> times						= new LinkedList<>();
    final private int maxPerMinute;

    // use ReentrantLock instead of synchronized for scalability
    private final ReentrantLock lock = new ReentrantLock(false);


    public LimitThroughPut(final int maxPerMinute) {
        this.maxPerMinute = maxPerMinute;
    }

    public long waitTime() {
        lock.lock();
        try {
            final long wait = cleanup();
            return wait;
        } finally {
            lock.unlock();
        }
    }

    public boolean hasNext() {
        lock.lock();
        try {
            final long wait = cleanup();
            return wait <= 0;
        } finally {
            lock.unlock();
        }
    }

    public boolean next() {
        lock.lock();
        try {
            final long wait = cleanup();
            if (wait > 0) {
                return false;
            }

            put();
            return true;
        } finally {
            lock.unlock();
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
