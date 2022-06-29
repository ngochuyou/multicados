/**
 *
 */
package multicados.internal.service.crud.rest.filter;

import java.util.Collection;
import java.util.List;

import javax.persistence.criteria.Path;

import multicados.internal.service.crud.rest.filter.Filter.AbstractFilterImplementor;
import multicados.internal.service.crud.rest.filter.Filter.Plural;

/**
 * @author Ngoc Huy
 *
 */
public abstract class AbstractPluralFilter<T> extends AbstractFilterImplementor<T> implements Plural<T> {

	T[] in;
	T[] ni;

	@Override
	public T[] getIn() {
		return in;
	}

	@SuppressWarnings("unchecked")
	public void setIn(T[] in) {
		this.in = in;
		expressionProducers.add((path, builder) -> builder.in((Path<Collection<?>>) path).value(List.of(in)));
	}

	@Override
	public T[] getNi() {
		return ni;
	}

	@SuppressWarnings("unchecked")
	public void setNi(T[] notIn) {
		this.ni = notIn;
		expressionProducers
				.add((path, builder) -> builder.not(builder.in((Path<Collection<?>>) path).value(List.of(ni))));
	}

}
