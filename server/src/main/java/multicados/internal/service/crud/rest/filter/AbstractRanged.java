/**
 * 
 */
package multicados.internal.service.crud.rest.filter;

import multicados.internal.service.crud.rest.filter.Filter.Ranged;

/**
 * @author Ngoc Huy
 *
 */
public abstract class AbstractRanged<T> implements Ranged<T> {

	T from;
	T to;

	public T getFrom() {
		return from;
	}

	public void setFrom(T from) {
		this.from = from;
	}

	public T getTo() {
		return to;
	}

	public void setTo(T to) {
		this.to = to;
	}

}
