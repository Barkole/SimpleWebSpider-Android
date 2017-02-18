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

import java.util.Collection;
import java.util.Dictionary;
import java.util.Map;
import java.util.Properties;

public final class ValidityHelper {

	public static void checkNotEmpty(final String name, final CharSequence value) {
		checkNotEmptyInternal("name", name);
		checkNotEmptyInternal(name, value);
	}

	public static void checkNotEmpty(final String name, final Collection<?> values) {
		checkNotEmptyInternal("name", name);
		checkNotNull(name, values);
		if (isEmpty(values)) {
			throw new IllegalArgumentException(name + " is empty");
		}
	}

	private static void checkNotEmptyInternal(final String name, final CharSequence value) {
		checkNotNullInternal(name, value);
		if (isEmpty(value)) {
			throw new IllegalArgumentException(name + " is empty");
		}
	}

	public static void checkNotNull(final String name, final Object value) {
		checkNotEmptyInternal("name", name);
		if (value == null) {
			throw new NullPointerException(name + " is null");
		}
	}

	private static void checkNotNullInternal(final String name, final Object value) {
		if (value == null) {
			throw new NullPointerException(name + " is null");
		}
	}

	public static boolean isEmpty(final CharSequence value) {
		return value == null || value.length() == 0;
	}

	public static boolean isEmpty(final Collection<?> values) {
		return values == null || values.size() == 0;
	}

	private ValidityHelper() {
		// Only static helpers
	}

	public static <T, S> boolean isEmpty(final Map<T, S> values) {
		return values == null || values.size() == 0;
	}

	public static <T, S> void checkNotEmpty(final String name, final Map<T, S> values) {
		checkNotEmptyInternal("name", name);
		checkNotNull(name, values);
		if (isEmpty(values)) {
			throw new IllegalArgumentException(name + " is empty");
		}
	}

	public static <T, S> boolean isEmpty(final Dictionary<T, S> values) {
		return values == null || values.size() == 0;
	}

	public static <T, S> void checkNotEmpty(final String name, final Dictionary<T, S> values) {
		checkNotEmptyInternal("name", name);
		checkNotNull(name, values);
		if (isEmpty(values)) {
			throw new IllegalArgumentException(name + " is empty");
		}
	}

	public static boolean isEmpty(final Properties values) {
		return values == null || values.size() == 0;
	}

	public static void checkNotEmpty(final String name, final Properties values) {
		checkNotEmptyInternal("name", name);
		checkNotNull(name, values);
		if (isEmpty(values)) {
			throw new IllegalArgumentException(name + " is empty");
		}
	}

	public static boolean isEmpty(final char[] value) {
		return value == null || value.length == 0;
	}

	public static void checkNotEmpty(final String name, final char[] values) {
		checkNotEmptyInternal("name", name);
		checkNotNull(name, values);
		if (isEmpty(values)) {
			throw new IllegalArgumentException(name + " is empty");
		}
	}

	public static boolean isEmpty(final short[] value) {
		return value == null || value.length == 0;
	}

	public static void checkNotEmpty(final String name, final short[] values) {
		checkNotEmptyInternal("name", name);
		checkNotNull(name, values);
		if (isEmpty(values)) {
			throw new IllegalArgumentException(name + " is empty");
		}
	}

	public static boolean isEmpty(final int[] value) {
		return value == null || value.length == 0;
	}

	public static void checkNotEmpty(final String name, final int[] values) {
		checkNotEmptyInternal("name", name);
		checkNotNull(name, values);
		if (isEmpty(values)) {
			throw new IllegalArgumentException(name + " is empty");
		}
	}

	public static boolean isEmpty(final long[] value) {
		return value == null || value.length == 0;
	}

	public static void checkNotEmpty(final String name, final long[] values) {
		checkNotEmptyInternal("name", name);
		checkNotNull(name, values);
		if (isEmpty(values)) {
			throw new IllegalArgumentException(name + " is empty");
		}
	}

	public static boolean isEmpty(final boolean[] value) {
		return value == null || value.length == 0;
	}

	public static void checkNotEmpty(final String name, final boolean[] values) {
		checkNotEmptyInternal("name", name);
		checkNotNull(name, values);
		if (isEmpty(values)) {
			throw new IllegalArgumentException(name + " is empty");
		}
	}

	public static boolean isEmpty(final float[] value) {
		return value == null || value.length == 0;
	}

	public static void checkNotEmpty(final String name, final float[] values) {
		checkNotEmptyInternal("name", name);
		checkNotNull(name, values);
		if (isEmpty(values)) {
			throw new IllegalArgumentException(name + " is empty");
		}
	}

	public static boolean isEmpty(final double[] value) {
		return value == null || value.length == 0;
	}

	public static void checkNotEmpty(final String name, final double[] values) {
		checkNotEmptyInternal("name", name);
		checkNotNull(name, values);
		if (isEmpty(values)) {
			throw new IllegalArgumentException(name + " is empty");
		}
	}

	public static boolean isEmpty(final Object[] value) {
		return value == null || value.length == 0;
	}

	public static void checkNotEmpty(final String name, final Object[] values) {
		checkNotEmptyInternal("name", name);
		checkNotNull(name, values);
		if (isEmpty(values)) {
			throw new IllegalArgumentException(name + " is empty");
		}
	}

}
