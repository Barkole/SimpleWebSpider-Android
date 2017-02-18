package com.android.webcrawler.dao.mem;

import com.android.webcrawler.dao.LinkDao;
import com.android.webcrawler.throttle.host.HostThrottler;
import com.android.webcrawler.util.MD5;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


/**
 * Created by Mike on 18.02.2017.
 */

class MemLinkDao implements LinkDao {
    private final MemDbHelper memDbHelper;
    private final Random random;


    public MemLinkDao(MemDbHelper memDbHelper) {
        this.memDbHelper = memDbHelper;
        this.random = new Random();
    }

    @Override
    public String removeNextAndCommit() {
        FixedSizeSet<String> queue = memDbHelper.getQueue();
        HostThrottler hostThrottler = memDbHelper.getHostThrottler();

        // Determine how many Link objects has to be loaded into memory for host throttling
        final int maxUrlsAtOnce = hostThrottler.getUrlsAtOnce();

        // Required for host throttler
        final List<String> urls = new ArrayList<>(maxUrlsAtOnce);

        // Activate all queued links, that are be checked for the best one
        for (int i = 0; i < maxUrlsAtOnce; i++) {
            String url = queue.remove();
            if (url == null) {
                // no more available
                break;
            }
            urls.add(url);
        }

        // If list is empty, there is none URL that could be loaded
        if (urls.isEmpty()) {
            return null;
        }

        // Get the best URL
        final String nextUrlString = hostThrottler.getBestFittingString(urls);

        // Put remaining urls back to queue
        urls.remove(nextUrlString);
        queue.addAll(urls);


        return nextUrlString;
    }

    @Override
    public void saveAndCommit(String link) {
        if (addHash(link)) {
            saveForced(link);
        }
    }

    @Override
    public void saveForced(String link) {
        FixedSizeSet<String> queue = memDbHelper.getQueue();
        queue.put(link);
    }

    @Override
    public boolean isQueueEmpty() {
        FixedSizeSet<String> queue = memDbHelper.getQueue();
        return queue.isEmpty();
    }

    private boolean addHash(final String url) {
        FixedSizeSet<String> hashes = memDbHelper.getHashes();
        String md5 = MD5.encodeString(url, MD5.UTF8);
        return hashes.put(md5);
    }
}
