/**
 * 
 */
package multicados.internal.service.crud;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.persistence.Tuple;

import org.hibernate.Session;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import multicados.internal.domain.DomainResource;
import multicados.internal.domain.DomainResourceContext;
import multicados.internal.domain.builder.DomainResourceBuilderFactory;
import multicados.internal.domain.repository.GenericRepository;
import multicados.internal.domain.validation.DomainResourceValidatorFactory;
import multicados.internal.helper.HibernateHelper;
import multicados.internal.service.crud.security.CRUDCredential;
import multicados.internal.service.crud.security.read.ReadSecurityManager;

/**
 * @author Ngoc Huy
 *
 */
public class GenericCRUDServiceImpl extends AbstractGenericCRUDService<Map<String, Object>> {

	private static final Pageable DEFAULT_PAGEABLE = Pageable.ofSize(10);

	private final ReadSecurityManager readSecurityManager;
	private final GenericRepository genericRepository;

	public GenericCRUDServiceImpl(DomainResourceContext resourceContext, DomainResourceBuilderFactory builderFactory,
			DomainResourceValidatorFactory validatorFactory, ReadSecurityManager readSecurityManager,
			GenericRepository genericRepository) throws Exception {
		super(resourceContext, builderFactory, validatorFactory);
		this.readSecurityManager = readSecurityManager;
		this.genericRepository = genericRepository;
	}

	private <E extends DomainResource> List<Map<String, Object>> resolveRows(Class<E> type, List<Tuple> tuples,
			List<String> checkedProperties) {
		Map<String, String> translatedAttributes = readSecurityManager.translate(type, checkedProperties);
		int span = checkedProperties.size();
		// @formatter:off
		return tuples.stream()
				.map(tuple -> IntStream.range(0, span)
					.mapToObj(j -> Map.entry(translatedAttributes.get(checkedProperties.get(j)), tuple.get(j)))
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
				.collect(Collectors.toList());
		// @formatter:on
	}

	@Override
	public <E extends DomainResource> List<Map<String, Object>> readAll(Class<E> type, Collection<String> properties,
			Pageable pageable, CRUDCredential credential, Session session) throws Exception {
		return readAll(type, properties, null, pageable, credential, session);
	}

	@Override
	public <E extends DomainResource> List<Map<String, Object>> readAll(Class<E> type, Collection<String> properties,
			CRUDCredential credential, Session session) throws Exception {
		return readAll(type, properties, DEFAULT_PAGEABLE, credential, session);
	}

	@Override
	public <E extends DomainResource> List<Map<String, Object>> readAll(Class<E> type, Collection<String> properties,
			Specification<E> specification, CRUDCredential credential, Session session) throws Exception {
		return readAll(type, properties, specification, DEFAULT_PAGEABLE, credential, session);
	}

	@Override
	public <E extends DomainResource> List<Map<String, Object>> readAll(Class<E> type, Collection<String> properties,
			Specification<E> specification, Pageable pageable, CRUDCredential credential, Session session)
			throws Exception {
		List<String> checkedProperties = readSecurityManager.check(type, properties, credential);
		List<Tuple> tuples = genericRepository.findAll(type, HibernateHelper.toSelector(checkedProperties),
				specification, pageable, session);

		return resolveRows(type, tuples, checkedProperties);
	}

}
