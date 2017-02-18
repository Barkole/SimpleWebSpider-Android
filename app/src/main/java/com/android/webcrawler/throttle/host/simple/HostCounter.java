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

import java.util.Date;

class HostCounter {
	private final String	host;
	private long			usageCounter;
	private Date			timestampUpdated;

	HostCounter(final String host) {
		this.host = host;
		this.usageCounter = 0;
		this.timestampUpdated = new Date();
	}

	HostCounter(final String domain, final Date timestampUpdated) {
		this.host = domain;
		this.usageCounter = 0;
		this.timestampUpdated = timestampUpdated;
	}

	String getHost() {
		return this.host;
	}

	long getUsageCounter() {
		return this.usageCounter;
	}

	Date getTimestampUpdated() {
		return new Date(this.timestampUpdated.getTime());
	}

	long getTimestampUpdatedLong() {
		return this.timestampUpdated.getTime();
	}

	long increaseUsageCounter() {
		update();
		return ++this.usageCounter;
	}

	private void update() {
		this.timestampUpdated = new Date();
	}

	@Override
	public String toString() {
		return "HostCounter [host=" + this.host + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.host == null) ? 0 : this.host.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final HostCounter other = (HostCounter) obj;
		if (this.host == null) {
			if (other.host != null) {
				return false;
			}
		} else if (!this.host.equals(other.host)) {
			return false;
		}
		return true;
	}

}
