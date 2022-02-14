/**
 * 
 */
package multicados.domain.entity;

import java.io.Serializable;

import multicados.internal.domain.IdentifiableDomainResource;

/**
 * @author Ngoc Huy
 *
 */
public abstract class Entity<T extends Serializable> implements IdentifiableDomainResource<T> {

	/* ==========METADATAS========== */
	public static final String id_ = "id";
	/* ==========METADATAS========== */

}
