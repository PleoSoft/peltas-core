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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.convert.converter.Converter;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

import io.peltas.alfresco.config.PeltastTestConfig;
import io.peltas.core.alfresco.PeltasEntry;
import io.peltas.core.alfresco.config.PeltasHandlerConfigurationProperties;
import io.peltas.core.alfresco.config.PeltasHandlerProperties;
import io.peltas.core.alfresco.integration.PeltasFormatUtil;
import io.peltas.core.alfresco.integration.PeltasHandler;
import io.peltas.core.batch.PeltasDataHolder;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@TestPropertySource(locations = "classpath:peltas-moveaction-property-mapper-test.properties")
@ContextConfiguration(classes = PeltastTestConfig.class)
public class PeltasPropertyMapperMoveActionTest {

	@Autowired
	PeltasHandlerConfigurationProperties pipeline;
	
	@Autowired
	List<Converter<?,?>> converters;
	
	@Autowired
	PeltasFormatUtil peltasFormatUtil;

	@Test
	public void missingPropertyValue() {
		PeltasEntry entry = new PeltasEntry();

		Builder<String, Object> documentcreatedValues = ImmutableMap.<String, Object>builder();

		Builder<String, Object> propValues = ImmutableMap.<String, Object>builder();
		propValues.put("{http://www.alfresco.org/model/content/1.0}created", "2018-07-17T12:32:34.596+0000");

		documentcreatedValues.put("/alfresco-access/transaction/properties/add", propValues.build())
				.put("/alfresco-access/transaction/action", "MOVE")
				.put("/alfresco-access/transaction/type", "cm:content");
		entry.setValues(documentcreatedValues.build());

		String documentcreatedHandler = pipeline.findFirstBestMatchHandler(entry);

		assertThat(documentcreatedHandler).isEqualTo("documentmove");

		PeltasHandler handler = new PeltasHandler(converters, peltasFormatUtil);

		Message<PeltasEntry> message = MessageBuilder.withPayload(entry)
				.setHeader("peltas.handler.configuration", new PeltasHandlerProperties()).build();
		PeltasHandlerProperties configuration = pipeline.getForHandler(documentcreatedHandler);

		PeltasHandlerProperties config = (PeltasHandlerProperties) message.getHeaders()
				.get("peltas.handler.configuration");
		BeanUtils.copyProperties(configuration, config);

		PeltasDataHolder processedPayload = handler.handle(message);

		assertThat(processedPayload).isNotNull();
		assertThat(processedPayload.getProperties()).isNotNull();
		assertThat(processedPayload.getBuilder()).isNotNull();

		Map<String, Object> builder = processedPayload.getBuilder();
		assertThat(builder.containsKey("name")).isTrue();
		assertThat(builder.get("name")).isEqualTo("");
	}

	@Test
	public void containsPropertyValue() {
		PeltasEntry entry = new PeltasEntry();

		Builder<String, Object> documentcreatedValues = ImmutableMap.<String, Object>builder();

		Builder<String, Object> propValues = ImmutableMap.<String, Object>builder();
		propValues.put("{http://www.alfresco.org/model/content/1.0}name", "test123");

		documentcreatedValues.put("/alfresco-access/transaction/properties/to", propValues.build())
				.put("/alfresco-access/transaction/action", "MOVE")
				.put("/alfresco-access/transaction/type", "cm:content")
				.put("/alfresco-access/transaction/path", "/app:company_home/cm:test123")
				.put("/alfresco-access/transaction/move/from/path", "/app:company_home/cm:teeeest");
		entry.setValues(documentcreatedValues.build());

		String documentcreatedHandler = pipeline.findFirstBestMatchHandler(entry);

		assertThat(documentcreatedHandler).isEqualTo("documentmove");

		PeltasHandler handler = new PeltasHandler(converters, peltasFormatUtil);

		Message<PeltasEntry> message = MessageBuilder.withPayload(entry)
				.setHeader("peltas.handler.configuration", new PeltasHandlerProperties()).build();
		PeltasHandlerProperties configuration = pipeline.getForHandler(documentcreatedHandler);

		PeltasHandlerProperties config = (PeltasHandlerProperties) message.getHeaders()
				.get("peltas.handler.configuration");
		BeanUtils.copyProperties(configuration, config);

		PeltasDataHolder processedPayload = handler.handle(message);

		assertThat(processedPayload).isNotNull();
		assertThat(processedPayload.getProperties()).isNotNull();
		assertThat(processedPayload.getBuilder()).isNotNull();

		Map<String, Object> builder = processedPayload.getBuilder();
		assertThat(builder.containsKey("name")).isTrue();
		assertThat(builder.get("name")).isEqualTo("test123");
	}
}
