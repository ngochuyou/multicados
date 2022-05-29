/**
 * 
 */
package multicados.internal.service.crud.security.read;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.hibernate.internal.util.collections.CollectionHelper;

import multicados.internal.domain.DomainResource;
import multicados.internal.domain.metadata.DomainResourceMetadata;
import multicados.internal.security.CredentialException;
import multicados.internal.service.crud.security.CRUDCredential;

/**
 * @author Ngoc Huy
 *
 */
public abstract class AbstractReadSecurityNode<D extends DomainResource> implements ReadSecurityNode<D> {

	private final DomainResourceMetadata<D> metadata;
	private final ReadFailureExceptionHandler exceptionHandler;

	public AbstractReadSecurityNode(DomainResourceMetadata<D> metadata, ReadFailureExceptionHandler exceptionHandler) {
		this.metadata = metadata;
		this.exceptionHandler = exceptionHandler;
	}

	@Override
	public List<String> check(Collection<String> requestedAttributes, CRUDCredential credential)
			throws CredentialException, UnknownAttributesException {
		String credentialValue = credential.evaluate();
		Set<String> authorizedAttributesByCredential = getAuthorizedAttributes(credentialValue);
		int authorizedAttributesSpan = authorizedAttributesByCredential.size();

		if (CollectionHelper.isEmpty(authorizedAttributesByCredential)) {
			exceptionHandler.doOnUnauthorizedCredential(metadata.getResourceType(), credentialValue);
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

	protected abstract String getActualAttributeName(String requestedName);

	protected abstract Set<String> getAuthorizedAttributes(String credentialValue);

	protected abstract List<String> getNonAssociationAttributes(String credentialValue);

	protected DomainResourceMetadata<D> getMetadata() {
		return metadata;
	}

}
