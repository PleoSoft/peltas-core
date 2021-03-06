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

package io.peltas.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import io.peltas.boot.PeltasHandlerConfigurationProperties;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@TestPropertySource(locations = "classpath:peltas-evaluator-config-duplicates-collection-test1.properties")
@ContextConfiguration(classes = PeltasTestConfiguration.class)
public class EvaluatorConfigDuplicateCollectionTest1 {

	@Autowired
	PeltasHandlerConfigurationProperties pipeline;

	@Test
	public void givenWrongConfiguration_peltasExcpetionsShouldBeThrown() {
		Assertions.assertThrows(PeltasException.class, () -> {
			pipeline.getEvaluatorConfigurationOccurencies();
		});
	}
}
