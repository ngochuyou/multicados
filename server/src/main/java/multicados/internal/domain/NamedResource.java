/**
 * 
 */
package multicados.internal.domain;

/**
 * @author Ngoc Huy
 *
 */
public interface NamedResource extends DomainResource {

	String getName();

	void setName(String name);

	String name_ = "name";

}
