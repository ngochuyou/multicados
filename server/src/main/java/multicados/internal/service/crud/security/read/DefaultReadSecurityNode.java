/**
 * 
 */
package multicados.internal.service.crud.security.read;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import multicados.internal.domain.DomainResource;
import multicados.internal.domain.metadata.DomainResourceMetadata;
import multicados.internal.helper.CollectionHelper;

/**
 * @author Ngoc Huy
 *
 */
public class DefaultReadSecurityNode<D extends DomainResource> extends AbstractReadSecurityNode<D> {

	private final Set<String> authorizedAttributes;
	private final List<String> nonAssociationAttributes;

	public DefaultReadSecurityNode(DomainResourceMetadata<D> metadata, ReadFailureExceptionHandler exceptionThrower) {
		super(metadata, exceptionThrower);
		authorizedAttributes = new HashSet<>(metadata.getAttributeNames());
		nonAssociationAttributes = authorizedAttributes.stream().filter(attribute -> !metadata.isAssociation(attribute))
				.collect(Collectors.toList());
	}

	@Override
	protected String getActualAttributeName(String requestedName) {
		return requestedName;
	}

	@Override
	public Map<String, String> translate(Collection<String> attributes) {
		return attributes.stream().map(attributeName -> Map.entry(attributeName, attributeName))
				.collect(CollectionHelper.toMap());
	}

	@Override
	protected Set<String> getAuthorizedAttributes(String credentialValue) {
		return authorizedAttributes;
	}

	@Override
	protected List<String> getNonAssociationAttributes(String credentialValue) {
		return nonAssociationAttributes;
	}

	@Override
	public String toString() {
		return String.format("%s<%s>", this.getClass().getSimpleName(),
				getMetadata().getResourceType().getSimpleName());
	}

}
