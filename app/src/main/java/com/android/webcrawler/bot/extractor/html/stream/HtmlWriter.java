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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
package com.android.webcrawler.bot.extractor.html.stream;

import android.util.Log;

import com.android.webcrawler.Constant;

import java.io.IOException;
import java.io.Writer;
import java.util.Properties;

final class HtmlWriter extends Writer {

	public static final char	lb			= '<';
	public static final char	rb			= '>';
	public static final char	dash		= '-';
	public static final char	excl		= '!';
	public static final char	singlequote	= '\'';
	public static final char	doublequote	= '"';

	private TagWriter			filterCont;
	private Properties			filterOpts;
	private final TagListener	scraper;
	private TagWriter			buffer;
	private String				filterTag;
	private boolean				inSingleQuote;
	private boolean				inDoubleQuote;
	private boolean				inComment;
	private boolean				inScript;
	private boolean				inStyle;
	private boolean				binaryUnsuspect;
	private final boolean		passbyIfBinarySuspect;

	public HtmlWriter(final boolean passbyIfBinarySuspect, final TagListener scraper, final int bufferSize) {
		this.scraper = scraper;
		this.buffer = new TagWriter(bufferSize);
		this.inSingleQuote = false;
		this.inDoubleQuote = false;
		this.inComment = false;
		this.inScript = false;
		this.inStyle = false;
		this.binaryUnsuspect = true;
		this.passbyIfBinarySuspect = passbyIfBinarySuspect;
		this.filterOpts = null;
		this.filterCont = null;
	}

	private static boolean binaryHint(final char c) {
		// space, punctiation and symbols, letters and digits (ASCII/latin)
		//if (c >= 31 && c < 128) return false;
		if (c > 31) {
			return false;
		}
		//  8 = backspace
		//  9 = horizontal tab
		// 10 = new line (line feed)
		// 11 = vertical tab
		// 12 = new page (form feed)
		// 13 = carriage return
		if (c > 7 && c <= 13) {
			return false;
		}

		return true;
	}

	public boolean binarySuspect() {
		return !this.binaryUnsuspect;
	}

