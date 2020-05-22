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

package io.peltas.boot;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.sql.DataSource;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.support.MapJobRegistry;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.explore.support.AbstractJobExplorerFactoryBean;
import org.springframework.batch.core.explore.support.MapJobExplorerFactoryBean;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.AbstractJobRepositoryFactoryBean;
import org.springframework.batch.core.repository.support.MapJobRepositoryFactoryBean;
import org.springframework.batch.core.step.builder.SimpleStepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.batch.BasicBatchConfigurer;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.batch.BatchProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionManagerCustomizers;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.peltas.alfresco.AlfrescoWorkspaceRestReader;
import io.peltas.boot.PeltasProperties.Authentication.BasicAuth;
import io.peltas.boot.PeltasProperties.Authentication.Header;
import io.peltas.boot.PeltasProperties.Authentication.X509;
import io.peltas.boot.PeltasProperties.Ssl;
import io.peltas.boot.PeltasProperties.Ssl.KeystoreType;
import io.peltas.core.PeltasEntry;
import io.peltas.core.PeltasException;
import io.peltas.core.StringToMapUtil;
import io.peltas.core.batch.PeltasDataHolder;
import io.peltas.core.batch.PeltasItemProcessor;
import io.peltas.core.batch.PeltasListenerAdapter;
import io.peltas.core.batch.PeltasProcessor;
import io.peltas.core.expression.ContainsExpressionEvaluator;
import io.peltas.core.expression.ContainsNotExpressionEvaluator;
import io.peltas.core.expression.ContainsNotPrefixStringExpressionEvaluator;
import io.peltas.core.expression.ContainsPrefixStringExpressionEvaluator;
import io.peltas.core.expression.EqualsExpressionEvaluator;
import io.peltas.core.expression.EvaluatorExpressionRegistry;
import io.peltas.core.http.HeaderInterceptor;
import io.peltas.core.http.TicketBasicAuthorizationInterceptor;
import io.peltas.core.integration.DoNotProcessHandler;
import io.peltas.core.integration.PeltasEntryHandler;
import io.peltas.core.integration.PeltasFormatUtil;
import io.peltas.core.repository.TxDataRepository;

// @Aspect FIXME: check pointcut for stopping
@Configuration
@PropertySource(ignoreResourceNotFound = true, value = { "classpath:/io/peltas/peltas-alfresco.properties" })
@EnableConfigurationProperties({ PeltasProperties.class, PeltasHandlerConfigurationProperties.class })
@AutoConfigureBefore({ BatchAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class })
public class PeltasBatchConfiguration extends BasicBatchConfigurer {

	private static final Logger LOGGER = LoggerFactory.getLogger(PeltasBatchConfiguration.class);

	@Value("${peltas.chunksize}")
	protected Integer chunkSize;

	protected final TxDataRepository dataRepository;

	private AbstractJobRepositoryFactoryBean jobRepositoryFactory;
	private AbstractJobExplorerFactoryBean jobExplorerFactory;

	private PlatformTransactionManager transactionManager;

	protected PeltasBatchConfiguration(BatchProperties properties, DataSource dataSource,
			TransactionManagerCustomizers transactionManagerCustomizers, PlatformTransactionManager transactionManager,
			TxDataRepository dataRepository) {
		super(properties, dataSource, transactionManagerCustomizers);

		this.transactionManager = transactionManager;
		this.dataRepository = dataRepository;
	}

	protected AbstractJobRepositoryFactoryBean createJobRepositoryFactory() throws Exception {
		return null;
	}

	protected AbstractJobExplorerFactoryBean createJobExplorerFactory() throws Exception {
		return null;
	}

	public AbstractJobExplorerFactoryBean getJobExplorerFactory() {
		return jobExplorerFactory;
	}

	public AbstractJobRepositoryFactoryBean getJobRepositoryFactory() {
		return jobRepositoryFactory;
	}

	@PostConstruct
	public void initialize() {
		try {
			jobRepositoryFactory = createJobRepositoryFactory();
			jobExplorerFactory = createJobExplorerFactory();

			if (jobRepositoryFactory == null) {
				jobRepositoryFactory = new MapJobRepositoryFactoryBean(transactionManager);
				jobRepositoryFactory.afterPropertiesSet();
			}

			if (jobExplorerFactory == null) {
				jobExplorerFactory = new MapJobExplorerFactoryBean((MapJobRepositoryFactoryBean) jobRepositoryFactory);
				((MapJobExplorerFactoryBean) jobExplorerFactory).afterPropertiesSet();
			}

		} catch (Exception ex) {
			throw new IllegalStateException("Unable to initialize Spring Batch", ex);
		}

		super.initialize();
	}

