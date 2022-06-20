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

	public static <T> void registerBean(Class<T> beanType, BeanDefinition beanDef) {
		doRegisterBean(ContextManager.applicationContext, beanType.getName(), beanDef);
	}

	public static <T> void registerBean(ApplicationContext applicationContext, Class<T> beanType,
			BeanDefinition beanDef) {
		doRegisterBean(applicationContext, beanType.getName(), beanDef);
	}

	private static void doRegisterBean(ApplicationContext applicationContext, String beanId, BeanDefinition beanDef) {
		BeanDefinitionRegistry registry = BeanDefinitionRegistry.class
				.cast(applicationContext.getAutowireCapableBeanFactory());

		registry.registerBeanDefinition(beanId, beanDef);

		logger.debug("Registering a new bean of type [{}] with id [{}]", beanDef.getBeanClassName(), beanId);
	}

	public static class ApplicationExitContextAccess {

		private volatile boolean hasExited = false;

		public synchronized ApplicationContext getContext() {
			if (hasExited) {
				throw new UnsupportedOperationException("Application has already exited");
			}

			hasExited = true;

			return ContextManager.applicationContext;
		}

	}

}
