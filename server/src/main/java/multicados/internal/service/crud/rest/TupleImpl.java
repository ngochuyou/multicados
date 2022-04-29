/**
 * 
 */
package multicados.internal.service.crud.rest;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.persistence.Tuple;
import javax.persistence.TupleElement;

import multicados.internal.helper.TypeHelper;

/**
 * @author Ngoc Huy
 *
 */
public class TupleImpl implements Tuple {

	private final Object[] values;

	public TupleImpl(Tuple tuple, List<Object> additions) {
		values = Stream.of(Stream.of(tuple.toArray()), Stream.of(additions.toArray())).flatMap(Function.identity())
				.toArray();
	}

	@Override
	public <X> X get(TupleElement<X> tupleElement) {
		return get(tupleElement.getAlias(), tupleElement.getJavaType());
	}

	@Override
	public <X> X get(String alias, Class<X> type) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object get(String alias) {
		throw new UnsupportedOperationException();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <X> X get(int i, Class<X> type) {
		Object value = values[i];

		if (value.getClass().equals(type)) {
			return (X) value;
		}

		try {
			return TypeHelper.cast((Class<Object>) value.getClass(), type, value);
		} catch (Exception any) {
			throw new IllegalArgumentException(any);
		}
	}

	@Override
	public Object get(int i) {
		return values[i];
	}

	@Override
	public Object[] toArray() {
		return values;
	}

	@Override
	public List<TupleElement<?>> getElements() {
		throw new UnsupportedOperationException();
	}

}
