/**
 * 
 */
package multicados.internal.service.crud.security.read;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import multicados.internal.domain.DomainResource;
import multicados.internal.domain.metadata.DomainResourceMetadata;

/**
 * @author Ngoc Huy
 *
 */
public class DefaultReadSecurityNode<D extends DomainResource> extends AbstractReadSecurityNode<D> {

	private final Set<String> authorizedAttributes;

	public DefaultReadSecurityNode(DomainResourceMetadata<D> metadata, ReadFailureExceptionHandler exceptionThrower) {
		super(metadata, exceptionThrower);
		authorizedAttributes = new HashSet<>(metadata.getNonLazyAttributeNames());
	}

	@Override
	protected String getActualAttributeName(String requestedName) {
		return requestedName;
	}

	@Override
	public Map<String, String> translate(Collection<String> attributes) {
		return authorizedAttributes.stream().map(attributeName -> Map.entry(attributeName, attributeName))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	@Override
	protected Set<String> getAuthorizedAttributes(String credentialValue) {
		return authorizedAttributes;
	}

	@Override
	public String toString() {
		return String.format("%s<%s>", this.getClass().getSimpleName(),
				getMetadata().getResourceType().getSimpleName());
	}

}
