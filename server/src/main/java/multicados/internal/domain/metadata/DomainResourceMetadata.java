/**
 * 
 */
package multicados.internal.domain.metadata;

import java.util.List;
import java.util.Map;

import multicados.internal.domain.DomainComponentType;
import multicados.internal.domain.DomainResource;

/**
 * A contract in describing a {@link DomainResource} type
 * 
 * @author Ngoc Huy
 *
 * @param <T>
 */
public interface DomainResourceMetadata<T extends DomainResource> {

	/**
	 * @return the {@link DomainResource} type which this metadata is describing
	 */
	Class<T> getResourceType();

	/**
	 * @return all of the attribute names owned by this {@link DomainResource} type,
	 *         including the ones from which were inherited from it's parents.
	 *         {@link DomainComponentType} attributes will be unwrapped all the way
	 *         and themselves will be included.
	 */
	List<String> getAttributeNames();

	/**
	 * @return the same as {@link DomainResourceMetadata#getAttributeNames()} except
	 *         that {@link DomainComponentType} attributes won't be unwrapped
	 */
	List<String> getWrappedAttributeNames();

	/**
	 * @param attributeName
	 * @return get the type of the requested attributeName
	 * @throws IllegalArgumentException if the attributeName isn't owned by this
	 *                                  {@link DomainResource}
	 */
	Class<?> getAttributeType(String attributeName);

	/**
	 * Non-lazy attributes are those which usually not retrieved from an data store
	 * 
	 * @return non-lazy attributes
	 */
	List<String> getNonLazyAttributeNames();

	/**
	 * An association is an owned attribute by this {@link DomainResource} and is
	 * described as another {@link DomainResource}
	 * 
	 * @param attributeName
	 * @return whether the requested attribute is an association
	 */
	boolean isAssociation(String attributeName);

	/**
	 * @param attributeName
	 * @return whether the association is plural or singular
	 * @throws IllegalArgumentException if the attributeName isn't owned by this
	 *                                  {@link DomainResource}
	 */
	AssociationType getAssociationType(String attributeName);

	/**
	 * @param associationName
	 * @return whether the association optional
	 * @throws IllegalArgumentException if the attributeName isn't owned by this
	 *                                  {@link DomainResource}
	 */
	boolean isAssociationOptional(String associationName);

	/**
	 * @param attributeName
	 * @return whether the association is a {@link DomainComponentType}
	 * @throws IllegalArgumentException if the attributeName isn't owned by this
	 *                                  {@link DomainResource}
	 */
	boolean isComponent(String attributeName);

	/**
	 * @return the full paths of every {@link DomainComponentType} owned by this
	 *         {@link DomainResource}
	 */
	Map<String, ComponentPath> getComponentPaths();

}
