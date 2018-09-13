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

    public static final String KEY_QUEUE_SIZE = "database.mem.queue.size";
    private static final int DFLT_QUEUE_SIZE = 10*1_024;


    private final Configuration configuration;
    volatile HostThrottler hostThrottler;
    volatile SimpleSet<String> queue;

    public MemDbHelperFactory(final Configuration configuration, final HostThrottler hostThrottler) {
        this.configuration = configuration;
        this.hostThrottler = hostThrottler;
        int size = configuration.getInt(KEY_QUEUE_SIZE, DFLT_QUEUE_SIZE);
        this.queue = new FixedSizeSet<>(size);
    }

    @Override
    public DbHelper buildDbHelper() throws SQLException {
        return new MemDbHelper(this);
    }

    void shutdown() {
        this.queue = null;
        this.hostThrottler = null;
    }

}
