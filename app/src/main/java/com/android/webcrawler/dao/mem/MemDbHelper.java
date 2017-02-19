package com.android.webcrawler.dao.mem;

import com.android.webcrawler.dao.DbHelper;
import com.android.webcrawler.dao.LinkDao;
import com.android.webcrawler.throttle.host.HostThrottler;

import java.sql.SQLException;

/**
 * Created by Mike on 18.02.2017.
 */

class MemDbHelper implements DbHelper {

    private final MemDbHelperFactory memDbHelperFactory;

    public MemDbHelper(MemDbHelperFactory memDbHelperFactory) {
        this.memDbHelperFactory = memDbHelperFactory;
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
        memDbHelperFactory.shutdown();
    }

    SmallSet<String> getQueue() {
        return memDbHelperFactory.queue;
    }

    SmallSet<String> getHashes() {
        return memDbHelperFactory.hashes;
    }

    HostThrottler getHostThrottler() {
        return memDbHelperFactory.hostThrottler;
    }
}
