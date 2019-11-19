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

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.AfterChunk;
import org.springframework.batch.core.annotation.AfterChunkError;
import org.springframework.batch.core.annotation.AfterProcess;
import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.core.annotation.AfterWrite;
import org.springframework.batch.core.annotation.BeforeChunk;
import org.springframework.batch.core.annotation.BeforeProcess;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.annotation.BeforeWrite;
import org.springframework.batch.core.annotation.OnProcessError;
import org.springframework.batch.core.scope.context.ChunkContext;

public class PeltasListener<I, O> {
	protected ChunkContext currentChunkContext;

	@BeforeChunk
	final public void beforeChunk(ChunkContext context) {
		currentChunkContext = context;
		onBeforeChunk(context);
	}

	protected void onBeforeChunk(ChunkContext context) {
	}

	@AfterChunk
	final public void afterChunk(ChunkContext context) {
		onAfterChunk(context);
	}

	protected void onAfterChunk(ChunkContext context) {
	}

	@AfterChunkError
	final public void afterChunkError(ChunkContext context) {
		onChunkError(context);
	}

	protected void onChunkError(ChunkContext context) {
	}

	@AfterProcess
	final public void afterProcess(I item, O result) {
		if (result != null) {
			onAfterProcess(item, result);
		}
	}

	protected void onAfterProcess(I item, O result) {
	}

	@BeforeProcess
	final public void beforeProcess(I item) {
		onBeforeProcess(item);
	}

	protected void onBeforeProcess(I item) {
	}

	@OnProcessError
	final public void onProcessError(I item, Exception e) {
		afterProcessError(item, e);
	}

	protected void afterProcessError(I item, Exception e) {
	}

	@AfterWrite
	final public void afterWrite(List<O> items) {
		onAfterWrite(items, currentChunkContext);
	}

	protected void onAfterWrite(List<O> items, ChunkContext currentChunkContext) {
	}

	@BeforeWrite
	final public void beforeWrite(List<O> items) {
		onBeforeWrite(items);
	}

	protected void onBeforeWrite(List<O> items) {
	}

	@BeforeStep
	final public void beforeStep(StepExecution stepExecution) {
		onBeforeStep(stepExecution);
	}

	protected void onBeforeStep(StepExecution stepExecution) {
	}

	@AfterStep
	final public ExitStatus afterStep(StepExecution stepExecution) {
		onAfterStep(stepExecution);
		return stepExecution.getExitStatus();
	}

	protected void onAfterStep(StepExecution stepExecution) {
	}

}
