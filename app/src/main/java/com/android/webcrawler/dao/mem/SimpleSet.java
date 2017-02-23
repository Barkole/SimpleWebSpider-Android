package com.android.webcrawler.dao.mem;

import java.util.Collection;

/**
 * Created by Mike on 19.02.2017.
 */
public interface SimpleSet<E> {
    E remove();

    boolean put(E e);

    void addAll(Collection<? extends E> c);
}
