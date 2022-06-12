/**
 * 
 */
package multicados.internal.helper;

import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.core.env.Environment;

/**
 * @author Ngoc Huy
 *
 */
public class SpringHelper {

	private SpringHelper() {}

	public static <T> T getOrDefault(Environment env, String propName, Function<String, T> producer, T defaultValue) {
		String configuredValue = env.getProperty(propName);

		if (!StringHelper.hasLength(configuredValue)) {
			return defaultValue;
		}

		return producer.apply(configuredValue);
	}
	
	public static <T> T[] getArrayOrDefault(Environment env, String propName, Function<String[], T[]> producer, T[] defaultValue) {
		String[] configuredValue = env.getProperty(propName, String[].class);

		if (configuredValue.length == 0) {
			return defaultValue;
		}

		return producer.apply(configuredValue);
	}

	public static <T> T getOrThrow(Environment env, String propName, Function<String, T> producer,
			Supplier<Exception> exceptionSupplier) throws Exception {
		String configuredValue = env.getProperty(propName);

		if (!StringHelper.hasLength(configuredValue)) {
			throw exceptionSupplier.get();
		}

		return producer.apply(configuredValue);
	}
}
