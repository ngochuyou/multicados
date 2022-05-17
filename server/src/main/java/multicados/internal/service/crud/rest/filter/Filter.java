/**
 * 
 */
package multicados.internal.service.crud.rest.filter;

/**
 * @author Ngoc Huy
 *
 */
public interface Filter<T> {

	public interface Singular<T> {

		T getEqual();

		T getNot();

		String getLike();

	}

	public interface Ranged<T> {

		T getFrom();

		T getTo();

	}

	public interface Plural<T> {

		T[] getIn();

		T[] getNi();

	}

}
