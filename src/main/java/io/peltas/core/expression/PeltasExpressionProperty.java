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

package io.peltas.core.expression;

import java.util.Collections;
import java.util.List;

public class PeltasExpressionProperty {
	private String format = null;
	private List<String> formatKeys = Collections.singletonList(",");
	private List<String> data;
	private Class<?> type = String.class;

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public List<String> getData() {
		return data;
	}

	public void setData(List<String> data) {
		this.data = data;
	}

	public Class<?> getType() {
		return type;
	}

	public void setType(Class<?> type) {
		this.type = type;
	}

	public List<String> getFormatKeys() {
		return formatKeys;
	}

	public void setFormatKeys(List<String> formatKeys) {
		this.formatKeys = formatKeys;
	}

	@Override
	public String toString() {
		return "PeltasExpresionProperty [format=" + format + ", data=" + data + ", type=" + type + ", formatKeys="
				+ formatKeys + "]";
	}
}
