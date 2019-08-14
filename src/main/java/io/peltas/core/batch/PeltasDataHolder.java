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
import java.util.Map;

import io.peltas.core.alfresco.PeltasEntry;
import io.peltas.core.alfresco.config.PeltasExpresionProperty;
import io.peltas.core.alfresco.config.PeltasHandlerProperties;

public class PeltasDataHolder {

	private final PeltasEntry auditEntry;
	private final Map<String, PeltasExpresionProperty> properties;
	private final Map<String, Object> builder;
	private final PeltasHandlerProperties config;

	public PeltasDataHolder(PeltasEntry auditEntry, Map<String, PeltasExpresionProperty> properties,
			Map<String, Object> builder, PeltasHandlerProperties config) {
		this.auditEntry = auditEntry;
		this.properties = Collections.unmodifiableMap(properties);
		this.builder = Collections.unmodifiableMap(builder);
		this.config = config;
	}

	public PeltasEntry getAuditEntry() {
		return auditEntry;
	}

	public Map<String, PeltasExpresionProperty> getProperties() {
		return properties;
	}

	public Map<String, Object> getBuilder() {
		return builder;
	}

	public PeltasHandlerProperties getConfig() {
		return config;
	}
}
