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
// serverCharBuffer.java 
// ---------------------------
// (C) by Michael Peter Christen; mc@yacy.net
// first published on http://www.anomic.de
// Frankfurt, Germany, 2004
//
// $LastChangedDate: 2006-02-20 23:57:42 +0100 (Mo, 20 Feb 2006) $
// $LastChangedRevision: 1715 $
// $LastChangedBy: borg-0300 $
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
package com.android.webcrawler.bot.extractor.html.stream;

import android.util.Log;

import com.android.webcrawler.Constant;

import java.io.IOException;
import java.io.Writer;
import java.util.Properties;

final class TagWriter extends Writer {

	private static final int    MAX_BUFFER_SIZE = 10 * 1024 * 1024;

	private static final char	singlequote	= '\'';
	private static final char	doublequote	= '"';
	private static final char	equal		= '=';

	private char[]				buffer;
	private int					offset;
	private int					length;

	public TagWriter() {
		this.buffer = new char[10];
		this.length = 0;
		this.offset = 0;
	}

	public TagWriter(final int initLength) {
		checkBufferSize(initLength);
		this.buffer = new char[initLength];
		this.length = 0;
		this.offset = 0;
	}

	public TagWriter(final char[] bb) {
		this.buffer = bb;
		this.length = bb.length;
		this.offset = 0;
	}

	//	public TagWriter(final char[] bb, final int initLength) {
	//		this.buffer = new char[initLength];
	//		System.arraycopy(bb, 0, this.buffer, 0, bb.length);
	//		this.length = bb.length;
	//		this.offset = 0;
	//	}

	//	public TagWriter(final char[] bb, final int of, final int le) {
	//		if (of * 2 > bb.length) {
	//			this.buffer = new char[le];
	//			System.arraycopy(bb, of, this.buffer, 0, le);
	//			this.length = le;
	//			this.offset = 0;
	//		} else {
	//			this.buffer = bb;
	//			this.length = le;
	//			this.offset = of;
	//		}
	//	}

	//	public TagWriter(final TagWriter bb) {
	//		this.buffer = bb.buffer;
	//		this.length = bb.length;
	//		this.offset = bb.offset;
	//	}

	//	public TagWriter(final File f) throws IOException {
	//		// initially fill the buffer with the content of a file
	//		if (f.length() > Integer.MAX_VALUE) {
	//			throw new IOException("file is too large for buffering");
	//		}
	//
	//		this.length = (int) f.length();
	//		this.buffer = new char[this.length * 2];
	//		this.offset = 0;
	//
	//		FileReader fr = null;
	//		try {
	//			fr = new FileReader(f);
	//			final char[] temp = new char[256];
	//			int c;
	//			while ((c = fr.read(temp)) > 0) {
	//				this.append(temp, 0, c);
	//			}
	//		} catch (final FileNotFoundException e) {
	//			throw new IOException("File not found: " + f.toString() + "; " + e.getMessage());
	//		} finally {
	//			if (fr != null) {
	//				fr.close();
	//			}
	//		}
	//	}

	public void clear() {
		this.buffer = new char[0];
		this.length = 0;
		this.offset = 0;
	}

	public int length() {
		return this.length;
	}

	private void grow() {
		int newsize = this.buffer.length * 2;
		if (newsize < 256) {
			newsize = 256;
		}
		Log.d(Constant.TAG, "Increase tag writer buffer: from " + this.buffer.length + " to " + newsize);
		checkBufferSize(newsize);

		final char[] tmp = new char[newsize];
		System.arraycopy(this.buffer, this.offset, tmp, 0, this.length);
		this.buffer = tmp;
		this.offset = 0;
	}

	@Override
	public void write(final int b) {
		write((char) b);
	}

	public void write(final char b) {
		if (this.offset + this.length + 1 > this.buffer.length) {
			grow();
		}
		this.buffer[this.offset + this.length++] = b;
	}

	@Override
	public void write(final char[] bb) {
		write(bb, 0, bb.length);
	}

	@Override
	public void write(final char[] bb, final int of, final int le) {
		while (this.offset + this.length + le > this.buffer.length) {
			grow();
		}
		System.arraycopy(bb, of, this.buffer, this.offset + this.length, le);
		this.length += le;
	}

	// do not use/implement the following method, a
	// "overridden method is a bridge method"
	// will occur
	//    public serverCharBuffer append(char b) {
	//        write(b);
	//        return this;
	//    }

	public TagWriter append(final int i) {
		write((char) (i));
		return this;
	}

	public TagWriter append(final char[] bb) {
		write(bb);
		return this;
	}

	public TagWriter append(final char[] bb, final int of, final int le) {
		write(bb, of, le);
		return this;
	}

	public TagWriter append(final String s) {
		return append(s, 0, s.length());
	}

	public TagWriter append(final String s, final int off, final int len) {
		final char[] temp = new char[len];
		s.getChars(off, (off + len), temp, 0);
		return append(temp);
	}

	public TagWriter append(final TagWriter bb) {
		return append(bb.buffer, bb.offset, bb.length);
	}

