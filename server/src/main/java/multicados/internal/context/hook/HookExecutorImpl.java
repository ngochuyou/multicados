/**
 * 
 */
package multicados.internal.context.hook;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;

import multicados.internal.config.Settings;
import multicados.internal.helper.SpringHelper;

/**
 * @author Ngoc Huy
 *
 */
public class HookExecutorImpl implements HookExecutor {

	private static final Logger logger = LoggerFactory.getLogger(HookExecutorImpl.class);

	public HookExecutorImpl(ApplicationContext applicationContext) throws Exception {
		doHooks(applicationContext);
	}

	private void doHooks(ApplicationContext applicationContext) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("Executing domain hooks");
		}

		final Set<BeanDefinition> hookDefinitions = scanForDomainHookDefinitions();

		for (final BeanDefinition beanDefinition : hookDefinitions) {
			final DomainContextHook hook = SpringHelper.tryInit(beanDefinition, applicationContext);

			if (logger.isDebugEnabled()) {
				logger.debug("Executing hook {}", hook.getClass().getName());
			}

			hook.hook(applicationContext);
		}
	}

	/**
	 * @return Qualified {@link DomainContextHook}s found under Settings.BASE_PACKAGE
	 */
	private Set<BeanDefinition> scanForDomainHookDefinitions() {
		final ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(
				false);

		scanner.addIncludeFilter(new AssignableTypeFilter(DomainContextHook.class));

		return scanner.findCandidateComponents(Settings.BASE_PACKAGE);
	}
}
