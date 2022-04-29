/**
 * 
 */
package multicados.internal.helper;

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Ngoc Huy
 *
 */
public class CollectionHelper {

	private CollectionHelper() {}

	public static <K, V> Collector<Map.Entry<K, V>, ?, Map<K, V>> toMap() {
		return Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue);
	}

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

	public static <K, V, C extends Collection<V>> Map<K, C> group(Collection<V> collection, Function<V, K> keyProducer,
			Supplier<C> collectionSupplier) {
		Map<K, C> result = new HashMap<>();

		for (V value : collection) {
			K key = keyProducer.apply(value);

			if (!result.containsKey(key)) {
				C group = collectionSupplier.get();

				group.add(value);
				result.put(key, group);
				continue;
			}

			result.get(key).add(value);
		}

		return result;
	}

	public static Type getGenericType(Collection<?> collection) {
		for (Object o : collection) {
			if (o != null) {
				return o.getClass();
			}
		}

		return null;
	}

}
