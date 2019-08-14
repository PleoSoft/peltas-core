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

package io.peltas.core.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.messaging.core.GenericMessagingTemplate;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import io.peltas.core.repository.PeltasTimestamp;
import io.peltas.core.repository.PeltasTimestampRepository;

@Configuration
@EnableJpaRepositories(basePackageClasses = PeltasTimestampRepository.class)
@EntityScan(basePackageClasses = PeltasTimestamp.class)
@EnableIntegration
@EnableTransactionManagement
public class BasicPeltasConfiguration {

	@Bean
	public PeltasDatasourceProperties peltasDatasourceProperties() {
		return new PeltasDatasourceProperties();
	}

	@Bean
	public PeltasDatasourceInitializer peltasDatasourceInitializer(DataSource dataSource,
			ResourceLoader resourceLoader) {
		return new PeltasDatasourceInitializer(dataSource, resourceLoader, peltasDatasourceProperties());
	}

	@Bean
	public CustomDatasourceProperties customDatasourceProperties() {
		return new CustomDatasourceProperties();
	}

	@Bean
	@ConditionalOnProperty("peltas.custom.datasource.enabled")
	public CustomDatasourceInitializer customDatasourceInitializer(DataSource dataSource,
			ResourceLoader resourceLoader) {
		return new CustomDatasourceInitializer(dataSource, resourceLoader, customDatasourceProperties());
	}

	@Bean
	public GenericMessagingTemplate messagingTemplate(BeanFactory beanFactory) {
		GenericMessagingTemplate template = new GenericMessagingTemplate();
		template.setBeanFactory(beanFactory);
		return template;
	}
}
