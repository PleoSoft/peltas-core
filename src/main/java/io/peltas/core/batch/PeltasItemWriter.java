package io.peltas.core.batch;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemWriter;

import io.peltas.core.alfresco.config.PipelineCollection;

public abstract class PeltasItemWriter<I, C> implements ItemWriter<PeltasDataHolder> {

	private static final Logger LOGGER = LoggerFactory.getLogger(PeltasItemWriter.class);

	public abstract I createItemInputParameters(PeltasDataHolder item);

	public abstract void itemExecution(String executionKey, I parameters);

	public abstract C createCollectionItemInputParameters(I itemParams, String collectionKey, Object collectionValue);

	public abstract void collectionExecution(String executionKey, C params);

	@SuppressWarnings("unchecked")
	public void write(List<? extends PeltasDataHolder> items) throws Exception {

		for (PeltasDataHolder item : items) {
			I parameterSourceMap = createItemInputParameters(item);

			List<String> executions = item.getConfig().getPipeline().getExecutions();

			for (String executionKey : executions) {
				LOGGER.trace("item: {} - executionKey: {}", item.getAuditEntry().getId(), executionKey);
				itemExecution(executionKey, parameterSourceMap);
			}

			Map<String, PipelineCollection> collections = item.getConfig().getPipeline().getCollections();
			if (collections != null) {
				Set<Entry<String, PipelineCollection>> collectionEntrySet = collections.entrySet();
				for (Entry<String, PipelineCollection> collectionEntry : collectionEntrySet) {
					String collectionKey = collectionEntry.getKey();

					PipelineCollection pipelineCollection = collectionEntry.getValue();
					List<String> collectionExecutions = pipelineCollection.getExecutions();

					Collection<Object> collectionValueList = (Collection<Object>) item.getBuilder().get(collectionKey);

					if (collectionValueList != null && !collectionValueList.isEmpty()) {
						for (Object collectionValue : collectionValueList) {
							C parameters = createCollectionItemInputParameters(parameterSourceMap, collectionKey,
									collectionValue);

							for (String executionKey : collectionExecutions) {
								LOGGER.trace("collection: {} - item: {} - executionKey: {}", collectionKey,
										item.getAuditEntry().getId(), executionKey);
								collectionExecution(executionKey, parameters);
							}
						}
					}
				}
			}
		}
	}
}
