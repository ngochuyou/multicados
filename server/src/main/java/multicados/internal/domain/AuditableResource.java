/**
 * 
 */
package multicados.internal.domain;

import java.io.Serializable;
import java.time.temporal.TemporalAccessor;

/**
 * @author Ngoc Huy
 *
 */
public interface AuditableResource<S extends Serializable, A extends ResourceAuditor<S>, T extends TemporalAccessor>
		extends DomainResource {

	T getCreatedTimestamp();

	void setCreatedTimestamp(T timestamp);

	A getCreator();

	void setCreator(A creator);

	T getUpdatedTimestamp();

	void setUpdatedTimestamp(T timestamp);

	A getUpdater();

	void setUpdater(A updater);

}
