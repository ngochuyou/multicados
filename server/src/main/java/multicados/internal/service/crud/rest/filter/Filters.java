/**
 *
 */
package multicados.internal.service.crud.rest.filter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;

/**
 * @author Ngoc Huy
 *
 */
public interface Filters {

	public class BooleanFilter extends AbstractSingularFilter<Boolean> {
	}

	public class StringFilter extends AbstractSingularPluralFilter<String> implements Filter.Matchable {

		private static final String LIKE_TEMPLATE = "%%%s%%";

		String like;

		@Override
		public String getLike() {
			return like;
		}

		@SuppressWarnings("unchecked")
		public void setLike(String like) {
			this.like = like;
			expressionProducers.add((path, builder) -> builder.like((Path<String>) path, getLikeValue()));
		}

		private String getLikeValue() {
			return String.format(LIKE_TEMPLATE, this.like);
		}

		@Override
		public List<BiFunction<Path<?>, CriteriaBuilder, Predicate>> getExpressionProducers() {
			// @formatter:off
			return Stream.of(
						singular.expressionProducers.stream(),
						plural.expressionProducers.stream(),
						expressionProducers.stream())
					.flatMap(Function.identity())
					.toList();
			// @formatter:on
		}

	}

	public class IntegerFilter extends AbstractSingularRangedPluralFilter<Integer> {
	}

	public class LongFilter extends AbstractSingularRangedPluralFilter<Long> {
	}

	public class FloatFilter extends AbstractSingularRangedPluralFilter<Float> {
	}

	public class DoubleFilter extends AbstractSingularRangedPluralFilter<Double> {
	}

	public class EnumFilter<E extends Enum<?>> extends AbstractSingularPluralFilter<E> {
	}

	public class BigIntegerFilter extends AbstractSingularRangedPluralFilter<BigDecimal> {
	}

	public class BigDecimalFilter extends AbstractSingularRangedPluralFilter<BigDecimal> {
	}

	public class LocalTimeFilter extends AbstractSingularRangedPluralFilter<LocalTime> {
	}

	public class LocalDateFilter extends AbstractSingularRangedPluralFilter<LocalDate> {
	}

	public class LocalDateTimeFilter extends AbstractSingularRangedPluralFilter<LocalDateTime> {
	}

	public class ZonedDateTimeFilter extends AbstractSingularRangedPluralFilter<ZonedDateTime> {
	}

	public class UUIDFilter extends AbstractSingularPluralFilter<UUID> {
	}

}
