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
package com.android.webcrawler.util;

import android.util.Log;

import com.android.webcrawler.Constant;

import java.net.MalformedURLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * this class exist to provide a system-wide normal form representation of urls, and to prevent that java.net.URL usage causes DNS queries which are
 * used in java.net. <p /> Based on de.anomic.yacy.yacyURL
 */
public class SimpleUrl {
	private final Pattern			backPathPattern	= Pattern.compile("(/[^/]+(?<!/\\.{1,2})/)[.]{2}(?=/|$)|/\\.(?=/)|/(?=/)");

	private int						port;

	// class variables
	private String					protocol;
	private String					host;
	private String					userInfo;
	private String					path;
	private String					quest;
	private String					ref;

	private final static String[]	hex				= { "%00", "%01", "%02", "%03", "%04", "%05", "%06", "%07", "%08", "%09", "%0A", "%0B", "%0C",
			"%0D", "%0E", "%0F", "%10", "%11", "%12", "%13", "%14", "%15", "%16", "%17", "%18", "%19", "%1A", "%1B", "%1C", "%1D", "%1E", "%1F",
			"%20", "%21", "%22", "%23", "%24", "%25", "%26", "%27", "%28", "%29", "%2A", "%2B", "%2C", "%2D", "%2E", "%2F", "%30", "%31", "%32",
			"%33", "%34", "%35", "%36", "%37", "%38", "%39", "%3A", "%3B", "%3C", "%3D", "%3E", "%3F", "%40", "%41", "%42", "%43", "%44", "%45",
			"%46", "%47", "%48", "%49", "%4A", "%4B", "%4C", "%4D", "%4E", "%4F", "%50", "%51", "%52", "%53", "%54", "%55", "%56", "%57", "%58",
			"%59", "%5A", "%5B", "%5C", "%5D", "%5E", "%5F", "%60", "%61", "%62", "%63", "%64", "%65", "%66", "%67", "%68", "%69", "%6A", "%6B",
			"%6C", "%6D", "%6E", "%6F", "%70", "%71", "%72", "%73", "%74", "%75", "%76", "%77", "%78", "%79", "%7A", "%7B", "%7C", "%7D", "%7E",
			"%7F", "%80", "%81", "%82", "%83", "%84", "%85", "%86", "%87", "%88", "%89", "%8A", "%8B", "%8C", "%8D", "%8E", "%8F", "%90", "%91",
			"%92", "%93", "%94", "%95", "%96", "%97", "%98", "%99", "%9A", "%9B", "%9C", "%9D", "%9E", "%9F", "%A0", "%A1", "%A2", "%A3", "%A4",
			"%A5", "%A6", "%A7", "%A8", "%A9", "%AA", "%AB", "%AC", "%AD", "%AE", "%AF", "%B0", "%B1", "%B2", "%B3", "%B4", "%B5", "%B6", "%B7",
			"%B8", "%B9", "%BA", "%BB", "%BC", "%BD", "%BE", "%BF", "%C0", "%C1", "%C2", "%C3", "%C4", "%C5", "%C6", "%C7", "%C8", "%C9", "%CA",
			"%CB", "%CC", "%CD", "%CE", "%CF", "%D0", "%D1", "%D2", "%D3", "%D4", "%D5", "%D6", "%D7", "%D8", "%D9", "%DA", "%DB", "%DC", "%DD",
			"%DE", "%DF", "%E0", "%E1", "%E2", "%E3", "%E4", "%E5", "%E6", "%E7", "%E8", "%E9", "%EA", "%EB", "%EC", "%ED", "%EE", "%EF", "%F0",
			"%F1", "%F2", "%F3", "%F4", "%F5", "%F6", "%F7", "%F8", "%F9", "%FA", "%FB", "%FC", "%FD", "%FE", "%FF" };

