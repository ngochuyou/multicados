/**
 * 
 */
package multicados.internal.service.crud.rest;

import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import multicados.internal.domain.DomainResource;
import multicados.internal.service.crud.security.CRUDCredential;

/**
 * @author Ngoc Huy
 *
 */
public class ReadMetadataImpl<D extends DomainResource> implements ReadMetadata<D> {

	private final Class<D> resourceType;

	private final List<String> checkedAttributes;
	private final List<ReadMetadata<?>> metadatas;
	private final CRUDCredential credential;

	private final String name;

	public ReadMetadataImpl(Class<D> resourceType, List<String> checkedAttributes, List<ReadMetadata<?>> metadata,
			CRUDCredential credential, String name) {
		this.resourceType = resourceType;
		this.checkedAttributes = checkedAttributes;
		this.metadatas = metadata;
		this.credential = credential;
		this.name = name;
	}

	@Override
	public List<String> getAttributes() {
		return checkedAttributes;
	}

	@Override
	public List<ReadMetadata<?>> getMetadatas() {
		return metadatas;
	}

	@Override
	public CRUDCredential getCredential() {
		return credential;
	}

	@Override
	public Class<D> getResourceType() {
		return resourceType;
	}

	@Override
	public Specification<D> getSpecification() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getName() {
		return name;
	}

}