	@Override
	public void write(final int c) throws IOException {
		if ((this.binaryUnsuspect) && (binaryHint((char) c))) {
			this.binaryUnsuspect = false;
			if (this.passbyIfBinarySuspect) {
				close();
			}
		}

		if (this.binaryUnsuspect || !this.passbyIfBinarySuspect) {
			if (this.inSingleQuote) {
				this.buffer.append(c);
				if (c == singlequote) {
					this.inSingleQuote = false;
				}
				// check error cases
				if ((c == rb) && (this.buffer.charAt(0) == lb)) {
					this.inSingleQuote = false;
					// the tag ends here. after filtering: pass on
					filterSentence(this.buffer.getChars(), singlequote);
					// buffer = new serverByteBuffer();
					this.buffer.reset();
				}
			} else if (this.inDoubleQuote) {
				this.buffer.append(c);
				if (c == doublequote) {
					this.inDoubleQuote = false;
				}
				// check error cases
				if (c == rb && this.buffer.charAt(0) == lb) {
					this.inDoubleQuote = false;
					// the tag ends here. after filtering: pass on
					filterSentence(this.buffer.getChars(), doublequote);
					// buffer = new serverByteBuffer();
					this.buffer.reset();
				}
			} else if (this.inComment) {
				this.buffer.append(c);
				if (c == rb && this.buffer.length() > 6 && this.buffer.charAt(this.buffer.length() - 3) == dash) {
					// comment is at end
					this.inComment = false;
					// buffer = new serverByteBuffer();
					this.buffer.reset();
				}
			} else if (this.inScript) {
				this.buffer.append(c);
				final int bufferLength = this.buffer.length();
				if ((c == rb) && (bufferLength > 14) && (this.buffer.charAt(bufferLength - 9) == lb) && (this.buffer.charAt(bufferLength - 8) == '/')
						&& (this.buffer.charAt(bufferLength - 7) == 's') && (this.buffer.charAt(bufferLength - 6) == 'c')
						&& (this.buffer.charAt(bufferLength - 5) == 'r') && (this.buffer.charAt(bufferLength - 4) == 'i')
						&& (this.buffer.charAt(bufferLength - 3) == 'p') && (this.buffer.charAt(bufferLength - 2) == 't')) {
					// script is at end
					this.inScript = false;
					// buffer = new serverByteBuffer();
					this.buffer.reset();
				}
			} else if (this.inStyle) {
				this.buffer.append(c);
				final int bufferLength = this.buffer.length();
				if ((c == rb) && (bufferLength > 13) && (this.buffer.charAt(bufferLength - 8) == lb) && (this.buffer.charAt(bufferLength - 7) == '/')
						&& (this.buffer.charAt(bufferLength - 6) == 's') && (this.buffer.charAt(bufferLength - 5) == 't')
						&& (this.buffer.charAt(bufferLength - 4) == 'y') && (this.buffer.charAt(bufferLength - 3) == 'l')
						&& (this.buffer.charAt(bufferLength - 2) == 'e')) {
					// style is at end
					this.inStyle = false;
					// buffer = new serverByteBuffer();
					this.buffer.reset();
				}
			} else {
				if (this.buffer.length() == 0) {
					if (c == rb) {
						// very strange error case; we just let it pass
					} else {
						this.buffer.append(c);
					}
				} else if (this.buffer.charAt(0) == lb) {
					if (c == singlequote) {
						this.inSingleQuote = true;
					}
					if (c == doublequote) {
						this.inDoubleQuote = true;
					}
					// fill in tag text
					if ((this.buffer.length() >= 3) && (this.buffer.charAt(1) == excl) && (this.buffer.charAt(2) == dash) && (c == dash)) {
						// this is the start of a comment
						this.inComment = true;
						this.buffer.append(c);
					} else if ((this.buffer.length() >= 6) && (this.buffer.charAt(1) == 's') && (this.buffer.charAt(2) == 'c')
							&& (this.buffer.charAt(3) == 'r') && (this.buffer.charAt(4) == 'i') && (this.buffer.charAt(5) == 'p') && (c == 't')) {
						// this is the start of a javascript
						this.inScript = true;
						this.buffer.append(c);
					} else if ((this.buffer.length() >= 5) && (this.buffer.charAt(1) == 's') && (this.buffer.charAt(2) == 't')
							&& (this.buffer.charAt(3) == 'y') && (this.buffer.charAt(4) == 'l') && (c == 'e')) {
						// this is the start of a css-style
						this.inStyle = true;
						this.buffer.append(c);
					} else if (c == rb) {
						this.buffer.append(c);
						// the tag ends here. after filtering: pass on
						filterSentence(this.buffer.getChars(), doublequote);
						// buffer = new serverByteBuffer();
						this.buffer.reset();
					} else if (c == lb) {
						// this is an error case
						// we consider that there is one rb missing
						if (this.buffer.length() > 0) {
							filterSentence(this.buffer.getChars(), doublequote);
						}
						// buffer = new serverByteBuffer();
						this.buffer.reset();
						this.buffer.append(c);
					} else {
						this.buffer.append(c);
					}
				} else {
					// fill in plain text
					if (c == lb) {
						// the text ends here
						if (this.buffer.length() > 0) {
							filterSentence(this.buffer.getChars(), doublequote);
						}
						// buffer = new serverByteBuffer();
						this.buffer.reset();
						this.buffer.append(c);
					} else {
						// simply append
						this.buffer.append(c);
					}
				}
			}
		}
	}

	private void filterSentence(final char[] in, final char quotechar) {
		if (in.length == 0) {
			return;
		}
		// scan the string and parse structure
		if (in.length > 2 && in[0] == lb) {

			// a tag
			String tag;
			int tagend;
			if (in[1] == '/') {
				// a closing tag
				tagend = tagEnd(in, 2);
				tag = new String(in, 2, tagend - 2);
				final char[] text = new char[in.length - tagend - 1];
				System.arraycopy(in, tagend, text, 0, in.length - tagend - 1);
				filterTag(tag, false, text, quotechar);
				return;
			}

			// an opening tag
			tagend = tagEnd(in, 1);
			tag = new String(in, 1, tagend - 1);

			final char[] text = new char[in.length - tagend - 1];
			System.arraycopy(in, tagend, text, 0, in.length - tagend - 1);
			filterTag(tag, true, text, quotechar);
			return;
		}

		// a text
		filterTag(null, true, in, quotechar);
		return;
	}

