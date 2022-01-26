/**
 * 
 */
package multicados.internal.helper;

import java.lang.reflect.Array;
import java.util.Collection;
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

}
