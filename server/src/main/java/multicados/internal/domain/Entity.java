/**
 * 
 */
package multicados.internal.domain;

import java.io.Serializable;

/**
 * @author Ngoc Huy
 *
 */
public abstract class Entity<T extends Serializable> implements IdentifiableDomainResource<T> {

	/* ==========METADATAS========== */
	public static final String id_ = "id";
	/* ==========METADATAS========== */

}
