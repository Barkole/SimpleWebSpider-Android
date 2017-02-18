package com.android.webcrawler.dao.mem;

import com.android.webcrawler.dao.DbHelper;
import com.android.webcrawler.dao.LinkDao;
import com.android.webcrawler.throttle.host.HostThrottler;

import java.sql.SQLException;

/**
 * Created by Mike on 18.02.2017.
 */

class MemDbHelper implements DbHelper {

    private final HostThrottler hostThrottler;
    private final FixedSizeSet<String> queue;
    private final FixedSizeSet<String> hashes;

    public MemDbHelper(final HostThrottler hostThrottler, FixedSizeSet<String> queue, FixedSizeSet<String> hashes) {
        this.hostThrottler = hostThrottler;
        this.queue = queue;
        this.hashes = hashes;
    }

    @Override
    public void beginTransaction() {
        // No transaction support
    }

    @Override
    public void close() throws SQLException {
        // Nothing to do
    }

    @Override
    public void commitTransaction() throws SQLException {
        // No transaction support
    }

    @Override
    public LinkDao getLinkDao() {
        return new MemLinkDao(this);
    }

    @Override
    public void rollbackTransaction() throws SQLException {
        // No transaction support
    }

    @Override
    public void shutdown() throws SQLException {
        queue.clear();
        hashes.clear();
    }

    FixedSizeSet<String> getQueue() {
        return queue;
    }

    FixedSizeSet<String> getHashes() {
        return hashes;
    }

    HostThrottler getHostThrottler() {
        return hostThrottler;
    }
}