	//    public serverCharBuffer append(Object o) {
	//        if (o instanceof String) return append((String) o);
	//        if (o instanceof char[]) return append((char[]) o);
	//        return null;
	//    }

	public char charAt(final int pos) {
		if (pos < 0) {
			throw new IndexOutOfBoundsException();
		}
		if (pos > this.length) {
			throw new IndexOutOfBoundsException();
		}
		return this.buffer[this.offset + pos];
	}

	public void deleteCharAt(final int pos) {
		if (pos < 0) {
			return;
		}
		if (pos >= this.length) {
			return;
		}
		if (pos == this.length - 1) {
			this.length--;
		} else {
			System.arraycopy(this.buffer, this.offset + pos + 1, this.buffer, this.offset + pos, this.length - pos - 1);
		}
	}

	public int indexOf(final char b) {
		return indexOf(b, 0);
	}

	public int indexOf(final char[] bs) {
		return indexOf(bs, 0);
	}

	public int indexOf(final char b, final int start) {
		if (start >= this.length) {
			return -1;
		}
		for (int i = start; i < this.length; i++) {
			if (this.buffer[this.offset + i] == b) {
				return i;
			}
		}
		return -1;
	}

	public int indexOf(final char[] bs, final int start) {
		if (start + bs.length > this.length) {
			return -1;
		}
		loop: for (int i = start; i <= this.length - bs.length; i++) {
			// first test only first char
			if (this.buffer[this.offset + i] != bs[0]) {
				continue loop;
			}

			// then test all remaining char
			for (int j = 1; j < bs.length; j++) {
				if (this.buffer[this.offset + i + j] != bs[j]) {
					continue loop;
				}
			}

			// found hit
			return i;
		}
		return -1;
	}

	public int lastIndexOf(final char b) {
		for (int i = this.length - 1; i >= 0; i--) {
			if (this.buffer[this.offset + i] == b) {
				return i;
			}
		}
		return -1;
	}

	public boolean startsWith(final char[] bs) {
		if (this.length < bs.length) {
			return false;
		}
		for (int i = 0; i < bs.length; i++) {
			if (this.buffer[this.offset + i] != bs[i]) {
				return false;
			}
		}
		return true;
	}

	public char[] getChars() {
		return getChars(0);
	}

	public char[] getChars(final int start) {
		return getChars(start, this.length);
	}

	public char[] getChars(final int start, final int end) {
		// start is inclusive, end is exclusive
		if (end > this.length) {
			throw new IndexOutOfBoundsException("getBytes: end > length");
		}
		if (start > this.length) {
			throw new IndexOutOfBoundsException("getBytes: start > length");
		}
		final char[] tmp = new char[end - start];
		System.arraycopy(this.buffer, this.offset + start, tmp, 0, end - start);
		return tmp;
	}

	public TagWriter trim(final int start) {
		// the end value is outside (+1) of the wanted target array
		if (start > this.length) {
			throw new IndexOutOfBoundsException("trim: start > length");
		}
		this.offset = this.offset + start;
		this.length = this.length - start;
		return this;
	}

	public TagWriter trim(final int start, final int end) {
		// the end value is outside (+1) of the wanted target array
		if (start > this.length) {
			throw new IndexOutOfBoundsException("trim: start > length");
		}
		if (end > this.length) {
			throw new IndexOutOfBoundsException("trim: end > length");
		}
		if (start > end) {
			throw new IndexOutOfBoundsException("trim: start > end");
		}
		this.offset = this.offset + start;
		this.length = end - start;
		return this;
	}

	public TagWriter trim() {
		int l = 0;
		while ((l < this.length) && (this.buffer[this.offset + l] <= ' ')) {
			l++;
		}
		int r = this.length;
		while ((r > 0) && (this.buffer[this.offset + r - 1] <= ' ')) {
			r--;
		}
		if (l > r) {
			r = l;
		}
		return trim(l, r);
	}

	public boolean isWhitespace(final boolean includeNonLetterBytes) {
		// returns true, if trim() would result in an empty serverByteBuffer
		if (includeNonLetterBytes) {
			char b;
			for (int i = 0; i < this.length; i++) {
				b = this.buffer[this.offset + i];
				if (((b >= '0') && (b <= '9')) || ((b >= 'A') && (b <= 'Z')) || ((b >= 'a') && (b <= 'z'))) {
					return false;
				}
			}
		} else {
			for (int i = 0; i < this.length; i++) {
				if (this.buffer[this.offset + i] > 32) {
					return false;
				}
			}
		}
		return true;
	}

	public int whitespaceStart(final boolean includeNonLetterBytes) {
		// returns number of whitespace char at the beginning of text
		if (includeNonLetterBytes) {
			char b;
			for (int i = 0; i < this.length; i++) {
				b = this.buffer[this.offset + i];
				if (((b >= '0') && (b <= '9')) || ((b >= 'A') && (b <= 'Z')) || ((b >= 'a') && (b <= 'z'))) {
					return i;
				}
			}
		} else {
			for (int i = 0; i < this.length; i++) {
				if (this.buffer[this.offset + i] > 32) {
					return i;
				}
			}
		}
		return this.length;
	}

