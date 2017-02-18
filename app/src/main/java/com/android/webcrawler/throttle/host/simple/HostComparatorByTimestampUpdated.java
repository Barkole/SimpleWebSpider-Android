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

import com.android.webcrawler.util.ValidityHelper;

import java.util.Comparator;
import java.util.Date;

class HostComparatorByTimestampUpdated implements Comparator<HostCounter> {

	/**
	 * @return <code>-1</code>, if o1 is before o2; <code>1</code>, if o2 is before o1; zero, if both have the some last updated timestamp
	 */
	@Override
	public int compare(final HostCounter hostCounter1, final HostCounter hostCounter2) {
		ValidityHelper.checkNotNull("hostCounter1", hostCounter1);
		ValidityHelper.checkNotNull("hostCounter2", hostCounter2);

		final Date timestampUpdated1 = hostCounter1.getTimestampUpdated();
		final Date timestampUpdated2 = hostCounter2.getTimestampUpdated();

		return timestampUpdated1.compareTo(timestampUpdated2);
	}

}