	public SimpleUrl(final SimpleUrl baseURL, String relPath) throws MalformedURLException {
		if (baseURL == null) {
			throw new MalformedURLException("base URL is null");
		}
		if (relPath == null) {
			throw new MalformedURLException("relPath is null");
		}

		this.protocol = baseURL.protocol;
		this.host = baseURL.host;
		this.port = baseURL.port;
		this.userInfo = baseURL.userInfo;

		if (relPath.startsWith("//")) {
			// a "network-path reference" as defined in rfc2396 denotes
			// a relative path that uses the protocol from the base url
			relPath = baseURL.protocol + ":" + relPath;
		}

		// FIXME baseURL.path could be null

		if (isAbsolute(relPath)) {
			this.path = baseURL.path;
		} else if (relPath.startsWith("/")) {
			this.path = relPath;
		} else if (baseURL.path.endsWith("/")) {
			//			if (relPath.startsWith("#") //
			//					|| relPath.startsWith("?")) {
			//				throw new MalformedURLException("relative path malformed: " + relPath);
			//			}
			this.path = baseURL.path + relPath;
		} else {
			if (relPath.startsWith("#") //
					|| relPath.startsWith("?")) {
				this.path = baseURL.path + relPath;
			} else {
				final int q = baseURL.path.lastIndexOf('/');
				if (q < 0) {
					this.path = relPath;
				} else {
					this.path = baseURL.path.substring(0, q + 1) + relPath;
				}
			}
		}

		this.quest = baseURL.quest;
		this.ref = baseURL.ref;

		this.path = resolveBackpath(this.path);
		identRef();
		identQuest();
		// escape();
	}

	public SimpleUrl(final String url) throws MalformedURLException {
		checkNotEmpty("url", url);

		parseURLString(url);
	}

	public SimpleUrl(final String protocol, final String host, final int port, final String path) throws MalformedURLException {
		checkNotEmpty("protocol", protocol);
		checkNotEmpty("host", host);

		this.protocol = protocol;
		this.host = host;
		this.port = port;
		this.path = path;
		identRef();
		identQuest();
		// escape();
	}

	private static void checkNotEmpty(final String name, final CharSequence value) throws MalformedURLException {
		ValidityHelper.checkNotEmpty("name", name);

		if (value == null) {
			throw new MalformedURLException(name + " is null");
		}
		if (ValidityHelper.isEmpty(value)) {
			throw new MalformedURLException(name + " is empty");
		}
	}

	/**
	 * Copy constructor
	 * 
	 * @param baseURL
	 *            must not be <code>null</code>
	 * @throws NullPointerException
	 *             if <code>baseUrl</code> is <code>null</code>
	 */
	public SimpleUrl(final SimpleUrl baseURL) {
		ValidityHelper.checkNotNull("baseURL", baseURL);

		this.host = baseURL.host;
		this.path = baseURL.path;
		this.port = baseURL.port;
		this.protocol = baseURL.protocol;
		this.quest = baseURL.quest;
		this.ref = baseURL.ref;
		this.userInfo = baseURL.userInfo;
	}

	/**
	 * Encode a string to the "x-www-form-urlencoded" form, enhanced with the UTF-8-in-URL proposal. This is what happens: <ul> <li>The ASCII
	 * characters 'a' through 'z', 'A' through 'Z', and '0' through '9' remain the same. <li>The unreserved characters - _ . ! ~ * ' ( ) remain the
	 * same. <li>All other ASCII characters are converted into the 3-character string "%xy", where xy is the two-digit hexadecimal representation of
	 * the character code <li>All non-ASCII characters are encoded in two steps: first to a sequence of 2 or 3 bytes, using the UTF-8 algorithm;
	 * secondly each of these bytes is encoded as "%xx". </ul>
	 * 
	 * @param s
	 *            The string to be encoded
	 * @return The encoded string
	 */
	// from: http://www.w3.org/International/URLUTF8Encoder.java
	public static String escape(final String s) {
		final StringBuilder sbuf = new StringBuilder();
		final int len = s.length();
		for (int i = 0; i < len; i++) {
			final int ch = s.charAt(i);
			if ('A' <= ch && ch <= 'Z') { // 'A'..'Z'
				sbuf.append((char) ch);
			} else if ('a' <= ch && ch <= 'z') { // 'a'..'z'
				sbuf.append((char) ch);
			} else if ('0' <= ch && ch <= '9') { // '0'..'9'
				sbuf.append((char) ch);
			} else if (ch == ' ') { // space
				sbuf.append("%20");
			} else if (ch == '&'
					|| ch == ':' // unreserved
					|| ch == '-' || ch == '_' || ch == '.' || ch == '!' || ch == '~' || ch == '*' || ch == '\'' || ch == '(' || ch == ')'
					|| ch == ';') {
				sbuf.append((char) ch);
			} else if (ch <= 0x007f) { // other ASCII
				sbuf.append(hex[ch]);
			} else if (ch <= 0x07FF) { // non-ASCII <= 0x7FF
				sbuf.append(hex[0xc0 | (ch >> 6)]);
				sbuf.append(hex[0x80 | (ch & 0x3F)]);
			} else { // 0x7FF < ch <= 0xFFFF
				sbuf.append(hex[0xe0 | (ch >> 12)]);
				sbuf.append(hex[0x80 | ((ch >> 6) & 0x3F)]);
				sbuf.append(hex[0x80 | (ch & 0x3F)]);
			}
		}
		return sbuf.toString();
	}

