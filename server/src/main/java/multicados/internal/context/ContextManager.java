/**
 * 
 */
package multicados.internal.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * @author Ngoc Huy
 *
 */
@Component
public class ContextManager implements ApplicationContextAware {

	private static final Logger logger = LoggerFactory.getLogger(ContextManager.class);

	private static ApplicationContext applicationContext;
	private static ApplicationExitContextAccess exitAcess = new ApplicationExitContextAccess();

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		ContextManager.applicationContext = applicationContext;
	}

	public static ApplicationExitContextAccess getExitAcess() {
		return exitAcess;
	}

	public static <T> T getBean(Class<T> beanType) {
		return applicationContext.getBean(beanType);
	}

	public static void registerBean(String beanId, BeanDefinition beanDef) {
		BeanDefinitionRegistry registry = BeanDefinitionRegistry.class
				.cast(applicationContext.getAutowireCapableBeanFactory());

		registry.registerBeanDefinition(beanId, beanDef);

		logger.debug("Registering a new bean of type [{}] with id [{}]", beanDef.getBeanClassName(), beanId);
	}

	public static class ApplicationExitContextAccess {

		private volatile boolean hasExited = false;

		public synchronized ApplicationContext getContext() throws IllegalAccessException {
			if (hasExited) {
				throw new IllegalAccessException("Application has already exited");
			}

			hasExited = true;

			return ContextManager.applicationContext;
		}

	}

}
