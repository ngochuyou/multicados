/**
 * 
 */
package multicados.internal.helper;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
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
	
	public static boolean isParentOf(Class<?> possibleParent, Class<?> child) {
		Stack<?> classStack = getClassStack(child);

		while (!classStack.isEmpty()) {
			if (classStack.pop().equals(possibleParent)) {
				return true;
			}
		}

		return false;
	}

	public static <T> T constructFromNonArgs(Class<T> clazz) throws InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		return clazz.getConstructor().newInstance();
	}
	
	public static Type getGenericType(Field field) {
		return ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
	}

}
