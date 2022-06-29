/**
 *
 */
package multicados.internal.service.crud.rest.filter;

import javax.persistence.criteria.Expression;

import multicados.internal.service.crud.rest.filter.Filter.AbstractFilterImplementor;
import multicados.internal.service.crud.rest.filter.Filter.Ranged;

/**
 * @author Ngoc Huy
 *
 */
public abstract class AbstractRangedFilter<T> extends AbstractFilterImplementor<T> implements Ranged<T> {

	T from;
	T to;

	@Override
	public T getFrom() {
		return from;
	}

	public void setFrom(T from) {
		this.from = from;

		if (to == null) {
			return;
		}

		addProducer();
	}

	@Override
	public T getTo() {
		return to;
	}

	public void setTo(T to) {
		this.to = to;

		if (from == null) {
			return;
		}

		addProducer();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void addProducer() {
		expressionProducers.add((path, builder) -> builder.between((Expression) path,
				(Expression) builder.literal(from), (Expression) builder.literal(to)));
	}

}
