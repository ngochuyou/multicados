/**
 * 
 */
package multicados.internal.config;

import static multicados.internal.helper.Utils.declare;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.hibernate.SessionFactory;
import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.cfg.AvailableSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
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
import multicados.internal.domain.DomainResourceContext;
import multicados.internal.domain.builder.DomainResourceBuilderFactory;
import multicados.internal.domain.repository.DatabaseInitializer;
import multicados.internal.domain.repository.GenericRepository;
import multicados.internal.domain.validation.DomainResourceValidatorFactory;
import multicados.internal.helper.StringHelper;
import multicados.internal.helper.TypeHelper;
import multicados.internal.helper.Utils;
import multicados.internal.security.CredentialFactory;
import multicados.internal.service.crud.GenericHibernateCRUDService;
import multicados.internal.service.crud.security.read.ReadSecurityManager;

/**
 * @author Ngoc Huy
 *
 */
@ComponentScan(Settings.BASE_PACKAGE)
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
		sessionFactory.setPackagesToScan(new String[] { env.getProperty(Settings.SCANNED_ENTITY_PACKAGES) });
		// snake_case for columns
		sessionFactory.setPhysicalNamingStrategy(new CamelCaseToUnderscoresNamingStrategy());

		Properties properties = new Properties();

		properties.put(AvailableSettings.DIALECT, "org.hibernate.dialect.MySQL8Dialect");
		properties.put(AvailableSettings.SHOW_SQL, true);
		properties.put(AvailableSettings.FORMAT_SQL, true);
		properties.put(AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS, "true");
		properties.put(AvailableSettings.HBM2DDL_AUTO, "update");
		properties.put(AvailableSettings.STATEMENT_BATCH_SIZE, 50);
		properties.put(AvailableSettings.ORDER_INSERTS, true);
		properties.put(AvailableSettings.ORDER_UPDATES, true);

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

	private List<BeanDefinition> sortContextBuilders(Set<BeanDefinition> beanDefs) throws Exception {
		// @formatter:off
		return Utils
				.declare(resolveBuildersOrder(beanDefs))
					.second(beanDefs)
				.then(this::doSort)
				.get();
		// @formatter:on
	}

	@SuppressWarnings("unchecked")
	private List<BeanDefinition> doSort(Map<Class<? extends ContextBuilder>, Integer> buildersOrder,
			Set<BeanDefinition> beanDefs) {
		final Logger logger = LoggerFactory.getLogger(BootEntry.class);

		logger.trace("Sorting {}(s)", ContextBuilder.class.getSimpleName());

		return beanDefs.stream().sorted((left, right) -> {
			try {
				Class<? extends ContextBuilder> leftType = (Class<? extends ContextBuilder>) Class
						.forName(left.getBeanClassName());
				Class<? extends ContextBuilder> rightType = (Class<? extends ContextBuilder>) Class
						.forName(right.getBeanClassName());

				if (buildersOrder.containsKey(rightType) && buildersOrder.containsKey(leftType)) {
					return Integer.compare(buildersOrder.get(leftType), buildersOrder.get(rightType));
				}

				if (!buildersOrder.containsKey(rightType)) {
					return -1;
				}

				return 1;
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				return 1;
			}
		}).collect(Collectors.toList());
	}

	@SuppressWarnings("unchecked")
	private Map<Class<? extends ContextBuilder>, Integer> resolveBuildersOrder(Set<BeanDefinition> beanDefs) {
		final Logger logger = LoggerFactory.getLogger(BootEntry.class);

		logger.trace("Resolving builders order", ContextBuilder.class.getSimpleName());
		// @formatter:off
		final Map<Class<? extends ContextBuilder>, Integer> buildersOrder = new HashMap<>();
		List<Class<? extends ContextBuilder>> builderTypes = List.of(
				DomainResourceContext.class,
				DomainResourceValidatorFactory.class,
				GenericRepository.class,
				CredentialFactory.class,
				DomainResourceBuilderFactory.class,
				ReadSecurityManager.class,
				GenericHibernateCRUDService.class,
				DatabaseInitializer.class
			);
		int size = builderTypes.size();
		
		for (int i = 0; i < size; i++) {
			buildersOrder.put(builderTypes.get(i), i);
		}

		final Map<Class<? extends ContextBuilder>, BeanDefinition> beansMap = beanDefs.stream()
				.map(bean -> {
					try {
						return Map.entry((Class<? extends ContextBuilder>) Class.forName(bean.getBeanClassName()), bean);
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
						return null;
					}
				})
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		// @formatter:on
		Map<Class<? extends ContextBuilder>, Integer> beansOrder = new HashMap<>();

		for (Map.Entry<Class<? extends ContextBuilder>, BeanDefinition> beanEntry : beansMap.entrySet()) {
			for (Map.Entry<Class<? extends ContextBuilder>, Integer> orderEntry : buildersOrder.entrySet()) {
				if (TypeHelper.isImplementedFrom(beanEntry.getKey(), orderEntry.getKey())) {
					beansOrder.put((Class<? extends ContextBuilder>) beanEntry.getKey(), orderEntry.getValue());
					break;
				}
			}
		}

		return beansOrder;
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

		return scanner.findCandidateComponents(Settings.BASE_PACKAGE);
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
			beanDef.setScope(BeanDefinition.SCOPE_SINGLETON);
			beanDef.setLazyInit(false);
			ContextManager.registerBean(beanDef.getBeanClassName(), beanDef);
			buildersTypes.add((Class<ContextBuilder>) Class.forName(beanDef.getBeanClassName()));
		}

		return buildersTypes;
	}

}
