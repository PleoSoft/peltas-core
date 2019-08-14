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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

import io.peltas.alfresco.config.PeltastTestConfig;
import io.peltas.core.alfresco.PeltasEntry;
import io.peltas.core.alfresco.config.PeltasHandlerConfigurationProperties;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@TestPropertySource(locations = "classpath:peltas-test.properties")
@ContextConfiguration(classes = PeltastTestConfig.class)
public class PeltasEvaluatorTest {

	@Autowired
	PeltasHandlerConfigurationProperties pipeline;

	@Test
	public void shouldFindHandlerWithTwoMatchers_whenBothEvaluatorsMatch() {
		final PeltasEntry entry = new PeltasEntry();

		final Builder<String, Object> documentcreatedValues = ImmutableMap.<String, Object>builder();
		documentcreatedValues
				.put("/alfresco-access/transaction/properties/add",
						ImmutableMap.of("{http://www.alfresco.org/model/content/1.0}description", "{en=description}")
								.toString())
				.put("/alfresco-access/transaction/type", "cm:content")
				.put("/alfresco-access/transaction/action", "CREATE").put("/alfresco-access/login/user", "test");
		entry.setValues(documentcreatedValues.build());

		final String documentcreatedHandler = pipeline.findFirstBestMatchHandler(entry);
		assertThat(documentcreatedHandler).isEqualTo("documentcreated");
	}

	@Test
	public void shouldNotFindHandlerWithTwoMatchers_whenOneEvaluatorsMatch() {
		final PeltasEntry entry = new PeltasEntry();

		final Builder<String, Object> documentcreatedValues = ImmutableMap.<String, Object>builder();
		documentcreatedValues
				.put("/alfresco-access/transaction/properties/add",
						ImmutableMap.of("{http://www.alfresco.org/model/content/1.0}description", "{en=description}")
								.toString())
				.put("/alfresco-access/transaction/action", "CREATE").put("/alfresco-access/login/user", "test");
		entry.setValues(documentcreatedValues.build());

		final String documentcreatedHandler = pipeline.findFirstBestMatchHandler(entry);
		assertThat(documentcreatedHandler).isNull();
	}

	@Test
	public void shouldFindDocumentUpdated2HandlerWithTwoMatchers_whenThreeEvaluatorsMatch() {
		final PeltasEntry entry = new PeltasEntry();

		final Builder<String, Object> documentcreatedValues = ImmutableMap.<String, Object>builder();
		documentcreatedValues
				.put("/alfresco-access/transaction/properties/add",
						ImmutableMap.of("{http://www.alfresco.org/model/content/1.0}description", "{en=description}")
								.toString())
				.put("/alfresco-access/transaction/action", "updateNodeProperties")
				.put("/alfresco-access/login/user", "test").put("/alfresco-access/transaction/type", "cm:content");
		entry.setValues(documentcreatedValues.build());

		final String documentUpdatedHandler = pipeline.findFirstBestMatchHandler(entry);
		// TODO ovdje trebamo bolji handling odradit jer nije "sorted"?
		assertThat(documentUpdatedHandler).isEqualTo("documentupdated2");
	}

	@Test
	public void shouldFindFolderUpdatedHandlerWithTwoMatchers_whenOneEvaluatorsMatch() {
		final PeltasEntry entry = new PeltasEntry();

		final Builder<String, Object> documentcreatedValues = ImmutableMap.<String, Object>builder();
		documentcreatedValues
				.put("/alfresco-access/transaction/properties/add",
						ImmutableMap.of("{http://www.alfresco.org/model/content/1.0}description", "{en=description}")
								.toString())
				.put("/alfresco-access/transaction/action", "updateNodeProperties")
				.put("/alfresco-access/login/user", "test").put("/alfresco-access/transaction/type", "cm:folder");
		entry.setValues(documentcreatedValues.build());

		final String folderUpdatedHandler = pipeline.findFirstBestMatchHandler(entry);
		assertThat(folderUpdatedHandler).isEqualTo("folderupdated");
	}

	@Test
	public void shouldFindUpdateNodePropertiesHandlerWithOneMatcher_whenOneEvaluatorsMatch() {
		final PeltasEntry entry = new PeltasEntry();

		final Builder<String, Object> documentcreatedValues = ImmutableMap.<String, Object>builder();
		documentcreatedValues
				.put("/alfresco-access/transaction/properties/add",
						ImmutableMap.of("{http://www.alfresco.org/model/content/1.0}description", "{en=description}")
								.toString())
				.put("/alfresco-access/transaction/action", "updateNodeProperties")
				.put("/alfresco-access/login/user", "test");
		entry.setValues(documentcreatedValues.build());

		final String documentUpdatedHandler = pipeline.findFirstBestMatchHandler(entry);
		assertThat(documentUpdatedHandler).isEqualTo("updateNodeProperties");
	}

	@Test
	public void shouldFindDateTestEvaluator_onlyOneEvaluatorsMatch() {
		final PeltasEntry entry = new PeltasEntry();

		final Builder<String, Object> documentcreatedValues = ImmutableMap.<String, Object>builder();
		documentcreatedValues.put("/alfresco-access/transaction/action", "DATETEST");
		entry.setValues(documentcreatedValues.build());

		final String documentUpdatedHandler = pipeline.findFirstBestMatchHandler(entry);
		assertThat(documentUpdatedHandler).isEqualTo("datetest");
	}
}
