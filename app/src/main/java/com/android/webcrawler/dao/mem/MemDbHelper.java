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
    private final int maxLength;

    public MemDbHelper(MemDbHelperFactory memDbHelperFactory, int maxLength) {
        this.memDbHelperFactory = memDbHelperFactory;
        this.maxLength = maxLength;
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
        return new MemLinkDao(this, maxLength);
    }

    @Override
    public void rollbackTransaction() throws SQLException {
        // No transaction support
    }

    @Override
    public void shutdown() throws SQLException {
        memDbHelperFactory.shutdown();
    }

    SimpleSet<String> getQueue() {
        return memDbHelperFactory.queue;
    }

    HostThrottler getHostThrottler() {
        return memDbHelperFactory.hostThrottler;
    }
}
