/**
 * 
 */
package multicados.internal.context;

import java.lang.reflect.Field;
import java.util.List;

import multicados.internal.domain.DomainResource;
import multicados.internal.domain.metadata.DomainResourceMetadata;

/**
 * Provide specific shared informations about a {@link DomainResource} that can
 * not be generally described by a {@link DomainResourceMetadata}
 * 
 * @author Ngoc Huy
 *
 */
public interface SpecificLogicScopingMetadata<D extends DomainResource> {

	/**
	 * @param resourceType
	 * @return attributes that were scoped to be used by any kind of logic
	 */
	List<Field> getScopedAttributeNames();

}
