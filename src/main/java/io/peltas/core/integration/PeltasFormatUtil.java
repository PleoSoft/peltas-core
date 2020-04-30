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

package io.peltas.core.integration;

import java.util.List;

public class PeltasFormatUtil {

	private final ThreadLocal<String> CURRENT_FORMAT = new ThreadLocal<>();
	private final ThreadLocal<List<String>> CURRENT_FORMAT_KEYS = new ThreadLocal<>();

	public String getCurrentFormat() {
		return CURRENT_FORMAT.get();
	}

	public List<String> getCurrentFormatKeys() {
		return CURRENT_FORMAT_KEYS.get();
	}

	void setCurrentFormat(String format) {
		CURRENT_FORMAT.set(format);
	}

	void setCurrentFormatKeys(List<String> formatKeys) {
		CURRENT_FORMAT_KEYS.set(formatKeys);
	}
}
