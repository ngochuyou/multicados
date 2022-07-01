/**
 * 
 */
package multicados.internal.context;

import multicados.internal.domain.DomainResource;

/**
 * Provide necessary dependencies for the process of building
 * {@link DomainLogicUtils} components
 * 
 * @author Ngoc Huy
 *
 */
public interface DomainLogicUtils extends ContextBuilder {

	/**
	 * @param <D>
	 * @param resourceType
	 * @return the {@link SpecificLogicScopingMetadata}
	 */
	<D extends DomainResource> SpecificLogicScopingMetadata<D> getScopingMetadata(Class<D> resourceType);

}
