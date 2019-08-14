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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

import io.peltas.alfresco.config.PeltastTestConfig;
import io.peltas.core.alfresco.PeltasEntry;
import io.peltas.core.alfresco.config.PeltasHandlerConfigurationProperties;
import io.peltas.core.alfresco.config.PeltasHandlerProperties;
import io.peltas.core.alfresco.integration.PeltasHandler;
import io.peltas.core.batch.PeltasDataHolder;

@TestPropertySource(locations = "classpath:peltas-test.properties")
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = PeltastTestConfig.class)
public class PeltasPropertyMapperTest {

	@Autowired
	PeltasHandlerConfigurationProperties pipeline;

	@Before
	public void setUp() {
	}

	@Test
	public void checkPropertyMapping_alfrescoAuditHolderIsNotNullAndContainsMappedProperties() {
		PeltasEntry entry = new PeltasEntry();

		ImmutableMap<String, Object> content = ImmutableMap.<String, Object>of("mimetype", "text/xml"); // v2
																										// test

		Builder<String, Object> documentcreatedValues = ImmutableMap.<String, Object>builder();

		Builder<String, Object> propValues = ImmutableMap.<String, Object>builder();
		propValues.put("{http://www.alfresco.org/model/content/1.0}description", "{en=description}") // v1
																										// test
				.put("{http://www.alfresco.org/model/content/1.0}created", "Thu Jun 14 13:44:58 UTC 2018")
				.put("{http://www.alfresco.org/model/system/1.0}store-protocol", "workspace")
				.put("{http://www.alfresco.org/model/system/1.0}store-identifier", "SpacesStore")
				.put("{http://www.alfresco.org/model/content/1.0}content", content)
				.put("{http://www.alfresco.org/model/system/1.0}node-uuid", "09ea11d8-810c-4e72-a9cc-ee8435af0963");

		documentcreatedValues.put("/alfresco-access/transaction/properties/add", propValues.build())
				.put("/alfresco-access/transaction/type", "cm:content")
				.put("/alfresco-access/transaction/action", "CREATE").put("/alfresco-access/login/user", "test");
		entry.setValues(documentcreatedValues.build());

		String documentcreatedHandler = pipeline.findFirstBestMatchHandler(entry);
		assertThat(documentcreatedHandler).isEqualTo("documentcreated");

		PeltasHandler handler = new PeltasHandler();

		Message<PeltasEntry> message = MessageBuilder.withPayload(entry)
				.setHeader("alfresco.handler.configuration", new PeltasHandlerProperties()).build();
		PeltasHandlerProperties configuration = pipeline.getForHandler(documentcreatedHandler);

		PeltasHandlerProperties config = (PeltasHandlerProperties) message.getHeaders()
				.get("alfresco.handler.configuration");
		BeanUtils.copyProperties(configuration, config);

		PeltasDataHolder processedPayload = handler.handle(message);

		assertThat(processedPayload).isNotNull();
		assertThat(processedPayload.getProperties()).isNotNull();
		assertThat(processedPayload.getBuilder()).isNotNull();

		Map<String, Object> builder = processedPayload.getBuilder();
		String format = processedPayload.getProperties().get("createdFormatted").getFormat();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);

		assertThat(builder.get("action")).isEqualTo("CREATE");
		assertThat(builder.get("type")).isEqualTo("cm:content");
		assertThat(builder.get("nodeRef")).isEqualTo("workspace://SpacesStore/09ea11d8-810c-4e72-a9cc-ee8435af0963");
		assertThat(builder.get("createdFormatted")).isEqualTo(LocalDate.of(2018, 06, 14).format(formatter));
		assertThat(builder.get("created")).isInstanceOf(Date.class);

		assertThat(builder.get("description")).isNotNull();
		assertThat(builder.get("description")).isInstanceOf(Map.class);

		Object en = ((Map<?, ?>) builder.get("description")).get("en");
		assertThat(en).isNotNull();
		assertThat(en).isInstanceOf(String.class);
		assertThat(en).isEqualTo("description");

