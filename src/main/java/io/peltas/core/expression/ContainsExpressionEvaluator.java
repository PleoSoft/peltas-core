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

package io.peltas.core.expression;

import java.util.List;

import io.peltas.core.PeltasEntry;
import io.peltas.core.integration.PeltasEntryHandler;

public class ContainsExpressionEvaluator extends AbstractEvalatorExpression {
	public ContainsExpressionEvaluator() {
		super("contains");
	}

	@Override
	public boolean isValueMapped(String evaluatorKey, String evaluatorValue, PeltasEntry auditEntry) {
		List<Object> mappedMultiValueProperty = PeltasEntryHandler.getMappedMultiValueProperty(evaluatorKey,
				auditEntry);
		return mappedMultiValueProperty.contains(evaluatorValue);
	}
}
