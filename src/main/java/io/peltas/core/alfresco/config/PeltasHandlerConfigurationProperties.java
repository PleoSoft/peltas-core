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

package io.peltas.core.alfresco.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import io.peltas.core.alfresco.PeltasEntry;
import io.peltas.core.alfresco.PeltasException;
import io.peltas.core.alfresco.config.expression.AbstractEvalatorExpression;
import io.peltas.core.alfresco.config.expression.EvaluatorExpressionRegistry;
import io.peltas.core.alfresco.integration.PeltasHandler;

@ConfigurationProperties(prefix = "peltas", ignoreInvalidFields = true, ignoreUnknownFields = true)
public class PeltasHandlerConfigurationProperties {

	private static final Logger LOGGER = LoggerFactory.getLogger(PeltasHandlerConfigurationProperties.class);

	private final Map<String, String> evaluatorsMap = new HashMap<>();
	private final LinkedMultiValueMap<String, String> shaMap = new LinkedMultiValueMap<>();
	private final Map<String, PeltasHandlerProperties> handlerConfigurationMap = new HashMap<>();
	private final Map<String, PipelineExecution> executionConfigurationMap = new HashMap<>();

	private final EvaluatorExpressionRegistry registry;

	private final Map<String, Map<String, String>> mappedExecutionsConfigResources;

	public PeltasHandlerConfigurationProperties(EvaluatorExpressionRegistry registry, Resource[] resources) {
		this.registry = registry;

		this.mappedExecutionsConfigResources = new HashMap<>();

		try {
			for (Resource resource : resources) {
				String filename = resource.getFilename();
				String configKey = StringUtils.getFilenameExtension(filename);
				if(!StringUtils.hasText(configKey)) {
					continue;
				}
				
				String key = filename.substring(0, filename.length() - configKey.length() - 1);
				
				Map<String, String> config = new HashMap<>();				
				try (InputStream is = resource.getInputStream()) {
					String configValue = FileCopyUtils.copyToString(new InputStreamReader(is));
					
					config.put(configKey, configValue);
				}
				
				this.mappedExecutionsConfigResources.put(key, config);
			}				
		} catch (IOException e) {
			throw new RuntimeException(e);
		} 
	}

	public void setHandler(Map<String, PeltasHandlerProperties> handler) {
		Set<Entry<String, PeltasHandlerProperties>> entrySet = handler.entrySet();
		handlerConfigurationMap.putAll(handler);

		for (Entry<String, PeltasHandlerProperties> entry : entrySet) {
			String evaluator = entry.getValue().getEvaluator();
			addEvaluator(evaluator, entry.getKey());
		}
	}

	public void setExecution(Map<String, PipelineExecution> executions) {
		executionConfigurationMap.putAll(executions);
	}

	public PipelineExecution getPipelineExecution(final String key) {
		PipelineExecution pipelineExecution = executionConfigurationMap.get(key);
		if (pipelineExecution == null) {
			pipelineExecution = new PipelineExecution();
			
			Map<String, String> config = this.mappedExecutionsConfigResources.get(key);
			if(config == null) {
				LOGGER.error("execution configuration is not found for: {}. Verify your execution folders and files!", key);	
			}
			
			pipelineExecution.setConfig(config);
			executionConfigurationMap.put(key, pipelineExecution);
		}
		return pipelineExecution;
	}

	private void addEvaluator(String evaluator, String key) {
		if (evaluatorsMap.containsKey(key)) {
			LOGGER.error("Please check your Peltas evaluator configuration. There are duplicate evaluator entries: {}",
					key);
			throw new PeltasException("duplicate evaluator entries: " + key);
		}
		evaluatorsMap.put(key, evaluator);
	}

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
					Object mappedValue = PeltasHandler.getMappedSingleValueProperty(eval, auditEntry);
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
		return handlerConfigurationMap.get(handler);
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

	public LinkedHashMap<String, PipelineExecution> asPipelineExecutions(List<String> pipeline) {
		LinkedHashMap<String, PipelineExecution> pipelineExecutionMap = new LinkedHashMap<>();
		for (String p : pipeline) {
			PipelineExecution pipelineExecution = this.getPipelineExecution(p);
			pipelineExecutionMap.put(p, pipelineExecution);
		}

		return pipelineExecutionMap;
	}
}
