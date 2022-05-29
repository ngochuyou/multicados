/**
 * 
 */
package multicados.internal.helper;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import multicados.internal.context.Loggable;
import multicados.internal.helper.FunctionHelper.HandledFunction;
import multicados.internal.helper.Utils.Access;

/**
 * @author Ngoc Huy
 *
 */
public class TypeHelper {

	static {
		make(with(String.class, Integer.class).toA(Object::toString).toB(Integer::valueOf));
	}

	public static <T> Stack<Class<? super T>> getClassStack(Class<T> clazz) {
		Stack<Class<? super T>> stack = new Stack<>();
		Class<? super T> superClass = clazz;

		while (superClass != null && !superClass.equals(Object.class)) {
			stack.add(superClass);
			superClass = (Class<? super T>) superClass.getSuperclass();
		}

		return stack;
	}
	
	public static <T> Queue<Class<? super T>> getClassQueue(Class<T> clazz) {
		Queue<Class<? super T>> queue = new ArrayDeque<>();
		Class<? super T> superClass = clazz;

		while (superClass != null && !superClass.equals(Object.class)) {
			queue.add(superClass);
			superClass = (Class<? super T>) superClass.getSuperclass();
		}

		return queue;
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

	private static <A, B> void make(TypeGraphEntry<A, B> entry) {
		TypeGraph.INSTANCE.add(entry);
	}

//	public static <A, B> B cast(Class<A> typeA, Class<B> typeB, A value) throws Exception {
//		return TypeGraph.INSTANCE.locate(typeA, typeB).apply(value);
//	}

	public static <A, B> B cast(Class<A> typeA, Class<B> typeB, A value) throws Exception {
		return TypeGraph.INSTANCE.locate(typeA, typeB).apply(value);
	}

	public static class TypeGraph implements Closeable, Loggable {

		private static final TypeGraph INSTANCE = new TypeGraph();

		private static final int PRIME = 37;

		private static final BiFunction<Class<?>, Integer, Integer> HASH_CODE_GENERATOR = (anyType,
				salt) -> (PRIME * salt) + anyType.hashCode();

		private Map<Integer, Map<Integer, HandledFunction<?, ?, Exception>>> convertersMap = new HashMap<>();

		private volatile Access access = new Access() {};

		private TypeGraph() {};

		private <A, B> int[] hash(Class<A> typeA, Class<B> typeB) {
			int aHash = HASH_CODE_GENERATOR.apply(typeA, 1);
			int bHash = HASH_CODE_GENERATOR.apply(typeB, aHash);

			return new int[] { aHash, bHash };
		}

		@SuppressWarnings("unchecked")
		private <A, B> HandledFunction<A, B, Exception> locate(Class<A> typeA, Class<B> typeB) {
			int[] hashPair = hash(typeA, typeB);

			if (!convertersMap.containsKey(hashPair[0])) {
				return null;
			}

			if (!convertersMap.get(hashPair[0]).containsKey(hashPair[1])) {
				return null;
			}

			return (HandledFunction<A, B, Exception>) convertersMap.get(hashPair[0]).get(hashPair[1]);
		}

		private <A, B> void add(TypeGraphEntry<A, B> entry) {
			Assert.notNull(access, Access.CLOSED_MESSAGE);
			int[] hashPair = hash(entry.typeA, entry.typeB);

			addBy(hashPair[0], hashPair[1], entry.toBFunction);
			addBy(hashPair[1], hashPair[0], entry.toAFunction);
		}

		private void addBy(int aHash, int bHash, HandledFunction<?, ?, Exception> converter) {
			if (convertersMap.containsKey(aHash)) {
				convertersMap.get(aHash).put(bHash, converter);
				return;
			}

			Map<Integer, HandledFunction<?, ?, Exception>> descriptorsByA = new HashMap<>();

			descriptorsByA.put(bHash, converter);
			convertersMap.put(aHash, descriptorsByA);
		}

		@Override
		public synchronized void close() throws IOException {
			Logger logger = LoggerFactory.getLogger(TypeHelper.TypeGraph.class);

			access = null;
			logger.trace(Access.getClosingMessage(this));

			try {
				convertersMap = Utils.declare(convertersMap).then(Map::entrySet).then(Set::stream)
						.then(stream -> stream
								.map(entry -> Map.entry(entry.getKey(), Collections.unmodifiableMap(entry.getValue())))
								.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
						.then(Collections::unmodifiableMap).get();
			} catch (Exception any) {
				throw new IOException(any);
			}
		}

	}

	public static <A, B> TypeGraphEntry<A, B> with(Class<A> typeA, Class<B> typeB) {
		return new TypeGraphEntry<A, B>().a(typeA).b(typeB);
	}

	private static class TypeGraphEntry<A, B> {

		private Class<A> typeA;
		private Class<B> typeB;
		private HandledFunction<A, B, Exception> toBFunction;
		private HandledFunction<B, A, Exception> toAFunction;

		private TypeGraphEntry() {}

		public TypeGraphEntry<A, B> a(Class<A> typeA) {
			this.typeA = typeA;
			return this;
		}

		public TypeGraphEntry<A, B> b(Class<B> typeB) {
			this.typeB = typeB;
			return this;
		}

		public TypeGraphEntry<A, B> toB(HandledFunction<A, B, Exception> toBFunction) {
			this.toBFunction = toBFunction;
			return this;
		}

		public TypeGraphEntry<A, B> toA(HandledFunction<B, A, Exception> toAFunction) {
			this.toAFunction = toAFunction;
			return this;
		}

	}

}
