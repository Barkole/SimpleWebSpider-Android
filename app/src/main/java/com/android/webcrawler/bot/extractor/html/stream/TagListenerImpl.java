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

import com.android.webcrawler.util.ValidityHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

class TagListenerImpl implements TagListener {
	private static final Map<String, Set<String>>	tagsWithoutContent	= new HashMap<String, Set<String>>(14);
	static {
		final Set<String> attrHref = new HashSet<String>(1);
		attrHref.add("href");

		final Set<String> attrSrc = new HashSet<String>(1);
		attrSrc.add("src");

		final Set<String> attrDomain = new HashSet<String>(1);
		attrDomain.add("domain");

		final Set<String> attrRdfAbout = new HashSet<String>(1);
		attrRdfAbout.add("rdf:about");

		final Set<String> attrRdfResource = new HashSet<String>(1);
		attrRdfResource.add("rdf:resource");

		final Set<String> attrUrl = new HashSet<String>(1);
		attrUrl.add("url");

		final Set<String> tagMember = new HashSet<String>(2);
		tagMember.add("href");
		tagMember.add("hrefreadonly");

		final Set<String> tagOutline = new HashSet<String>(3);
		tagOutline.add("htmlUrl");
		tagOutline.add("url");
		tagOutline.add("xmlUrl");

		// html
		tagsWithoutContent.put("a", attrHref);
		tagsWithoutContent.put("frame", attrSrc);
		tagsWithoutContent.put("iframe", attrSrc);
		tagsWithoutContent.put("ilayer", attrSrc);

		// rss & rdf
		tagsWithoutContent.put("atom:link", attrHref);
		tagsWithoutContent.put("category", attrDomain);
		tagsWithoutContent.put("item", attrRdfAbout);
		tagsWithoutContent.put("rdf:li", attrRdfResource);
		tagsWithoutContent.put("textinput", attrRdfResource);
		tagsWithoutContent.put("source", attrUrl);

		// atom
		tagsWithoutContent.put("link", attrHref);
		tagsWithoutContent.put("collection", attrHref);
		tagsWithoutContent.put("member", tagMember);

		// opml
		tagsWithoutContent.put("outline", tagOutline);
	}

	private static final Set<String>				tagsWithContent		= new HashSet<String>(8);
	static {
		// rss & rdf
		tagsWithContent.add("comments");
		tagsWithContent.add("docs");
		tagsWithContent.add("link");
		tagsWithContent.add("url");
		tagsWithContent.add("wfw:commentRss");

		// atom
		tagsWithContent.add("id");

		// opml
		tagsWithContent.add("docs");
		tagsWithContent.add("ownerId");
	}

	private final Set<String>						links				= new HashSet<String>();

	@Override
	public boolean isTagWithoutContent(final String tag) {
		return tagsWithoutContent.containsKey(tag);
	}

	@Override
	public boolean isTagWithContent(final String tag) {
		return tagsWithContent.contains(tag);
	}

	@Override
	public void scrapeTagWithoutContent(final String tagname, final Properties tagopts) {
		if (!ValidityHelper.isEmpty(tagopts)) {
			final Set<String> attributes = tagsWithoutContent.get(tagname);
			if (!ValidityHelper.isEmpty(attributes)) {
				for (final String attribute : attributes) {
					final String link = (String) tagopts.get(attribute);
					if (!ValidityHelper.isEmpty(link)) {
						final String trimmedLink = link.trim();
						if (!ValidityHelper.isEmpty(trimmedLink)) {
							this.links.add(trimmedLink);
						}
					}
				}
			}
		}
	}

	@Override
	public void scrapeTagWithContent(final String tagname, final Properties tagopts, final char[] text) {
		if (!ValidityHelper.isEmpty(text)) {
			final String link = String.valueOf(text);
			if (!ValidityHelper.isEmpty(link)) {
				final String trimmedLink = link.trim();
				if (!ValidityHelper.isEmpty(trimmedLink)) {
					this.links.add(trimmedLink);
				}
			}
		}
	}

	public List<String> getLinks() {
		final List<String> linkList = new ArrayList<String>(this.links.size());
		for (final String link : this.links) {
			linkList.add(link);
		}

		return linkList;
	}

}
