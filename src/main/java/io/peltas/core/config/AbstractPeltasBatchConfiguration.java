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

import java.util.Date;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.SimpleStepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.messaging.Message;
import org.springframework.messaging.core.GenericMessagingTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.util.StringUtils;

import io.peltas.core.batch.ItemRouter;
import io.peltas.core.batch.PeltasItemProcessor;
import io.peltas.core.batch.PeltasListener;
import io.peltas.core.repository.database.CustomDatasourceInitializer;
import io.peltas.core.repository.database.CustomDatasourceProperties;
import io.peltas.core.repository.database.PeltasTimestamp;

@Configuration
@EnableIntegration
@EnableTransactionManagement
public abstract class AbstractPeltasBatchConfiguration<I, O> {

	@Autowired
	private JobLauncher jobLauncher;

	@Autowired
	private JobRepository jobRepository;

	@Autowired
	private JobBuilderFactory jobBuilderFactory;

	@Autowired
	private StepBuilderFactory stepBuilderFactory;

	@Autowired
	private PlatformTransactionManager platformTransactionManager;

	@Autowired
	private ItemWriter<O> writer;

	@Bean
	public GenericMessagingTemplate messagingTemplate(BeanFactory beanFactory) {
		GenericMessagingTemplate template = new GenericMessagingTemplate();
		template.setBeanFactory(beanFactory);
		return template;
	}

	@Bean
	public ItemRouter<I> router() {
		return new ItemRouter<I>() {
			@Override
			public String handleMessage(Message<I> message) {
				return "starthandler";
			}
		};
	}

	@Bean
	abstract public ItemReader<I> reader();

	@Bean
	abstract public PeltasItemProcessor<I, O> processor();

	@Bean
	public PeltasListener<I, O> listener() {
		return processor();
	}

	protected PeltasTimestamp peltasTimestampEntry(I entry) {
		return new PeltasTimestamp(getApplicationName(), null, new Date());
	}

	public String getApplicationName() {
		String[] split = StringUtils.split(this.getClass().getSimpleName(), "$");
		return split[0];
	}

	@Bean
	public Job job() throws Exception {
		return jobBuilderFactory.get("peltas.entry").repository(jobRepository).start(
				step(jobRepository, stepBuilderFactory, platformTransactionManager, writer, processor(), listener()))
				.build();
	}

	public Step step(JobRepository jobRepository, StepBuilderFactory stepBuilderFactory,
			PlatformTransactionManager transactionManager, ItemWriter<O> peltasWriter,
			PeltasItemProcessor<I, O> peltasProcessor, PeltasListener<I, O> peltasListener) throws Exception {
		SimpleStepBuilder<I, O> builder = stepBuilderFactory.get("peltas.entry").<I, O>chunk(getChunkSize())
				.reader(reader()).processor(peltasProcessor).writer(peltasWriter);

		return builder.repository(jobRepository).transactionManager(transactionManager).build();
	}

	@Bean
	public IntegrationFlow flow() {
		ItemRouter<I> actionRouter = router();
		return IntegrationFlows.from("peltas.entry").route(actionRouter, "handleMessage").get();
	}

	protected int getChunkSize() {
		return 1;
	}

	protected JobParameters getJobParameters() {
		return new JobParametersBuilder().addLong("auditId", System.currentTimeMillis()).toJobParameters();
	}

	protected JobExecution startJob(JobParameters jobParameters, JobRepository jobRepository,
			JobBuilderFactory jobBuilderFactory, StepBuilderFactory stepBuilderFactory,
			PlatformTransactionManager platformTransactionManager) {
		try {
			JobExecution run = jobLauncher.run(job(), jobParameters);
			return run;
		} catch (Throwable e) {
			System.exit(-1);
		}

		return null;
	}

	public JobExecution launchJob() {
		JobExecution run = startJob(getJobParameters(), jobRepository, jobBuilderFactory, stepBuilderFactory,
				platformTransactionManager);
		return run;
	}

	@Configuration
	@ConditionalOnProperty(name = "peltas.custom.datasource.enabled", havingValue = "true")
	public class CustomDatasourceConfiguration {

		@Bean
		public CustomDatasourceProperties peltasCustomDatasourceProperties() {
			return new CustomDatasourceProperties();
		}

		@Bean
		public CustomDatasourceInitializer peltasCustomDatasourceInitializer(DataSource dataSource,
				ResourceLoader resourceLoader) {
			return new CustomDatasourceInitializer(dataSource, resourceLoader, peltasCustomDatasourceProperties());
		}
	}
}
