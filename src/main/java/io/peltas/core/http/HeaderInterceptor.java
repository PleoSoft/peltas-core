package io.peltas.core.http;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

public class HeaderInterceptor implements ClientHttpRequestInterceptor {

	private final String key;
	private final String value;

	public HeaderInterceptor(String key, String value) {
		this.key = key;
		this.value = value;
	}

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
			throws IOException {

		HttpHeaders headers = request.getHeaders();
		if (!headers.containsKey(key)) {
			headers.set(key, value);
		}
		return execution.execute(request, body);
	}

}
