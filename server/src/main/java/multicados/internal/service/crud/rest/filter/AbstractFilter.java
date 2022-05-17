/**
 * 
 */
package multicados.internal.service.crud.rest.filter;

/**
 * @author Ngoc Huy
 *
 */
public abstract class AbstractFilter<T> implements Filter<T>, Filter.Plural<T>, Filter.Singular<T>, Filter.Ranged<T> {

	private AbstractSingular<T> singular = new AbstractSingular<T>() {};
	private AbstractPlural<T> plural = new AbstractPlural<T>() {};
	private AbstractRanged<T> ranged = new AbstractRanged<T>() {};

	public T getEqual() {
		return singular.equal;
	}

	public void setEqual(T equal) {
		singular.equal = equal;
	}

	public T getNot() {
		return singular.not;
	}

	public void setNot(T not) {
		singular.not = not;
	}

	public String getLike() {
		return singular.like;
	}

	public void setLike(String like) {
		singular.like = like;
	}

	public T getFrom() {
		return ranged.from;
	}

	public void setFrom(T from) {
		ranged.from = from;
	}

	public T getTo() {
		return ranged.to;
	}

	public void setTo(T to) {
		ranged.to = to;
	}

	public T[] getIn() {
		return plural.in;
	}

	public void setIn(T[] in) {
		plural.in = in;
	}

	public T[] getNi() {
		return plural.ni;
	}

	public void setNotIn(T[] notIn) {
		plural.ni = notIn;
	}

}
