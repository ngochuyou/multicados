/**
 *
 */
package multicados.internal.domain;

import java.time.temporal.TemporalAccessor;

/**
 * @author Ngoc Huy
 *
 */
public interface SpannedResource<T extends TemporalAccessor> extends DomainResource {

	T getStartTimestamp();

	void setStartTimestamp(T temporal);

	T getEndTimestamp();

	void setEndTimestamp(T temporal);

}
