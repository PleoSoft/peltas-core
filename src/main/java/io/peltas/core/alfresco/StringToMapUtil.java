/**
 * Copyright 2019 Pleo Soft d.o.o. (pleosoft.com)

 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.peltas.core.alfresco;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.util.StringUtils;

public class StringToMapUtil {

	public static final char MAP_BEGIN = '{';
	public static final char MAP_END = '}';
	public static final char ARRAY_BEGIN = '[';
	public static final char ARRAY_END = ']';
	public static final char WHITESPACE = ' ';

	/**
	 * a map conversion of a map.toString() string result jsonString should starts
	 * with { and ends with } like: {key=val, key2=val2}
	 * 
	 * @param inputString string starting with { or [ and ending with } or ]
	 *                    respectively
	 * @param delimiter   the delimiter used to find a map entry (key delimiter)
	 * @return a map representation of the inputString
	 */
	static public Map<String, Object> stringToMap(final String inputString, final char delimiter) {
		if (!StringUtils.hasText(inputString)) {
			throw new IllegalArgumentException("the string provided cannot be empty");
		}

		if (!inputString.startsWith("{") && !inputString.endsWith("}")) {
			throw new IllegalArgumentException("the string provided is not a map representation");
		}

		final HashMap<String, Object> map = new HashMap<>();

		String formated = inputString.substring(1, inputString.length() - 1);

		int currentKeyStartIndex = 0;
		for (int i = 0; i < formated.length(); i++) {
			char currentChar = formated.charAt(i);

			if (' ' == currentChar) {
				currentKeyStartIndex = i;
			}

			if ('=' == currentChar) {
				// key is on the left
				String key = formated.substring(currentKeyStartIndex, i);
				Object value = null;

				for (++i; i < formated.length(); i++) {
					currentChar = formated.charAt(i);
					if (WHITESPACE == currentChar) {
						// TODO first non empty char (tab, space ...)
						continue;
					}

					if (MAP_BEGIN == currentChar) {
						// it is an object => map
						// find the end '}'
						int end = findMapValue(formated, i, MAP_BEGIN, MAP_END);
						value = formated.substring(i, ++end).trim();
						i = end;
//						if (ObjectUtils.isEmpty(value)) {
//							// not an object find the first delimiter
//							for (++i; i < formated.length(); i++) {
//								currentChar = formated.charAt(i);
//								if (delimiter == currentChar) {
//									value = formated.substring(k, i++).trim();
//									break;
//								}
//							}
//						}
					} else if (ARRAY_BEGIN == currentChar) {
						// it is an array = > list
						// find the end ']'

						int end = findMapValue(formated, i, ARRAY_BEGIN, ARRAY_END);
						value = formated.substring(i, ++end).trim();

						i = end;
						// value = stringToMap(v, delimiter);

//						if (ObjectUtils.isEmpty(value)) {
//							value = valueAsList(value, delimiter);
//						}
					} else {
						// it is the value
						// find the end ','

						int end = findMapValue(formated, i, ' ', delimiter);
						value = formated.substring(i, end++).trim();
						i = end;
					}

					if ("null".equals(value)) {
						value = null;
					}

					map.put(key.trim(), value);
					break;
				}

				// we go to the next key
				currentKeyStartIndex = i;
			}
		}
		return map;
	}

	/**
	 * 
	 * @param string
	 * @param i
	 * @param first  if ' ' than only one last is exepcted
	 * @param last
	 * @return the index of the string's map value end char
	 */
	private static int findMapValue(final String string, int i, final char first, final char last) {
		int found = 0;

		if (first == WHITESPACE) {
			found = 1;
		}

		for (; i < string.length(); i++) {
			char currentChar = string.charAt(i);

			if (first != WHITESPACE && first == currentChar) {
				found++;
			}

			if (last == currentChar) {
				found--;
			}

			if (found < 1) {
				break;
			}
		}

		return i;
	}

	// TODO: should be refactored during the changes of arrays/object(map) handling
	static public List<Object> valueAsList(final Object value) {
		return valueAsList(value, ',');
	}

	// TODO: should be refactored during the changes of arrays/object(map) handling
	static public List<Object> valueAsList(final Object value, final char delimiter) {
		if (value == null || "null".equals(value)) {
			return Collections.emptyList();
		}

		List<Object> values = new ArrayList<>();
		if (value instanceof String) {
			String tmp = ((String) value).trim();
			String[] array = StringUtils.delimitedListToStringArray((tmp.substring(1, tmp.length() - 1)),
					String.valueOf(delimiter));

			for (String v : array) {
				values.add(v.trim());
			}
		} else if (value instanceof Collection) {
			values.addAll(((Collection<?>) value));
		} else {
			throw new IllegalArgumentException("no support for: " + value.getClass());
		}
		return values;
	}
}
