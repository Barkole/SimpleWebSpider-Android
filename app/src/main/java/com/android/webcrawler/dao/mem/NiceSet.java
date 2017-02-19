package com.android.webcrawler.dao.mem;

import org.arakhne.afc.references.SoftArrayList;
import org.arakhne.afc.references.SoftHashSet;
import org.arakhne.afc.references.WeakHashSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by Mike on 19.02.2017.
 */

public class NiceSet<E> implements SmallSet<E> {
    private final Set<E> weakSet;
    private final List<E> softList;
    private final List<E> hardList;
    private final Random rnd;

    // use ReentrantLock instead of synchronized for scalability
    private final ReentrantLock lock;

    private final int weakSize;
    private final int softSize;
    private final int hardSize;

    private final String label;


    public NiceSet(int hardSize, int softSize, int weakSize, String label) {
        if (hardSize < 0) {
            throw new IllegalArgumentException(String.format("Hard size must be at least zero [hardSize=%s]", hardSize));
        }
        if (softSize<hardSize) {
            throw new IllegalArgumentException(String.format("Soft size is lower than hard size [hardSize=%s, softSize=%s]", hardSize, softSize));
        }
        if (weakSize<softSize) {
            throw new IllegalArgumentException(String.format("Waek size is lower than soft size [softSize=%s, weakSize=%s]", softSize, weakSize));
        }

        this.hardSize = hardSize;
        this.softSize = softSize;
        this.weakSize = weakSize;
        this.label = label;
        hardList = new ArrayList<>(hardSize+1);
        softList = new SoftArrayList<>(softSize+1);
        weakSet = new WeakHashSet<>(weakSize+1);
        rnd = new Random();
        lock = new ReentrantLock(false);
    }

    @Override
    public void clear() {
        lock.lock();
        try {
            weakSet.clear();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean contains(E e) {
        lock.lock();
        try {
            return weakSet.contains(e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean isEmpty() {
        lock.lock();
        try {
            return weakSet.isEmpty();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public E remove() {
        lock.lock();
        try {
            int size = weakSet.size();
            if (size == 0) {
                return null;
            }

            // First try random
            E element = removeAt(weakSet, rnd.nextInt(size));

            // Then get just first one
            if (element == null) {
                element = removeAt(weakSet, 0);
            }

            // Then clear othere lists
            removeFromLists(element);

            return element;
        } finally {
            lock.unlock();
        }
    }

    private E removeAt(Collection<E> c, int i) {
        int counter = 0;
        for (Iterator<E> iterator = c.iterator(); iterator.hasNext(); counter++) {
            E element = iterator.next();
            if (counter >= i) {
                iterator.remove();
                return element;
            }
        }
        return null;
    }

    private E removeFromLists(E e) {
        if (e != null) {
            softList.remove(e);
            hardList.remove(e);
        }
        return e;
    }

    @Override
    public boolean put(E e) {
        lock.lock();
        try {
            boolean added = weakSet.add(e);
            if (added) {
                hardList.add(e);
                softList.add(e);
                ensureLimits();
            }
            return added;
        } finally {
            lock.unlock();
        }
    }

    private void ensureLimits() {
        while (hardList.size() > hardSize) {
            removeAt(hardList, 0);
        }
        while (softList.size() > softSize) {
            removeAt(softList, 0);
        }
        while (weakSet.size() > weakSize) {
            removeAt(weakSet, 0);
        }
    }

    @Override
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
