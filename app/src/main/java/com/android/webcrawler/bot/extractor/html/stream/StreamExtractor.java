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
package com.android.webcrawler.bot.extractor.html.stream;

import android.util.Log;

import com.android.webcrawler.Configuration;
import com.android.webcrawler.Constant;
import com.android.webcrawler.bot.extractor.LinkExtractor;
import com.android.webcrawler.util.SimpleUrl;
import com.android.webcrawler.util.StringUtils;
import com.android.webcrawler.util.ValidityHelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import static android.webkit.ConsoleMessage.MessageLevel.LOG;

public class StreamExtractor implements LinkExtractor {

	public static final String	EXTRACTOR_HTML_STREAM_MAX_URL_LENGTH			= "extractor.html-stream.max-url-length";
	private static final int	EXTRACTOR_HTML_STREAM_MAX_URL_LENGTH_DEFAULT	= 1024;

	public static final String	EXTRACTOR_HTML_STREAM_BUFFER_SIZE				= "extractor.html-stream.buffer-size-bytes";
	private static final int	EXTRACTOR_HTML_STREAM_BUFFER_SIZE_BYTES_DEFAULT	= 4096;

	final private Configuration configuration;

	public StreamExtractor(final Configuration configuration) {
		this.configuration = configuration;
	}

	public List<String> getUrls(final InputStream body, final String baseUrl) throws IOException {
		ValidityHelper.checkNotNull("body", body);

		int bufferSize = this.configuration.getInt(EXTRACTOR_HTML_STREAM_BUFFER_SIZE, EXTRACTOR_HTML_STREAM_BUFFER_SIZE_BYTES_DEFAULT);
		if (bufferSize <= 0) {
			Log.wtf(Constant.TAG, "Configuration " + EXTRACTOR_HTML_STREAM_BUFFER_SIZE + " is invalid. Using default value: "
					+ EXTRACTOR_HTML_STREAM_BUFFER_SIZE_BYTES_DEFAULT);
			bufferSize = EXTRACTOR_HTML_STREAM_BUFFER_SIZE_BYTES_DEFAULT;
		}

		final TagListenerImpl listener = new TagListenerImpl();
		final HtmlWriter htmlWriter = new HtmlWriter(true, listener, bufferSize);

		parse(body, htmlWriter, baseUrl, bufferSize);

		final List<String> links = getLinks(baseUrl, listener.getLinks());

		return links;
	}

	private List<String> getLinks(final String baseUrl, final List<String> extractedLinks) throws MalformedURLException {
		int maxUrlLength = this.configuration.getInt(EXTRACTOR_HTML_STREAM_MAX_URL_LENGTH, EXTRACTOR_HTML_STREAM_MAX_URL_LENGTH_DEFAULT);
		if (maxUrlLength <= 0) {
			Log.wtf(Constant.TAG, "Configuration " + EXTRACTOR_HTML_STREAM_MAX_URL_LENGTH + " is invalid. Using default value: "
					+ EXTRACTOR_HTML_STREAM_MAX_URL_LENGTH_DEFAULT);
			maxUrlLength = EXTRACTOR_HTML_STREAM_MAX_URL_LENGTH_DEFAULT;
		}
		final SimpleUrl url = new SimpleUrl(baseUrl);
		final List<String> links = new ArrayList<String>(extractedLinks.size());
		for (final String reference : extractedLinks) {
			if (reference.contains("<") || reference.contains(">")) {
				Log.wtf(Constant.TAG, "Ignoring possible invalid reference based on URL \"" + baseUrl + "\":\n" + StringUtils.clipping(reference, 128));
				continue;
			}
			try {
				final SimpleUrl newUrl = SimpleUrl.newURL(url, reference);
				if (newUrl == null) {
					Log.d(Constant.TAG, "Ignoring reference \"" + reference + "\" based on URL \"" + baseUrl + "\", because it contains nothing");
					continue;
				}
				final String normalformedUrl = newUrl.toNormalform(false, true);

				if (normalformedUrl.length() > maxUrlLength) {
					Log.d(Constant.TAG, "Ignoring reference \"" + reference + "\" based on URL \"" + baseUrl + "\", because its size is greater than "
								+ maxUrlLength);
					continue;
				}
				links.add(normalformedUrl);
			} catch (final Exception e) {
				if (e instanceof RuntimeException) {
					Log.e(Constant.TAG, "Ignoring reference \"" + reference + "\" based on URL \"" + baseUrl + "\":" + e, e);
				} else {
					Log.wtf(Constant.TAG, "Ignoring reference \"" + reference + "\" based on URL \"" + baseUrl + "\":" + e);
					Log.d(Constant.TAG, "Ignoring reference \"" + reference + "\" based on URL \"" + baseUrl + "\":" + e, e);
				}
			}
		}
		return links;
	}

	private void parse(final InputStream sourceStream, final HtmlWriter target, final String baseUrl, final int bufferSize) throws IOException {
		final Reader source = new InputStreamReader(sourceStream);
		final char[] buffer = new char[bufferSize];
		long count = 0;

		for (int n = 0; -1 != (n = source.read(buffer));) {
			target.write(buffer, 0, n);
			count += n;

			if (target.binarySuspect()) {
				Log.i(Constant.TAG, "Skip binary content: \"" + baseUrl + "\"");
				break;
			}
		}
		target.flush();

		Log.d(Constant.TAG, "Loaded url \"" + baseUrl + "\": " + count + " bytes");

		target.close();
	}

}
