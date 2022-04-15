/**
 * 
 */
package multicados.internal.service.crud;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.persistence.Tuple;

import org.hibernate.Session;
import org.springframework.data.domain.Pageable;

import multicados.internal.domain.DomainResource;
import multicados.internal.domain.DomainResourceContext;
import multicados.internal.domain.builder.DomainResourceBuilderFactory;
import multicados.internal.domain.repository.GenericRepository;
import multicados.internal.domain.validation.ValidatorFactory;
import multicados.internal.helper.HibernateHelper;
import multicados.internal.service.crud.security.CRUDCredential;
import multicados.internal.service.crud.security.read.ReadSecurityManager;

/**
 * @author Ngoc Huy
 *
 */
public class GenericCRUDServiceImpl extends AbstractGenericCRUDService<Map<String, Object>> {

	private final ReadSecurityManager readSecurityManager;
	private final GenericRepository genericRepository;

	public GenericCRUDServiceImpl(DomainResourceContext resourceContext, DomainResourceBuilderFactory builderFactory,
			ValidatorFactory validatorFactory, ReadSecurityManager readSecurityManager,
			GenericRepository genericRepository) throws Exception {
		super(resourceContext, builderFactory, validatorFactory);
		this.readSecurityManager = readSecurityManager;
		this.genericRepository = genericRepository;
	}

	@Override
	public <E extends DomainResource> List<Map<String, Object>> readAll(Class<E> type, Collection<String> properties,
			Pageable pageable, CRUDCredential credential, Session session) throws Exception {
		List<String> checkedProperties = readSecurityManager.check(type, properties, credential);
		List<Tuple> tuples = genericRepository.findAll(type, HibernateHelper.toSelector(checkedProperties), pageable,
				session);

		return null;
	}

	@Override
	public <E extends DomainResource> List<Map<String, Object>> readAll(Class<E> type, Collection<String> properties,
			CRUDCredential credential, Session session) throws Exception {
		return readAll(type, properties, null, credential, session);
	}

}
