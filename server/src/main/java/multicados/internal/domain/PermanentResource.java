/**
 *
 */
package multicados.internal.domain;

/**
 * @author Ngoc Huy
 *
 */
public interface PermanentResource extends DomainResource {

	boolean isActive();

	void setActive(boolean activeState);

	static final String ACTIVE = "active";

}
