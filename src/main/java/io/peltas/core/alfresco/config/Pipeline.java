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

import java.util.List;
import java.util.Map;

public class Pipeline {

	private List<String> executions;
	private String writer;
	private Map<String, PipelineCollection> collections;

	public Map<String, PipelineCollection> getCollections() {
		return collections;
	}

	public void setCollections(Map<String, PipelineCollection> collections) {
		this.collections = collections;
	}

	public PipelineCollection getCollection(String collectionKey) {
		return collections.get(collectionKey);
	}

	public List<String> getExecutions() {
		return executions;
	}

	public void setExecutions(List<String> executions) {
		this.executions = executions;
	}

	public String getWriter() {
		return writer;
	}

	public void setWriter(String writer) {
		this.writer = writer;
	}
}
