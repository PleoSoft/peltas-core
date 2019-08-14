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

package io.peltas.core.rest;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

public class HeaderRequestInterceptor implements ClientHttpRequestInterceptor {

	private final Map<String, String> headers;

	public HeaderRequestInterceptor(Map<String, String> headers) {
		this.headers = headers;
	}

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
			throws IOException {
		for (Entry<String, String> entry : headers.entrySet()) {
			request.getHeaders().set(entry.getKey(), entry.getValue());
		}
		return execution.execute(request, body);
	}

}
