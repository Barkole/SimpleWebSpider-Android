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

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class MD5 {
	private static ThreadLocal<MessageDigest>	MD5	= new ThreadLocal<MessageDigest>() {

														@Override
														protected MessageDigest initialValue() {
															try {
																return MessageDigest.getInstance("MD5");
															} catch (final NoSuchAlgorithmException e) {
																throw new RuntimeException("Failed to get MD5 digest instance", e);
															}
														}
													};

	public static final String UTF8 = "UTF-8";

	public static final String encodeString(final String string) throws RuntimeException {
		return encodeString(string, null);
	}

	/**
	 * * Retrieves a hexidecimal character sequence representing the MD5 * digest of the specified character sequence, using the specified * encoding
	 * to first convert the character sequence into a byte sequence. * If the specified encoding is null, then ISO-8859-1 is assumed * * @param string
	 * the string to encode. * @param encoding the encoding used to convert the string into the * byte sequence to submit for MD5 digest * @return a
	 * hexidecimal character sequence representing the MD5 * digest of the specified string * @throws HsqlUnsupportedOperationException if an MD5
	 * digest * algorithm is not available through the * java.security.MessageDigest spi or the requested * encoding is not available
	 */
	public static final String encodeString(final String string, final String encoding) throws RuntimeException {
		return StringUtils.bytesToHex(digestString(string, encoding), false);
	}

	/**
	 * * Retrieves a byte sequence representing the MD5 digest of the * specified character sequence, using the specified encoding to * first convert
	 * the character sequence into a byte sequence. * If the specified encoding is null, then ISO-8859-1 is * assumed. * * @param string the string to
	 * digest. * @param encoding the character encoding. * @return the digest as an array of 16 bytes. * @throws HsqlUnsupportedOperationException if
	 * an MD5 digest * algorithm is not available through the * java.security.MessageDigest spi or the requested * encoding is not available
	 */
	public static byte[] digestString(final String string, String encoding) throws RuntimeException {
		byte[] data;
		if (encoding == null) {
			encoding = "ISO-8859-1";
		}
		try {
			data = string.getBytes(encoding);
		} catch (final UnsupportedEncodingException x) {
			throw new RuntimeException("Unsupported encoding: " + encoding, x);
		}
		return digestBytes(data);
	}

	/**
	 * * Retrieves a byte sequence representing the MD5 digest of the * specified byte sequence. * * @param data the data to digest. * @return the MD5
	 * digest as an array of 16 bytes. * @throws HsqlUnsupportedOperationException if an MD5 digest * algorithm is not available through the *
	 * java.security.MessageDigest spi
	 */
	public static final byte[] digestBytes(final byte[] data) throws RuntimeException {
		return MD5.get().digest(data);
	}
}