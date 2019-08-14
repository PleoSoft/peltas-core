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
import static org.junit.Assert.assertNull;

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

@TestPropertySource(locations = "classpath:peltas-evaluator-config-collection-test.properties")
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = PeltastTestConfig.class)
public class EvaluatorConfigCollectionTest {

	@Autowired
	PeltasHandlerConfigurationProperties pipeline;

	@Test
	public void givenGoodConfiguration_allIsOK() {
		MultiValueMap<String, String> occurencies = pipeline.getEvaluatorConfigurationOccurencies();

		List<String> evaluatorAspect = occurencies.get(
				"/alfresco-access/transaction/aspects/add=contains<>{http://www.alfresco.org/model/content/1.0}versionable");
		assertEquals(1, evaluatorAspect.size());

		List<String> evaluatorAspect2 = occurencies.get(
				"/alfresco-access/transaction/aspects/add=contains<>{http://www.alfresco.org/model/content/1.0}versionable2");
		assertEquals(1, evaluatorAspect2.size());

		List<String> evaluatorAspect3 = occurencies.get(
				"/alfresco-access/transaction/aspects/add=contains<>{http://www.alfresco.org/model/content/1.0}versionable3");
		assertEquals(1, evaluatorAspect3.size());

		List<String> evaluatorSomething = occurencies.get("something=TEST");
		assertEquals(1, evaluatorSomething.size());

		List<String> evaluatorFolder = occurencies.get("/alfresco-access/transaction/type=cm:folder");
		assertNull(evaluatorFolder);
	}
}
