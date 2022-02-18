/**
 * 
 */
package multicados.internal.domain;

/**
 * @author Ngoc Huy
 *
 */
public interface PermanentResource extends DomainResource {

	Boolean isActive();

	void setActive(Boolean activeState);

	String active_ = "active";
	
}
