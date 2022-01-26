/**
 * 
 */
package multicados.internal.helper;

/**
 * @author Ngoc Huy
 *
 */
public class FunctionHelper {
	// we use FIRST, SECOND, THIRD,... to avoid types conflict
	private FunctionHelper() {}

	@FunctionalInterface
	public interface HandledFunction<FIRST, RETURN, EXCEPTION extends Exception> {

		RETURN apply(FIRST input) throws EXCEPTION;

	}

	@FunctionalInterface
	public interface HandledBiFunction<FIRST, SECOND, RETURN, EXCEPTION extends Exception> {

		RETURN apply(FIRST fisrt, SECOND second) throws EXCEPTION;

	}

	@FunctionalInterface
	public interface HandledTriFunction<FIRST, SECOND, THIRD, RETURN, EXCEPTION extends Exception> {

		RETURN apply(FIRST fisrt, SECOND second, THIRD third) throws EXCEPTION;

	}

	@FunctionalInterface
	public interface HandledConsumer<FIRST, EXCEPTION extends Exception> {

		void accept(FIRST input) throws EXCEPTION;

	}

	@FunctionalInterface
	public interface HandledBiConsumer<FIRST, SECOND, EXCEPTION extends Exception> {

		void accept(FIRST one, SECOND two) throws EXCEPTION;

	}

	@FunctionalInterface
	public interface HandledTriConsumer<FIRST, SECOND, THIRD, EXCEPTION extends Exception> {

		void accept(FIRST one, SECOND two, THIRD three) throws EXCEPTION;

	}

	@FunctionalInterface
	public interface HandledSupplier<RETURN, EXCEPTION extends Exception> {

		RETURN get() throws EXCEPTION;

	}

}
