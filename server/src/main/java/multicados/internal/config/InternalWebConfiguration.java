/**
 *
 */
package multicados.internal.config;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executor;

import javax.sql.DataSource;

import org.hibernate.FlushMode;
import org.hibernate.SessionFactory;
import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.cfg.AvailableSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module;

import multicados.internal.file.engine.image.ImageService;
import multicados.internal.helper.SpringHelper;
import multicados.service.domain.customer.CredentialResetService;

/**
 * @author Ngoc Huy
 *
 */
@ComponentScan(Settings.BASE_PACKAGE)
@Configuration
@EnableTransactionManagement
@EnableWebMvc
@EnableSpringDataWebSupport
@EnableAsync
@EnableScheduling
@EnableCaching(proxyTargetClass = true)
@Import(DomainLogicContextConfiguration.class)
public class InternalWebConfiguration implements WebMvcConfigurer {

	private static final Logger logger = LoggerFactory.getLogger(InternalWebConfiguration.class);

	@Bean
	public FactoryBean<SessionFactory> sessionFactory(DataSource dataSource, Environment env) throws Exception {
		logger.info("Creating {} bean", LocalSessionFactoryBean.class.getName());

		final LocalSessionFactoryBean sessionFactory = new LocalSessionFactoryBean();

		sessionFactory.setDataSource(dataSource);
		sessionFactory.setPackagesToScan(new String[] { env.getProperty(Settings.SCANNED_ENTITY_PACKAGES) });
		// snake_case for columns
		sessionFactory.setPhysicalNamingStrategy(new CamelCaseToUnderscoresNamingStrategy());

		final Properties properties = new Properties();

		properties.put(AvailableSettings.DIALECT, "org.hibernate.dialect.MySQL8Dialect");
		properties.put(AvailableSettings.SHOW_SQL, true);
		properties.put(AvailableSettings.FORMAT_SQL, true);
		properties.put(AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS, "true");
		properties.put(AvailableSettings.HBM2DDL_AUTO, "update");
		properties.put(AvailableSettings.STATEMENT_BATCH_SIZE, 50);
		properties.put(AvailableSettings.ORDER_INSERTS, true);
		properties.put(AvailableSettings.ORDER_UPDATES, true);
		properties.put(Settings.HBM_FLUSH_MODE,
				SpringHelper.getOrDefault(env, Settings.HBM_FLUSH_MODE, FlushMode::valueOf, FlushMode.MANUAL));

		sessionFactory.setHibernateProperties(properties);

		return sessionFactory;
	}

	@Bean
	public TransactionManager transactionManager(SessionFactory sessionFactory) {
		return new HibernateTransactionManager(sessionFactory);
	}

	@Bean
	public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
		final MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter();

		jsonConverter.setDefaultCharset(StandardCharsets.UTF_8);

		return jsonConverter;
	}

	@Bean(name = ImageService.EXECUTOR_NAME)
	public Executor imageServiceExecutor() {
		final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

		executor.setCorePoolSize(5);
		executor.setMaxPoolSize(10);
		executor.setQueueCapacity(50);
		executor.setThreadNamePrefix(ImageService.EXECUTOR_NAME);

		return executor;
	}

	@Bean(name = CredentialResetService.EXECUTOR_NAME)
	public Executor credentialResetMailer() {
		final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

		executor.setCorePoolSize(10);
		executor.setMaxPoolSize(20);
		executor.setQueueCapacity(100);
		executor.setThreadNamePrefix(CredentialResetService.EXECUTOR_NAME);

		return executor;
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Override
	public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
		final Hibernate5Module h5module = new Hibernate5Module();

		h5module.disable(Hibernate5Module.Feature.FORCE_LAZY_LOADING);

		for (final HttpMessageConverter<?> mc : converters) {
			if (mc instanceof MappingJackson2HttpMessageConverter
					|| mc instanceof MappingJackson2XmlHttpMessageConverter) {
				((AbstractJackson2HttpMessageConverter) mc).getObjectMapper().registerModule(h5module);
				return;
			}
		}
	}

}
