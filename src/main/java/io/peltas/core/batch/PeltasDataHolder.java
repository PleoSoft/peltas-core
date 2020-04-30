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

package io.peltas.core.batch;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.peltas.core.PeltasEntry;
import io.peltas.core.expression.PeltasExpressionProperty;
import io.peltas.core.expression.PeltasHandlerProperties;

public class PeltasDataHolder {

	private final PeltasEntry auditEntry;
	private final Map<String, PeltasExpressionProperty> properties;
	private final Map<String, Object> builder;
	private final PeltasHandlerProperties config;
	private Map<String, Object> additionalData;

	public PeltasDataHolder(PeltasEntry auditEntry, Map<String, PeltasExpressionProperty> properties,
			Map<String, Object> builder, PeltasHandlerProperties config) {
		this.auditEntry = auditEntry;
		this.properties = Collections.unmodifiableMap(properties);
		this.builder = Collections.unmodifiableMap(builder);
		this.config = config;
	}

	public PeltasEntry getAuditEntry() {
		return auditEntry;
	}

	public Map<String, PeltasExpressionProperty> getProperties() {
		return properties;
	}

	public Map<String, Object> getBuilder() {
		return builder;
	}

	public PeltasHandlerProperties getConfig() {
		return config;
	}

	public void addAdditionalData(String key, Object value) {
		if (additionalData == null) {
			additionalData = new HashMap<>();
		}
		additionalData.put(key, value);
	}

	public Map<String, Object> getAdditionalData() {
		return additionalData != null ? Collections.unmodifiableMap(additionalData) : Collections.emptyMap();
	}
}
