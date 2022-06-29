/**
 *
 */
package multicados.internal.domain.metadata;

import java.util.Map;

import multicados.internal.domain.DomainResource;

/**
 * Strategy for building {@link DomainResourceMetadata}
 *
 *
 * @author Ngoc Huy
 *
 */
public interface DomainResourceMetadataBuilder {

	/**
	 * @param <D>
	 * @param resourceType
	 * @param onGoingMetadatas a metadata cache contains previously built
	 *                         {@link DomainResourceMetadata}. Metadata-s in this
	 *                         cache are required to be built top-down regarding the
	 *                         inheritance tree
	 * @return
	 * @throws Exception
	 */
	<D extends DomainResource> DomainResourceMetadata<D> build(Class<D> resourceType,
			Map<Class<? extends DomainResource>, DomainResourceMetadata<? extends DomainResource>> onGoingMetadatas) throws Exception;

}
