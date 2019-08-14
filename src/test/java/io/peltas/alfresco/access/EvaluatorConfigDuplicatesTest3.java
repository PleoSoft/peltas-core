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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import io.peltas.alfresco.config.PeltastTestConfig;
import io.peltas.core.alfresco.PeltasException;
import io.peltas.core.alfresco.config.PeltasHandlerConfigurationProperties;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@TestPropertySource(locations = "classpath:peltas-evaluator-config-duplicates-test3.properties")
@ContextConfiguration(classes = PeltastTestConfig.class)
public class EvaluatorConfigDuplicatesTest3 {

	@Autowired
	PeltasHandlerConfigurationProperties pipeline;

	@Test
	public void ambigousEvaluatorConfig_shouldFailWithPeltasException() {
		Assertions.assertThrows(PeltasException.class, () -> {
			pipeline.getEvaluatorConfigurationOccurencies();
		});
	}
}
