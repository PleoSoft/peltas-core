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

import org.springframework.messaging.Message;
import org.springframework.messaging.core.GenericMessagingTemplate;
import org.springframework.messaging.support.MessageBuilder;

import io.peltas.core.batch.PeltasDataHolder;
import io.peltas.core.batch.PeltasExecutionItemWriter;

public class PeltasIntegrationWriter<I, C> extends PeltasExecutionItemWriter<I, C> {

	private final GenericMessagingTemplate template;
	private final boolean executionEvents;

	public PeltasIntegrationWriter(GenericMessagingTemplate template) {
		this(template, false);
	}

	public PeltasIntegrationWriter(GenericMessagingTemplate template, boolean executionEvents) {
		this.template = template;
		this.executionEvents = executionEvents;
	}

	@Override
	public void beforeExecution() {
	}

	@Override
	public void itemExecution(String executionKey, I parameters, PeltasDataHolder item) {
		MessageBuilder<PeltasIntegrationMessage<I>> messageBuilder = MessageBuilder
				.withPayload(new PeltasIntegrationMessage<I>(item, parameters, executionKey));

		Message<PeltasIntegrationMessage<I>> message = messageBuilder.build();
		template.send(executionEvents ? executionKey : "peltas_item_execution", message);
	}

	@Override
	public void collectionExecution(String executionKey, C params, PeltasDataHolder item) {
		MessageBuilder<PeltasIntegrationMessage<C>> messageBuilder = MessageBuilder
				.withPayload(new PeltasIntegrationMessage<C>(item, params, executionKey));

		Message<PeltasIntegrationMessage<C>> message = messageBuilder.build();
		template.send(executionEvents ? executionKey : "peltas_collection_execution", message);
	}

	public static class PeltasIntegrationMessage<I> {

		private final PeltasDataHolder item;
		private final String executionKey;
		private final I parameters;

		public PeltasIntegrationMessage(PeltasDataHolder item, I parameters, String executionKey) {
			this.item = item;
			this.parameters = parameters;
			this.executionKey = executionKey;
		}

		public PeltasDataHolder getItem() {
			return item;
		}

		public I getParameters() {
			return parameters;
		}

		public String getExecutionKey() {
			return executionKey;
		}
	}
}
