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

package io.peltas.core.batch;

import java.lang.reflect.ParameterizedType;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import io.peltas.core.PeltasEntry;

public abstract class AbstractPeltasRestReader<R> extends PeltasItemReader {

	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractPeltasRestReader.class);

	protected final RestTemplate restTemplate;

	public AbstractPeltasRestReader(final String applicationName, final RestTemplate restTemplate) {
		super(applicationName);
		this.restTemplate = restTemplate;
	}

	@Override
	public PeltasEntry read() throws Exception, UnexpectedInputException, ParseException {
		PeltasEntry read = super.read();
		if (read == null) {
			read = doRetryRead();
		}
		return read;
	}

	protected PeltasEntry doRetryRead() throws Exception {
		return null;
	}

	@Override
	protected void onOpen() {
		@SuppressWarnings("unchecked")
		Class<R> responseClass = (Class<R>) ((ParameterizedType) getClass().getGenericSuperclass())
				.getActualTypeArguments()[0];

		String queryString = getQueryString();
		HttpMethod httpMethod = getHttpMethod();
		HttpEntity<?> httpEntity = getHttpEntity();
		LOGGER.trace("METHOD: {} - QUERY: {} - ENTITY {}", httpMethod.name(), queryString, httpEntity);
		ResponseEntity<R> response = restTemplate.exchange(queryString, httpMethod, httpEntity, responseClass);
		R entries = response.getBody();
		entries = onResponseReceived(entries);

		List<PeltasEntry> collection = retreiveCollection(entries);
		setList(collection);
	}

	abstract protected List<PeltasEntry> retreiveCollection(R response);

	protected R onResponseReceived(R auditEntries) {
		return auditEntries;
	}

	abstract protected String getQueryString();

	protected HttpEntity<?> getHttpEntity() {
		return new HttpEntity<>("");
	}

	protected HttpMethod getHttpMethod() {
		return HttpMethod.GET;
	}

	public RestTemplate getRestTemplate() {
		return restTemplate;
	}
}
