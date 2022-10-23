/**
 *
 */
package multicados.internal.helper;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import multicados.internal.context.Loggable;

/**
 * @author Ngoc Huy
 *
 */
public class Utils {

	private Utils() {
		throw new UnsupportedOperationException();
	}

	// we use FIRST, SECOND, THIRD,... to avoid FunctionalInterface types conflict
	public static <FIRST> Declaration<FIRST> declare(FIRST val) {
		return new Declaration<>(val);
	}

	public static <FIRST, SECOND> BiDeclaration<FIRST, SECOND> declare(FIRST one, SECOND two) {
		return new BiDeclaration<>(one, two);
	}

	public static <FIRST, SECOND, THIRD> TriDeclaration<FIRST, SECOND, THIRD> declare(FIRST one, SECOND two,
			THIRD three) {
		return new TriDeclaration<>(one, two, three);
	}

	private interface ArgumentContext {

	}

	private interface SingularArgument<FIRST> extends ArgumentContext {

		<RETURN> Declaration<RETURN> then(HandledFunction<FIRST, RETURN, Exception> fnc) throws Exception;

		Declaration<FIRST> consume(HandledConsumer<FIRST, Exception> consumer) throws Exception;

		<NEXT_FIRST, SECOND> BiDeclaration<NEXT_FIRST, SECOND> flat(
				HandledFunction<FIRST, NEXT_FIRST, Exception> nextFirstArgProducer,
				HandledFunction<FIRST, SECOND, Exception> secondArgProducer) throws Exception;

		<NEXT_FIRST, SECOND, THIRD> TriDeclaration<NEXT_FIRST, SECOND, THIRD> flat(
				HandledFunction<FIRST, NEXT_FIRST, Exception> nextFirstArgProducer,
				HandledFunction<FIRST, SECOND, Exception> secondArgProducer,
				HandledFunction<FIRST, THIRD, Exception> thirdArgProducer) throws Exception;

		<SECOND> BiDeclaration<SECOND, FIRST> prepend(SECOND secondArg);

		<SECOND> BiDeclaration<SECOND, FIRST> prepend(Declaration<SECOND> declaration);

		<SECOND> BiDeclaration<SECOND, FIRST> prepend(HandledFunction<FIRST, SECOND, Exception> fnc) throws Exception;

		<SECOND> BiDeclaration<FIRST, SECOND> second(SECOND second) throws Exception;

		<RETURN> BiDeclaration<FIRST, RETURN> second(HandledFunction<FIRST, RETURN, Exception> fnc) throws Exception;

		FIRST get();

		<RETURN> Declaration<RETURN> boundThen(HandledBiFunction<FIRST, Declaration<FIRST>, RETURN, Exception> fnc)
				throws Exception;

		Declaration<FIRST> boundConsume(HandledBiConsumer<FIRST, Declaration<FIRST>, Exception> consumer)
				throws Exception;

		<RETURN> Declaration<RETURN> exit(HandledFunction<FIRST, RETURN, Exception> fnc) throws Exception;

		<RETURN> Declaration<RETURN> exit(RETURN value);

	}

	private interface BiArgument<FIRST, SECOND> extends SingularArgument<FIRST> {

		Declaration<FIRST> useFirst();

		FIRST getFirst();

		Declaration<SECOND> useSecond();

		SECOND getSecond();

		<RETURN> Declaration<RETURN> then(HandledBiFunction<FIRST, SECOND, RETURN, Exception> fnc) throws Exception;

		<RETURN_ONE, RETURN_TWO> BiDeclaration<RETURN_ONE, RETURN_TWO> map(
				HandledFunction<FIRST, RETURN_ONE, Exception> firstProducer,
				HandledFunction<SECOND, RETURN_TWO, Exception> secondProducer) throws Exception;

		BiDeclaration<FIRST, SECOND> consume(HandledBiConsumer<FIRST, SECOND, Exception> consumer) throws Exception;

		<RETURN> Declaration<RETURN> boundThen(
				HandledTriFunction<FIRST, SECOND, BiDeclaration<FIRST, SECOND>, RETURN, Exception> fnc)
				throws Exception;

		BiDeclaration<FIRST, SECOND> boundConsume(
				HandledTriConsumer<FIRST, SECOND, BiDeclaration<FIRST, SECOND>, Exception> fnc) throws Exception;

