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

public class PeltasHandlerProperties {
	private String evaluator;
	private PeltasMapper mapper;
	private Pipeline pipeline;
	private String handlerName;

	public String getEvaluator() {
		return evaluator;
	}

	public void setEvaluator(String evaluator) {
		this.evaluator = evaluator;
	}

	public PeltasMapper getMapper() {
		return mapper;
	}

	public void setMapper(PeltasMapper mapper) {
		this.mapper = mapper;
	}

	public Pipeline getPipeline() {
		return pipeline;
	}

	public void setPipeline(Pipeline pipeline) {
		this.pipeline = pipeline;
	}
	
	public String getHandlerName() {
		return handlerName;
	}
	
	public void setHandlerName(String handlerName) {
		this.handlerName = handlerName;
	}
}
