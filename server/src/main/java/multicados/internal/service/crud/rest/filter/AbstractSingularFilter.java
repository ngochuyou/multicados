/**
 *
 */
package multicados.internal.service.crud.rest.filter;

import multicados.internal.service.crud.rest.filter.Filter.AbstractFilterImplementor;
import multicados.internal.service.crud.rest.filter.Filter.Singular;

/**
 * @author Ngoc Huy
 *
 */
public abstract class AbstractSingularFilter<T> extends AbstractFilterImplementor<T> implements Singular<T> {

	T equal;
	T not;

	@Override
	public T getEqual() {
		return equal;
	}

	public void setEqual(T equal) {
		this.equal = equal;
		expressionProducers.add((path, builder) -> builder.equal(path, this.equal));
	}

	@Override
	public T getNot() {
		return not;
	}

	public void setNot(T not) {
		this.not = not;
		expressionProducers.add((path, builder) -> builder.notEqual(path, this.not));
	}

}