		<THIRD> TriDeclaration<FIRST, SECOND, THIRD> append(HandledBiFunction<FIRST, SECOND, THIRD, Exception> fnc)
				throws Exception;

		<THIRD> TriDeclaration<THIRD, FIRST, SECOND> prepend(HandledBiFunction<FIRST, SECOND, THIRD, Exception> fnc)
				throws Exception;

		<THIRD> TriDeclaration<FIRST, SECOND, THIRD> third(THIRD third) throws Exception;

		BiDeclaration<SECOND, FIRST> biInverse();

		<RETURN> Declaration<RETURN> exit(HandledBiFunction<FIRST, SECOND, RETURN, Exception> fnc) throws Exception;

	}

	private interface TriArgument<FIRST, SECOND, THIRD> extends BiArgument<FIRST, SECOND> {

		Declaration<THIRD> useThird();

		THIRD getThird();

		<RETURN> Declaration<RETURN> then(HandledTriFunction<FIRST, SECOND, THIRD, RETURN, Exception> fnc)
				throws Exception;

		<RETURN_ONE, RETURN_TWO, RETURN_THREE> TriDeclaration<RETURN_ONE, RETURN_TWO, RETURN_THREE> map(
				HandledFunction<FIRST, RETURN_ONE, Exception> firstProducer,
				HandledFunction<SECOND, RETURN_TWO, Exception> secondProducer,
				HandledFunction<THIRD, RETURN_THREE, Exception> thirdProducer) throws Exception;

		<NEXT_FIRST> TriDeclaration<NEXT_FIRST, SECOND, THIRD> thenPrepend(
				HandledTriFunction<FIRST, SECOND, THIRD, NEXT_FIRST, Exception> nextFirstProducer) throws Exception;

		TriDeclaration<FIRST, SECOND, THIRD> consume(HandledTriConsumer<FIRST, SECOND, THIRD, Exception> consumer)
				throws Exception;

		@Override
		<NEXT_SECOND> TriDeclaration<FIRST, NEXT_SECOND, THIRD> second(NEXT_SECOND nextSecond);

		TriDeclaration<THIRD, SECOND, FIRST> triInverse();

	}

	public static class Declaration<FIRST> implements SingularArgument<FIRST> {

		protected FIRST firstArg;

		private Declaration(FIRST val) {
			this.firstArg = val;
		}

		@Override
		public <RETURN> Declaration<RETURN> then(HandledFunction<FIRST, RETURN, Exception> fnc) throws Exception {
			return new Declaration<>(fnc.apply(firstArg));
		}

		@Override
		public <NEXT_FIRST, SECOND> BiDeclaration<NEXT_FIRST, SECOND> flat(
				HandledFunction<FIRST, NEXT_FIRST, Exception> nextFirstArgProducer,
				HandledFunction<FIRST, SECOND, Exception> secondArgProducer) throws Exception {
			return new BiDeclaration<>(nextFirstArgProducer.apply(firstArg), secondArgProducer.apply(firstArg));
		}

		@Override
		public <NEXT_FIRST, SECOND, THIRD> TriDeclaration<NEXT_FIRST, SECOND, THIRD> flat(
				HandledFunction<FIRST, NEXT_FIRST, Exception> nextFirstArgProducer,
				HandledFunction<FIRST, SECOND, Exception> secondArgProducer,
				HandledFunction<FIRST, THIRD, Exception> thirdArgProducer) throws Exception {
			// @formatter:off
			return new TriDeclaration<>(nextFirstArgProducer.apply(firstArg),
					secondArgProducer.apply(firstArg),
					thirdArgProducer.apply(firstArg));
			// @formatter:on
		}

		@Override
		public <SECOND> BiDeclaration<SECOND, FIRST> prepend(SECOND secondArg) {
			return new BiDeclaration<>(secondArg, firstArg);
		}

		@Override
		public <SECOND> BiDeclaration<SECOND, FIRST> prepend(Declaration<SECOND> declaration) {
			return new BiDeclaration<>(declaration.firstArg, firstArg);
		}

		@Override
		public <SECOND> BiDeclaration<SECOND, FIRST> prepend(HandledFunction<FIRST, SECOND, Exception> fnc)
				throws Exception {
			return new BiDeclaration<>(fnc.apply(firstArg), firstArg);
		}

