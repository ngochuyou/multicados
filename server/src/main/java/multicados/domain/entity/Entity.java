/**
 * 
 */
package multicados.domain.entity;

import java.io.Serializable;

import multicados.internal.domain.IdentifiableResource;

/**
 * @author Ngoc Huy
 *
 */
public abstract class Entity<T extends Serializable> implements IdentifiableResource<T> {

	/* ==========METADATAS========== */
	public static final String id_ = "id";
	/* ==========METADATAS========== */

}
