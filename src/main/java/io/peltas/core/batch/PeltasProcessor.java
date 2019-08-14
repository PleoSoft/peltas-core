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
import org.springframework.messaging.core.GenericMessagingTemplate;
import org.springframework.messaging.support.MessageBuilder;

import io.peltas.core.alfresco.PeltasEntry;
import io.peltas.core.alfresco.config.PeltasHandlerProperties;
import io.peltas.core.alfresco.integration.DoNotProcessMarker;
import io.peltas.core.repository.PeltasTimestamp;
import io.peltas.core.repository.PeltasTimestampRepository;

public class PeltasProcessor extends PeltasItemProcessor<PeltasEntry, PeltasDataHolder> {

	private static final Logger LOGGER = LoggerFactory.getLogger(PeltasProcessor.class);

	public static final String ID_SEPARATOR = "___";

	private final String applicationName;
	private final PeltasTimestampRepository auditRepository;
	private final AtomicInteger counter = new AtomicInteger(0);
	private PeltasEntry lastAuditEntry;

	public PeltasProcessor(String applicationName, GenericMessagingTemplate template,
			PeltasTimestampRepository auditRepository) {
		super(template);
		this.applicationName = applicationName;
		this.auditRepository = auditRepository;
	}

	@Override
	protected void doWithMessage(MessageBuilder<PeltasEntry> messageBuilder) {
		messageBuilder.setHeader("alfresco.handler.configuration", new PeltasHandlerProperties());
	}

	@Override
	protected boolean shouldSkipPayload(PeltasDataHolder item) {
		if (item instanceof DoNotProcessMarker) {
			return true;
		}
		return false;
	}

	@Override
	protected void onItemProcessed(PeltasEntry item, PeltasDataHolder holder) {
		counter.incrementAndGet();
	}

	@Override
	protected void onBeforeProcess(PeltasEntry item) {
		lastAuditEntry = item;
	}

	@Override
	public void onBeforeChunk(ChunkContext context) {
		PeltasTimestamp timestamp = auditRepository.findTopByApplicationNameOrderByAccessDesc(applicationName);
		if (timestamp != null) {
			String[] auditIdSplitted = timestamp.getRef().split(ID_SEPARATOR);
			Integer nodesCount = nodesCountToInteger(auditIdSplitted[1]);
			checkNodesCount(nodesCount);
		}

		counter.set(0);
	}

	protected void checkNodesCount(Integer nodesCount) {

	}

	@Override
	protected void onAfterWrite(List<PeltasDataHolder> items, ChunkContext currentChunkContext) {
		PeltasTimestamp timestamp = (PeltasTimestamp) currentChunkContext.getAttribute("peltasTimestamp");
		if (timestamp == null) {
			String newRef = "1" + ID_SEPARATOR + nodesCountToString(0);
			timestamp = new PeltasTimestamp(applicationName, newRef, new Date());
		}

		String[] auditIdSplitted = timestamp.getRef().split(ID_SEPARATOR);
		Integer nodesCount = nodesCountToInteger(auditIdSplitted[1]);
		Integer processed = nodesCount + counter.get();
		// todo use string builder
		String newRef = getCurrentRef() + ID_SEPARATOR + nodesCountToString(processed);
		timestamp.setRef(newRef);
		PeltasTimestamp peltasTimestamp;
		peltasTimestamp = auditRepository.save(timestamp);
		currentChunkContext.setAttribute("peltasTimestamp", peltasTimestamp);
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

}