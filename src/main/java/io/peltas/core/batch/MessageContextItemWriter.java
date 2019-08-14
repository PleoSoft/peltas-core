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

import java.util.List;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemWriter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

import io.peltas.core.batch.MessageContext.MessageContextHolder;

public class MessageContextItemWriter<T> implements ItemWriter<T>, ItemStream {

	private final ItemWriter<T> delegate;

	public MessageContextItemWriter(ItemWriter<T> delegate) {
		this.delegate = delegate;
	}

	@Override
	public void write(List<? extends T> items) throws Exception {
		for (T item : items) {
			List<Object> stackItems = MessageContextHolder.getStackedItems(item);
			Builder<T> builder = ImmutableList.<T>builder().add(item);
			if (!stackItems.isEmpty()) {
				builder.addAll((Iterable<? extends T>) stackItems);
			}
			delegate.write(builder.build());
			MessageContextHolder.removeMessageItem(item);
		}
	}

	@Override
	public void close() throws ItemStreamException {
		if (delegate instanceof ItemStream) {
			((ItemStream) this.delegate).close();
		}
	}

	@Override
	public void open(ExecutionContext executionContext) throws ItemStreamException {
		if (delegate instanceof ItemStream) {
			((ItemStream) this.delegate).open(executionContext);
		}
	}

	@Override
	public void update(ExecutionContext executionContext) throws ItemStreamException {
		if (delegate instanceof ItemStream) {
			((ItemStream) this.delegate).update(executionContext);
		}
	}
}
