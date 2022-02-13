/**
 * 
 */
package multicados.internal.domain.metadata;

import java.util.List;

import multicados.internal.domain.DomainResource;

/**
 * @author Ngoc Huy
 *
 */
public interface DomainResourceMetadata<T extends DomainResource> {

	Class<T> getResourceType();

	List<String> getAttributeNames();

	Class<?> getAttributeType(String attributeName);

	List<String> getNonLazyAttributeNames();

	boolean isAssociation(String attributeName);

//	AssociationType getAssociationType(String associationName);

}
