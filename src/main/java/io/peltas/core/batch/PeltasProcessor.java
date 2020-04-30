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

import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.scope.context.ChunkContext;

import io.peltas.boot.PeltasHandlerConfigurationProperties;
import io.peltas.core.PeltasEntry;
import io.peltas.core.integration.DoNotProcessHandler;
import io.peltas.core.integration.PeltasEntryHandler;
import io.peltas.core.repository.TxDataRepository;
import io.peltas.core.repository.jpa.PeltasTimestamp;

public class PeltasProcessor extends PeltasItemProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(PeltasProcessor.class);

	public static final String ID_SEPARATOR = "___";

	private final String applicationName;
	private final TxDataRepository auditRepository;
	private final AtomicInteger counter = new AtomicInteger(0);
	private PeltasEntry lastAuditEntry;
	private final PeltasListenerAdapter peltasListenerAdapter;

	public PeltasProcessor(String applicationName, TxDataRepository auditRepository, PeltasEntryHandler handler,
			PeltasHandlerConfigurationProperties handlerProperties, boolean expectionOnNoMatch,
			DoNotProcessHandler doNotProcessHandler, PeltasListenerAdapter peltasListenerAdapter) {
		super(handler, handlerProperties, expectionOnNoMatch, doNotProcessHandler, peltasListenerAdapter);
		this.applicationName = applicationName;
		this.auditRepository = auditRepository;
		this.peltasListenerAdapter = peltasListenerAdapter;
	}

	@Override
	protected void onItemProcessed(PeltasEntry item, PeltasDataHolder holder) {
		counter.incrementAndGet();
	}

	@Override
	protected void onItemSkipped(PeltasEntry item, PeltasDataHolder holder) {
		LOGGER.info("Processor skipped ID {}", item.getId());
	}

	@Override
	protected void onBeforeProcess(PeltasEntry item) {
		lastAuditEntry = item;
	}

	@Override
	public void onBeforeChunk(ChunkContext context) {
		PeltasTimestamp timestamp = auditRepository.readTx(getCurrentApplicationName());
		if (timestamp != null) {
			String[] auditIdSplitted = timestamp.getRef().split(ID_SEPARATOR);
			Integer nodesCount = nodesCountToInteger(auditIdSplitted[1]);
			checkNodesCount(nodesCount);
		}

		if (counter.get() == 0) {
			counter.set(0);
		}
	}

	protected void checkNodesCount(Integer nodesCount) {

	}

	protected void onAfterWrite(List<PeltasDataHolder> items, ChunkContext currentChunkContext) {
		PeltasTimestamp timestamp = (PeltasTimestamp) currentChunkContext.getAttribute("peltasTimestamp");
		if (timestamp == null) {
			String newRef = "1" + ID_SEPARATOR + nodesCountToString(0);
			timestamp = new PeltasTimestamp(getCurrentApplicationName(), newRef, new Date());
		}

		String[] auditIdSplitted = timestamp.getRef().split(ID_SEPARATOR);
		Integer nodesCount = nodesCountToInteger(auditIdSplitted[1]);
		Integer processed = nodesCount + counter.get();
		// todo use string builder
		String newRef = getCurrentRef() + ID_SEPARATOR + nodesCountToString(processed);
		timestamp.setRef(newRef);
		PeltasTimestamp peltasTimestamp = auditRepository.writeTx(timestamp);
		currentChunkContext.setAttribute("peltasTimestamp", peltasTimestamp != null ? peltasTimestamp : timestamp);
	}

	protected String getCurrentRef() {
		return lastAuditEntry.getId();
	}

	protected String nodesCountToString(Integer nodesCount) {
		return nodesCount.toString();
	}

	protected Integer nodesCountToInteger(String nodesCount) {
		return Integer.valueOf(nodesCount);
	}

	public String getCurrentApplicationName() {
		return peltasListenerAdapter.applicationNameSuffix(applicationName);
	}
}