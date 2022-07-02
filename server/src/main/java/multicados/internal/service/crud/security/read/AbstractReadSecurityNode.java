/**
 *
 */
package multicados.internal.service.crud.security.read;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.hibernate.internal.util.collections.CollectionHelper;
import org.springframework.security.core.GrantedAuthority;

import multicados.internal.domain.DomainResource;
import multicados.internal.domain.metadata.DomainResourceAttributesMetadata;
import multicados.internal.domain.metadata.DomainResourceMetadata;
import multicados.internal.security.CredentialException;

/**
 * @author Ngoc Huy
 *
 */
public abstract class AbstractReadSecurityNode<D extends DomainResource> implements ReadSecurityNode<D> {

	private final DomainResourceAttributesMetadata<D> metadata;
	private final ReadFailureExceptionHandler exceptionHandler;

	@SuppressWarnings("unchecked")
	public AbstractReadSecurityNode(DomainResourceMetadata<D> metadata, ReadFailureExceptionHandler exceptionHandler) {
		this.metadata = metadata.unwrap(DomainResourceAttributesMetadata.class);
		this.exceptionHandler = exceptionHandler;
	}

	@Override
	public List<String> check(Collection<String> requestedAttributes, GrantedAuthority credential)
			throws CredentialException, UnknownAttributesException {
		String credentialValue = credential.getAuthority();
		Set<String> authorizedAttributesByCredential = getAuthorizedAttributes(credentialValue);
		int authorizedAttributesSpan = authorizedAttributesByCredential.size();

		if (CollectionHelper.isEmpty(authorizedAttributesByCredential)) {
			exceptionHandler.doOnUnauthorizedCredential(metadata.getResourceType(), credentialValue);
		}

		if (CollectionHelper.isEmpty(requestedAttributes)) {
			requestedAttributes = giveSomeAttributes(authorizedAttributesByCredential);
		}

		List<String> checkedAttributes = new ArrayList<>(authorizedAttributesSpan);
		List<String> unauthorziedAttributes = new ArrayList<>(authorizedAttributesSpan);

		for (String requestedAttribute : requestedAttributes) {
			String actualAttributeName = getActualAttributeName(requestedAttribute);

			if (!authorizedAttributesByCredential.contains(actualAttributeName)) {
				unauthorziedAttributes.add(requestedAttribute);
				continue;
			}

			checkedAttributes.add(actualAttributeName);
		}

		if (!unauthorziedAttributes.isEmpty()) {
			exceptionHandler.doOnUnauthorizedAttribute(metadata.getResourceType(), credentialValue,
					unauthorziedAttributes);
		}

		return checkedAttributes;
	}

	private List<String> giveSomeAttributes(Set<String> authorizedAttributesByCredential) {
		return metadata.getNonLazyAttributeNames().stream().filter(authorizedAttributesByCredential::contains).toList();
	}

	protected abstract String getActualAttributeName(String requestedName);

	protected abstract Set<String> getAuthorizedAttributes(String credentialValue);

	protected DomainResourceAttributesMetadata<D> getMetadata() {
		return metadata;
	}

}
