/**
 *
 */
package multicados.internal.domain;

import javax.persistence.MappedSuperclass;

/**
 * @author Ngoc Huy
 *
 */
@MappedSuperclass
public interface NamedResource extends DomainResource {

	String getName();

	void setName(String name);

}
