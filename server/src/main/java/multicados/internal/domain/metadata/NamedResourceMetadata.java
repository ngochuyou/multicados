/**
 * 
 */
package multicados.internal.domain.metadata;

import java.lang.reflect.Field;
import java.util.List;

import multicados.internal.domain.NamedResource;

/**
 * @author Ngoc Huy
 *
 */
public interface NamedResourceMetadata<N extends NamedResource> extends DomainResourceMetadata<N> {

	/**
	 * @param resourceType
	 * @return attributes that were scoped to be used in {@link NamedResource}
	 *         logics
	 */
	List<Field> getScopedAttributeNames();

}
