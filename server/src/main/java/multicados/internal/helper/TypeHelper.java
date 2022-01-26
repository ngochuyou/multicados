/**
 * 
 */
package multicados.internal.helper;

import java.lang.reflect.InvocationTargetException;
import java.util.Stack;

/**
 * @author Ngoc Huy
 *
 */
public class TypeHelper {

	public static <T> Stack<Class<? super T>> getClassStack(Class<T> clazz) {
		Stack<Class<? super T>> stack = new Stack<>();
		Class<? super T> superClass = clazz;

		while (superClass != null && !superClass.equals(Object.class)) {
			stack.add(superClass);
			superClass = (Class<? super T>) superClass.getSuperclass();
		}

		return stack;
	}

	public static boolean isImplementedFrom(Class<?> type, Class<?> superType) {
		for (Class<?> i : type.getInterfaces()) {
			if (i.equals(superType)) {
				return true;
			}
		}

		return false;
	}

	public static <T> T constructFromNonArgs(Class<T> clazz) throws InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		return clazz.getConstructor().newInstance();
	}

}
