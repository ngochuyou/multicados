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

	private static final String LIKE_TEMPLATE = "%%%s%%";

	T equal;
	T not;
	String like;

	public T getEqual() {
		return equal;
	}

	public void setEqual(T equal) {
		this.equal = equal;
		expressionProducers.add((attributeName, path, builder) -> builder.equal(path.get(attributeName), this.equal));
	}

	public T getNot() {
		return not;
	}

	public void setNot(T not) {
		this.not = not;
		expressionProducers.add((attributeName, path, builder) -> builder.notEqual(path.get(attributeName), this.not));
	}

	public String getLike() {
		return like;
	}

	public void setLike(String like) {
		this.like = like;
		expressionProducers
				.add((attributeName, path, builder) -> builder.like(path.get(attributeName), getLikeValue()));
	}

	private String getLikeValue() {
		return String.format(LIKE_TEMPLATE, this.like);
	}

}
