/**
 * 
 */
package multicados.internal.domain;

import java.io.Serializable;

/**
 * @author Ngoc Huy
 *
 */
public interface Entity<S extends Serializable> extends IdentifiableResource<S> {

	/* ==========METADATAS========== */
	String id_ = "id";
	/* ==========METADATAS========== */

}
