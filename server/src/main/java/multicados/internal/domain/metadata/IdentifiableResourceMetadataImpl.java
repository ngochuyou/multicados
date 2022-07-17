/**
 * 
 */
package multicados.internal.domain.metadata;

import multicados.internal.domain.IdentifiableResource;

/**
 * @author Ngoc Huy
 *
 */
public class IdentifiableResourceMetadataImpl<I extends IdentifiableResource<?>>
		extends AbstractDomainResourceMetadata<I> implements IdentifiableResourceMetadata<I> {

	private final boolean isIdentifierAutoGenerated;

	public IdentifiableResourceMetadataImpl(Class<I> resourceType, boolean isIdentifierAutoGenerated) {
		super(resourceType);
		this.isIdentifierAutoGenerated = isIdentifierAutoGenerated;
	}

	@Override
	public boolean isIdentifierAutoGenerated() {
		return isIdentifierAutoGenerated;
	}

}