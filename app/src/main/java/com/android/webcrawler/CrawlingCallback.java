package com.android.webcrawler;

/**
 * Created by Mike on 18.02.2017.
 */

public interface CrawlingCallback {
    void onPageCrawlingCompleted(String url);

    void onPageCrawlingFailed(String url, int errorCode);

    void onPageCrawlingFinished();

}
