/**
 * 
 */
package multicados.internal.service.crud;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import org.springframework.data.domain.Pageable;

import multicados.internal.domain.DomainResource;
import multicados.internal.domain.DomainResourceContext;
import multicados.internal.domain.builder.DomainResourceBuilderFactory;
import multicados.internal.domain.validation.ValidatorFactory;
import multicados.internal.service.crud.security.CRUDCredential;

/**
 * @author Ngoc Huy
 *
 */
public class GenericCRUDServiceImpl extends AbstractGenericCRUDService<Map<String, Object>> {

	public GenericCRUDServiceImpl(DomainResourceContext resourceContext, DomainResourceBuilderFactory builderFactory,
			ValidatorFactory validatorFactory) throws Exception {
		super(resourceContext, builderFactory, validatorFactory);
	}

	@Override
	public <E extends DomainResource> List<Map<String, Object>> readAll(Class<E> type, Collection<String> properties,
			Pageable pageable, CRUDCredential credential, EntityManager entityManager) {
		return null;
	}

	@Override
	public <E extends DomainResource> List<Map<String, Object>> readAll(Class<E> type, Collection<String> properties,
			CRUDCredential credential, EntityManager entityManager) {
		return null;
	}

}
