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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.FileReader;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.web.client.RestTemplate;

import io.peltas.core.PeltasEntry;

//@TestPropertySource(locations = "classpath:application.properties")
public class AbstractPeltasRestReaderTest {

	private MockRestServiceServer mockServer;

	// private PeltasTimestampRepository auditRepository =
	// mock(PeltasTimestampRepository.class);

	private AbstractPeltasRestReader<TestDataResponse> reader;

	@BeforeEach
	public void setup() {
		final RestTemplate restTemplate = new RestTemplate();
		mockServer = MockRestServiceServer.createServer(restTemplate);

		reader = new AbstractPeltasRestReader<TestDataResponse>("test", restTemplate) {

			@Override
			protected List<PeltasEntry> retreiveCollection(TestDataResponse response) {
				return response.getData();
			}

			@Override
			protected String getQueryString() {
				return "testdata";
			}
		};
	}

	@Test
	public void start() throws Exception {
		String json = FileCopyUtils
				.copyToString(new FileReader(ResourceUtils.getFile("classpath:json/test-data.json")));

		mockServer.expect(MockRestRequestMatchers.requestTo("/testdata"))
				.andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
				.andRespond(MockRestResponseCreators.withSuccess(json, MediaType.APPLICATION_JSON));

		ExecutionContext executionContext = new ExecutionContext();
		reader.open(executionContext);

		PeltasEntry read = reader.read();
		assertEquals("a", read.getId());
		assertEquals("b", read.getApplication());
		read = reader.read();
		assertNull(read);
	}
}