		@Override
		public Declaration<FIRST> consume(HandledConsumer<FIRST, Exception> consumer) throws Exception {
			consumer.accept(firstArg);
			return this;
		}

		@Override
		public <SECOND> BiDeclaration<FIRST, SECOND> second(SECOND secondArg) throws Exception {
			return new BiDeclaration<>(firstArg, secondArg);
		}

		@Override
		public <RETURN> BiDeclaration<FIRST, RETURN> second(HandledFunction<FIRST, RETURN, Exception> fnc)
				throws Exception {
			return new BiDeclaration<>(firstArg, fnc.apply(firstArg));
		}

		@Override
		public FIRST get() {
			return firstArg;
		}

		@Override
		public <RETURN> Declaration<RETURN> exit(HandledFunction<FIRST, RETURN, Exception> fnc) throws Exception {
			return new ExitDeclaration<>(fnc.apply(firstArg));
		}

		@Override
		public <RETURN> Declaration<RETURN> exit(RETURN value) {
			return new ExitDeclaration<>(value);
		}

		@Override
		public <RETURN> Declaration<RETURN> boundThen(
				HandledBiFunction<FIRST, Declaration<FIRST>, RETURN, Exception> fnc) throws Exception {
			return new Declaration<>(fnc.apply(firstArg, this));
		}

		@Override
		public Declaration<FIRST> boundConsume(HandledBiConsumer<FIRST, Declaration<FIRST>, Exception> consumer)
				throws Exception {
			consumer.accept(firstArg, this);
			return this;
		}

	}

	public static class ExitDeclaration<FIRST> extends Declaration<FIRST> implements SingularArgument<FIRST> {

		private ExitDeclaration(FIRST val) {
			super(val);
		}

	}

	public static class BiDeclaration<FIRST, SECOND> extends Declaration<FIRST> implements BiArgument<FIRST, SECOND> {

		protected SECOND secondArg;

		private BiDeclaration(FIRST one, SECOND two) {
			super(one);
			secondArg = two;
		}

		@Override
		public <RETURN> Declaration<RETURN> then(Utils.HandledBiFunction<FIRST, SECOND, RETURN, Exception> fnc)
				throws Exception {
			return new Declaration<>(fnc.apply(firstArg, secondArg));
		}

		@Override
		public <RETURN_ONE, RETURN_TWO> BiDeclaration<RETURN_ONE, RETURN_TWO> map(
				HandledFunction<FIRST, RETURN_ONE, Exception> firstProducer,
				HandledFunction<SECOND, RETURN_TWO, Exception> secondProducer) throws Exception {
			return new BiDeclaration<>(firstProducer.apply(firstArg),
					secondProducer.apply(secondArg));
		}

		@Override
		public <THIRD> TriDeclaration<FIRST, SECOND, THIRD> append(
				HandledBiFunction<FIRST, SECOND, THIRD, Exception> fnc) throws Exception {
			return new TriDeclaration<>(firstArg, secondArg, fnc.apply(firstArg, secondArg));
		}

		@Override
		public <THIRD> TriDeclaration<THIRD, FIRST, SECOND> prepend(
				HandledBiFunction<FIRST, SECOND, THIRD, Exception> fnc) throws Exception {
			return new TriDeclaration<>(fnc.apply(firstArg, secondArg), firstArg, secondArg);
		}

		@Override
		public BiDeclaration<FIRST, SECOND> consume(Utils.HandledBiConsumer<FIRST, SECOND, Exception> consumer)
				throws Exception {
			consumer.accept(firstArg, secondArg);
			return this;
		}

		@Override
		public <THIRD> TriDeclaration<FIRST, SECOND, THIRD> third(THIRD thirdArg) throws Exception {
			return new TriDeclaration<>(firstArg, secondArg, thirdArg);
		}

		public <THIRD> TriDeclaration<FIRST, SECOND, THIRD> third(
				Utils.HandledBiFunction<FIRST, SECOND, THIRD, Exception> fnc) throws Exception {
			return new TriDeclaration<>(firstArg, secondArg, fnc.apply(firstArg, secondArg));
		}

