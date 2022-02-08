/**
 * 
 */
package multicados.internal.config;

import static multicados.internal.helper.Utils.declare;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.hibernate.SessionFactory;
import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import multicados.application.BootEntry;
import multicados.internal.context.ContextBuilder;
import multicados.internal.context.ContextManager;
import multicados.internal.domain.DomainResourceContextProvider;
import multicados.internal.domain.builder.DomainResourceBuilderFactory;
import multicados.internal.domain.repository.DatabaseInitializer;
import multicados.internal.domain.repository.GenericRepository;
import multicados.internal.helper.StringHelper;

/**
 * @author Ngoc Huy
 *
 */
@ComponentScan(Constants.BASE_PACKAGE)
@Configuration
@EnableTransactionManagement
@EnableWebMvc
public class WebConfiguration implements WebMvcConfigurer {

	private static final Logger logger = LoggerFactory.getLogger(WebConfiguration.class);

	@Bean
	public FactoryBean<SessionFactory> sessionFactory(DataSource dataSource) {
		logger.info("Creating {} bean", LocalSessionFactoryBean.class.getName());

		Environment env = ContextManager.getBean(Environment.class);
		LocalSessionFactoryBean sessionFactory = new LocalSessionFactoryBean();

		sessionFactory.setDataSource(dataSource);
		sessionFactory.setPackagesToScan(new String[] { env.getProperty("multicados.scanned-packages.entity") });
		// snake_case for columns
		sessionFactory.setPhysicalNamingStrategy(new CamelCaseToUnderscoresNamingStrategy());

		Properties properties = new Properties();

		properties.put("hibernate.dialect", "org.hibernate.dialect.MySQL8Dialect");
		properties.put("hibernate.show_sql", true);
		properties.put("hibernate.format_sql", true);
		properties.put("hibernate.id.new_generator_mappings", "true");
		properties.put("hibernate.hbm2ddl.auto", "update");
		properties.put("hibernate.flush_mode", "MANUAL");
		properties.put("hibernate.jdbc.batch_size", 50);
		properties.put("hibernate.order_inserts", true);
		properties.put("hibernate.order_updates", true);

		sessionFactory.setHibernateProperties(properties);

		return sessionFactory;
	}

	@EventListener(ApplicationReadyEvent.class)
	private void doWhenReady() throws IllegalAccessException {
		final Logger logger = LoggerFactory.getLogger(BootEntry.class);

		logger.info("Invoking application-ready event");

		try {
			// @formatter:off
			declare(scan())
				.then(this::sortContextBuilders)
				.identical(this::logContextBuilders)
				.then(this::registerContextBuilders)
				.identical(this::summaryContextBuilders);
			// @formatter:on
		} catch (Exception any) {
			any.printStackTrace();
			SpringApplication.exit(ContextManager.getExitAcess().getContext());
		}
	}

	@SuppressWarnings("unchecked")
	private List<BeanDefinition> sortContextBuilders(Set<BeanDefinition> beanDefs) {
		final Logger logger = LoggerFactory.getLogger(BootEntry.class);

		logger.trace("Sorting {}(s)", ContextBuilder.class.getSimpleName());
		// @formatter:off
		final Map<Class<? extends ContextBuilder>, Integer> buildersOrder = Map.of(
				DomainResourceContextProvider.class, 0,
				GenericRepository.class, 1,
				DomainResourceBuilderFactory.class, 2,
				DatabaseInitializer.class, 3);
		// @formatter:on
		return beanDefs.stream().sorted((left, right) -> {
			try {
				Class<? extends ContextBuilder> leftType = (Class<? extends ContextBuilder>) Class
						.forName(left.getBeanClassName());
				Class<? extends ContextBuilder> rightType = (Class<? extends ContextBuilder>) Class
						.forName(right.getBeanClassName());

				if (!buildersOrder.containsKey(rightType)) {
					return -1;
				}

				if (!buildersOrder.containsKey(leftType)) {
					return 1;
				}

				return Integer.compare(buildersOrder.get(leftType), buildersOrder.get(leftType));
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				return 1;
			}
		}).collect(Collectors.toList());
	}

	private void logContextBuilders(List<BeanDefinition> beanDefs) {
		final Logger logger = LoggerFactory.getLogger(BootEntry.class);

		logger.debug("{}(s): {}", ContextBuilder.class.getSimpleName(),
				StringHelper.join(BeanDefinition::getBeanClassName, beanDefs.toArray(BeanDefinition[]::new)));
	}

	private Set<BeanDefinition> scan() {
		final Logger logger = LoggerFactory.getLogger(BootEntry.class);

		logger.trace("Scanning for {}(s)", ContextBuilder.class.getSimpleName());

		ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);

		scanner.addIncludeFilter(new AssignableTypeFilter(ContextBuilder.class));

		return scanner.findCandidateComponents(Constants.BASE_PACKAGE);
	}

	private void summaryContextBuilders(List<Class<ContextBuilder>> buildersTypes) throws Exception {
		final Logger logger = LoggerFactory.getLogger(BootEntry.class);

		logger.trace("Doing summaries on {}(s)", ContextBuilder.class.getSimpleName());

		for (Class<ContextBuilder> builderType : buildersTypes) {
			ContextManager.getBean(builderType).summary();
		}
	}

	@SuppressWarnings("unchecked")
	private List<Class<ContextBuilder>> registerContextBuilders(List<BeanDefinition> beanDefs)
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
			ClassNotFoundException {
		final Logger logger = LoggerFactory.getLogger(BootEntry.class);

		logger.trace("Registering {}(s)", ContextBuilder.class.getSimpleName());

		List<Class<ContextBuilder>> buildersTypes = new ArrayList<>();

		for (BeanDefinition beanDef : beanDefs) {
			beanDef.setLazyInit(false);
			ContextManager.registerBean(beanDef.getBeanClassName(), new GenericBeanDefinition(beanDef));
			buildersTypes.add((Class<ContextBuilder>) Class.forName(beanDef.getBeanClassName()));
		}

		return buildersTypes;
	}

}