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
package com.android.webcrawler;

import android.os.Handler;
import android.os.Process;
import android.util.Log;

import com.android.webcrawler.bot.Crawler;
import com.android.webcrawler.util.ValidityHelper;

public class CrawlerRunner implements Runnable {

	private final Crawler crawler;
	private final String	url;
	private final CrawlingCallback callback;
	private Handler handler;

	public CrawlerRunner(final Crawler crawler, final String url, CrawlingCallback callback, Handler handler) {
		ValidityHelper.checkNotNull("crawler", crawler);
		ValidityHelper.checkNotEmpty("url", url);
		ValidityHelper.checkNotNull("callback", callback);
		ValidityHelper.checkNotNull("handler", handler);

		this.crawler = crawler;
		this.url = url;
		this.callback = callback;
		this.handler = handler;
	}

	@Override
	public void run() {
		Log.d(Constant.TAG, String.format("Start task [url=%s]", url));
		Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);
		callback.onPageCrawlingCompleted(url);
		this.crawler.crawl(url);
	}

}
