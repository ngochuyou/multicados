/**
 *
 */
package multicados.internal.context;

import java.util.concurrent.atomic.AtomicBoolean;

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

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		ContextManager.applicationContext = applicationContext;
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

	public class Access {

		private static final AtomicBoolean HAS_EXITED = new AtomicBoolean(false);

		public static ApplicationContext getExitContext() {
			assertOpen();

			HAS_EXITED.set(true);

			return ContextManager.applicationContext;
		}

		private static void assertOpen() {
			if (HAS_EXITED.get()) {
				throw new UnsupportedOperationException("Application has already exited");
			}
		}

		public static ApplicationContext getContext() {
			assertOpen();
			return applicationContext;
		}

	}

}
