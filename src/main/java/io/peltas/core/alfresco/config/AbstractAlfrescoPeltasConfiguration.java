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

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
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
import io.peltas.core.batch.ItemRouter;
import io.peltas.core.batch.PeltasDataHolder;
import io.peltas.core.batch.PeltasItemProcessor;
import io.peltas.core.batch.PeltasProcessor;
import io.peltas.core.config.AbstractPeltasBatchConfiguration;
import io.peltas.core.repository.TxDataRepository;

// @Aspect FIXME: check pointcut for stopping
public abstract class AbstractAlfrescoPeltasConfiguration
		extends AbstractPeltasBatchConfiguration<PeltasEntry, PeltasDataHolder> {

	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractAlfrescoPeltasConfiguration.class);

	public static final String AUDIT_ID_SEPARATOR = "___";

	@Autowired
	protected GenericMessagingTemplate messagingTemplate;

	@Autowired
	private JobLauncher jobLauncher;

	@Value("${peltas.chunksize}")
	protected Integer chunkSize;

	@Autowired
	protected TxDataRepository dataRepository;

	public String alfrescoAuditApplication() {
		return alfrescoAuditProperties().getApplication();
	}

	@Override
	public PeltasItemProcessor<PeltasEntry, PeltasDataHolder> processor() {
		return new PeltasProcessor(alfrescoAuditApplication(), messagingTemplate, dataRepository);
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

	@Override
	public ItemRouter<PeltasEntry> router() {
		return new PeltasRouter(alfrescoHandlerProperties(), alfrescoAuditProperties(), false);
	}

//	@Override
//	public PeltasItemWriter<?, ?> writer() {
//
//		Classifier<PeltasDataHolder, PeltasItemWriter<I, C>> classifier1 = new Classifier<PeltasDataHolder, PeltasItemWriter<I, C>>() {
//			private static final long serialVersionUID = 4361778429527541028L;
//
//			@Override
//			public PeltasItemWriter<?, ?> classify(final PeltasDataHolder classifiable) {
//				PeltasHandlerProperties config = classifiable.getConfig();
//				List<String> pipeline = config.getPipeline().getExecutions();
//				if (pipeline == null || pipeline.size() == 0) {
//					return new EmptyItemWriter<>();
//				}
//
//				return itemWriter;
//			}
//		};
//
//		return new ClassifierCompositeItemWriterBuilder<PeltasDataHolder>().classifier(classifier1).build();
//	}

	@Bean
	public DoNotProcessHandler doNotProcessHandler() {
		return new DoNotProcessHandler();
	}

	@Override
	protected int getChunkSize() {
		return chunkSize;
	}
}
