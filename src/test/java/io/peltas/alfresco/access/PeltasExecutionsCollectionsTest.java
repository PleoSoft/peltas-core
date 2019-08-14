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

package io.peltas.alfresco.access;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doAnswer;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

import io.peltas.alfresco.config.PeltastTestConfig;
import io.peltas.core.alfresco.PeltasEntry;
import io.peltas.core.alfresco.config.PeltasHandlerConfigurationProperties;
import io.peltas.core.alfresco.config.PeltasHandlerProperties;
import io.peltas.core.alfresco.config.PipelineCollection;
import io.peltas.core.alfresco.config.PipelineExecution;
import io.peltas.core.alfresco.integration.PeltasHandler;
import io.peltas.core.batch.PeltasDataHolder;
import io.peltas.core.batch.PeltasJdbcBatchWriter;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@TestPropertySource(locations = "classpath:peltas-executions-collection.properties")
@ContextConfiguration(classes = PeltastTestConfig.class)
public class PeltasExecutionsCollectionsTest {

	@Autowired
	PeltasHandlerConfigurationProperties properties;

	@Mock
	NamedParameterJdbcTemplate jdbcTemplate;

	@BeforeEach
	public void setup() {
		doAnswer(new Answer<Map<String, Object>>() {
			@Override
			public Map<String, Object> answer(InvocationOnMock invocation) {
				final Object[] args = invocation.getArguments();
				final Map<String, Object> keyMap = new HashMap<String, Object>();
				keyMap.put("id", 1);
				return keyMap;
			}
		}).when(jdbcTemplate).queryForMap(Mockito.any(String.class), Mockito.any(MapSqlParameterSource.class));
	}

	public PeltasDataHolder getAuditHolderForAuditEntry(PeltasEntry entry) {
		final String documentcreatedHandler = properties.findFirstBestMatchHandler(entry);

		final PeltasHandler handler = new PeltasHandler();

		final Message<PeltasEntry> message = MessageBuilder.withPayload(entry)
				.setHeader("alfresco.handler.configuration", new PeltasHandlerProperties()).build();
		final PeltasHandlerProperties configuration = properties.getForHandler(documentcreatedHandler);

		final PeltasHandlerProperties config = (PeltasHandlerProperties) message.getHeaders()
				.get("alfresco.handler.configuration");
		BeanUtils.copyProperties(configuration, config);

		return handler.handle(message);
	}

	@Test
	public void checkExecutionsAndCollections_documentCreatedAllOK() throws Exception {
		final PeltasEntry entry = new PeltasEntry();
		entry.setId("123546");
		entry.setUser("admin");
		entry.setTime(new Timestamp(new Date().getTime()));
		entry.setApplication("test");

		final Builder<String, Object> documentcreatedValues = ImmutableMap.<String, Object>builder();
		documentcreatedValues
				.put("/alfresco-access/transaction/properties/add",
						ImmutableMap.of("{http://www.alfresco.org/model/content/1.0}description", "{en=description}",
								"{http://www.alfresco.org/model/content/1.0}created", "Thu Jun 14 13:44:58 UTC 2018",
								"{http://www.alfresco.org/model/system/1.0}store-protocol", "workspace",
								"{http://www.alfresco.org/model/system/1.0}store-identifier", "SpacesStore",
								"{http://www.alfresco.org/model/system/1.0}node-uuid",
								"09ea11d8-810c-4e72-a9cc-ee8435af0963"))
				.put("/alfresco-access/transaction/type", "cm:content")
				.put("/alfresco-access/transaction/action", "CREATE")
				.put("/alfresco-access/transaction/path", "cm:app/test")
				.put("/alfresco-access/transaction/user", "admin")
				.put("/alfresco-access/transaction/aspects/add",
						ImmutableList.of("{http://www.alfresco.org/model/content/1.0}indexControl",
								"{http://www.alfresco.org/model/content/1.0}ownable"))
				.put("/alfresco-access/login/user", "test");
		entry.setValues(documentcreatedValues.build());

		final PeltasDataHolder processedPayload = getAuditHolderForAuditEntry(entry);
		final ArrayList<PeltasDataHolder> list = new ArrayList<>();
		list.add(processedPayload);

		final List<String> pipeline = processedPayload.getConfig().getPipeline().getExecutions();
		assertArrayEquals(Arrays.asList("batch_bi_case", "batch_bi_case_action").toArray(), pipeline.toArray());

		final LinkedHashMap<String, PipelineExecution> pipelineExecutionMap = properties.asPipelineExecutions(pipeline);
		assertArrayEquals(Arrays.asList("batch_bi_case", "batch_bi_case_action").toArray(),
				pipelineExecutionMap.keySet().toArray());

		final Map<String, PipelineCollection> collections = processedPayload.getConfig().getPipeline().getCollections();
		assertEquals(1, collections.size());
		assertArrayEquals(Arrays.asList("batch_bi_case_action_aspect", "batch_bi_case_action_aspect2").toArray(),
				collections.get("aspect").getExecutions().toArray());

		final PeltasJdbcBatchWriter writer = new PeltasJdbcBatchWriter(jdbcTemplate, properties);
		writer.write(list);
	}
}
