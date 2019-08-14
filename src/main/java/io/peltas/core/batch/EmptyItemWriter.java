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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemWriter;

public class EmptyItemWriter<T> implements ItemWriter<T> {

	private static final Logger LOGGER = LoggerFactory.getLogger(EmptyItemWriter.class);

	@Override
	public void write(List<? extends T> items) throws Exception {

		int size = items == null ? 0 : items.size();
		if (size > 0) {
			LOGGER.info("You are using the default EmptyItemWriter, therefore nothing will be written! Items size: "
					+ size);
		}
	}
}
