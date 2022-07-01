/**
 * 
 */
package multicados.internal.context;

import java.lang.reflect.Field;
import java.util.List;

import multicados.internal.domain.DomainResource;

/**
 * @author Ngoc Huy
 *
 */
public class MetadataProviderImpl<D extends DomainResource> implements SpecificLogicScopingMetadata<D> {

	private final List<Field> scopedAttributeNames;

	public MetadataProviderImpl(List<Field> scopedAttributeNames) {
		this.scopedAttributeNames = scopedAttributeNames;
	}

	@Override
	public List<Field> getScopedAttributeNames() {
		return scopedAttributeNames;
	}

}
