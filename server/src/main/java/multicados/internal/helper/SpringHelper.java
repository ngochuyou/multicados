/**
 *
 */
package multicados.internal.helper;

import java.util.function.Supplier;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;

import multicados.internal.helper.Utils.HandledFunction;
import multicados.internal.security.DomainUserDetails;

/**
 * @author Ngoc Huy
 *
 */
public abstract class SpringHelper {

	private SpringHelper() {}

	public static <T> T getOrDefault(Environment env, String propName, HandledFunction<String, T, Exception> producer,
			T defaultValue) throws Exception {
		String configuredValue = env.getProperty(propName);

		if (!StringUtils.hasLength(configuredValue)) {
			return defaultValue;
		}

		return producer.apply(configuredValue);
	}

	public static <T> T[] getArrayOrDefault(Environment env, String propName,
			HandledFunction<String[], T[], Exception> producer, T[] defaultValue) throws Exception {
		String[] configuredValue = env.getProperty(propName, String[].class);

		if (configuredValue == null || configuredValue.length == 0) {
			return defaultValue;
		}

		return producer.apply(configuredValue);
	}

	public static <T> T getOrThrow(Environment env, String propName, HandledFunction<String, T, Exception> producer,
			Supplier<Exception> thrower) throws Exception {
		String configuredValue = env.getProperty(propName);

		if (!StringUtils.hasLength(configuredValue)) {
			throw thrower.get();
		}

		return producer.apply(configuredValue);
	}

	public static DomainUserDetails getUserDetails(Authentication authentication) {
		return getUserDetails(authentication, null);
	}

	public static DomainUserDetails getUserDetails(Authentication authentication, DomainUserDetails anonymousProfile) {
		if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
			return anonymousProfile;
		}

		return DomainUserDetails.class.cast(authentication.getPrincipal());
	}

	@SuppressWarnings("unchecked")
	public static <T> T tryInit(BeanDefinition beanDef, ApplicationContext context)
			throws BeansException, IllegalStateException, ClassNotFoundException {
		return (T) context.getAutowireCapableBeanFactory().createBean(Class.forName(beanDef.getBeanClassName()));
	}

}
