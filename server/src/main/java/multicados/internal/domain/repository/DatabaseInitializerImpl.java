/**
 *
 */
package multicados.internal.domain.repository;

import java.lang.reflect.Constructor;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.type.filter.AssignableTypeFilter;

import multicados.internal.config.Settings;
import multicados.internal.context.ContextBuilder;
import multicados.internal.helper.StringHelper;

/**
 * @author Ngoc Huy
 *
 */
public class DatabaseInitializerImpl extends ContextBuilder.AbstractContextBuilder implements DatabaseInitializer {

	private static final String FLAG_OFF = "off";

	@Autowired
	public DatabaseInitializerImpl(ApplicationContext applicationContext, Environment env) throws Exception {
		String flagValue = Optional.ofNullable(env.getProperty(Settings.DUMMY_DATABASE_MODE))
				.orElse(StringHelper.EMPTY_STRING).toLowerCase();

		if (flagValue.equals(FLAG_OFF)) {
			return;
		}

		invoke(scan(), applicationContext);
	}

	private Set<BeanDefinition> scan() {
		ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
		final Logger logger = LoggerFactory.getLogger(DatabaseInitializerImpl.class);

		scanner.addIncludeFilter(new AssignableTypeFilter(DatabaseInitializer.DatabaseInitializerContributor.class));
		logger.trace("Scanning for {}", DatabaseInitializer.DatabaseInitializerContributor.class.getSimpleName());

		return scanner.findCandidateComponents(Settings.BASE_PACKAGE);
	}

	@SuppressWarnings("unchecked")
	private void invoke(Set<BeanDefinition> beanDefs, ApplicationContext applicationContext) throws Exception {
		final Logger logger = LoggerFactory.getLogger(DatabaseInitializerImpl.class);

		logger.trace("Invoking {} contributor(s)", beanDefs.size());

		for (BeanDefinition beanDef : beanDefs) {
			Class<? extends DatabaseInitializer.DatabaseInitializerContributor> contributorClass = (Class<? extends DatabaseInitializerContributor>) Class
					.forName(beanDef.getBeanClassName());

			try {
				Constructor<DatabaseInitializer.DatabaseInitializerContributor> constructor;
				Object[] args;

				tryClause: try {
					constructor = (Constructor<DatabaseInitializerContributor>) contributorClass.getConstructor();
					args = new Object[0];
				} catch (NoSuchMethodException nsme) {
					for (Constructor<?> cons : contributorClass.getConstructors()) {
						if (cons.isAnnotationPresent(Autowired.class)) {
							constructor = (Constructor<DatabaseInitializerContributor>) cons;
							args = Stream.of(constructor.getParameterTypes()).map(applicationContext::getBean)
									.toArray();
							break tryClause;
						}
					}

					throw nsme;
				}

				DatabaseInitializer.DatabaseInitializerContributor contributor = constructor.newInstance(args);

				contributor.contribute();
			} catch (NoSuchMethodException nsme) {
				throw new IllegalArgumentException(String.format(
						"Provide at least a non-args constructor or one constructor which is annotated with @%s. %s type %s",
						Autowired.class.getName(),
						DatabaseInitializer.DatabaseInitializerContributor.class.getSimpleName(),
						contributorClass.getSimpleName()));
			} catch (SecurityException se) {
				throw se;
			}
		}
	}

}
