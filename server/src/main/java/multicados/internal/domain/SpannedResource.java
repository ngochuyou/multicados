/**
 * 
 */
package multicados.internal.domain;

import java.time.temporal.Temporal;

/**
 * @author Ngoc Huy
 *
 */
public interface SpannedResource<T extends Temporal> {

	T getAppliedTimestamp();

	T getDroppedTimestamp();

	String appliedTimestamp_ = "appliedTimestamp";
	String droppedTimestamp_ = "droppedTimestamp";

}
