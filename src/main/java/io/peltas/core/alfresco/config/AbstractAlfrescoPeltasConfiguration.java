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
import java.util.List;

import javax.sql.DataSource;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.builder.ClassifierCompositeItemWriterBuilder;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.classify.Classifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.messaging.core.GenericMessagingTemplate;

import io.peltas.core.alfresco.PeltasEntry;
import io.peltas.core.alfresco.PeltasException;
import io.peltas.core.alfresco.PeltasScheduler;
import io.peltas.core.alfresco.config.expression.ContainsExpressionEvaluator;
import io.peltas.core.alfresco.config.expression.EqualsExpressionEvaluator;
import io.peltas.core.alfresco.config.expression.EvaluatorExpressionRegistry;
import io.peltas.core.alfresco.integration.DoNotProcessHandler;
import io.peltas.core.alfresco.integration.PeltasHandler;
import io.peltas.core.alfresco.integration.PeltasRouter;
import io.peltas.core.batch.EmptyItemWriter;
import io.peltas.core.batch.ItemRouter;
import io.peltas.core.batch.PeltasDataHolder;
import io.peltas.core.batch.PeltasJdbcBatchWriter;
import io.peltas.core.config.AbstractPeltasConfiguration;
import io.peltas.core.config.EnablePeltasInMemory;
import io.peltas.core.repository.PeltasTimestampRepository;

@PropertySource(ignoreResourceNotFound = true, value = { "classpath:io/peltas/peltas.properties" })
// @Aspect
@EnablePeltasInMemory
public abstract class AbstractAlfrescoPeltasConfiguration
		extends AbstractPeltasConfiguration<PeltasEntry, PeltasDataHolder> implements InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractAlfrescoPeltasConfiguration.class);

	public static final String AUDIT_ID_SEPARATOR = "___";

	@Autowired
	protected GenericMessagingTemplate template;

	@Autowired
	protected PeltasTimestampRepository auditRepository;

	@Autowired
	protected JdbcTemplate jdbcTemplate;

	@Autowired
	private JobLauncher jobLauncher;

	@Value("${peltas.chunksize}")
	protected Integer chunkSize;

	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	private PeltasJdbcBatchWriter peltasJdbcBatchWriter;

	@Override
	public void afterPropertiesSet() throws Exception {
		namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
	}

	public String alfrescoAuditApplication() {
		return alfrescoAuditProperties().getApplication();
	}

	@Bean
	public EvaluatorExpressionRegistry evaluatorExpressionRegistry() {
		EqualsExpressionEvaluator equalsExpressionEvaluator = new EqualsExpressionEvaluator();

		EvaluatorExpressionRegistry registry = new EvaluatorExpressionRegistry(equalsExpressionEvaluator);
		registry.registerEvaluator(new ContainsExpressionEvaluator());
		registry.registerEvaluator(equalsExpressionEvaluator);

		return registry;
	}

	@Bean(initMethod = "checkEvaluatorConfigurationOccurencies")
	public PeltasHandlerConfigurationProperties alfrescoHandlerProperties() {
		return new PeltasHandlerConfigurationProperties(evaluatorExpressionRegistry());
	}

	@Bean
	public PeltasProperties alfrescoAuditProperties() {
		return new PeltasProperties();
	}

	@Bean
	public PeltasHandler auditProcessorHandler() {
		return new PeltasHandler();
	}

	@Bean
	@ConditionalOnProperty(value = "peltas.scheduler.enabled", matchIfMissing = true)
	public Object scheduler() throws IOException, Exception {
		return new PeltasScheduler(jobLauncher, job());
	}

	@AfterThrowing(value = "(execution(* io.peltas.alfresco.access..*(..)))", throwing = "e")
	public void logException(JoinPoint thisJoinPoint, PeltasException e) {
		LOGGER.error("exiting Peltas", e);
		System.exit(-1);
	}

	@Bean
	public PeltasDatasourceProperties alfrescoAccessDatasourceProperties() {
		return new PeltasDatasourceProperties();
	}

	@Bean
	public PeltasDatasourceInitializer alfrescoAccessDatasourceInitializer(DataSource dataSource,
			ResourceLoader resourceLoader) {
		return new PeltasDatasourceInitializer(dataSource, resourceLoader, alfrescoAccessDatasourceProperties());
	}

	@Override
	public ItemRouter<PeltasEntry> router() {
		return new PeltasRouter(alfrescoHandlerProperties(), alfrescoAuditProperties(), false);
	}

	// @Override
	// public PeltasItemProcessor<AlfrescoAuditEntry, PeltasDataHolder>
	// processor() {
	// try {
	// return new PeltasProcessor(alfrescoAuditApplication(), template,
	// auditRepository, licenseChecker());
	// } catch (IOException e) {
	// throw new PeltasException("Cannot load license", e);
	// }
	// }

	@Override
	public ItemWriter<PeltasDataHolder> writer() {
		final PeltasHandlerConfigurationProperties alfrescoHandlerProperties = alfrescoHandlerProperties();

		Classifier<PeltasDataHolder, ItemWriter<? super PeltasDataHolder>> classifier1 = new Classifier<PeltasDataHolder, ItemWriter<? super PeltasDataHolder>>() {
			@Override
			public ItemWriter<? super PeltasDataHolder> classify(final PeltasDataHolder classifiable) {
				String writerName = classifiable.getConfig().getPipeline().getWriter();

				if ("jdbchandler".equals(writerName)) {
					PeltasHandlerProperties config = classifiable.getConfig();
					List<String> pipeline = config.getPipeline().getExecutions();
					if (pipeline == null || pipeline.size() == 0) {
						return new EmptyItemWriter<>();
					}

					if (peltasJdbcBatchWriter == null) {
						peltasJdbcBatchWriter = new PeltasJdbcBatchWriter(namedParameterJdbcTemplate,
								alfrescoHandlerProperties);
					}
					return peltasJdbcBatchWriter;
				}

				throw new RuntimeException("Writer not handled: " + writerName);
			}
		};

		return new ClassifierCompositeItemWriterBuilder<PeltasDataHolder>().classifier(classifier1).build();
	}

	@Bean
	public DoNotProcessHandler doNotProcessHandler() {
		return new DoNotProcessHandler();
	}

	@Override
	protected int getChunkSize() {
		return chunkSize;
	}

}