	private void filterTag(final String tag, final boolean opening, final char[] content, final char quotechar) {
		// TODO Check, if empty content
		//		if (content == null || content.length == 0) {
		//			// No content avaible, that could contain any tag
		//			return;
		//		}
		if (this.filterTag == null) {
			// we are not collection tag text
			if (tag == null) {
				return;
			}

			// we have a new tag
			if (opening) {
				if ((this.scraper != null) && (this.scraper.isTagWithoutContent(tag))) {
					// this single tag is collected at once here
					final TagWriter charBuffer = new TagWriter(content);
					this.scraper.scrapeTagWithoutContent(tag, charBuffer.propParser());
					try {
						charBuffer.close();
					} catch (final IOException e) {
						Log.wtf(Constant.TAG, "Failed to close tag writer", e);
					}
				}
				if (((this.scraper != null) && (this.scraper.isTagWithContent(tag)))) {
					final TagWriter scb = new TagWriter(content);

					final Properties properties = scb.propParser();
					try {
						scb.close();
					} catch (final IOException e) {
						Log.wtf(Constant.TAG, "Failed to close tag writer", e);
					}

					if (content[content.length - 1] == '/') {
						// A simple empty tag! This single tag is collected at once here
						this.scraper.scrapeTagWithContent(tag, properties, null);
					} else {
						// ok, start collecting
						this.filterTag = tag;
						this.filterOpts = properties;
						this.filterCont = new TagWriter();
					}

					return;
				} else {
					// we ignore that thing and return it again
					return;
				}
			}

			// we ignore that thing and return it again
			return;

		}

		// we are collection tag text for the tag 'filterTag'
		if (tag == null) {
			this.filterCont.append(content);
			return;
		}

		// it's a tag! which one?
		//		if ((opening) || (!(tag.equalsIgnoreCase(this.filterTag)))) {
		//			// this tag is not our concern. just add it
		//			this.filterCont.append(genTag0raw(tag, opening, content));
		//			return;
		//		}

		// it's our closing tag! return complete result.
		if (this.scraper != null) {
			this.scraper.scrapeTagWithContent(this.filterTag, this.filterOpts, this.filterCont.getChars());
		}
		this.filterTag = null;
		this.filterOpts = null;
		this.filterCont = null;
		return;
	}

	private static int tagEnd(final char[] tag, final int start) {
		char c;
		for (int i = start; i < tag.length; i++) {
			c = tag[i];
			if (c != '!' && c != '-' && (c < '0' || c > '9') && (c < 'a' || c > 'z') && (c < 'A' || c > 'Z')) {
				return i;
			}
		}
		return tag.length - 1;
	}

	private void filterFinalize(final char quotechar) {
		// it's our closing tag! return complete result.
		if (this.scraper != null && this.filterCont != null) {
			this.scraper.scrapeTagWithContent(this.filterTag, this.filterOpts, this.filterCont.getChars());
		}
		this.filterTag = null;
		this.filterOpts = null;
		this.filterCont = null;
	}

	@Override
	public void close() throws IOException {
		final char quotechar = (this.inSingleQuote) ? singlequote : doublequote;
		if (this.buffer != null) {
			if (this.buffer.length() > 0) {
				filterSentence(this.buffer.getChars(), quotechar);
			}
			this.buffer = null;
		}
		filterFinalize(quotechar);
		this.filterTag = null;
		this.filterOpts = null;
		this.filterCont = null;
	}

	@Override
	public void write(final char b[]) throws IOException {
		write(b, 0, b.length);
	}

	@Override
	public void write(final char b[], final int off, final int len) throws IOException {
		if ((off | len | (b.length - (len + off)) | (off + len)) < 0) {
			throw new IndexOutOfBoundsException();
		}
		for (int i = off; i < (len - off); i++) {
			this.write(b[i]);
		}
	}

	@Override
	public void flush() throws IOException {
		// Nothing to do
	}
}