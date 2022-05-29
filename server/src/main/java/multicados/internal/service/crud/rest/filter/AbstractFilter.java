/**
 * 
 */
package multicados.internal.service.crud.rest.filter;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;

import multicados.internal.helper.Utils.TriFunction;

/**
 * @author Ngoc Huy
 *
 */
public abstract class AbstractFilter<T> implements Filter<T>, Filter.Plural<T>, Filter.Singular<T>, Filter.Ranged<T> {

	private AbstractSingularFilter<T> singular = new AbstractSingularFilter<T>() {};
	private AbstractPluralFilter<T> plural = new AbstractPluralFilter<T>() {};
	private AbstractRangedFilter<T> ranged = new AbstractRangedFilter<T>() {};

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

	public String getLike() {
		return singular.like;
	}

	public void setLike(String like) {
		singular.setLike(like);
	}

	public T getFrom() {
		return ranged.from;
	}

	public void setFrom(T from) {
		ranged.setFrom(from);
	}

	public T getTo() {
		return ranged.to;
	}

	public void setTo(T to) {
		ranged.setTo(to);
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
	public List<TriFunction<String, Path<?>, CriteriaBuilder, Predicate>> getExpressionProducers() {
		return Stream
				.of(singular.getExpressionProducers().stream(), ranged.getExpressionProducers().stream(),
						plural.getExpressionProducers().stream())
				.flatMap(Function.identity()).collect(Collectors.toList());
	}

}