	@Override
	protected JobExplorer createJobExplorer() throws Exception {
		return getJobExplorerFactory().getObject();
	}

	@Bean
	@ConditionalOnMissingBean
	public JobExplorer jobExplorer() throws Exception {
		return getJobExplorer();
	}

	@Bean
	@ConditionalOnMissingBean
	public JobRegistry jobRegistry() throws Exception {
		return new MapJobRegistry();
	}

	@Override
	protected JobRepository createJobRepository() throws Exception {
		return getJobRepositoryFactory().getObject();
	}

	@Bean
	@ConditionalOnMissingBean
	public JobRepository jobRepository() {
		return getJobRepository();
	}

	@Bean
	@ConditionalOnMissingBean
	public StepBuilderFactory stepBuilders() throws Exception {
		return new StepBuilderFactory(jobRepository(), getTransactionManager());
	}

	@Bean
	@ConditionalOnMissingBean
	public JobBuilderFactory jobBuilders() throws Exception {
		return new JobBuilderFactory(getJobRepository());
	}

	@Bean
	@ConditionalOnMissingBean
	public JobLauncher jobLauncher() throws Exception {
		SimpleJobLauncher launcher = new SimpleJobLauncher();
		launcher.setJobRepository(getJobRepository());
		launcher.setTaskExecutor(new SyncTaskExecutor());
		return launcher;
	}

	@Override
	protected PlatformTransactionManager createTransactionManager() {
		return transactionManager;
	}

	@Bean
	@ConditionalOnMissingBean
	public HttpMessageConverters messageConverters(ObjectProvider<HttpMessageConverter<?>> converters) {
		return new HttpMessageConverters(converters.orderedStream().collect(Collectors.toList()));
	}

	@Bean
	// @ConditionalOnMissingBean(MappingJackson2HttpMessageConverter.class)
	public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
		MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
		ObjectMapper mapper = new ObjectMapper();
		mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		converter.setObjectMapper(mapper);

