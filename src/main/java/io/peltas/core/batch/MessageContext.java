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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.messaging.Message;
import org.springframework.util.StringUtils;

public class MessageContext<T> {

	private T item;
	private Message<T> message;
	private final ChunkContext chunkContext;

	MessageContext(ChunkContext chunkContext) {
		this.chunkContext = chunkContext;
	}

	public Message<T> getMessage() {
		return message;
	}

	public void setMessage(Message<T> message) {
		this.message = message;
	}

	public ChunkContext getChunkContext() {
		return chunkContext;
	}

	public T getItem() {
		return item;
	}

	public void setItem(T item) {
		this.item = item;
	}

	@SuppressWarnings("unchecked")
	public List<T> getStackedItems() {
		return (List<T>) message.getHeaders().get("peltas.stack");
	}

	public void setStackedItems(List<T> stackedItems) {
		getStackedItems().addAll(stackedItems);
	}

	public static class MessageContextHolder {
		static private Map<Object, MessageContext<?>> messageContextMap = new HashMap<>(1);

		@SuppressWarnings("unchecked")
		static public <T> MessageContext<T> getMessageContext(T item) {
			MessageContext<T> messageContext = (MessageContext<T>) messageContextMap.get(item);
			return messageContext;
		}

		static void addMessageContext(MessageContext<?> messageContext) {
			Object item = messageContext.getItem();
			messageContextMap.put(item, messageContext);
		}

		static MessageContext<?> removeMessageContext(MessageContext<?> messageContext) {
			MessageContext<?> removed = removeMessageItem(messageContext.getItem());
			return removed;
		}

		static MessageContext<?> removeMessageItem(Object item) {
			MessageContext<?> removed = (MessageContext<?>) messageContextMap.remove(item);
			return removed;
		}

		static void clear() {
			messageContextMap.clear();
		}

		static public List<Object> getStackedItems(Object item) {
			List<Object> stackedItems = getStackList(item);
			if (stackedItems == null) {
				stackedItems = Collections.emptyList();
			}
			return Collections.unmodifiableList(stackedItems);
		}

		static public <I, T> void addAllStackedItems(I entry, List<T> items) {
			for (T item : items) {
				addStackItem(entry, item);
			}
		}

		@SuppressWarnings("unchecked")
		static public <I, T> void addStackItem(I entry, T item) {
			Object entryId = entry;
			if (entry instanceof Message) {
				String tempId = ((Message<?>) entry).getHeaders().get("peltas.stack.id", String.class);
				if (StringUtils.hasText(tempId)) {
					entryId = tempId;
				}
			}

			List<T> list = (List<T>) getStackList(entryId);
			if (list == null) {
				MessageContext<T> messageContext = getMessageContext(item);
				list = new ArrayList<>();
				messageContext.setStackedItems(list);
			}
			list.add(item);
		}

		private static List<Object> getStackList(Object item) {
			Object entryId = item;
			if (item instanceof Message) {
				String tempId = ((Message<?>) item).getHeaders().get("peltas.stack.id", String.class);
				if (StringUtils.hasText(tempId)) {
					entryId = tempId;
				}
			}

			MessageContext<Object> messageContext = getMessageContext(entryId);
			if (messageContext == null) {
				return null;
			}
			List<Object> stackedItems = messageContext.getStackedItems();
			return stackedItems;
		}

	}
}
