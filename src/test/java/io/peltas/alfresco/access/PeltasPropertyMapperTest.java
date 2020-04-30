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
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
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
import io.peltas.boot.PeltasHandlerConfigurationProperties;
import io.peltas.core.PeltasEntry;
import io.peltas.core.batch.PeltasDataHolder;
import io.peltas.core.expression.PeltasHandlerProperties;
import io.peltas.core.integration.PeltasConversionException;
import io.peltas.core.integration.PeltasFormatUtil;
import io.peltas.core.integration.PeltasEntryHandler;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@TestPropertySource(locations = "classpath:peltas-test.properties")
@ContextConfiguration(classes = PeltastTestConfig.class)
public class PeltasPropertyMapperTest {

	@Autowired
	PeltasHandlerConfigurationProperties pipeline;
	
	@Autowired
	List<Converter<?,?>> converters;
	
	@Autowired
	PeltasFormatUtil peltasFormatUtil;

	@Test
	public void checkPropertyMapping_alfrescoAuditHolderIsNotNullAndContainsMappedProperties() {
		PeltasEntry entry = new PeltasEntry();

		ImmutableMap<String, Object> content = ImmutableMap.<String, Object>of("mimetype", "text/xml");

		Builder<String, Object> documentcreatedValues = ImmutableMap.<String, Object>builder();

		Builder<String, Object> propValues = ImmutableMap.<String, Object>builder();
		propValues.put("{http://www.alfresco.org/model/content/1.0}description", ImmutableMap.of("en", "description"))
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

		PeltasEntryHandler handler = new PeltasEntryHandler(converters, peltasFormatUtil);
		PeltasHandlerProperties configuration = pipeline.getForHandler(documentcreatedHandler);

		PeltasDataHolder processedPayload = handler.handle(entry, configuration);

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

	@Test
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

		PeltasEntryHandler handler = new PeltasEntryHandler(converters, peltasFormatUtil);

		PeltasHandlerProperties configuration = pipeline.getForHandler(documentcreatedHandler);

		Assertions.assertThrows(PeltasConversionException.class, () -> {
			handler.handle(entry, configuration);
		});
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

		PeltasEntryHandler handler = new PeltasEntryHandler(converters, peltasFormatUtil);

		PeltasHandlerProperties configuration = pipeline.getForHandler(documentcreatedHandler);

		//handler.handle(message);

		PeltasDataHolder processedPayload = handler.handle(entry, configuration);;

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

		PeltasEntryHandler handler = new PeltasEntryHandler(converters, peltasFormatUtil);

		PeltasHandlerProperties configuration = pipeline.getForHandler(documentcreatedHandler);

		PeltasDataHolder processedPayload = handler.handle(entry, configuration);;

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

		PeltasEntryHandler handler = new PeltasEntryHandler(converters, peltasFormatUtil);

		PeltasHandlerProperties configuration = pipeline.getForHandler(documentcreatedHandler);

		PeltasDataHolder processedPayload = handler.handle(entry, configuration);;

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

		PeltasEntryHandler handler = new PeltasEntryHandler(converters, peltasFormatUtil);
		
		PeltasHandlerProperties configuration = pipeline.getForHandler(documentcreatedHandler);

		PeltasDataHolder processedPayload = handler.handle(entry, configuration);;

		assertThat(processedPayload).isNotNull();
		assertThat(processedPayload.getProperties()).isNotNull();
		assertThat(processedPayload.getBuilder()).isNotNull();

		Map<String, Object> builder = processedPayload.getBuilder();
		assertThat(builder.containsKey("nullprop")).isTrue();
		assertThat(builder.get("nullprop")).isNull();
	}
}