		@Override
		public Declaration<FIRST> useFirst() {
			return new Declaration<>(firstArg);
		}

		@Override
		public Declaration<SECOND> useSecond() {
			return new Declaration<>(secondArg);
		}

		@Override
		public BiDeclaration<SECOND, FIRST> biInverse() {
			return new BiDeclaration<>(secondArg, firstArg);
		}

		@Override
		public <RETURN> Declaration<RETURN> boundThen(
				HandledTriFunction<FIRST, SECOND, BiDeclaration<FIRST, SECOND>, RETURN, Exception> fnc)
				throws Exception {
			return new Declaration<>(fnc.apply(firstArg, secondArg, this));
		}

		@Override
		public BiDeclaration<FIRST, SECOND> boundConsume(
				HandledTriConsumer<FIRST, SECOND, BiDeclaration<FIRST, SECOND>, Exception> consumer) throws Exception {
			consumer.accept(firstArg, secondArg, this);
			return this;
		}

		@Override
		public FIRST getFirst() {
			return firstArg;
		}

		@Override
		public SECOND getSecond() {
			return secondArg;
		}

		@Override
		public <RETURN> Declaration<RETURN> exit(HandledBiFunction<FIRST, SECOND, RETURN, Exception> fnc)
				throws Exception {
			return new ExitDeclaration<>(fnc.apply(firstArg, secondArg));
		}

	}

	public static class TriDeclaration<FIRST, SECOND, THIRD> extends BiDeclaration<FIRST, SECOND>
			implements TriArgument<FIRST, SECOND, THIRD> {

		protected THIRD thirdArg;

		private TriDeclaration(FIRST one, SECOND two, THIRD three) {
			super(one, two);
			thirdArg = three;
		}

		@Override
		public <RETURN> Declaration<RETURN> then(Utils.HandledTriFunction<FIRST, SECOND, THIRD, RETURN, Exception> fnc)
				throws Exception {
			return new Declaration<>(fnc.apply(firstArg, secondArg, thirdArg));
		}

		@Override
		public <RETURN_ONE, RETURN_TWO, RETURN_THREE> TriDeclaration<RETURN_ONE, RETURN_TWO, RETURN_THREE> map(
				HandledFunction<FIRST, RETURN_ONE, Exception> firstProducer,
				HandledFunction<SECOND, RETURN_TWO, Exception> secondProducer,
				HandledFunction<THIRD, RETURN_THREE, Exception> thirdProducer) throws Exception {
			return new TriDeclaration<>(firstProducer.apply(firstArg),
					secondProducer.apply(secondArg), thirdProducer.apply(thirdArg));
		}

		@Override
		public <NEXT_FIRST> TriDeclaration<NEXT_FIRST, SECOND, THIRD> thenPrepend(
				HandledTriFunction<FIRST, SECOND, THIRD, NEXT_FIRST, Exception> nextFirstProducer) throws Exception {
			return new TriDeclaration<>(nextFirstProducer.apply(firstArg, secondArg, thirdArg), secondArg, thirdArg);
		}

		@Override
		public TriDeclaration<FIRST, SECOND, THIRD> consume(
				HandledTriConsumer<FIRST, SECOND, THIRD, Exception> consumer) throws Exception {
			consumer.accept(firstArg, secondArg, thirdArg);
			return this;
		}

		@Override
		public Declaration<THIRD> useThird() {
			return new Declaration<>(thirdArg);
		}

		@Override
		public THIRD getThird() {
			return thirdArg;
		}

		@Override
		public TriDeclaration<THIRD, SECOND, FIRST> triInverse() {
			return new TriDeclaration<>(thirdArg, secondArg, firstArg);
		}

		@Override
		public <NEXT_SECOND> TriDeclaration<FIRST, NEXT_SECOND, THIRD> second(NEXT_SECOND nextSecond) {
			return new TriDeclaration<>(firstArg, nextSecond, thirdArg);
		}

	}

	private static final String CLOSED_ACCESS_MESSAGE_TEMPLATE = "Access to %s was closed";
	private static final String CLOSING_ACCESS_MESSAGE_TEMPLATE = "Closing access to %s";

	public interface Access {

		public static String getClosingMessage(Loggable instance) {
			return String.format(CLOSING_ACCESS_MESSAGE_TEMPLATE, instance.getLoggableName());
		}

