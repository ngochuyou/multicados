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

	T getAppliedTimestamp();

	T getDroppedTimestamp();

}
