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

import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;

import io.peltas.core.PeltasEntry;

public class PeltasListenerAdapter {

	protected void onBeforeChunk(ChunkContext context) {
	}

	protected void onAfterChunk(ChunkContext context) {
	}

	protected void onChunkError(ChunkContext context) {
	}

	protected void onAfterProcess(PeltasEntry item, PeltasDataHolder result) {
	}

	protected void onBeforeProcess(PeltasEntry item) {
	}

	protected void afterProcessError(PeltasEntry item, Exception e) {
	}

	protected void onAfterWrite(List<PeltasDataHolder> items, ChunkContext currentChunkContext) {
	}

	protected void onBeforeWrite(List<PeltasDataHolder> items) {
	}

	protected void onBeforeStep(StepExecution stepExecution) {
	}

	protected void onAfterStep(StepExecution stepExecution) {
	}

	public String applicationNameSuffix(String applicationName) {
		return applicationName;
	}
}
