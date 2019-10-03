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

import java.util.ArrayList;
import java.util.UUID;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.messaging.Message;
import org.springframework.messaging.core.GenericMessagingTemplate;
import org.springframework.messaging.support.MessageBuilder;

import io.peltas.core.batch.MessageContext.MessageContextHolder;

public abstract class PeltasItemProcessor<I, O> extends PeltasListener<I, O> implements ItemProcessor<I, O> {
	private final GenericMessagingTemplate template;

	public PeltasItemProcessor(GenericMessagingTemplate template) {
		this.template = template;
	}

	@SuppressWarnings("unchecked")
	@Override
	final public O process(I item) throws Exception {
		if (shouldSkipItem(item)) {
			return null;
		}

		String auditStackId = UUID.randomUUID().toString();
		MessageBuilder<I> messageBuilder = MessageBuilder.withPayload(item)
				.setHeader("peltas.stack", new ArrayList<O>()).setHeader("peltas.stack.id", auditStackId);
		doWithMessage(messageBuilder);
		Message<I> message = messageBuilder.build();

		@SuppressWarnings("unused")
		Class<I> clazzI = (Class<I>) item.getClass();
		MessageContext<Object> messageContext = new MessageContext<>(this.currentChunkContext);
		messageContext.setItem(auditStackId);
		messageContext.setMessage((Message<Object>) message);
		MessageContextHolder.addMessageContext(messageContext);

		Message<O> ret = (Message<O>) template.sendAndReceive("peltas.entry", message);
		@SuppressWarnings("unused")
		Class<O> clazzO = (Class<O>) ret.getPayload().getClass();

		MessageContextHolder.removeMessageContext(messageContext);
		O payload = ret.getPayload();
		if (shouldSkipPayload(payload)) {
			onItemSkipped(item, payload);
			return null;
		}

		messageContext.setItem(ret.getPayload());
		MessageContextHolder.addMessageContext(messageContext);
		onItemProcessed(item, payload);
		return payload;
	}

	protected void onItemSkipped(I item, O payload) {
	}
	
	protected void onItemProcessed(I item, O payload) {
	}

	protected boolean shouldSkipItem(I item) {
		return false;
	}

	protected boolean shouldSkipPayload(O item) {
		return false;
	}

	protected void doWithMessage(MessageBuilder<I> messageBuilder) {
	}
}
