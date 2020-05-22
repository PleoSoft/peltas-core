package io.peltas.core.http;

import java.io.IOException;
import java.net.URI;
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

	private void authenticate(ClientHttpRequestExecution execution)
			throws IOException, JsonParseException, JsonMappingException {

		LOGGER.trace("authenticate() no ticket found - getting a new one");

		HttpMethod method = HttpMethod.POST;
		String json = "{\"userId\":\"" + username + "\",\"password\":\"" + password + "\"}";

		final URI uri = URI.create(this.loginUrl);
		final ClientHttpRequest authRequest = new HttpComponentsClientHttpRequestFactory().createRequest(uri, method);

		authRequest.getHeaders().set("Accept", "application/json");
		authRequest.getHeaders().set("Content-Type", "application/json");
		try (ClientHttpResponse response = execution.execute(authRequest, json.getBytes())) {
			HashMap<String, Object> map = this.mapper.readValue(response.getBody(),
					new TypeReference<HashMap<String, Object>>() {
					});

			ticket = (String) ((Map<Object, Object>) map.get("entry")).get("id");

			LOGGER.trace("authenticate() new ticket retreived - {}", ticket);
		}
	}
}
