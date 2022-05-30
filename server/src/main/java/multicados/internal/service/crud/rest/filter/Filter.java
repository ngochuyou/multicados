/**
 * 
 */
package multicados.internal.service.crud.rest.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;

/**
 * @author Ngoc Huy
 *
 */
public interface Filter<T> {

	List<BiFunction<Path<?>, CriteriaBuilder, Predicate>> getExpressionProducers();

	public static abstract class AbstractFilterImplementor<T> implements Filter<T> {

		protected final List<BiFunction<Path<?>, CriteriaBuilder, Predicate>> expressionProducers = new ArrayList<>(
				INIT_CAPACITY);

		private static final int INIT_CAPACITY = 7;

		@Override
		public List<BiFunction<Path<?>, CriteriaBuilder, Predicate>> getExpressionProducers() {
			return expressionProducers;
		}

	}

	public interface Singular<T> extends Filter<T> {

		T getEqual();

		T getNot();

	}

	public interface Matchable extends Filter<String> {

		String getLike();

	}

	public interface Ranged<T> extends Filter<T> {

		T getFrom();

		T getTo();

	}

	public interface Plural<T> extends Filter<T> {

		T[] getIn();

		T[] getNi();

	}

}
