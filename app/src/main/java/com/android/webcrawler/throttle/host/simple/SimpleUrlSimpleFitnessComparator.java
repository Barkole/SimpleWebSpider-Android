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

package com.android.webcrawler.throttle.host.simple;

import com.android.webcrawler.util.SimpleUrl;

import java.util.Comparator;
import java.util.Map;

class SimpleUrlSimpleFitnessComparator implements Comparator<SimpleUrl> {
	private static final int				SECOND_IS_FITTER	= -1;
	private static final int				FIRST_IS_FITTER		= 1;
	private static final int				EQUALS				= 0;

	private final Map<String, HostCounter>	domains;

	SimpleUrlSimpleFitnessComparator(final Map<String, HostCounter> domains) {
		this.domains = domains;
	}

	@Override
	public int compare(final SimpleUrl simpleUrl1, final SimpleUrl simpleUrl2) {
		final HostCounter domainCounter1 = getDomainCounter(simpleUrl1);
		final HostCounter domainCounter2 = getDomainCounter(simpleUrl2);

		/* Not available domains are always the fittest one */
		if (domainCounter1 == null && domainCounter2 == null) {
			return EQUALS;
		}
		if (domainCounter1 == null) {
			return FIRST_IS_FITTER;
		}
		if (domainCounter2 == null) {
			return SECOND_IS_FITTER;
		}

		/* Fitness calculation is required */
		if (domainCounter1.getUsageCounter() < domainCounter2.getUsageCounter()) {
			return FIRST_IS_FITTER;
		}
		if (domainCounter2.getUsageCounter() < domainCounter1.getUsageCounter()) {
			return SECOND_IS_FITTER;
		}

		if (domainCounter1.getTimestampUpdated().before(domainCounter2.getTimestampUpdated())) {
			return FIRST_IS_FITTER;
		}
		if (domainCounter2.getTimestampUpdated().before(domainCounter1.getTimestampUpdated())) {
			return SECOND_IS_FITTER;
		}

		return EQUALS;
	}

	private HostCounter getDomainCounter(final SimpleUrl simpleUrl) {
		final String host = simpleUrl.getHost();
		if (host == null) {
			new IllegalArgumentException("SimpleUrl " + simpleUrl + " does not contain any host");
		}
		return this.domains.get(host);
	}

}