	public static void main(final String[] args) {
		final String[][] test = new String[][] { new String[] { null, "http://www.anomic.de/home/test?x=1#home" },
				new String[] { null, "http://www.anomic.de/home/test?x=1" }, new String[] { null, "http://www.anomic.de/home/test#home" },
				new String[] { null, "ftp://ftp.anomic.de/home/test#home" }, new String[] { null, "http://www.anomic.de/home/../abc/" },
				new String[] { null, "mailto:abcdefg@nomailnomail.com" }, new String[] { "http://www.anomic.de/home", "test" },
				new String[] { "http://www.anomic.de/home", "test/" }, new String[] { "http://www.anomic.de/home/", "test" },
				new String[] { "http://www.anomic.de/home/", "test/" }, new String[] { "http://www.anomic.de/home/index.html", "test.htm" },
				new String[] { "http://www.anomic.de/home/index.html", "http://www.yacy.net/test" },
				new String[] { "http://www.anomic.de/home/index.html", "ftp://ftp.yacy.net/test" },
				new String[] { "http://www.anomic.de/home/index.html", "../test" },
				new String[] { "http://www.anomic.de/home/index.html", "mailto:abcdefg@nomailnomail.com" }, new String[] { null, "news:de.test" },
				new String[] { "http://www.anomic.de/home", "news:de.test" },
				new String[] { "http://www.anomic.de/home", "ftp://ftp.anomic.de/src" }, new String[] { null, "ftp://ftp.delegate.org/" },
				new String[] { "http://www.anomic.de/home", "ftp://ftp.delegate.org/" },
				new String[] { "http://www.anomic.de", "mailto:yacy@weltherrschaft.org" }, new String[] { "http://www.anomic.de", "javascipt:temp" },
				new String[] { null, "http://yacy-websuche.de/wiki/index.php?title=De:IntroInformationFreedom&action=history" },
				new String[] { null, "http://diskusjion.no/index.php?s=5bad5f431a106d9a8355429b81bb0ca5&showuser=23585" },
				new String[] { null, "http://diskusjion.no/index.php?s=5bad5f431a106d9a8355429b81bb0ca5&amp;showuser=23585" } };
		String environment, url;
		SimpleUrl aURL, aURL1;
		java.net.URL jURL;
		for (int i = 0; i < test.length; i++) {
			environment = test[i][0];
			url = test[i][1];
			try {
				aURL = SimpleUrl.newURL(environment, url);
			} catch (final MalformedURLException e) {
				aURL = null;
			}
			if (environment == null) {
				try {
					jURL = new java.net.URL(url);
				} catch (final MalformedURLException e) {
					jURL = null;
				}
			} else {
				try {
					jURL = new java.net.URL(new java.net.URL(environment), url);
				} catch (final MalformedURLException e) {
					jURL = null;
				}
			}

			// check equality to java.net.URL
			if (((aURL == null) && (jURL != null)) || ((aURL != null) && (jURL == null))
					|| ((aURL != null) && (jURL != null) && (!(jURL.toString().equals(aURL.toString()))))) {
				System.out.println("Difference for environment=" + environment + ", url=" + url + ":");
				System.out.println((jURL == null) ? "jURL rejected input" : "jURL=" + jURL.toString());
				System.out.println((aURL == null) ? "aURL rejected input" : "aURL=" + aURL.toString());
			}

			// check stability: the normalform of the normalform must be equal to the normalform
			if (aURL != null) {
				try {
					aURL1 = new SimpleUrl(aURL.toNormalform(true, true));
					if (!(aURL1.toNormalform(true, true).equals(aURL.toNormalform(true, true)))) {
						System.out.println("no stability for url:");
						System.out.println("aURL0=" + aURL.toString());
						System.out.println("aURL1=" + aURL1.toString());
					}
				} catch (final MalformedURLException e) {
					System.out.println("no stability for url:");
					System.out.println("aURL0=" + aURL.toString());
					System.out.println("aURL1 cannot be computed:" + e.getMessage());
				}
			}
		}
	}