		public static String getClosedMessage(Loggable instance) {
			return String.format(CLOSED_ACCESS_MESSAGE_TEMPLATE, instance.getLoggableName());
		}

	}

	public class Wrapper<T> {

		private T value;

		public Wrapper(T value) {
			this.value = value;
		}

		public Wrapper<T> set(T value) {
			this.value = value;
			return this;
		}

		public T get() {
			return value;
		}

	}

	public static class Entry<K, V> implements Map.Entry<K, V> {

		private K key;
		private V value;

		public Entry(K key, V value) {
			super();
			this.key = key;
			this.value = value;
		}

		public static <K, V> Entry<K, V> entry(K key, V val) {
			return new Entry<>(key, val);
		}

		public static <K, V> Entry<K, V> uncheckedEntry(K key, V val) {
			return new Entry<>(key, val);
		}

		@Override
		public K getKey() {
			return key;
		}

		public void setKey(K key) {
			this.key = key;
		}

		@Override
		public V getValue() {
			return value;
		}

		@Override
		public V setValue(V value) {
			V oldVal = this.value;

			this.value = value;

			return oldVal;
		}

		public <T> T map(BiFunction<K, V, T> mapper) {
			return mapper.apply(key, value);
		}

		@Override
		public int hashCode() {
			return Objects.hash(key, value);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if ((obj == null) || (getClass() != obj.getClass()))
				return false;
			Entry<?, ?> other = (Entry<?, ?>) obj;
			return Objects.equals(key, other.key) && Objects.equals(value, other.value);
		}

	}

	private abstract static class LazyLoader<V, L> {

		protected static final Logger logger = LoggerFactory.getLogger(Utils.LazyLoader.class);
		private static final String LOG_MESSAGE = "Invoking lazy loader first time ever";

		protected V value;

		protected L loader;

		protected void log() {
			logger.debug(LOG_MESSAGE);
		}

	}

	public static class LazySupplier<V> extends LazyLoader<V, Supplier<V>> {

		public LazySupplier(Supplier<V> supplier) {
			this.loader = () -> {
				log();
				LazySupplier.this.value = supplier.get();
				LazySupplier.this.loader = () -> LazySupplier.this.value;
				return LazySupplier.this.value;
			};
		}

		public V get() {
			return loader.get();
		}

		@Override
		public String toString() {
			return Optional.ofNullable(get()).map(Object::toString).orElse(StringHelper.NULL);
		}

	}

	public static class HandledLazySupplier<V> extends LazyLoader<V, HandledSupplier<V, Exception>> {

		public HandledLazySupplier(HandledSupplier<V, Exception> supplier) {
			this.loader = new HandledSupplier<>() {
				@Override
				public V get() throws Exception {
					log();
					HandledLazySupplier.this.value = supplier.get();
					HandledLazySupplier.this.loader = () -> HandledLazySupplier.this.value;
					return HandledLazySupplier.this.value;
				}
			};
		}

		public V get() throws Exception {
			return loader.get();
		}

		@Override
		public String toString() {
			try {
				return Optional.ofNullable(get()).map(Object::toString).orElse(StringHelper.NULL);
			} catch (Exception any) {
				any.printStackTrace();
				return StringHelper.NULL;
			}
		}

	}

	public static class LazyFunction<T, V> extends LazyLoader<V, Function<T, V>> {

		public LazyFunction(Function<T, V> producer) {
			this.loader = arg -> {
				log();
				LazyFunction.this.value = producer.apply(arg);
				LazyFunction.this.loader = nextArg -> LazyFunction.this.value;
				return LazyFunction.this.value;
			};
		}

		public V get(T arg) {
			return this.loader.apply(arg);
		}

	}

	@FunctionalInterface
	public interface TriFunction<FIRST, SECOND, THIRD, RETURN> {

		RETURN apply(FIRST f, SECOND s, THIRD t);

	}

	@FunctionalInterface
	public interface TriConsummer<FIRST, SECOND, THIRD> {

		void accept(FIRST f, SECOND s, THIRD t);

	}

	@FunctionalInterface
	public interface HandledFunction<FIRST, RETURN, EXCEPTION extends Exception> {

		RETURN apply(FIRST input) throws EXCEPTION;

