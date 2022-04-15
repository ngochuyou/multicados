/**
 * 
 */
package multicados.internal.helper;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Ngoc Huy
 *
 */
public class CollectionHelper {

	private CollectionHelper() {}

	public static boolean isEmpty(Collection<?> collection) {
		return collection == null || collection.isEmpty();
	}

	public static <K, V> Map<V, K> inverse(Map<K, V> map) {
		return map.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
	}

	@SuppressWarnings("unchecked")
	public static <T> T[] join(Class<T> type, T[]... arrays) {
		return Stream.of(arrays).flatMap(array -> Stream.of(array))
				.toArray(size -> (T[]) Array.newInstance(type, size));
	}

	public static String[] toArray(String... strings) {
		return strings;
	}

	public static Object[] toArray(Object... strings) {
		return strings;
	}

	public static <K, V, C extends Collection<V>> Map<K, C> group(Collection<V> collections, Function<V, K> keyProducer,
			Supplier<C> collectionSupplier) {
		Map<K, C> result = new HashMap<>();

		collections.stream().forEach(value -> {
			K key = keyProducer.apply(value);

			if (result.get(key) == null) {
				C collection = collectionSupplier.get();

				collection.add(value);
				result.put(key, collection);
				return;
			}

			result.get(key).add(value);
		});

		return result;
	}

}
