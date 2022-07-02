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
public class NamedResourceMetadataImpl<N extends NamedResource> extends AbstractDomainResourceMetadata<N>
		implements NamedResourceMetadata<N> {

	private final List<Field> scopedAttributes;

	public NamedResourceMetadataImpl(Class<N> resourceType, List<Field> scopedAttributes) {
		super(resourceType);
		this.scopedAttributes = scopedAttributes;
	}

	@Override
	public List<Field> getScopedAttributeNames() {
		return scopedAttributes;
	}

}
