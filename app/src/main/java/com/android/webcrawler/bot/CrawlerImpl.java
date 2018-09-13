/**
 * Simple Web Spider - <http://simplewebspider.sourceforge.net/>
 * Copyright (C) 2009  <berendona@users.sourceforge.net>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.android.webcrawler.bot;

import android.util.Log;

import com.android.webcrawler.Configuration;
import com.android.webcrawler.Constant;
import com.android.webcrawler.bot.extractor.LinkExtractor;
import com.android.webcrawler.bot.http.HttpClient;
import com.android.webcrawler.bot.http.HttpClientFactory;
import com.android.webcrawler.dao.DbHelper;
import com.android.webcrawler.dao.DbHelperFactory;
import com.android.webcrawler.dao.LinkDao;
import com.android.webcrawler.util.SimpleUrl;
import com.android.webcrawler.util.StringUtils;
import com.android.webcrawler.util.ValidityHelper;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CrawlerImpl implements Crawler {

	private final DbHelperFactory	dbHelperFactory;
	private final LinkExtractor linkExtractor;
	private final HttpClientFactory httpClientFactory;
	private final Configuration configuration;

	public CrawlerImpl(final DbHelperFactory dbHelperFactory, final LinkExtractor linkExtractor, final HttpClientFactory httpClientFactory,
					   final Configuration configuration) {
		this.dbHelperFactory = dbHelperFactory;
		this.linkExtractor = linkExtractor;
		this.httpClientFactory = httpClientFactory;
		this.configuration = configuration;
	}

	private HttpClient getHttpConnection(final String baseUrl) {
		final HttpClient httpClient = this.httpClientFactory.buildHttpClient();

		try {
			final String normalform = new SimpleUrl(baseUrl).toNormalform(false, true);
			httpClient.createConnection(normalform);

			final int statusCode = httpClient.getStatusCode();
			if (statusCode < 200 || statusCode >= 300) {
				String newLocation = httpClient.getReponseHeader("Location");
				if (newLocation == null) {
					Log.i(Constant.TAG, "Failed to load URL \"" + baseUrl + "\":" + httpClient.getStatusLine());
				} else {
					Log.i(Constant.TAG, "Failed to load URL \"" + baseUrl + "\", responses with new location: " + newLocation );
					List urls = new ArrayList(1);
					urls.add(newLocation);
					saveLinks(urls);
				}
				httpClient.releaseConnection();
				return null;
			}
		} catch (final Exception e) {
			if (e instanceof RuntimeException) {
				Log.e(Constant.TAG, "Failed to load URL \"" + baseUrl + "\"", e);
			} else {
				Log.i(Constant.TAG, "Failed to load URL \"" + baseUrl + "\": " + e);
				Log.d(Constant.TAG, "Failed to load URL \"" + baseUrl + "\"", e);
			}
			return null;
		}

		return httpClient;
	}

	/*
	 * (non-Javadoc)
	 * @see simplespider.simplespider_core.bot.Crawler#crawl(java.lang.String)
	 */
	public void crawl(final String baseUrl) {
		ValidityHelper.checkNotEmpty("baseUrl", baseUrl);

		try {
			final HttpClient httpClient = getHttpConnection(baseUrl);
			if (httpClient == null) {
				return;
			}

			final List<String> urls;
			try {
				urls = getLinks(baseUrl, httpClient);
			} finally {
				// clean up the connection resources
				httpClient.releaseConnection();
			}

			if (urls == null) {
				// Error occurs, try it later
				setLinkUndone(baseUrl);
			} else {
				saveLinks(urls);
			}
		} catch (final Throwable e) {
			Log.wtf(Constant.TAG, "Failed to crawl URL \"" + baseUrl + "\"", e);
		}
	}

	private boolean isProtocolSupported(String url) {
		url = url.trim();
		final int p = url.indexOf(':');
		if (p < 0) {
			if (url.startsWith("www.")) {
				return true;
			}
			Log.i(Constant.TAG, "Protocol is not given: " + url);
			return false;
		}

		final String protocol = url.substring(0, p).trim().toLowerCase();
		return "http".equals(protocol) // 
				|| "https".equals(protocol);
	}

	private void saveLinks(final List<String> urls) throws SQLException {
		final DbHelper dbHelper = this.dbHelperFactory.buildDbHelper();
		try {
			final LinkDao linkDao = dbHelper.getLinkDao();

			for (final String url : urls) {
				if (!isProtocolSupported(url)) {
					Log.d(Constant.TAG, "Ignoring not supported protocol; url: " + url);
					continue;
				}

				SimpleUrl simpleUrl;
				try {
					simpleUrl = new SimpleUrl(url);
				} catch (final Exception e) {
					Log.i(Constant.TAG, "Ignoring malformed URL \"" + url + "\"", e);
					continue;
				}

				final String cleanedUrl = simpleUrl.toNormalform(false, true);
				try {
					linkDao.saveAndCommit(cleanedUrl);
				} catch (final Exception e) {
					Log.wtf(Constant.TAG, "Failed to save url: " + cleanedUrl, e);
					dbHelper.rollbackTransaction();
				}
			}
		} finally {
			try {
				dbHelper.close();
			} catch (final Exception e) {
				Log.wtf(Constant.TAG, "Failed to close database connection", e);
			}
		}
	}

	private List<String> getLinks(final String baseUrl, final HttpClient httpClient) throws SQLException, MalformedURLException {
		final String realBaseUrl;
		//		try {
		realBaseUrl = httpClient.getRedirectedUrl();
		//		} catch (final URIException e) {
		//			LOG.warn("Failed to get URI after redirection for URL \"" + baseUrl + "\"", e);
		//			return null;
		//		}

		final String cleanedRealBaseUrl = new SimpleUrl(realBaseUrl).toNormalform(false, true);

		final InputStream bodyAsStream;
		try {
			bodyAsStream = httpClient.getResponseBodyAsStream();
		} catch (final IOException e) {
			Log.wtf(Constant.TAG, "Failed to get body for url \"" + cleanedRealBaseUrl + "\"", e);
			return null;
		}

		if (bodyAsStream == null) {
			Log.wtf(Constant.TAG, "Failed to get body for url \"" + cleanedRealBaseUrl + "\"");
			return null;
		}

		final String mimeType = httpClient.getMimeType();
		// Only supporting HTTP and mime type plain and html
		// If not mime type is defined, so hope it will be plain or html ;-)
		if (ValidityHelper.isEmpty(mimeType) //
				|| isMimeSupported(mimeType) //
		) {
			try {
				return this.linkExtractor.getUrls(bodyAsStream, cleanedRealBaseUrl);
			} catch (final IOException e) {
				Log.wtf(Constant.TAG, "Failed to extract links from body for url \"" + cleanedRealBaseUrl + "\"", e);
				return null;
			}
		} else {
			if (isMimeExcluded(mimeType)) {
				Log.d(Constant.TAG, "Excluded mime type \"" + mimeType + "\": Ignoring URL \"" + baseUrl + "\"");
			} else {
				Log.i(Constant.TAG, "Not supporting mime type \"" + mimeType + "\": Ignoring URL \"" + baseUrl + "\"");
			}
			return new ArrayList<String>(0);
		}
	}

	private boolean isMimeSupported(String mimeType) {
		if (ValidityHelper.isEmpty(mimeType)) {
			return false;
		}

		mimeType = mimeType.toLowerCase();
		return "text/plain".equals(mimeType) //
				|| "text/html".equals(mimeType) //
				|| "text/xml".equals(mimeType) //
				|| "text/x-opml".equals(mimeType) //
				|| "text/x-opml+xml".equals(mimeType) //
				|| "application/atom+xml".equals(mimeType) //
				|| "application/atomcoll+xml".equals(mimeType) //
				|| "application/atomserv+xml".equals(mimeType) //
				|| "application/html+xml".equals(mimeType) //
				|| "application/rdf+xml".equals(mimeType) //
				|| "application/rss+xml".equals(mimeType) //
				|| "application/xml".equals(mimeType);
	}

	private boolean isMimeExcluded(String mimeType) {
		if (ValidityHelper.isEmpty(mimeType)) {
			return false;
		}

		mimeType = mimeType.toLowerCase();
		return mimeType.startsWith("image/") //
				|| "text/css".equals(mimeType);
	}

	private void setLinkUndone(final String baseUrl) {
		try {
			final DbHelper dbHelper = this.dbHelperFactory.buildDbHelper();
			try {
				dbHelper.beginTransaction();
				try {
					final LinkDao linkDao = dbHelper.getLinkDao();
					linkDao.saveForced(baseUrl);
				} catch (final Exception e) {
					try {
						dbHelper.rollbackTransaction();
					} catch (final Exception e2) {
						Log.wtf(Constant.TAG, "Failed to rollback connection", e2);
					}
					throw e;
				}
			} finally {
				try {
					dbHelper.close();
				} catch (final Exception e) {
					Log.wtf(Constant.TAG, "Failed to close database connection", e);
				}
			}
		} catch (final Exception e) {
			Log.wtf(Constant.TAG, "Failed to resave url: " + baseUrl, e);
		}
	}

}
