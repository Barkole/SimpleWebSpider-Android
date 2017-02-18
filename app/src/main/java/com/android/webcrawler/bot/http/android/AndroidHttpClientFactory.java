package com.android.webcrawler.bot.http.android;

import android.os.Build;

import com.android.webcrawler.bot.http.HttpClient;
import com.android.webcrawler.bot.http.HttpClientFactory;

import java.net.CookieHandler;
import java.net.CookieManager;

/**
 * Created by Mike on 18.02.2017.
 */

public class AndroidHttpClientFactory implements HttpClientFactory {
    public AndroidHttpClientFactory() {
        disableConnectionReuseIfNecessary();
        enableCookieManager();
    }

    private void enableCookieManager() {
        CookieManager cookieManager = new CookieManager();
        CookieHandler.setDefault(cookieManager);
    }

    private void disableConnectionReuseIfNecessary() {
        // Work around pre-Froyo bugs in HTTP connection reuse.
        if (Integer.parseInt(Build.VERSION.SDK) < Build.VERSION_CODES.FROYO) {
            System.setProperty("http.keepAlive", "false");
        }
    }

    @Override
    public HttpClient buildHttpClient() {
        return new AndroidHttpClient();
    }
}
