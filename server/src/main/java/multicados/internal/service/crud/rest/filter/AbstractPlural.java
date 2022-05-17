/**
 * 
 */
package multicados.internal.service.crud.rest.filter;

import multicados.internal.service.crud.rest.filter.Filter.Plural;

/**
 * @author Ngoc Huy
 *
 */
public abstract class AbstractPlural<T> implements Plural<T> {

	T[] in;
	T[] ni;

	public T[] getIn() {
		return in;
	}

	public void setIn(T[] in) {
		this.in = in;
	}

	public T[] getNi() {
		return ni;
	}

	public void setNi(T[] notIn) {
		this.ni = notIn;
	}

}
