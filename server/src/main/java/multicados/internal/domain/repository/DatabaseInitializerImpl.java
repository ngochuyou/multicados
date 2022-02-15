/**
 * 
 */
package multicados.internal.domain.repository;

import java.lang.reflect.Constructor;
import java.util.Set;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.type.filter.AssignableTypeFilter;

import multicados.internal.config.Constants;
import multicados.internal.context.ContextManager;
import multicados.internal.helper.Utils;

/**
 * @author Ngoc Huy
 *
 */
public class DatabaseInitializerImpl implements DatabaseInitializer {

	private static final String FLAG_KEY = "multicados.dummy-database-initializer";
	private static final String FLAG_OFF = "off";

	public DatabaseInitializerImpl(Environment env) throws Exception {
		if (env.getProperty(FLAG_KEY).toLowerCase().equals(FLAG_OFF)) {
			return;
		}

		Utils.declare(scan()).identical(this::invoke);
	}

	private Set<BeanDefinition> scan() {
		ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
		final Logger logger = LoggerFactory.getLogger(DatabaseInitializerImpl.class);

		scanner.addIncludeFilter(new AssignableTypeFilter(DatabaseInitializer.DatabaseInitializerContributor.class));
		logger.trace("Scanning for {}", DatabaseInitializer.DatabaseInitializerContributor.class.getSimpleName());

		return scanner.findCandidateComponents(Constants.BASE_PACKAGE);
	}

	@SuppressWarnings("unchecked")
	private void invoke(Set<BeanDefinition> beanDefs) throws Exception {
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
							args = Stream.of(constructor.getParameterTypes()).map(ContextManager::getBean).toArray();
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
