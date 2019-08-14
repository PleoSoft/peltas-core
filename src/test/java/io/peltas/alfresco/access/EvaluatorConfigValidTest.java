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

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.MultiValueMap;

import io.peltas.alfresco.config.PeltastTestConfig;
import io.peltas.core.alfresco.config.PeltasHandlerConfigurationProperties;

@TestPropertySource(locations = "classpath:peltas-evaluator-config-valid-test.properties")
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = PeltastTestConfig.class)
public class EvaluatorConfigValidTest {

	@Autowired
	PeltasHandlerConfigurationProperties pipeline;

	@Test
	public void evaluatorConfig_OK() {
		MultiValueMap<String, String> occurencies = pipeline.getEvaluatorConfigurationOccurencies();
		List<String> evaluatorCreate = occurencies.get("/alfresco-access/transaction/action=CREATE");
		assertEquals(2, evaluatorCreate.size());

		List<String> evaluatorContent = occurencies.get("/alfresco-access/transaction/type=cm:content");
		assertEquals(1, evaluatorContent.size());

		List<String> evaluatorFolder = occurencies.get("/alfresco-access/transaction/type=cm:folder");
		assertEquals(1, evaluatorFolder.size());
	}
}
