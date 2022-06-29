/**
 *
 */
package multicados.internal.domain;

import java.io.Serializable;

/**
 * @author Ngoc Huy
 *
 */
public interface IdentifiableResource<S extends Serializable> extends DomainResource {

	S getId();

	void setId(S id);

}
