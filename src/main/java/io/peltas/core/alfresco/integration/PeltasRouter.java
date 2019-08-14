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

package io.peltas.core.alfresco.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.messaging.Message;

import io.peltas.core.alfresco.PeltasEntry;
import io.peltas.core.alfresco.PeltasException;
import io.peltas.core.alfresco.config.PeltasHandlerConfigurationProperties;
import io.peltas.core.alfresco.config.PeltasHandlerProperties;
import io.peltas.core.alfresco.config.PeltasProperties;
import io.peltas.core.batch.ItemRouter;

public class PeltasRouter implements ItemRouter<PeltasEntry> {

	private static final Logger LOGGER = LoggerFactory.getLogger(PeltasHandlerConfigurationProperties.class);

	private final PeltasHandlerConfigurationProperties pipeline;
	private final PeltasProperties properties;
	private final boolean expectionOnNoMatch;

	public PeltasRouter(PeltasHandlerConfigurationProperties pipeline, PeltasProperties properties,
			boolean expectionOnNoMatch) {
		this.pipeline = pipeline;
		this.properties = properties;
		this.expectionOnNoMatch = expectionOnNoMatch;
	}

	@Override
	public String handleMessage(Message<PeltasEntry> message) {
		PeltasEntry entry = message.getPayload();

		LOGGER.trace("handleMessage() {}", entry);
		String bestMatchHandler = pipeline.findFirstBestMatchHandler(entry);
		LOGGER.debug("handleMessage() best match handler {}", bestMatchHandler);

		if (bestMatchHandler == null) {
			if (expectionOnNoMatch) {
				LOGGER.warn("handleMessage() failed to find a configured handler for {}", entry);
				throw new PeltasException("no handler was found");
			}

			String noMatchHandler = properties.getNoMatchHandler();
			LOGGER.warn("handleMessage() redirecting to noMatchHandler {} - ENTRY ID: {}", noMatchHandler, entry.getId());
			return noMatchHandler;
		}

		PeltasHandlerProperties configuration = pipeline.getForHandler(bestMatchHandler);

		PeltasHandlerProperties config = (PeltasHandlerProperties) message.getHeaders()
				.get("alfresco.handler.configuration");
		BeanUtils.copyProperties(configuration, config);

		return "auditprocess";
	}
}
