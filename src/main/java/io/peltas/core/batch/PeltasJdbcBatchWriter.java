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

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemWriter;
import org.springframework.integration.transformer.ObjectToMapTransformer;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import com.google.common.collect.ImmutableMap;

import io.peltas.core.alfresco.PeltasEntry;
import io.peltas.core.alfresco.config.PeltasHandlerConfigurationProperties;
import io.peltas.core.alfresco.config.PipelineCollection;
import io.peltas.core.alfresco.config.PipelineExecution;

public class PeltasJdbcBatchWriter implements ItemWriter<PeltasDataHolder> {

	private static final Logger LOGGER = LoggerFactory.getLogger(PeltasJdbcBatchWriter.class);

	private final PeltasHandlerConfigurationProperties properties;
	private final NamedParameterJdbcOperations namedParameterJdbcTemplate;

	public PeltasJdbcBatchWriter(NamedParameterJdbcTemplate template, PeltasHandlerConfigurationProperties properties) {
		this.properties = properties;
		this.namedParameterJdbcTemplate = template;
	}

	@SuppressWarnings("unchecked")
	public void write(List<? extends PeltasDataHolder> items) throws Exception {

		for (PeltasDataHolder item : items) {
			MapSqlParameterSource parameterSourceMap = createSqlParameterSource(item);

			LinkedHashMap<String, PipelineExecution> pipelineMap = properties
					.asPipelineExecutions(item.getConfig().getPipeline().getExecutions());
			Set<Entry<String, PipelineExecution>> pipelines = pipelineMap.entrySet();
			for (Entry<String, PipelineExecution> entry : pipelines) {
				PipelineExecution pipelineExecution = entry.getValue();
				String executionKey = entry.getKey();

				String sql = pipelineExecution.getConfigValue("sql");
				Map<String, Object> sqlResult = namedParameterJdbcTemplate.queryForMap(sql, parameterSourceMap);
				addSources(executionKey, parameterSourceMap, sqlResult);

				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("doWithItem() executing sql: {} with data {}", sql, parameterSourceMap.getValues());
				}
			}

			Map<String, PipelineCollection> collections = item.getConfig().getPipeline().getCollections();
			if (collections != null) {
				LOGGER.debug("doWithItem() executing collections: {}", collections);

				Set<Entry<String, PipelineCollection>> collectionEntrySet = collections.entrySet();
				for (Entry<String, PipelineCollection> collectionEntry : collectionEntrySet) {
					String collectionKey = collectionEntry.getKey();

					PipelineCollection pipelineCollection = collectionEntry.getValue();
					List<String> executions = pipelineCollection.getExecutions();

					Collection<Object> collectionValueList = (Collection<Object>) item.getBuilder().get(collectionKey);

					if (collectionValueList != null && !collectionValueList.isEmpty()) {

						MapSqlParameterSource collectionSqlMapsource = new MapSqlParameterSource(
								parameterSourceMap.getValues());

						for (Object collectionValue : collectionValueList) {
							if (collectionValue instanceof Map) {
								Message<Map<String, ?>> message = new GenericMessage<Map<String, ?>>(
										ImmutableMap.of(collectionKey, collectionValue));
								ObjectToMapTransformer transformer = new ObjectToMapTransformer();
								transformer.setShouldFlattenKeys(true);
								Map<String, ?> payload = (Map<String, ?>) transformer.transform(message).getPayload();
								collectionSqlMapsource.addValues(payload);
							} else {
								collectionSqlMapsource.addValue(collectionKey, collectionValue);
							}

							for (String execution : executions) {
								PipelineExecution executionCollection = properties.getPipelineExecution(execution);

								String collectionSql = executionCollection.getConfigValue("sql");
								Map<String, Object> sqlResult = namedParameterJdbcTemplate.queryForMap(collectionSql,
										collectionSqlMapsource);
								addSources(execution, collectionSqlMapsource, sqlResult);
								if (LOGGER.isTraceEnabled()) {
									LOGGER.trace("doWithItem() executing collection sql in batch: {}", collectionSql);
								}
							}
						}
					}
				}
			}
		}
	}

	private void addSources(String executionKey, MapSqlParameterSource parameterSourceMap, Map<String, Object> key) {
		Set<Entry<String, Object>> entrySet = key.entrySet();

		Map<String, Object> keyMap = new HashMap<>();
		for (Entry<String, Object> keyEntry : entrySet) {
			keyMap.put(executionKey + "." + keyEntry.getKey(), keyEntry.getValue());
		}

		if (keyMap.size() > 0) {
			parameterSourceMap.addValues(keyMap);
		}
	}

	public static MapSqlParameterSource createSqlParameterSource(PeltasDataHolder item) {
		PeltasEntry auditEntry = item.getAuditEntry();
		Map<String, Object> builder = item.getBuilder();

		MapSqlParameterSource parameterSourceMap = new MapSqlParameterSource();

		// TODO FIXME: getUserFullname ?
//		ImmutableMap<String, ? extends Object> auditMap = ImmutableMap.of("audit.id", auditEntry.getId(), "audit.user",
//				auditEntry.getUser(), "audit.userfull", auditEntry.getUserFullname(), "audit.time",
//				auditEntry.getTime());

		ImmutableMap<String, ? extends Object> auditMap = ImmutableMap.of("audit.id", auditEntry.getId(), "audit.user",
				auditEntry.getUser(), "audit.time", auditEntry.getTime());

		parameterSourceMap.addValues(builder);
		parameterSourceMap.addValues(auditMap);
		return parameterSourceMap;
	}
}
