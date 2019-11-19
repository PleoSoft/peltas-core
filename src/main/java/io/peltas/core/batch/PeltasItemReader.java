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
import java.util.List;

import org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader;

public class PeltasItemReader<T> extends AbstractItemCountingItemStreamItemReader<T> {

	private final List<T> list = new ArrayList<>();
	private final String applicationName;

	protected int lastCount = 0;

	public PeltasItemReader(String applicationName) {
		this(applicationName, null);
	}

	public PeltasItemReader(String applicationName, List<T> list) {
		this.applicationName = applicationName;
		setName(getCurrentApplicationName());
		setList(list);
	}

	@Override
	protected void doOpen() throws Exception {
		onOpen();
	}

	@Override
	protected T doRead() throws Exception {
		if (!list.isEmpty()) {
			T entry = list.remove(0);
			onRead(entry);
			return entry;
		}

		return null;
	}

	protected void setList(List<T> list) {
		this.list.clear();
		if (list != null) {
			this.list.addAll(list);
		}
		lastCount = this.list.size();
		setCurrentItemCount(0);
		setMaxItemCount(lastCount > 0 ? lastCount : 1);
	}

	protected void onRead(T entry) {
	}

	protected void onOpen() {
	}

	@Override
	protected void doClose() throws Exception {
		onClose();
	}
	
	protected void onClose() {
	}

	public String getCurrentApplicationName() {
		return applicationName;
	}
}
