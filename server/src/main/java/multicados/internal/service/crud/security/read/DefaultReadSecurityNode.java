/**
 * 
 */
package multicados.internal.service.crud.security.read;

import java.util.HashSet;
import java.util.Set;

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
		authorizedAttributes = new HashSet<>(metadata.getAttributeNames());
	}

	@Override
	protected String getActualAttributeName(String requestedName) {
		return requestedName;
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