		assertThat(builder.get("content")).isNotNull();
		assertThat(((Map<?, ?>) builder.get("content")).get("mimetype")).isEqualTo("text/xml");
	}

	@Test(expected = ConversionFailedException.class)
	public void invalidDateFormat_alfrescoAuditHolderIsNotNullAndContainsMappedProperties() {
		PeltasEntry entry = new PeltasEntry();

		Builder<String, Object> documentcreatedValues = ImmutableMap.<String, Object>builder();

		Builder<String, Object> propValues = ImmutableMap.<String, Object>builder();
		propValues.put("{http://www.alfresco.org/model/content/1.0}created", "Thu Jun 14 13:44:58 UTC");

		documentcreatedValues.put("/alfresco-access/transaction/properties/add", propValues.build())
				.put("/alfresco-access/transaction/type", "cm:content")
				.put("/alfresco-access/transaction/action", "CREATE");
		entry.setValues(documentcreatedValues.build());

		String documentcreatedHandler = pipeline.findFirstBestMatchHandler(entry);
		assertThat(documentcreatedHandler).isEqualTo("documentcreated");

		PeltasHandler handler = new PeltasHandler();

		Message<PeltasEntry> message = MessageBuilder.withPayload(entry)
				.setHeader("alfresco.handler.configuration", new PeltasHandlerProperties()).build();
		PeltasHandlerProperties configuration = pipeline.getForHandler(documentcreatedHandler);

		PeltasHandlerProperties config = (PeltasHandlerProperties) message.getHeaders()
				.get("alfresco.handler.configuration");
		BeanUtils.copyProperties(configuration, config);

		handler.handle(message);
	}

	@Test
	public void validDateFormatOld_alfrescoAuditHolderIsNotNullAndContainsMappedProperties() {
		PeltasEntry entry = new PeltasEntry();

		Builder<String, Object> documentcreatedValues = ImmutableMap.<String, Object>builder();

		Builder<String, Object> propValues = ImmutableMap.<String, Object>builder();
		propValues.put("{http://www.alfresco.org/model/content/1.0}created", "Thu Jun 14 13:44:58 UTC 2018");

		documentcreatedValues.put("/alfresco-access/transaction/properties/add", propValues.build())
				.put("/alfresco-access/transaction/type", "cm:content")
				.put("/alfresco-access/transaction/action", "CREATE");
		entry.setValues(documentcreatedValues.build());

		String documentcreatedHandler = pipeline.findFirstBestMatchHandler(entry);
		assertThat(documentcreatedHandler).isEqualTo("documentcreated");

		PeltasHandler handler = new PeltasHandler();

		Message<PeltasEntry> message = MessageBuilder.withPayload(entry)
				.setHeader("alfresco.handler.configuration", new PeltasHandlerProperties()).build();
		PeltasHandlerProperties configuration = pipeline.getForHandler(documentcreatedHandler);

		PeltasHandlerProperties config = (PeltasHandlerProperties) message.getHeaders()
				.get("alfresco.handler.configuration");
		BeanUtils.copyProperties(configuration, config);

		handler.handle(message);

		PeltasDataHolder processedPayload = handler.handle(message);

		assertThat(processedPayload).isNotNull();
		assertThat(processedPayload.getProperties()).isNotNull();
		assertThat(processedPayload.getBuilder()).isNotNull();

		Map<String, Object> builder = processedPayload.getBuilder();
		assertThat(builder.get("created")).isInstanceOf(Date.class);
	}

	@Test
	public void validDateFormatV1_alfrescoAuditHolderIsNotNullAndContainsMappedProperties() {
		PeltasEntry entry = new PeltasEntry();

		Builder<String, Object> documentcreatedValues = ImmutableMap.<String, Object>builder();

		Builder<String, Object> propValues = ImmutableMap.<String, Object>builder();
		propValues.put("{http://www.alfresco.org/model/content/1.0}created", "2018-07-17T12:32:34.596+0000");

		documentcreatedValues.put("/alfresco-access/transaction/properties/add", propValues.build())
				.put("/alfresco-access/transaction/action", "DATETEST");
		entry.setValues(documentcreatedValues.build());

		String documentcreatedHandler = pipeline.findFirstBestMatchHandler(entry);
		assertThat(documentcreatedHandler).isEqualTo("datetest");

		PeltasHandler handler = new PeltasHandler();

		Message<PeltasEntry> message = MessageBuilder.withPayload(entry)
				.setHeader("alfresco.handler.configuration", new PeltasHandlerProperties()).build();
		PeltasHandlerProperties configuration = pipeline.getForHandler(documentcreatedHandler);

		PeltasHandlerProperties config = (PeltasHandlerProperties) message.getHeaders()
				.get("alfresco.handler.configuration");
		BeanUtils.copyProperties(configuration, config);

		PeltasDataHolder processedPayload = handler.handle(message);

		assertThat(processedPayload).isNotNull();
		assertThat(processedPayload.getProperties()).isNotNull();
		assertThat(processedPayload.getBuilder()).isNotNull();

		Map<String, Object> builder = processedPayload.getBuilder();
		assertThat(builder.get("created")).isInstanceOf(Date.class);
	}

	@Test
	public void emptyPropertyValue() {
		PeltasEntry entry = new PeltasEntry();

		Builder<String, Object> documentcreatedValues = ImmutableMap.<String, Object>builder();

		Builder<String, Object> propValues = ImmutableMap.<String, Object>builder();
		propValues.put("{http://www.alfresco.org/model/content/1.0}created", "2018-07-17T12:32:34.596+0000");

		documentcreatedValues.put("/alfresco-access/transaction/properties/add", propValues.build())
				.put("/alfresco-access/transaction/action", "EMPTYPROPTEST");
		entry.setValues(documentcreatedValues.build());

		String documentcreatedHandler = pipeline.findFirstBestMatchHandler(entry);

		assertThat(documentcreatedHandler).isEqualTo("emptyprop");

		PeltasHandler handler = new PeltasHandler();

		Message<PeltasEntry> message = MessageBuilder.withPayload(entry)
				.setHeader("alfresco.handler.configuration", new PeltasHandlerProperties()).build();
		PeltasHandlerProperties configuration = pipeline.getForHandler(documentcreatedHandler);

		PeltasHandlerProperties config = (PeltasHandlerProperties) message.getHeaders()
				.get("alfresco.handler.configuration");
		BeanUtils.copyProperties(configuration, config);

		PeltasDataHolder processedPayload = handler.handle(message);

		assertThat(processedPayload).isNotNull();
		assertThat(processedPayload.getProperties()).isNotNull();
		assertThat(processedPayload.getBuilder()).isNotNull();

		Map<String, Object> builder = processedPayload.getBuilder();
		assertThat(builder.containsKey("empty")).isTrue();
		assertThat(builder.get("empty")).isEqualTo("");
	}

	@Test
	public void nullPropertyValue() {
		PeltasEntry entry = new PeltasEntry();

		Builder<String, Object> documentcreatedValues = ImmutableMap.<String, Object>builder();

		Builder<String, Object> propValues = ImmutableMap.<String, Object>builder();
		propValues.put("{http://www.alfresco.org/model/content/1.0}created", "2018-07-17T12:32:34.596+0000");

		documentcreatedValues.put("/alfresco-access/transaction/properties/add", propValues.build())
				.put("/alfresco-access/transaction/action", "NULLPROPTEST");
		entry.setValues(documentcreatedValues.build());

		String documentcreatedHandler = pipeline.findFirstBestMatchHandler(entry);

		assertThat(documentcreatedHandler).isEqualTo("nullprop");

		PeltasHandler handler = new PeltasHandler();

		Message<PeltasEntry> message = MessageBuilder.withPayload(entry)
				.setHeader("alfresco.handler.configuration", new PeltasHandlerProperties()).build();
		PeltasHandlerProperties configuration = pipeline.getForHandler(documentcreatedHandler);

		PeltasHandlerProperties config = (PeltasHandlerProperties) message.getHeaders()
				.get("alfresco.handler.configuration");
		BeanUtils.copyProperties(configuration, config);

		PeltasDataHolder processedPayload = handler.handle(message);

		assertThat(processedPayload).isNotNull();
		assertThat(processedPayload.getProperties()).isNotNull();
		assertThat(processedPayload.getBuilder()).isNotNull();

		Map<String, Object> builder = processedPayload.getBuilder();
		assertThat(builder.containsKey("nullprop")).isTrue();
		assertThat(builder.get("nullprop")).isNull();
	}
}