	public int whitespaceEnd(final boolean includeNonLetterBytes) {
		// returns position of whitespace at the end of text
		if (includeNonLetterBytes) {
			char b;
			for (int i = this.length - 1; i >= 0; i--) {
				b = this.buffer[this.offset + i];
				if (((b >= '0') && (b <= '9')) || ((b >= 'A') && (b <= 'Z')) || ((b >= 'a') && (b <= 'z'))) {
					return i + 1;
				}
			}
		} else {
			for (int i = this.length - 1; i >= 0; i--) {
				if (this.buffer[this.offset + i] > 32) {
					return i + 1;
				}
			}
		}
		return 0;
	}

	@Override
	public String toString() {
		return new String(this.buffer, this.offset, this.length);
	}

	public String toString(final int left, final int rightbound) {
		return new String(this.buffer, this.offset + left, rightbound - left);
	}

	public Properties propParser() {
		// extract a=b or a="b" - relations from the buffer
		int pos = this.offset;
		int start;
		String key;
		final Properties p = new Properties();
		// eat up spaces at beginning
		while ((pos < this.length) && (this.buffer[pos] <= 32)) {
			pos++;
		}
		while (pos < this.length) {
			// pos is at start of next key
			start = pos;
			while ((pos < this.length) && (this.buffer[pos] != equal)) {
				pos++;
			}
			if (pos >= this.length) {
				break; // this is the case if we found no equal
			}
			key = new String(this.buffer, start, pos - start).trim().toLowerCase();
			// we have a key
			pos++;
			// find start of value
			while ((pos < this.length) && (this.buffer[pos] <= 32)) {
				pos++;
			}
			// doublequotes are obligatory. However, we want to be fuzzy if they
			// are ommittet
			if (pos >= this.length) {
				// error case: input ended too early
				break;
			} else if (this.buffer[pos] == doublequote) {
				// search next doublequote
				pos++;
				start = pos;
				while ((pos < this.length) && (this.buffer[pos] != doublequote)) {
					pos++;
				}
				if (pos >= this.length) {
					break; // this is the case if we found no parent doublequote
				}
				p.setProperty(key, new String(this.buffer, start, pos - start).trim());
				pos++;
			} else if (this.buffer[pos] == singlequote) {
				// search next singlequote
				pos++;
				start = pos;
				while ((pos < this.length) && (this.buffer[pos] != singlequote)) {
					pos++;
				}
				if (pos >= this.length) {
					break; // this is the case if we found no parent singlequote
				}
				p.setProperty(key, new String(this.buffer, start, pos - start).trim());
				pos++;
			} else {
				// search next whitespace
				start = pos;
				while ((pos < this.length) && (this.buffer[pos] > 32)) {
					pos++;
				}
				p.setProperty(key, new String(this.buffer, start, pos - start).trim());
			}
			// pos should point now to a whitespace: eat up spaces
			while ((pos < this.length) && (this.buffer[pos] <= 32)) {
				pos++;
				// go on with next loop
			}
		}
		return p;
	}

	public static boolean equals(final char[] buffer, final char[] pattern) {
		return equals(buffer, 0, pattern);
	}

	public static boolean equals(final char[] buffer, final int offset, final char[] pattern) {
		// compares two char arrays: true, if pattern appears completely at offset position
		if (buffer.length < offset + pattern.length) {
			return false;
		}
		for (int i = 0; i < pattern.length; i++) {
			if (buffer[offset + i] != pattern[i]) {
				return false;
			}
		}
		return true;
	}

	public void reset() {
		this.length = 0;
		this.offset = 0;
	}

	//	public void reset(final int newSize) {
	//		resize(newSize);
	//		this.reset();
	//	}
	//
	//	public void resize(final int newSize) {
	//		if (newSize < 0) {
	//			throw new IllegalArgumentException("Illegal array size: " + newSize);
	//		}
	//		if (LOG.isDebugEnabled()) {
	//			LOG.debug("Resize tag writer before: from " + this.buffer.length + " to " + newSize);
	//		}
	//		final char[] v = new char[newSize];
	//		System.arraycopy(this.buffer, 0, v, 0, newSize > this.buffer.length ? this.buffer.length : newSize);
	//		this.buffer = v;
	//	}

	public char toCharArray()[] {
		final char[] newbuf = new char[this.length];
		System.arraycopy(this.buffer, 0, newbuf, 0, this.length);
		return newbuf;
	}

	@Override
	public void close() throws IOException {
		clear();
	}

	@Override
	public void flush() throws IOException {
		// TODO Auto-generated method stub        
	}

	private static void checkBufferSize(int newSize) {
		if (newSize > MAX_BUFFER_SIZE) {
			throw new IllegalStateException(String.format("New buffer size exceeds max buffer size [newSize=%s, maxSize=%s]", newSize, MAX_BUFFER_SIZE));
		}
	}

}