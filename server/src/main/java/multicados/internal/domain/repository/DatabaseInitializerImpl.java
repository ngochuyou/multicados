/**
 * 
 */
package multicados.internal.domain.repository;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.type.filter.AssignableTypeFilter;

import multicados.internal.config.Constants;
import multicados.internal.context.ContextManager;
import multicados.internal.helper.TypeHelper;
import multicados.internal.helper.Utils;

/**
 * @author Ngoc Huy
 *
 */
public class DatabaseInitializerImpl implements DatabaseInitializer {

	private static final String FLAG_KEY = "multicados.database-initializer";
	private static final String FLAG_OFF = "off";

	public DatabaseInitializerImpl() throws Exception {

		final Environment env = ContextManager.getBean(Environment.class);

		if (env.getProperty(FLAG_KEY).equals(FLAG_OFF)) {
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
	private void invoke(Set<BeanDefinition> beanDefs) throws ClassNotFoundException, InstantiationException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		final Logger logger = LoggerFactory.getLogger(DatabaseInitializerImpl.class);

		logger.trace("Invoking {} contributor(s)", beanDefs.size());

		for (BeanDefinition beanDef : beanDefs) {
			Class<? extends DatabaseInitializer.DatabaseInitializerContributor> contributorClass = (Class<? extends DatabaseInitializerContributor>) Class
					.forName(beanDef.getBeanClassName());

			try {
				DatabaseInitializer.DatabaseInitializerContributor contributor = TypeHelper
						.constructFromNonArgs(contributorClass);

				contributor.contribute();
			} catch (NoSuchMethodException nsme) {
				throw new IllegalArgumentException(String.format("A non-args constructor is required on %s type %s",
						DatabaseInitializer.DatabaseInitializerContributor.class.getSimpleName(),
						contributorClass.getSimpleName()));
			} catch (SecurityException se) {
				throw se;
			}
		}
	}

}
