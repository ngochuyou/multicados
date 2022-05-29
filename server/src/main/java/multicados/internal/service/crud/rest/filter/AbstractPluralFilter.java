/**
 * 
 */
package multicados.internal.service.crud.rest.filter;

import java.util.List;

import multicados.internal.service.crud.rest.filter.Filter.AbstractFilterImplementor;
import multicados.internal.service.crud.rest.filter.Filter.Plural;

/**
 * @author Ngoc Huy
 *
 */
public abstract class AbstractPluralFilter<T> extends AbstractFilterImplementor<T> implements Plural<T> {

	T[] in;
	T[] ni;

	public T[] getIn() {
		return in;
	}

	public void setIn(T[] in) {
		this.in = in;
		expressionProducers
				.add((attributeName, path, builder) -> builder.in(path.get(attributeName)).value(List.of(in)));
	}

	public T[] getNi() {
		return ni;
	}

	public void setNi(T[] notIn) {
		this.ni = notIn;
		expressionProducers.add(
				(attributeName, path, builder) -> builder.not(builder.in(path.get(attributeName)).value(List.of(ni))));
	}

}
