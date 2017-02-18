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
package com.android.webcrawler.bot.http;

import java.io.IOException;
import java.io.InputStream;

public interface HttpClient {

	public abstract void createConnection(final String url) throws IOException;

	public abstract int getStatusCode() throws IOException;

	public abstract String getStatusLine() throws IOException;

	public abstract String getRedirectedUrl();

	public abstract InputStream getResponseBodyAsStream() throws IOException;

	public abstract void releaseConnection();

	public abstract String getMimeType();

	public abstract String getReponseHeader(String location);
}