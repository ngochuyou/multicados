/**
 * 
 */
package multicados.internal.domain.metadata;

import java.util.List;
import java.util.Map;

import multicados.internal.domain.DomainResource;

/**
 * @author Ngoc Huy
 *
 */
public interface DomainResourceMetadata<T extends DomainResource> {

	Class<T> getResourceType();

	List<String> getAttributeNames();

	List<String> getEnclosedAttributeNames();

	Class<?> getAttributeType(String attributeName);

	Map<String, Class<?>> getAttributeTypes();

	List<String> getNonLazyAttributeNames();

	boolean isAssociation(String attributeName);

	boolean isAssociationOptional(String associationName);

	boolean isComponent(String attributeName);
	
	Map<String, ComponentPath> getComponentPaths();

	AssociationType getAssociationType(String attributeName);
	
}
