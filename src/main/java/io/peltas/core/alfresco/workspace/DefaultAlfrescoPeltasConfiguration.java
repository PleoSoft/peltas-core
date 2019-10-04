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

package io.peltas.core.alfresco.workspace;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.support.BasicAuthorizationInterceptor;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.peltas.core.alfresco.PeltasEntry;
import io.peltas.core.alfresco.config.AbstractAlfrescoPeltasConfiguration;
import io.peltas.core.alfresco.config.PeltasProperties;
import io.peltas.core.alfresco.config.PeltasProperties.Authentication.BasicAuth;
import io.peltas.core.batch.AbstractPeltasRestReader;

@Configuration
public class DefaultAlfrescoPeltasConfiguration extends AbstractAlfrescoPeltasConfiguration {

	@Bean
	public RestTemplate restTemplate() {
		RestTemplate restTemplate = new RestTemplate();

		BasicAuth basicAuth = alfrescoAuditProperties().getAuth().getBasic();
		restTemplate.getInterceptors()
				.add(new BasicAuthorizationInterceptor(basicAuth.getUsername(), basicAuth.getPassword()));
		restTemplate.getMessageConverters().add(0, mappingJacksonHttpMessageConverter());
		return restTemplate;
	}

	@Override
	public AbstractPeltasRestReader<PeltasEntry, AlfrescoWorkspaceNodes> reader() {
		RestTemplate restTemplate = restTemplate();

		PeltasProperties properties = alfrescoAuditProperties();
		return new AlfrescoWorkspaceRestReader(restTemplate, properties, dataRepository);
	}

	private MappingJackson2HttpMessageConverter mappingJacksonHttpMessageConverter() {
		MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
		converter.setObjectMapper(objectMapper());
		return converter;
	}

	private ObjectMapper objectMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		return mapper;
	}
}
