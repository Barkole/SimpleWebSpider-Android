package com.android.webcrawler.bot.http.android;

import com.android.webcrawler.bot.http.HttpClient;
import com.android.webcrawler.util.ValidityHelper;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Mike on 18.02.2017.
 */

public class AndroidHttpClient implements HttpClient {
    private static final String KEY_CONTENT_TYPE = "Content-Type";

    private HttpURLConnection urlConnection;
    private URL url;

    @Override
    public void createConnection(String urlString) throws IOException {
        ValidityHelper.checkNotEmpty("urlString", urlString);

        if (this.url != null) {
            throw new IllegalStateException("There is an already open connection");
        }

        url = new URL(urlString);
        urlConnection = (HttpURLConnection) url.openConnection();
    }

    @Override
    public int getStatusCode() throws IOException {
        checkForOpenConnection();
        return urlConnection.getResponseCode();
    }

    @Override
    public String getStatusLine() throws IOException {
        checkForOpenConnection();
        return urlConnection.getResponseMessage();
    }

    @Override
    public String getRedirectedUrl() {
        checkForOpenConnection();
        return urlConnection.getURL().toExternalForm();
    }

    @Override
    public InputStream getResponseBodyAsStream() throws IOException {
        checkForOpenConnection();
        InputStream bufferedInputStream = new BufferedInputStream(urlConnection.getInputStream());
        return bufferedInputStream;
    }

    @Override
    public void releaseConnection() {
        checkForOpenConnection();
        urlConnection.disconnect();
        urlConnection = null;
        url = null;
    }

    @Override
    public String getMimeType() {
        checkForOpenConnection();

        String contentTypeValue = urlConnection.getHeaderField(KEY_CONTENT_TYPE);
        if (contentTypeValue == null) {
            return null;
        }

        // Only the first part is the mime type: e.g. "text/html; charset=UTF-8"
        final String[] split = contentTypeValue.split(";", -1);
        return split[0];
    }

    private void checkForOpenConnection() {
        if (this.url == null || this.urlConnection == null) {
            throw new IllegalStateException("There is no open connection");
        }
    }
}
