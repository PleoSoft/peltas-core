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
import org.springframework.batch.item.ItemProcessor;

import io.peltas.boot.PeltasHandlerConfigurationProperties;
import io.peltas.core.PeltasEntry;
import io.peltas.core.PeltasException;
import io.peltas.core.expression.PeltasHandlerProperties;
import io.peltas.core.integration.DoNotProcessHandler;
import io.peltas.core.integration.PeltasEntryHandler;

public abstract class PeltasItemProcessor implements ItemProcessor<PeltasEntry, PeltasDataHolder> {

	private static final Logger LOGGER = LoggerFactory.getLogger(PeltasItemProcessor.class);

	private final PeltasHandlerConfigurationProperties handlerProperties;
	private final PeltasEntryHandler handler;
	private final boolean expectionOnNoMatch;
	private final DoNotProcessHandler doNotProcessHandler;
	private final PeltasListenerAdapter peltasListenerAdapter;

	protected ChunkContext currentChunkContext;

	public PeltasItemProcessor(PeltasEntryHandler handler, PeltasHandlerConfigurationProperties handlerProperties,
			boolean expectionOnNoMatch, DoNotProcessHandler doNotProcessHandler,
			PeltasListenerAdapter peltasListenerAdapter) {
		this.handlerProperties = handlerProperties;
		this.expectionOnNoMatch = expectionOnNoMatch;
		this.handler = handler;
		this.doNotProcessHandler = doNotProcessHandler;
		this.peltasListenerAdapter = peltasListenerAdapter;
	}

	@Override
	final public PeltasDataHolder process(PeltasEntry item) throws Exception {
		if (shouldSkipItem(item)) {
			return null;
		}

		LOGGER.trace("handleMessage() {}", item);
		String bestMatchHandler = handlerProperties.findFirstBestMatchHandler(item);
		LOGGER.trace("handleMessage() best match handler {}", bestMatchHandler);

		if (bestMatchHandler == null) {
			if (expectionOnNoMatch) {
				LOGGER.error("handleMessage() failed to find a configured handler for {}", item);
				throw new PeltasException("no handler was found");
			}

			LOGGER.debug("no match for ENTRY ID: {}", item.getId());
			doNotProcessHandler.handle(item);
			return null;

		} else {
			PeltasHandlerProperties configuration = handlerProperties.getForHandler(bestMatchHandler);
			PeltasDataHolder payload = handler.handle(item, configuration);

			if (shouldSkipPayload(payload)) {
				onItemSkipped(item, payload);
				return null;
			}

			onItemProcessed(item, payload);
			return payload;
		}
	}

	protected void onItemSkipped(PeltasEntry item, PeltasDataHolder payload) {
	}

	protected void onItemProcessed(PeltasEntry item, PeltasDataHolder payload) {
	}

	protected boolean shouldSkipItem(PeltasEntry item) {
		return false;
	}

	protected boolean shouldSkipPayload(PeltasDataHolder item) {
		return false;
	}

	@BeforeChunk
	final public void beforeChunk(ChunkContext context) {
		currentChunkContext = context;
		onBeforeChunk(context);
		peltasListenerAdapter.onBeforeChunk(currentChunkContext);
	}

	protected void onBeforeChunk(ChunkContext context) {
	}

	@AfterChunk
	final public void afterChunk(ChunkContext context) {
		onAfterChunk(context);
		peltasListenerAdapter.onAfterChunk(context);
	}

	protected void onAfterChunk(ChunkContext context) {
	}

	@AfterChunkError
	final public void afterChunkError(ChunkContext context) {
		onChunkError(context);
		peltasListenerAdapter.onChunkError(context);
	}

	protected void onChunkError(ChunkContext context) {
	}

	@AfterProcess
	final public void afterProcess(PeltasEntry item, PeltasDataHolder result) {
		if (result != null) {
			onAfterProcess(item, result);
			peltasListenerAdapter.onAfterProcess(item, result);
		}
	}

	protected void onAfterProcess(PeltasEntry item, PeltasDataHolder result) {
	}

	@BeforeProcess
	final public void beforeProcess(PeltasEntry item) {
		onBeforeProcess(item);
		peltasListenerAdapter.onBeforeProcess(item);
	}

	protected void onBeforeProcess(PeltasEntry item) {
	}

	@OnProcessError
	final public void onProcessError(PeltasEntry item, Exception e) {
		afterProcessError(item, e);
		peltasListenerAdapter.afterProcessError(item, e);
	}

	protected void afterProcessError(PeltasEntry item, Exception e) {
	}

	@AfterWrite
	final public void afterWrite(List<PeltasDataHolder> items) {
		onAfterWrite(items, currentChunkContext);
		peltasListenerAdapter.onAfterWrite(items, currentChunkContext);
	}

	protected void onAfterWrite(List<PeltasDataHolder> items, ChunkContext currentChunkContext2) {
	}

	@BeforeWrite
	final public void beforeWrite(List<PeltasDataHolder> items) {
		onBeforeWrite(items);
		peltasListenerAdapter.onBeforeWrite(items);
	}

	protected void onBeforeWrite(List<PeltasDataHolder> items) {
	}

	@BeforeStep
	final public void beforeStep(StepExecution stepExecution) {
		onBeforeStep(stepExecution);
		peltasListenerAdapter.onBeforeStep(stepExecution);
	}

	protected void onBeforeStep(StepExecution stepExecution) {
	}

	@AfterStep
	final public ExitStatus afterStep(StepExecution stepExecution) {
		onAfterStep(stepExecution);
		peltasListenerAdapter.onAfterStep(stepExecution);
		return stepExecution.getExitStatus();
	}

	protected void onAfterStep(StepExecution stepExecution) {
	}
}
