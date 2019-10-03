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

import org.springframework.batch.core.configuration.annotation.AbstractBatchConfiguration;
import org.springframework.batch.core.configuration.annotation.BatchConfigurer;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.explore.support.MapJobExplorerFactoryBean;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.MapJobRepositoryFactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.Assert;

@Configuration
public class PeltasInMemoryConfiguration extends AbstractBatchConfiguration
		implements BatchConfigurer, InitializingBean {

	private MapJobRepositoryFactoryBean jobRepositoryFactory;

	@Autowired(required = false)
	private PlatformTransactionManager platformTransactionManager;

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(platformTransactionManager,
				"PlatformTransactionManager is not available for Peltas.io. You should not use @EnableBatchProcessing");
		jobRepositoryFactory = new MapJobRepositoryFactoryBean(this.platformTransactionManager);
	}

	@Override
	public void setImportMetadata(AnnotationMetadata importMetadata) {
		AnnotationAttributes enabled = AnnotationAttributes
				.fromMap(importMetadata.getAnnotationAttributes(EnableBatchProcessing.class.getName(), false));
		Assert.isNull(enabled, "@EnableBatchProcessing is present on importing class " + importMetadata.getClassName()
				+ ". Consider removing it for Peltas.io InMemory Configuration");
	}

	@Bean
	public JobBuilderFactory jobBuilders() throws Exception {
		return new JobBuilderFactory(jobRepository());
	}

	@Bean
	public StepBuilderFactory stepBuilders() throws Exception {
		return new StepBuilderFactory(jobRepository(), platformTransactionManager);
	}

	@Bean
	public JobRepository jobRepository() throws Exception {
		return this.jobRepositoryFactory.getObject();
	}

	@Bean
	public JobLauncher jobLauncher() throws Exception {
		SimpleJobLauncher launcher = new SimpleJobLauncher();
		launcher.setJobRepository(jobRepository());
		launcher.setTaskExecutor(new SyncTaskExecutor());
		return launcher;
	}

	@Bean
	public JobExplorer jobExplorer() throws Exception {
		return new MapJobExplorerFactoryBean(this.jobRepositoryFactory).getObject();
	}

	@ConditionalOnBean(PlatformTransactionManager.class)
	@Bean
	public PlatformTransactionManager transactionManager() throws Exception {
		return platformTransactionManager;
	}

	@Override
	public JobRepository getJobRepository() throws Exception {
		return jobRepository();
	}

	@Override
	public PlatformTransactionManager getTransactionManager() throws Exception {
		return this.jobRepositoryFactory.getTransactionManager();
	}

	@Override
	public JobLauncher getJobLauncher() throws Exception {
		return jobLauncher();
	}

	@Override
	public JobExplorer getJobExplorer() throws Exception {
		return jobExplorer();
	}
}