		// getMessageConverters().add(0, converter);
		return converter;
	}

	@Bean
	@ConditionalOnMissingBean(RestOperations.class)
	@ConditionalOnProperty(value = "peltas.authenticationType", havingValue = "basicauth", matchIfMissing = false)
	public RestTemplate restTemplateBasicAuth(PeltasProperties properties, HttpMessageConverters converters) {
		RestTemplate restTemplate = new RestTemplate();

		BasicAuth basicAuth = properties.getAuth().getBasic();
		restTemplate.getInterceptors()
				.add(new BasicAuthenticationInterceptor(basicAuth.getUsername(), basicAuth.getPassword()));

		restTemplate.setMessageConverters(converters.getConverters());
		return restTemplate;
	}

	@Bean
	@ConditionalOnMissingBean(RestOperations.class)
	@ConditionalOnProperty(value = "peltas.authenticationType", havingValue = "header", matchIfMissing = false)
	public RestTemplate restTemplateHeader(PeltasProperties properties, HttpMessageConverters converters) {
		RestTemplate restTemplate = new RestTemplate();

		Header header = properties.getAuth().getHeader();
		restTemplate.getInterceptors().add(new HeaderInterceptor(header.getKey(), header.getValue()));

		restTemplate.setMessageConverters(converters.getConverters());
		return restTemplate;
	}

	@Bean
	@ConditionalOnMissingBean(RestOperations.class)
	@ConditionalOnProperty(value = "peltas.authenticationType", havingValue = "headerssl", matchIfMissing = false)
	public RestTemplate restTemplateHeaderSsl(PeltasProperties properties, HttpMessageConverters converters)
			throws Throwable {

		Ssl ssl = properties.getSsl();
		SSLContext sslContext = setupSslContext(ssl).build();

		RestTemplate restTemplate = createSslRestTemplate(sslContext, ssl);

		Header header = properties.getAuth().getHeader();
		restTemplate.getInterceptors().add(new HeaderInterceptor(header.getKey(), header.getValue()));

		restTemplate.setMessageConverters(converters.getConverters());
		return restTemplate;
	}

	@Bean
	@ConditionalOnMissingBean(RestOperations.class)
	@ConditionalOnProperty(value = "peltas.authenticationType", havingValue = "alfrescoticketssl", matchIfMissing = false)
	public RestTemplate restTemplateBasicAuthSsl(PeltasProperties properties, HttpMessageConverters converters)
			throws Throwable {

		Ssl ssl = properties.getSsl();
		SSLContext sslContext = setupSslContext(ssl).build();

		RestTemplate restTemplate = createSslRestTemplate(sslContext, ssl);

		BasicAuth basicAuth = properties.getAuth().getBasic();
		String loginUrl = UriComponentsBuilder.fromHttpUrl(properties.getHost()).path(properties.getLoginUrl()).build()
				.toString();
		restTemplate.getInterceptors().add(
				new TicketBasicAuthorizationInterceptor(basicAuth.getUsername(), basicAuth.getPassword(), loginUrl));

		restTemplate.setMessageConverters(converters.getConverters());
		return restTemplate;
	}

	@Bean
	@ConditionalOnMissingBean(RestOperations.class)
	@ConditionalOnProperty(value = "peltas.authenticationType", havingValue = "basicauthssl", matchIfMissing = false)
	public RestTemplate restTemplateAlfrescoTicketSsl(PeltasProperties properties, HttpMessageConverters converters)
			throws Throwable {

		Ssl ssl = properties.getSsl();
		SSLContext sslContext = setupSslContext(ssl).build();

		RestTemplate restTemplate = createSslRestTemplate(sslContext, ssl);

		BasicAuth basicAuth = properties.getAuth().getBasic();
		restTemplate.getInterceptors()
				.add(new BasicAuthenticationInterceptor(basicAuth.getUsername(), basicAuth.getPassword()));

		restTemplate.setMessageConverters(converters.getConverters());
		return restTemplate;
	}

	@Bean
	@ConditionalOnMissingBean(RestOperations.class)
	@ConditionalOnProperty(value = "peltas.authenticationType", havingValue = "x509", matchIfMissing = false)
	public RestTemplate restTemplateX509Ssl(PeltasProperties properties, HttpMessageConverters converters)
			throws UnrecoverableKeyException, Exception {

		X509 x509props = properties.getAuth().getX509();
		SSLContextBuilder sslContextBuilder = properties.getSsl() != null ? setupSslContext(properties.getSsl())
				: new SSLContextBuilder();
		SSLContext sslContext = sslContextBuilder.setKeyStoreType(x509props.getKeystoreType().toString())
				.loadKeyMaterial(
						keyStore(x509props.getKeyStore(), x509props.getKeyStorePass(), x509props.getKeystoreType()),
						x509props.getKeyStorePass())
				.build();
		return createSslRestTemplate(sslContext, properties.getSsl());
	}

	private KeyStore keyStore(String file, char[] password, KeystoreType keystoreType) throws Exception {
		KeyStore keyStore = KeyStore.getInstance(keystoreType.toString());
		File key = ResourceUtils.getFile(file);
		try (InputStream in = new FileInputStream(key)) {
			keyStore.load(in, password);
		}
		return keyStore;
	}

	private SSLContextBuilder setupSslContext(Ssl ssl) throws NoSuchAlgorithmException, KeyStoreException,
			CertificateException, MalformedURLException, IOException {
		return new SSLContextBuilder().setKeyStoreType(ssl.getKeystoreType().toString())
				.loadTrustMaterial(new File(ssl.getTrustStore()).toURI().toURL(), ssl.getTrustStorePass());
	}

	private RestTemplate createSslRestTemplate(SSLContext sslContext, Ssl ssl) {
		HostnameVerifier hostnameVerifier = null;
		if (ssl != null && ssl.getHostVerify() == true) {
			hostnameVerifier = new DefaultHostnameVerifier();
		} else {
			hostnameVerifier = new NoopHostnameVerifier();
		}
		SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(sslContext, hostnameVerifier);
		HttpClient httpClient = HttpClients.custom().setSSLSocketFactory(socketFactory).build();
		HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
		return new RestTemplate(factory);
	}

	@Bean
	@ConditionalOnMissingBean(RestOperations.class)
	@ConditionalOnProperty(value = "peltas.authenticationType", havingValue = "alfrescoticket", matchIfMissing = false)
	public RestTemplate restTemplateAlfrescoTicket(PeltasProperties properties, HttpMessageConverters converters) {
		RestTemplate restTemplate = new RestTemplate();

		BasicAuth basicAuth = properties.getAuth().getBasic();
		String loginUrl = UriComponentsBuilder.fromHttpUrl(properties.getHost()).path(properties.getLoginUrl()).build()
				.toString();
		restTemplate.getInterceptors().add(
				new TicketBasicAuthorizationInterceptor(basicAuth.getUsername(), basicAuth.getPassword(), loginUrl));

		restTemplate.setMessageConverters(converters.getConverters());
		return restTemplate;
	}

	@Bean
	@ConditionalOnMissingBean(ItemReader.class)
	public ItemReader<PeltasEntry> reader(RestTemplate restTemplate, PeltasProperties properties) {
		return new AlfrescoWorkspaceRestReader(restTemplate, properties, dataRepository, properties.getApplication());
	}

	@Bean
	@ConditionalOnMissingBean(PeltasListenerAdapter.class)
	public PeltasListenerAdapter peltasListenerAdapter() {
		return new PeltasListenerAdapter();
	}

	@Bean
	@ConditionalOnMissingBean(PeltasItemProcessor.class)
	public PeltasItemProcessor processor(PeltasProperties properties, List<Converter<?, ?>> converters,
			PeltasFormatUtil peltasFormatUtil, FormatterRegistry formatterRegistry,
			@Qualifier("integrationConversionService") ConversionService conversionService,
			@Qualifier("mvcConversionService") ConversionService conversionService2,
			PeltasHandlerConfigurationProperties handlerProperties, DoNotProcessHandler doNotProcessHandler,
			PeltasListenerAdapter peltasListenerAdapter) {
		return new PeltasProcessor(properties.getApplication(), dataRepository,
				new PeltasEntryHandler(converters, peltasFormatUtil), handlerProperties, false, doNotProcessHandler,
				peltasListenerAdapter);
	}

	@Bean
	@ConditionalOnMissingBean(EvaluatorExpressionRegistry.class)
	public EvaluatorExpressionRegistry evaluatorExpressionRegistry() {
		EvaluatorExpressionRegistry registry = new EvaluatorExpressionRegistry(new EqualsExpressionEvaluator());
		registry.registerEvaluator(new ContainsExpressionEvaluator());
		registry.registerEvaluator(new ContainsNotExpressionEvaluator());
		registry.registerEvaluator(new ContainsPrefixStringExpressionEvaluator());
		registry.registerEvaluator(new ContainsNotPrefixStringExpressionEvaluator());

		return registry;
	}

	@Bean
	public Converter<?, ?> stringToMapConverter() {
		return new Converter<String, HashMap<String, Object>>() {
			@Override
			public HashMap<String, Object> convert(String source) {
				LOGGER.trace("converting String -> HashMap: {}", source);

				final HashMap<String, Object> map = new HashMap<>();
				if (StringUtils.hasText(source)) {
					final Map<String, Object> stringToMap = StringToMapUtil.stringToMap(source, ',');
					map.putAll(stringToMap);
				}

				return map;
			}
		};
	}

	// TODO FIXME revisit this
	@AfterThrowing(value = "(execution(* io.peltas.alfresco.access..*(..)))", throwing = "e")
	public void logException(JoinPoint thisJoinPoint, PeltasException e) {
		LOGGER.error("exiting Peltas", e);
		System.exit(-1);
	}

	@Bean
	@ConditionalOnMissingBean(DoNotProcessHandler.class)
	public DoNotProcessHandler doNotProcessHandler() {
		return new DoNotProcessHandler();
	}

	@Bean
	@ConditionalOnMissingBean
	public Job job(Step step, JobBuilderFactory jobBuilderFactory, JobRepository jobRepository) throws Exception {
		return jobBuilderFactory.get("peltas").repository(jobRepository).start(step).build();
	}

	@Bean
	@ConditionalOnMissingBean
	public Step step(JobRepository jobRepository, StepBuilderFactory stepBuilderFactory,
			PlatformTransactionManager transactionManager, ItemWriter<PeltasDataHolder> peltasWriter,
			PeltasItemProcessor peltasProcessor, PeltasListenerAdapter peltasListener, ItemReader<PeltasEntry> reader,
			PeltasProperties properties) throws Exception {
		SimpleStepBuilder<PeltasEntry, PeltasDataHolder> builder = stepBuilderFactory.get("peltas")
				.<PeltasEntry, PeltasDataHolder>chunk(properties.getChunksize()).reader(reader)
				.processor(peltasProcessor).writer(peltasWriter);

		return builder.repository(jobRepository).transactionManager(transactionManager).build();
	}

	@Bean
	public PeltasFormatUtil peltasFormatUtil() {
		return new PeltasFormatUtil();
	}
}
