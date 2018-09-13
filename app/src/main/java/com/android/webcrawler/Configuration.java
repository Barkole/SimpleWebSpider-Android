package com.android.webcrawler;

import java.util.HashMap;

/**
 * Created by Mike on 18.02.2017.
 */

public class Configuration {
    private final HashMap<String, Integer> ints = new HashMap<>();

    private final HashMap<String, Long> longs = new HashMap<>();

    public Integer putInt(String key, int value) {
        return ints.put(key, value);
    }

    public int getInt(String key, int defaultValue) {
        Integer integer = ints.get(key);
        return integer == null ? defaultValue : integer;
    }

    public Long putLong(String key, long value) {
        return longs.put(key, value);
    }

    public long getLong(String key, long defaultValue) {
        Long aLong = longs.get(key);
        return aLong == null ? defaultValue : aLong;
    }
}
