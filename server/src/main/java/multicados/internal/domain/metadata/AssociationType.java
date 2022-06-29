/**
 *
 */
package multicados.internal.domain.metadata;

import java.util.Collection;

/**
 * @author Ngoc Huy
 *
 */
public enum AssociationType {

	/**
	 * Singular
	 */
	ENTITY,

	/**
	 * Plural, could be a {@link Collection} or an array
	 */
	COLLECTION

}
