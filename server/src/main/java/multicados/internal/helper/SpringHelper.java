/**
 * 
 */
package multicados.internal.helper;

import java.util.function.Supplier;

import org.springframework.core.env.Environment;

import multicados.internal.helper.Utils.HandledFunction;

/**
 * @author Ngoc Huy
 *
 */
public class SpringHelper {

	private SpringHelper() {}

	public static <T> T getOrDefault(Environment env, String propName, HandledFunction<String, T, Exception> producer,
			T defaultValue) throws Exception {
		String configuredValue = env.getProperty(propName);

		if (!StringHelper.hasLength(configuredValue)) {
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

		if (!StringHelper.hasLength(configuredValue)) {
			throw thrower.get();
		}

		return producer.apply(configuredValue);
	}

}
