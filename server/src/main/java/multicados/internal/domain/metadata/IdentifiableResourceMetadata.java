/**
 * 
 */
package multicados.internal.domain.metadata;

import multicados.internal.domain.IdentifiableResource;

/**
 * @author Ngoc Huy
 *
 */
public interface IdentifiableResourceMetadata<I extends IdentifiableResource<?>> extends DomainResourceMetadata<I> {

	boolean isIdentifierAutoGenerated();

}