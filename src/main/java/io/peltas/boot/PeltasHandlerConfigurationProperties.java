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

package io.peltas.boot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import io.peltas.core.PeltasEntry;
import io.peltas.core.PeltasException;
import io.peltas.core.expression.AbstractEvalatorExpression;
import io.peltas.core.expression.EvaluatorExpressionRegistry;
import io.peltas.core.expression.PeltasHandlerProperties;
import io.peltas.core.integration.PeltasEntryHandler;

@ConfigurationProperties(prefix = "peltas", ignoreInvalidFields = true, ignoreUnknownFields = true)
public class PeltasHandlerConfigurationProperties {

	private static final Logger LOGGER = LoggerFactory.getLogger(PeltasHandlerConfigurationProperties.class);

	private final Map<String, String> evaluatorsMap = new HashMap<>();
	private final LinkedMultiValueMap<String, String> shaMap = new LinkedMultiValueMap<>();
	private final Map<String, PeltasHandlerProperties> handlerConfigurationMap = new HashMap<>();

	private final EvaluatorExpressionRegistry registry;

	public PeltasHandlerConfigurationProperties(EvaluatorExpressionRegistry registry) {
		this.registry = registry;
	}

	public void setHandler(Map<String, PeltasHandlerProperties> handler) {
		Set<Entry<String, PeltasHandlerProperties>> entrySet = handler.entrySet();
		handlerConfigurationMap.putAll(handler);

		for (Entry<String, PeltasHandlerProperties> entry : entrySet) {
			String evaluator = entry.getValue().getEvaluator();
			addEvaluator(evaluator, entry.getKey());
		}
	}

	private void addEvaluator(String evaluator, String key) {
		if (evaluatorsMap.containsKey(key)) {
			LOGGER.error("Please check your Peltas evaluator configuration. There are duplicate evaluator entries: {}",
					key);
			throw new PeltasException("duplicate evaluator entries: " + key);
		}
		evaluatorsMap.put(key, evaluator);
	}

	// TODO refactor: this method does not belong inhere
	public String findFirstBestMatchHandler(PeltasEntry auditEntry) {
		Set<Entry<String, String>> entrySet = evaluatorsMap.entrySet();
		LinkedMultiValueMap<String, Integer> matchMap = new LinkedMultiValueMap<>();
		Map<String, Integer> highestEvaluatorRank = new HashMap<>();
		for (Entry<String, String> evaluatorEntry : entrySet) {
			List<String> evaluators = new ArrayList<>();
			String evaluator = evaluatorEntry.getValue();
			if (evaluator.contains("|")) {
				String[] evaluatorArray = StringUtils.delimitedListToStringArray(evaluator, "|");
				evaluators.addAll(Arrays.asList(evaluatorArray));
			} else {
				evaluators.add(evaluator);
			}

			for (String eval : evaluators) {
				if (eval.contains("=")) {
					String[] evalKeyVal = StringUtils.delimitedListToStringArray(eval, "=");
					String evalKeyRight = evalKeyVal[1];

					String[] expressionKeyVal = StringUtils.delimitedListToStringArray(evalKeyRight, "<>");
					String expression = null;
					if (expressionKeyVal.length > 1) {
						expression = expressionKeyVal[0].trim();
						evalKeyRight = evalKeyRight.replaceFirst(expression + "<>", "").trim();
					}
					AbstractEvalatorExpression evaluatorExpression = this.registry.getEvaluatorExpression(expression);
					if (evaluatorExpression.isValueMapped(evalKeyVal[0], evalKeyRight, auditEntry)) {
						matchMap.add(evaluatorEntry.getKey(), 1);
					}
				} else {
					// TODO use equals evaluator
					Object mappedValue = PeltasEntryHandler.getMappedSingleValueProperty(eval, auditEntry);
					if (mappedValue != null) {
						matchMap.add(evaluatorEntry.getKey(), 1);
					}
				}
			}

			highestEvaluatorRank.put(evaluatorEntry.getKey(), evaluators.size());
		}

		Set<Entry<String, List<Integer>>> matchMapSet = matchMap.entrySet();
		String bestMatchEvaluatorKey = null;
		Integer bestMatchSize = 0;
		for (Entry<String, List<Integer>> entry : matchMapSet) {
			List<Integer> matchList = entry.getValue();
			if (matchList.size() > bestMatchSize) {
				bestMatchSize = matchList.size();
				bestMatchEvaluatorKey = entry.getKey();
			}
		}

		if (bestMatchEvaluatorKey != null) {
			List<Integer> match = matchMap.get(bestMatchEvaluatorKey);
			if (match != null) {
				Integer evaluatorSize = highestEvaluatorRank.get(bestMatchEvaluatorKey);
				if (evaluatorSize == match.size()) {
					return bestMatchEvaluatorKey;
				}
			}
		}

		return null;
	}

	public PeltasHandlerProperties getForHandler(String handler) {
		PeltasHandlerProperties props = handlerConfigurationMap.get(handler);
		if (props != null && props.getHandlerName() == null) {
			props.setHandlerName(handler);
		}
		return props;
	}

	public MultiValueMap<String, String> getEvaluatorConfigurationOccurencies() {
		Set<Entry<String, String>> entrySet = evaluatorsMap.entrySet();

		LinkedMultiValueMap<String, String> all = new LinkedMultiValueMap<>();

		for (Entry<String, String> entry : entrySet) {
			String evaluator = entry.getValue();

			if (evaluator.contains("|")) {
				String[] evaluatorArray = StringUtils.delimitedListToStringArray(evaluator, "|");
				for (String eval : evaluatorArray) {
					all.add(eval, entry.getKey());
					shaMap.add(entry.getKey(), sha(eval));
				}
			} else {
				all.add(evaluator, entry.getKey());
				shaMap.add(entry.getKey(), sha(evaluator));
			}
		}

//		Entry<String, List<String>> lastError = null;
		// Set<Entry<String, List<String>>> mergedConfig = all.entrySet();
		// for (Entry<String, List<String>> entry : mergedConfig) {
		// List<String> evaluators = entry.getValue();
		// int size = evaluators.size();
		// if (size > 1) {
		// // potential duplicated evaluator check
		// compareEvaluatorsConfiguration(evaluators);
		//
		// LOGGER.warn("Peltas configuration is ambigous. {} has {}
		// occurencies", entry.getKey(), size);
		// lastError = entry;
		// }
		// }

		checkEvaluatorConfigurationOccurencies();

		return all;
	}

	private void checkEvaluatorConfigurationOccurencies() {
		Set<Entry<String, String>> entrySet = evaluatorsMap.entrySet();
		for (Entry<String, String> entry : entrySet) {
			List<String> shaList = shaMap.get(entry.getKey());

			Set<Entry<String, List<String>>> shaMapEntries = shaMap.entrySet();
			for (Entry<String, List<String>> shaMapEntry : shaMapEntries) {
				if (!entry.getKey().equals(shaMapEntry.getKey())) {
					List<String> comparingShaList = shaMapEntry.getValue();
					if (shaList.containsAll(comparingShaList)) {
						LOGGER.error(
								"Please check your Peltas evaluator configuration. There are duplicate entries with impredictable consequences! {} and {} seem to respond to the same evaluators",
								entry.getKey(), shaMapEntry.getKey());

						throw new PeltasException("Peltas configuration is ambigous. " + entry.getKey() + "  and "
								+ shaMapEntry.getKey() + " seem to respond to the same evaluators");
					}
				}
			}
		}
	}

	private String sha(String string) {
		return DigestUtils.sha1Hex(string);
	}
}
