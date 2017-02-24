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

    private static final int QUEUE_SIZE = 100*1024;

    private final Configuration configuration;
    volatile HostThrottler hostThrottler;
    volatile SimpleSet<String> queue;

    public MemDbHelperFactory(final Configuration configuration, final HostThrottler hostThrottler) {
        this.configuration = configuration;
        this.hostThrottler = hostThrottler;
        this.queue = new FixedSizeSet<>(QUEUE_SIZE);
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
