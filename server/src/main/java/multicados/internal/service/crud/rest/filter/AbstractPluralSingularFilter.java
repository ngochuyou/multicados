/**
 * 
 */
package multicados.internal.service.crud.rest.filter;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;

import multicados.internal.service.crud.rest.filter.Filter.AbstractFilterImplementor;

/**
 * @author Ngoc Huy
 *
 */
public abstract class AbstractPluralSingularFilter<T> extends AbstractFilterImplementor<T>
		implements Filter<T>, Filter.Plural<T>, Filter.Singular<T> {

	protected AbstractSingularFilter<T> singular = new AbstractSingularFilter<T>() {};
	protected AbstractPluralFilter<T> plural = new AbstractPluralFilter<T>() {};

	public T getEqual() {
		return singular.equal;
	}

	public void setEqual(T equal) {
		singular.setEqual(equal);
	}

	public T getNot() {
		return singular.not;
	}

	public void setNot(T not) {
		singular.setNot(not);
	}

	public T[] getIn() {
		return plural.in;
	}

	public void setIn(T[] in) {
		plural.setIn(in);
	}

	public T[] getNi() {
		return plural.ni;
	}

	public void setNi(T[] notIn) {
		plural.setNi(notIn);
	}

	@Override
	public List<BiFunction<Path<?>, CriteriaBuilder, Predicate>> getExpressionProducers() {
		// @formatter:off
		return Stream.of(
					singular.expressionProducers.stream(),
					plural.expressionProducers.stream())
				.flatMap(Function.identity())
				.collect(Collectors.toList());
		// @formatter:on
	}

}
