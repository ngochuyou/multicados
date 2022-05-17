/**
 * 
 */
package multicados.internal.service.crud.rest.filter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;

/**
 * @author Ngoc Huy
 *
 */
public interface Filters {

	public class BooleanFilter extends AbstractPluralSingularFilter<Boolean> {
	};

	public class StringFilter extends AbstractPluralSingularFilter<String> {
	};

	public class IntegerFilter extends AbstractFilter<Integer> {
	};

	public class LongFilter extends AbstractFilter<Long> {
	};

	public class FloatFilter extends AbstractFilter<Float> {
	};

	public class DoubleFilter extends AbstractFilter<Double> {
	};

	public class EnumFilter<E extends Enum<?>> extends AbstractPluralSingularFilter<E> {
	};

	public class BigIntegerFilter extends AbstractFilter<BigInteger> {
	};

	public class BigDecimalFilter extends AbstractFilter<BigDecimal> {
	};

	public class LocalTimeFilter extends AbstractFilter<LocalTime> {
	};

	public class LocalDateFilter extends AbstractFilter<LocalDate> {
	};

	public class LocalDateTimeFilter extends AbstractFilter<LocalDateTime> {
	};

	public class ZonedDateTimeFilter extends AbstractFilter<ZonedDateTime> {
	};

}
