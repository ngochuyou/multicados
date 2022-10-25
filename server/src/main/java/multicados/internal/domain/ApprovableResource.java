/**
 * 
 */
package multicados.internal.domain;

import java.io.Serializable;
import java.time.temporal.Temporal;

/**
 * @author Ngoc Huy
 *
 */
public interface ApprovableResource<S extends Serializable, I extends IdentifiableResource<S>, T extends Temporal>
		extends DomainResource {

	I getApprovedBy();

	void setApprovedBy(I approvedBy);

	T getApprovedTimestamp();

	void setApprovedTimestamp(T approvedTimestamp);

}
