package com.android.webcrawler.dao.mem;

import android.util.Log;

import com.android.webcrawler.Constant;
import com.android.webcrawler.dao.LinkDao;
import com.android.webcrawler.throttle.host.HostThrottler;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


/**
 * Created by Mike on 18.02.2017.
 */

class MemLinkDao implements LinkDao {
    private final MemDbHelper memDbHelper;
    private final int maxLength;
    private final Random random;


    public MemLinkDao(MemDbHelper memDbHelper, int maxLength) {
        this.memDbHelper = memDbHelper;
        this.maxLength = maxLength;
        this.random = new Random();
    }

    @Override
    public String removeNextAndCommit() {
        SimpleSet<String> queue = memDbHelper.getQueue();
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
        if (link != null && link.length() <= maxLength) {
            saveForced(link);
        } else {
            Log.i(Constant.TAG, String.format("Ignoring too long URL [link=%s]", link));
        }
    }

    @Override
    public void saveForced(String link) {
        SimpleSet<String> queue = memDbHelper.getQueue();
        queue.put(link);
    }
}
