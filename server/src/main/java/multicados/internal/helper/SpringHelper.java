/**
 * 
 */
package multicados.internal.helper;

import java.util.function.Supplier;
import java.util.stream.Stream;

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
			Supplier<Exception> exceptionSupplier) throws Exception {
		String configuredValue = env.getProperty(propName);

		if (!StringHelper.hasLength(configuredValue)) {
			throw exceptionSupplier.get();
		}

		return producer.apply(configuredValue);
	}

	public static float[] getFloatsOrDefault(Environment env, String propNames, Float[] defaultNonPrims)
			throws Exception {
		Float[] nonPrims = getArrayOrDefault(env, propNames,
				values -> Stream.of(values).map(Float::valueOf).toArray(Float[]::new), defaultNonPrims);
		int size = nonPrims.length;
		float[] prims = new float[size];

		for (int i = 0; i < size; i++) {
			prims[i] = nonPrims[i];
		}

		return prims;
	}

	public static <T> int[] getIntsOrDefault(Environment env, String propNames, Integer[] defaultNonPrims)
			throws Exception {
		Integer[] nonPrims = getArrayOrDefault(env, propNames,
				values -> Stream.of(values).map(Integer::valueOf).toArray(Integer[]::new), defaultNonPrims);
		int size = nonPrims.length;
		int[] prims = new int[size];

		for (int i = 0; i < size; i++) {
			prims[i] = nonPrims[i];
		}

		return prims;
	}

}
