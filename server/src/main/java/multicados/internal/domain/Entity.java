/**
 * 
 */
package multicados.internal.domain;

import java.io.Serializable;

/**
 * @author Ngoc Huy
 *
 */
public abstract class Entity<T extends Serializable> implements DomainResource {

	public abstract T getId();

	public abstract void setId(T id);
	
	/* ==========METADATAS========== */
	public static final String id_ = "id";
	/* ==========METADATAS========== */

}