		static <F, E extends Exception> HandledFunction<F, F, E> identity() {
			return first -> first;
		}

	}

	@FunctionalInterface
	public interface HandledSupplier<RETURN, EXCEPTION extends Exception> {

		RETURN get() throws EXCEPTION;

	}

	@FunctionalInterface
	public interface HandledTriConsumer<FIRST, SECOND, THIRD, EXCEPTION extends Exception> {

		void accept(FIRST one, SECOND two, THIRD three) throws EXCEPTION;

	}

	@FunctionalInterface
	public interface HandledBiConsumer<FIRST, SECOND, EXCEPTION extends Exception> {

		void accept(FIRST one, SECOND two) throws EXCEPTION;

	}

	@FunctionalInterface
	public interface HandledConsumer<FIRST, EXCEPTION extends Exception> {

		void accept(FIRST input) throws EXCEPTION;

	}

	@FunctionalInterface
	public interface HandledTriFunction<FIRST, SECOND, THIRD, RETURN, EXCEPTION extends Exception> {

		RETURN apply(FIRST fisrt, SECOND second, THIRD third) throws EXCEPTION;

	}

	@FunctionalInterface
	public interface HandledBiFunction<FIRST, SECOND, RETURN, EXCEPTION extends Exception> {

		RETURN apply(FIRST fisrt, SECOND second) throws EXCEPTION;

	}

	public interface When {

	}

	public interface SupplyWhen<RETURN> extends When {

		SupplyWhen<RETURN> then(RETURN value);

		RETURN orElse(RETURN value);

		SupplyWhen<RETURN> then(HandledSupplier<RETURN, Exception> function);

		RETURN orElse(HandledSupplier<RETURN, Exception> function) throws Exception;

		RETURN orElseThrow(Supplier<Exception> exceptionSupplier) throws Exception;

	}

	public static <RETURN> SupplyWhen<RETURN> when(boolean predicate) {
		return new SupplyWhenImpl<>(predicate);
	}

	private abstract static class AbstractWhen {

		protected final boolean predicate;

		public AbstractWhen(boolean predicate) {
			this.predicate = predicate;
		}

	}

	private static class SupplyWhenImpl<RETURN> extends AbstractWhen implements SupplyWhen<RETURN> {

		private RETURN valWhenTrue;
		private HandledSupplier<RETURN, Exception> supplierWhenTrue;

		public SupplyWhenImpl(boolean predicate) {
			super(predicate);
		}

		@Override
		public SupplyWhen<RETURN> then(RETURN value) {
			valWhenTrue = value;
			return this;
		}

		@Override
		public RETURN orElse(RETURN value) {
			return predicate ? valWhenTrue : value;
		}

		@Override
		public SupplyWhen<RETURN> then(HandledSupplier<RETURN, Exception> function) {
			supplierWhenTrue = function;
			return this;
		}

		@Override
		public RETURN orElse(HandledSupplier<RETURN, Exception> function) throws Exception {
			return predicate ? supplierWhenTrue.get() : function.get();
		}

		@Override
		public RETURN orElseThrow(Supplier<Exception> exceptionSupplier) throws Exception {
			if (!predicate) {
				throw exceptionSupplier.get();
			}

			return supplierWhenTrue.get();
		}

	}

	public interface ConsumeWhen extends When {

		ConsumeWhen then(HandledConsumer<Boolean, Exception> consumer);

		void orElse(HandledConsumer<Boolean, Exception> consumer) throws Exception;

	}

	public static ConsumerWhenImpl doWhen(boolean predicate) {
		return new ConsumerWhenImpl(predicate);
	}

	public static class ConsumerWhenImpl extends AbstractWhen implements ConsumeWhen {

		private HandledConsumer<Boolean, Exception> consumerWhenTrue;

		public ConsumerWhenImpl(boolean predicate) {
			super(predicate);
		}

		@Override
		public ConsumeWhen then(HandledConsumer<Boolean, Exception> consumer) {
			consumerWhenTrue = consumer;
			return this;
		}

		@Override
		public void orElse(HandledConsumer<Boolean, Exception> consumer) throws Exception {
			if (predicate) {
				consumerWhenTrue.accept(true);
				return;
			}

			consumer.accept(false);
		}

	}

}
