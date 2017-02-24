package com.android.webcrawler.dao.mem;

import android.util.Log;

import com.android.webcrawler.Constant;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by Mike on 18.02.2017.
 */

class FixedSizeSet<E>  implements SimpleSet<E> {
    private final int maxSize;
    private final Set<E> set = new LinkedHashSet<>();
    private final Random rnd = new Random();
    private long exceededCount = 0;

    // use ReentrantLock instead of synchronized for scalability
    private final ReentrantLock lock;

    public FixedSizeSet(int maxSize) {
        this.maxSize = maxSize;
        this.lock = new ReentrantLock(false);
    }

    public E remove() {
        lock.lock();
        try {
            int size = set.size();
            if (size == 0) {
                return null;
            }

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
        } finally {
            lock.unlock();
        }
    }

    public boolean put(E e) {
        lock.lock();
        try {
            if (set.size() >= maxSize) {
                if (++exceededCount >= 1_000) {
                    Log.d(Constant.TAG, String.format("Queue has limit exceeded: Remove one random [size=%s, maxSize=%s]", set.size(), maxSize));
                    exceededCount = 0;
                }
                remove();
            } else if ((set.size()) % 1_000 == 0){
                Log.d(Constant.TAG, String.format("Queue added new element [size=%s, maxSize=%s]", set.size(), maxSize));
            }
            return set.add(e);
        } finally {
            lock.unlock();
        }
    }

    public void addAll(Collection<? extends E> c) {
        lock.lock();
        try {
            for (E e: c) {
                put(e);
            }
        } finally {
            lock.unlock();
        }
    }

}
