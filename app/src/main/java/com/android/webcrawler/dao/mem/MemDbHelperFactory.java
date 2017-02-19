package com.android.webcrawler.dao.mem;

import com.android.webcrawler.Configuration;
import com.android.webcrawler.dao.DbHelper;
import com.android.webcrawler.dao.DbHelperFactory;
import com.android.webcrawler.throttle.host.HostThrottler;

import java.sql.SQLException;

/**
 * Created by Mike on 18.02.2017.
 */

public class MemDbHelperFactory implements DbHelperFactory {

    public static final String KEY_QUEUE_SIZE = "database.mem.queue.size";
    private static final int DFLT_QUEUE_SIZE = 1_024;

    public static final String KEY_HASHES_SIZE = "database.mem.hashes.size";
    private static final int DFLT_HASHES_SIZE = 1_024;

    private final Configuration configuration;
    volatile HostThrottler hostThrottler;
    volatile SmallSet<String> queue;
    volatile SmallSet<String> hashes;

    public MemDbHelperFactory(final Configuration configuration, final HostThrottler hostThrottler) {
        this.configuration = configuration;
        this.hostThrottler = hostThrottler;
        this.queue = buildQueue(this.configuration);
        this.hashes = buildHashes(this.configuration);
    }

    private SmallSet<String> buildHashes(Configuration configuration) {
        int size = configuration.getInt(KEY_HASHES_SIZE, DFLT_HASHES_SIZE);
        return new NiceSet<>(size, size*10, size*100, "hashes");
    }

    private SmallSet<String> buildQueue(Configuration configuration) {
        int size = configuration.getInt(KEY_QUEUE_SIZE, DFLT_QUEUE_SIZE);
        return new NiceSet<>(size, size*10, size*100, "queue");
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

}