	// TODO Replace this logic by public constructor
	public static SimpleUrl newURL(final SimpleUrl baseURL, final String relPath) throws MalformedURLException {
		if (baseURL == null //
				|| isAbsolute(relPath)) {
			return new SimpleUrl(relPath);
		}

		if (ValidityHelper.isEmpty(relPath)) {
			return new SimpleUrl(baseURL);
		}

		return new SimpleUrl(baseURL, relPath);
	}

	// TODO Replace this logic by public constructor
	public static SimpleUrl newURL(final String baseURL, final String relPath) throws MalformedURLException {
		if (baseURL == null //
				|| isAbsolute(relPath)) {
			return new SimpleUrl(relPath);
		}

		if (ValidityHelper.isEmpty(relPath)) {
			return new SimpleUrl(baseURL);
		}

		return new SimpleUrl(new SimpleUrl(baseURL), relPath);
	}

	private static boolean isAbsolute(final String path) {
		if (ValidityHelper.isEmpty(path)) {
			return false;
		}

		// Use only find, so we have no need to define the whole complex URI RegExp
		final Pattern protocalPattern = Pattern.compile("[a-zA-Z]+:");
		final Matcher protocolMatcher = protocalPattern.matcher(path);
		// If the expression is found AND is found at the beginning of given string, so we assume that is an absolute URI according to URI definition
		return protocolMatcher.find() && protocolMatcher.start() == 0;
	}

	// from: http://www.w3.org/International/unescape.java
	public static String unescape(final String s) {
		final StringBuilder sbuf = new StringBuilder();
		final int l = s.length();
		int ch = -1;
		int b, sumb = 0;
		for (int i = 0, more = -1; i < l; i++) {
			/* Get next byte b from URL segment s */
			switch (ch = s.charAt(i)) {
			case '%':
				ch = s.charAt(++i);
				final int hb = (Character.isDigit((char) ch) ? ch - '0' : 10 + Character.toLowerCase((char) ch) - 'a') & 0xF;
				ch = s.charAt(++i);
				final int lb = (Character.isDigit((char) ch) ? ch - '0' : 10 + Character.toLowerCase((char) ch) - 'a') & 0xF;
				b = (hb << 4) | lb;
				break;
			case '+':
				b = ' ';
				break;
			default:
				b = ch;
			}
			/* Decode byte b as UTF-8, sumb collects incomplete chars */
			if ((b & 0xc0) == 0x80) { // 10xxxxxx (continuation byte)
				sumb = (sumb << 6) | (b & 0x3f); // Add 6 bits to sumb
				if (--more == 0) {
					sbuf.append((char) sumb); // Add char to sbuf
				}
			} else if ((b & 0x80) == 0x00) { // 0xxxxxxx (yields 7 bits)
				sbuf.append((char) b); // Store in sbuf
			} else if ((b & 0xe0) == 0xc0) { // 110xxxxx (yields 5 bits)
				sumb = b & 0x1f;
				more = 1; // Expect 1 more byte
			} else if ((b & 0xf0) == 0xe0) { // 1110xxxx (yields 4 bits)
				sumb = b & 0x0f;
				more = 2; // Expect 2 more bytes
			} else if ((b & 0xf8) == 0xf0) { // 11110xxx (yields 3 bits)
				sumb = b & 0x07;
				more = 3; // Expect 3 more bytes
			} else if ((b & 0xfc) == 0xf8) { // 111110xx (yields 2 bits)
				sumb = b & 0x03;
				more = 4; // Expect 4 more bytes
			} else /* if ((b & 0xfe) == 0xfc) */{ // 1111110x (yields 1 bit)
				sumb = b & 0x01;
				more = 5; // Expect 5 more bytes
			}
			/* We don't test if the UTF-8 encoding is well-formed */
		}
		return sbuf.toString();
	}

