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

    public static final String KEY_QUEUE_MAX_SIZE = "database.mem.queue.max-size";
    private static final int DFLT_QUEUE_MAX_SIZE = 100_000;

    public static final String KEY_HASHES_MAX_SIZE = "database.mem.hashes.max-size";
    private static final int DFLT_HASHES_MAX_SIZE = 100_000;

    private final Configuration configuration;
    private final HostThrottler hostThrottler;
    private final FixedSizeSet<String> queue;
    private final FixedSizeSet<String> hashes;

    public MemDbHelperFactory(final Configuration configuration, final HostThrottler hostThrottler) {
        this.configuration = configuration;
        this.hostThrottler = hostThrottler;
        this.queue = buildQueue(this.configuration);
        this.hashes = buildHashes(this.configuration);
    }

    private FixedSizeSet<String> buildHashes(Configuration configuration) {
        int maxSize = configuration.getInt(KEY_HASHES_MAX_SIZE, DFLT_HASHES_MAX_SIZE);
        return new FixedSizeSet<>(maxSize, "hashes");
    }

    private FixedSizeSet<String> buildQueue(Configuration configuration) {
        int maxSize = configuration.getInt(KEY_QUEUE_MAX_SIZE, DFLT_QUEUE_MAX_SIZE);
        return new FixedSizeSet<>(maxSize, "queue");
    }

    @Override
    public DbHelper buildDbHelper() throws SQLException {
        return new MemDbHelper(hostThrottler, queue, hashes);
    }

}
