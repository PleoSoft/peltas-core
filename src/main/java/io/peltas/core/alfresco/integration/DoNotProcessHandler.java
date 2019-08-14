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
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;

import io.peltas.core.alfresco.PeltasEntry;

public class DoNotProcessHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(DoNotProcessHandler.class);

	@ServiceActivator(inputChannel = "donotprocess")
	public Object handle(Message<PeltasEntry> message) {
		LOGGER.debug("handle() on donotprocess channel - Entry ID: {}", message.getPayload().getId());
		return new NotProcessableEntry();
	}

}