	public int compareTo(final Object h) {
		assert (h instanceof SimpleUrl);
		return toString().compareTo(((SimpleUrl) h).toString());
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof SimpleUrl)) {
			return false;
		}
		final SimpleUrl other = (SimpleUrl) obj;
		if (this.host == null) {
			if (other.host != null) {
				return false;
			}
		} else if (!this.host.equals(other.host)) {
			return false;
		}
		if (this.path == null) {
			if (other.path != null) {
				return false;
			}
		} else if (!this.path.equals(other.path)) {
			return false;
		}
		if (this.port != other.port) {
			return false;
		}
		if (this.protocol == null) {
			if (other.protocol != null) {
				return false;
			}
		} else if (!this.protocol.equals(other.protocol)) {
			return false;
		}
		if (this.quest == null) {
			if (other.quest != null) {
				return false;
			}
		} else if (!this.quest.equals(other.quest)) {
			return false;
		}
		if (this.ref == null) {
			if (other.ref != null) {
				return false;
			}
		} else if (!this.ref.equals(other.ref)) {
			return false;
		}
		if (this.userInfo == null) {
			if (other.userInfo != null) {
				return false;
			}
		} else if (!this.userInfo.equals(other.userInfo)) {
			return false;
		}
		return true;
	}

	/**
	 * Escapes the following parts of the url, this object already contains: <ul> <li>path: see {@link #escape(String)}</li> <li>ref: same as
	 * above</li> <li>quest: same as above without the ampersand ("&amp;") and the equals symbol</li> </ul>
	 */
	// private void escape() {
	// if (this.path != null && this.path.indexOf('%') == -1) {
	// escapePath();
	// }
	// if (this.quest != null && this.quest.indexOf('%') == -1) {
	// escapeQuest();
	// }
	// if (this.ref != null && this.ref.indexOf('%') == -1) {
	// escapeRef();
	// }
	// }
	// private void escapePath() {
	// final String[] pathp = this.path.split("/", -1);
	// String ptmp = "";
	// for (int i = 0; i < pathp.length; i++) {
	// ptmp += "/" + escape(pathp[i]);
	// }
	// this.path = ptmp.substring((ptmp.length() > 0) ? 1 : 0);
	// }
	//
	// private void escapeQuest() {
	// final String[] questp = this.quest.split("&", -1);
	// String qtmp = "";
	// for (int i = 0; i < questp.length; i++) {
	// if (questp[i].indexOf('=') != -1) {
	// qtmp += "&" + escape(questp[i].substring(0, questp[i].indexOf('=')));
	// qtmp += "=" + escape(questp[i].substring(questp[i].indexOf('=') + 1));
	// } else {
	// qtmp += "&" + escape(questp[i]);
	// }
	// }
	// this.quest = qtmp.substring((qtmp.length() > 0) ? 1 : 0);
	// }
	//
	// private void escapeRef() {
	// this.ref = escape(this.ref);
	// }
	public String getAuthority() {
		return ((this.port >= 0) && (this.host != null)) ? this.host + ":" + this.port : ((this.host != null) ? this.host : "");
	}

	public String getFile() {
		return getFile(true);
	}

	public String getFile(final boolean includeReference) {
		// this is the path plus quest plus ref
		// if there is no quest and no ref the result is identical to getPath
		// this is defined according to http://java.sun.com/j2se/1.4.2/docs/api/java/net/URL.html#getFile()
		final StringBuilder sb = new StringBuilder();
		sb.append(this.path);

		if (!ValidityHelper.isEmpty(this.quest)) {
			sb.append('?').append(this.quest);
		}

		if (includeReference && !ValidityHelper.isEmpty(this.ref)) {
			sb.append('#').append(this.ref);
		}

		return sb.toString();
	}

	public String getFileName() {
		// this is a method not defined in any sun api
		// it returns the last portion of a path without any reference
		final int p = this.path.lastIndexOf('/');
		if (p < 0) {
			return this.path;
		}
		if (p == this.path.length() - 1) {
			return ""; // no file name, this is a path to a directory
		}
		return this.path.substring(p + 1); // the 'real' file name
	}

	public String getHost() {
		return this.host;
	}

	public String getPath() {
		return this.path;
	}

	public int getPort() {
		return this.port;
	}

	public String getProtocol() {
		return this.protocol;
	}

	public String getQuery() {
		return this.quest;
	}

	public String getRef() {
		return this.ref;
	}

	public String getUserInfo() {
		return this.userInfo;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.host == null) ? 0 : this.host.hashCode());
		result = prime * result + ((this.path == null) ? 0 : this.path.hashCode());
		result = prime * result + this.port;
		result = prime * result + ((this.protocol == null) ? 0 : this.protocol.hashCode());
		result = prime * result + ((this.quest == null) ? 0 : this.quest.hashCode());
		result = prime * result + ((this.ref == null) ? 0 : this.ref.hashCode());
		result = prime * result + ((this.userInfo == null) ? 0 : this.userInfo.hashCode());
		return result;
	}

	private void identPort(final String inputURL, final int dflt) throws MalformedURLException {
		// identify ref in file
		final int r = this.host.indexOf(':');
		if (r < 0) {
			this.port = dflt;
		} else {
			try {
				final String portStr = this.host.substring(r + 1);
				if (portStr.trim().length() > 0) {
					this.port = Integer.parseInt(portStr);
				} else {
					this.port = -1;
				}
				this.host = this.host.substring(0, r);
			} catch (final NumberFormatException e) {
				throw new MalformedURLException("wrong port in host fragment '" + this.host + "' of input url '" + inputURL + "'");
			}
		}
	}

	private void identQuest() {
		// identify quest in file
		final int r = this.path.indexOf('?');
		if (r < 0) {
			this.quest = null;
		} else {
			this.quest = this.path.substring(r + 1);
			this.path = this.path.substring(0, r);
		}
	}

	private void identRef() {
		// identify ref in file
		final int r = this.path.indexOf('#');
		if (r < 0) {
			this.ref = null;
		} else {
			this.ref = this.path.substring(r + 1);
			this.path = this.path.substring(0, r);
		}
	}

	public boolean isCGI() {
		final String ls = this.path.toLowerCase();
		return ((ls.indexOf(".cgi") >= 0) || (ls.indexOf(".exe") >= 0) || (ls.indexOf(";jsessionid=") >= 0) || (ls.indexOf("sessionid/") >= 0)
				|| (ls.indexOf("phpsessid=") >= 0) || (ls.indexOf("search.php?sid=") >= 0) || (ls.indexOf("memberlist.php?sid=") >= 0));
	}

	public boolean isPOST() {
		return !ValidityHelper.isEmpty(this.quest);
	}

	// language calculation
	public String language() {
		String language = "en";
		final int pos = this.host.lastIndexOf(".");
		if ((pos > 0) && (this.host.length() - pos == 3)) {
			language = this.host.substring(pos + 1).toLowerCase();
		}
		return language;
	}

	private void parseURLString(String url) throws MalformedURLException {
		// identify protocol
		assert (url != null);
		url = url.trim();
		int p = url.indexOf(':');
		if (p < 0) {
			if (url.startsWith("www.")) {
				url = "http://" + url;
				p = 4;
			} else {
				throw new MalformedURLException("protocol is not given in '" + url + "'");
			}
		}
		this.protocol = url.substring(0, p).toLowerCase().trim();
		if (url.length() < p + 4) {
			throw new MalformedURLException("URL not parseable: '" + url + "'");
		}
		if (url.substring(p + 1, p + 3).equals("//")) {
			// identify host, userInfo and file for http and ftp protocol
			final int q = url.indexOf('/', p + 3);
			int r;
			if (q < 0) {
				if ((r = url.indexOf('@', p + 3)) < 0) {
					this.host = url.substring(p + 3);
					this.userInfo = null;
				} else {
					this.host = url.substring(r + 1);
					this.userInfo = url.substring(p + 3, r);
				}
				this.path = "/";
			} else {
				this.host = url.substring(p + 3, q);
				if ((r = this.host.indexOf('@')) < 0) {
					this.userInfo = null;
				} else {
					this.userInfo = this.host.substring(0, r);
					this.host = this.host.substring(r + 1);
				}
				this.path = url.substring(q);
			}

			this.path = resolveBackpath(this.path);
			identPort(url, (this.protocol.equals("http") ? 80 : ((this.protocol.equals("https")) ? 443 : ((this.protocol.equals("ftp")) ? 21 : -1))));
			identRef();
			identQuest();
			// escape();
		} else {
			// this is not a http or ftp url
			if (this.protocol.equals("mailto")) {
				// parse email url
				final int q = url.indexOf('@', p + 3);
				if (q < 0) {
					throw new MalformedURLException("wrong email address: " + url);
				}
				this.userInfo = url.substring(p + 1, q);
				this.host = url.substring(q + 1);
				this.path = null;
				this.port = -1;
				this.quest = null;
				this.ref = null;
			} else if (this.protocol.equals("javascript")) {
				// parse email url
				this.userInfo = null;
				this.host = null;
				this.path = url.substring(p + 1);
				this.port = -1;
				this.quest = null;
				this.ref = null;
			} else {
				throw new MalformedURLException("unknown protocol: " + url);
			}
		}

		// handle international domains
		if (this.host != null // 
				&& !Punycode.isBasic(this.host)) {
			try {
				final int d1 = this.host.lastIndexOf('.');
				if (d1 >= 0) {
					final String tld = this.host.substring(d1 + 1);
					final String dom = this.host.substring(0, d1);
					final int d0 = dom.lastIndexOf('.');
					if (d0 >= 0) {
						this.host = dom.substring(0, d0) + ".xn--" + Punycode.encode(dom.substring(d0 + 1)) + "." + tld;
					} else {
						this.host = "xn--" + Punycode.encode(dom) + "." + tld;
					}
				}
			} catch (final Punycode.PunycodeException e) {
				Log.wtf(Constant.TAG, String.format("Failed to handle international domain [host=%s]", this.host), e);
			}
		}
	}

	/** resolve '..' */
	private String resolveBackpath(String myPath) {
		if (myPath.length() == 0 || myPath.charAt(0) != '/') {
			myPath = "/" + myPath;
		}

		final Matcher matcher = this.backPathPattern.matcher(myPath);
		while (matcher.find()) {
			myPath = matcher.replaceAll("");
			matcher.reset(myPath);
		}

		return myPath.equals("") ? "/" : myPath;
	}

	public String toNormalform(final boolean includeReference) {
		// generates a normal form of the URL
		if (this.protocol.equals("mailto")) {
			return this.protocol + ":" + this.userInfo + "@" + this.host;
		}

		final String resolvedPath = resolveBackpath(this.getFile(includeReference));

		return this.protocol + "://" //
				+ ((this.userInfo != null) ? (this.userInfo + "@") : ("")) //
				+ ((getHost() != null) ? getHost().toLowerCase() : ("")) //
				+ (hasDefaultPort() ? "" : ":" + this.port) //
				+ resolvedPath;
	}

	public String toNormalform(final boolean includeReference, final boolean stripAmp) {
		String result = toNormalform(includeReference);
		if (stripAmp) {
			result = result.replaceAll("&amp;", "&");
		}
		return result;
	}

	private boolean hasDefaultPort() {
		return this.port < 0 //
				|| (this.port == 21 && this.protocol.equals("ftp")) //
				|| (this.port == 80 && this.protocol.equals("http")) //
				|| (this.port == 443 && this.protocol.equals("https"));
	}

	@Override
	public String toString() {
		return toNormalform(true, false);
	}
}
