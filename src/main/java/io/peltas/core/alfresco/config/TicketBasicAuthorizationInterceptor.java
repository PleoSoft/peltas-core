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
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.Base64Utils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TicketBasicAuthorizationInterceptor implements ClientHttpRequestInterceptor {

	private static final Logger LOGGER = LoggerFactory.getLogger(TicketBasicAuthorizationInterceptor.class);

	private final String username;
	private final String password;
	private final ObjectMapper mapper;
	private final String loginUrl;

	private String ticket;

	public TicketBasicAuthorizationInterceptor(@Nullable String username, @Nullable String password, String loginUrl) {
		Assert.doesNotContain(username, ":", "Username must not contain a colon");
		this.username = (username != null ? username : "");
		this.password = (password != null ? password : "");

		this.loginUrl = loginUrl;
		this.mapper = new ObjectMapper();
	}

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
			throws IOException {

		if (this.ticket == null) {
			authenticate(execution);
		}

		String token = Base64Utils.encodeToString((this.ticket).getBytes(StandardCharsets.UTF_8));
		request.getHeaders().add("Authorization", "Basic " + token);

		ClientHttpResponse response = execution.execute(request, body);

		final HttpStatus httpStatus = response.getStatusCode();
		if (HttpStatus.UNAUTHORIZED.equals(httpStatus)) {
			authenticate(execution);
		}

		return response;
	}

	@SuppressWarnings("unchecked")
	private void authenticate(ClientHttpRequestExecution execution)
			throws IOException, JsonParseException, JsonMappingException {

		LOGGER.trace("authenticate() no ticket found - getting a new one");

		ClientHttpResponse response = null;
		try {
			HttpMethod method = HttpMethod.GET;
			String json = "";

			String uriString = this.loginUrl;
			if (!this.loginUrl.endsWith("/api/login")) {
				method = HttpMethod.POST;
			}

			if (HttpMethod.GET.equals(method)) {
				// legacy
				uriString = uriString + "?u=" + username + "&pw=" + password + "&format=json";
			} else {
				// from 5.2.2
				json = "{\"userId\":\"" + username + "\",\"password\":\"" + password + "\"}";
			}

			final URI uri = new URI(uriString);
			final ClientHttpRequest authRequest = new HttpComponentsClientHttpRequestFactory().createRequest(uri,
					method);

			authRequest.getHeaders().set("Accept", "application/json");
			response = execution.execute(authRequest, json.getBytes());

			HashMap<String, Object> map = this.mapper.readValue(response.getBody(),
					new TypeReference<HashMap<String, Object>>() {
					});

			if (HttpMethod.GET.equals(method)) {
				// legacy
				ticket = (String) ((Map<Object, Object>) map.get("data")).get("ticket");
			} else {
				// from 5.2.2
				ticket = (String) ((Map<Object, Object>) map.get("entry")).get("id");
			}

			LOGGER.trace("authenticate() new ticket retreived - {}", ticket);

		} catch (final URISyntaxException e) {
			throw new IllegalStateException(e);
		} finally {
			if (response != null) {
				response.close();
			}
		}
	}
}
