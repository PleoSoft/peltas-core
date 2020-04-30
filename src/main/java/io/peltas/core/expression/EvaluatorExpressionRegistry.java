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

import java.util.HashMap;
import java.util.Map;

public class EvaluatorExpressionRegistry {

	private final Map<String, AbstractEvalatorExpression> map = new HashMap<>();
	private final AbstractEvalatorExpression defaultExpressionEvaluator;

	public EvaluatorExpressionRegistry(AbstractEvalatorExpression defaultExpressionEvaluator) {
		this.defaultExpressionEvaluator = defaultExpressionEvaluator;
		registerEvaluator(defaultExpressionEvaluator);
	}

	public void registerEvaluator(AbstractEvalatorExpression evaluatorExpresion) {
		map.put(evaluatorExpresion.getExpression().trim(), evaluatorExpresion);
	}

	public AbstractEvalatorExpression getEvaluatorExpression(String expression) {
		if (expression == null) {
			return defaultExpressionEvaluator;
		}

		AbstractEvalatorExpression evalatorExpression = map.get(expression);

		if (evalatorExpression == null) {
			return defaultExpressionEvaluator;
		}

		return evalatorExpression;
	}
}
