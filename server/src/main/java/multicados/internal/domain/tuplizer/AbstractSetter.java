/**
 * 
 */
package multicados.internal.domain.tuplizer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import multicados.internal.helper.FunctionHelper.HandledBiConsumer;
import multicados.internal.helper.FunctionHelper.HandledBiFunction;

/**
 * @author Ngoc Huy
 *
 */
public abstract class AbstractSetter implements Setter {

	private final boolean isPrimitive;

	private final HandledBiConsumer<Object, Object, Exception> invoker;

	@SafeVarargs
	public AbstractSetter(String propName, Class<?> owningType,
			HandledBiFunction<String, Class<?>, Method, Exception>... setterMethodProducer) throws Exception {
		boolean isPrimitive = true;
		HandledBiConsumer<Object, Object, Exception> invoker;

		if (setterMethodProducer.length != 0) {
			isPrimitive = setterMethodProducer[0].apply(propName, owningType).getParameterTypes()[0].isPrimitive();
			invoker = (source, val) -> ((Method) getMember()).invoke(source, val);
		} else {
			Field field = owningType.getDeclaredField(propName);

			isPrimitive = field.getType().isPrimitive();
			invoker = (source, val) -> ((Field) getMember()).set(source, val);
		}
		
		this.isPrimitive = isPrimitive;
		this.invoker = invoker;
	}

	@Override
	public void set(Object source, Object val) throws Exception {
		if (val == null && isPrimitive) {
			throw new IllegalArgumentException("A null value was used for a primitive property");
		}

		invoker.accept(source, val);
	}

}
