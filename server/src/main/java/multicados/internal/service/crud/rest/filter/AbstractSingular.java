/**
 * 
 */
package multicados.internal.service.crud.rest.filter;

import multicados.internal.service.crud.rest.filter.Filter.Singular;

/**
 * @author Ngoc Huy
 *
 */
public abstract class AbstractSingular<T> implements Singular<T> {

	T equal;
	T not;
	String like;

	public T getEqual() {
		return equal;
	}

	public void setEqual(T equal) {
		this.equal = equal;
	}

	public T getNot() {
		return not;
	}

	public void setNot(T not) {
		this.not = not;
	}

	public String getLike() {
		return like;
	}

	public void setLike(String like) {
		this.like = like;
	}

}
