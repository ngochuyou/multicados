/**
 *
 */
package multicados.internal.helper;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
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

	@SafeVarargs
	public static <T, C extends Collection<T>> C join(Collector<T, ?, C> collector, Collection<T>... elements) {
		return Stream.of(elements).flatMap(Collection::stream).collect(collector);
	}

	public static <T> T[] toArray(@SuppressWarnings("unchecked") T... elements) {
		return elements;
	}

	public static <K, V, C extends Collection<V>> Map<K, C> group(Collection<V> collection, Function<V, K> keyProducer,
			Supplier<C> collectionSupplier) {
		final Map<K, C> result = new HashMap<>();

		for (final V value : collection) {
			result.computeIfAbsent(keyProducer.apply(value), key -> collectionSupplier.get()).add(value);
		}

		return result;
	}

	public static Type getGenericType(Collection<?> collection) {
		for (final Object o : collection) {
			if (o != null) {
				return o.getClass();
			}
		}

		return null;
	}

	public static <K, V> Map<K, V> getOrEmpty(Map<K, V> map) {
		return map == null || map.isEmpty() ? Collections.emptyMap() : map;
	}

}
