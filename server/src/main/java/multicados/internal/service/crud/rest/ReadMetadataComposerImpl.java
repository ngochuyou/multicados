/**
 * 
 */
package multicados.internal.service.crud.rest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import multicados.internal.domain.DomainResource;
import multicados.internal.domain.DomainResourceContext;
import multicados.internal.security.CredentialException;
import multicados.internal.service.crud.security.CRUDCredential;
import multicados.internal.service.crud.security.read.ReadSecurityManager;
import multicados.internal.service.crud.security.read.UnknownAttributesException;

/**
 * @author Ngoc Huy
 *
 */
public class ReadMetadataComposerImpl implements ReadMetadataComposer {

	private final ReadSecurityManager readSecurityManager;

	public ReadMetadataComposerImpl(DomainResourceContext resourceContext, ReadSecurityManager readSecurityManager)
			throws Exception {
		this.readSecurityManager = readSecurityManager;
	}

	@Override
	public <D extends DomainResource> ReadMetadata<D> compose(RestQuery<D> restQuery, CRUDCredential credential)
			throws CredentialException, UnknownAttributesException {
		Class<D> resourceType = restQuery.getResourceType();
		List<String> checkedAttributes = readSecurityManager.check(resourceType, restQuery.getProperties(), credential);
		List<ReadMetadata<?>> associatedMetadatas = new ArrayList<>();

		for (RestQuery<?> associatedQueries : Optional.ofNullable(restQuery.getQueries())
				.orElse(Collections.emptyList())) {
			associatedMetadatas.add(compose(associatedQueries, credential));
		}

		return new ReadMetadataImpl<>(resourceType, checkedAttributes, associatedMetadatas, credential,
				restQuery.getName());
	}

}
