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

public final class StringUtils {
	private StringUtils() {
		// Only static helpers
	}

	public static CharSequence clipping(final CharSequence value, final int maxLength) {
		if (ValidityHelper.isEmpty(value) // 
				|| (value.length() <= maxLength)) {
			return value;
		}

		if (maxLength <= 3) {
			return "...".substring(0, maxLength);
		}

		return value.subSequence(0, maxLength - 3) + "...";
	}

	static public String byteToHex(final byte b) {
		final StringBuilder sb = new StringBuilder(2);
		byteToHex(b, sb);
		return sb.toString();
	}

	static public void byteToHex(final byte b, final StringBuilder sb) {
		// Returns hex String representation of byte b
		final char hexDigit[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
		sb.append(hexDigit[(b >> 4) & 0x0f]);
		sb.append(hexDigit[b & 0x0f]);
	}

	static public String charToHex(final char c) {
		final StringBuilder sb = new StringBuilder(4);
		charToHex(c, sb);
		return sb.toString();
	}

	static public void charToHex(final char c, final StringBuilder sb) {
		// Returns hex String representation of char c
		final byte hi = (byte) (c >>> 8);
		final byte lo = (byte) (c & 0xff);
		byteToHex(hi, sb);
		byteToHex(lo, sb);
	}

	static public String stringToHex(final String s, final boolean prefix) {
		final StringBuilder sb;
		if (prefix) {
			sb = new StringBuilder(s.length() + 2);
			sb.append("0x");
		} else {
			sb = new StringBuilder(s.length());
		}
		stringToHex(s, sb);
		return sb.toString();
	}

	static public void stringToHex(final String s, final StringBuilder sb) {
		for (int i = 0; i < s.length(); i++) {
			charToHex(s.charAt(i), sb);
		}
	}

	static public String bytesToHex(final byte[] b, final boolean prefix) {
		final StringBuilder sb;
		if (prefix) {
			sb = new StringBuilder(b.length + 2);
			sb.append("0x");
		} else {
			sb = new StringBuilder(b.length);
		}
		bytesToHex(b, sb);
		return sb.toString();
	}

	static public void bytesToHex(final byte[] b, final StringBuilder sb) {
		for (int i = 0; i < b.length; i++) {
			byteToHex(b[i], sb);
		}
	}
}
