/**
 * 
 */
package multicados.internal.service.crud;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.persistence.Tuple;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;

import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import multicados.internal.domain.DomainResource;
import multicados.internal.domain.DomainResourceContext;
import multicados.internal.domain.DomainResourceGraphCollectors;
import multicados.internal.domain.builder.DomainResourceBuilderFactory;
import multicados.internal.domain.metadata.ComponentPath;
import multicados.internal.domain.metadata.DomainResourceMetadata;
import multicados.internal.domain.repository.GenericRepository;
import multicados.internal.domain.repository.Selector;
import multicados.internal.domain.validation.DomainResourceValidatorFactory;
import multicados.internal.helper.HibernateHelper;
import multicados.internal.helper.SpecificationHelper;
import multicados.internal.security.CredentialException;
import multicados.internal.service.crud.rest.RestQuery;
import multicados.internal.service.crud.security.CRUDCredential;
import multicados.internal.service.crud.security.read.ReadSecurityManager;
import multicados.internal.service.crud.security.read.UnknownAttributesException;

/**
 * @author Ngoc Huy
 *
 */
public class GenericCRUDServiceImpl extends AbstractGenericRestHibernateCRUDService<Map<String, Object>> {

	private static final Logger logger = LoggerFactory.getLogger(GenericCRUDServiceImpl.class);

	private static final Pageable DEFAULT_PAGEABLE = Pageable.ofSize(10);

	private final ReadSecurityManager readSecurityManager;
	private final GenericRepository genericRepository;

	private final Map<Class<? extends DomainResource>, Function<List<String>, Selector<? extends DomainResource, Tuple>>> selectorsMap;

	public GenericCRUDServiceImpl(DomainResourceContext resourceContext, DomainResourceBuilderFactory builderFactory,
			DomainResourceValidatorFactory validatorFactory, ReadSecurityManager readSecurityManager,
			GenericRepository genericRepository) throws Exception {
		super(resourceContext, builderFactory, validatorFactory);
		this.readSecurityManager = readSecurityManager;
		this.genericRepository = genericRepository;
		selectorsMap = Collections.unmodifiableMap(resolveSelectorsMap(resourceContext));
	}

	private Map<Class<? extends DomainResource>, Function<List<String>, Selector<? extends DomainResource, Tuple>>> resolveSelectorsMap(
			DomainResourceContext resourceContext) {
		logger.trace("Resolving selectors map");

		for (Class<DomainResource> type : resourceContext.getResourceGraph()
				.collect(DomainResourceGraphCollectors.toTypesSet())) {
			DomainResourceMetadata<DomainResource> metadata = resourceContext.getMetadata(type);

			if (metadata == null) {
				logger.trace("Skipping type {}", type.getName());
				continue;
			}

			List<String> attributes = metadata.getAttributeNames();
			Map<String, ComponentPath> componentPaths = metadata.getComponentPaths();
			Map<String, BiFunction<String, Root<? extends DomainResource>, Path<?>>> pathResolvers = new HashMap<>();

			for (String attribute : attributes) {
				if (!componentPaths.containsKey(attribute)) {
					pathResolvers.put(attribute, (name, root) -> root.get(name));
					continue;
				}

				
			}
		}

		return Map.of();
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
	public <E extends DomainResource> List<Map<String, Object>> readAll(
	// @formatter:off
			Class<E> type,
			Collection<String> properties,
			Pageable pageable,
			CRUDCredential credential,
			Session session) throws Exception {
		// @formatter:on
		return readAll(type, properties, null, pageable, credential, session);
	}

	@Override
	public <E extends DomainResource> List<Map<String, Object>> readAll(
	// @formatter:off
			Class<E> type,
			Collection<String> properties,
			CRUDCredential credential,
			Session session) throws Exception {
		// @formatter:on
		return readAll(type, properties, DEFAULT_PAGEABLE, credential, session);
	}

	@Override
	public <E extends DomainResource> List<Map<String, Object>> readAll(
	// @formatter:off
			Class<E> type,
			Collection<String> properties,
			Specification<E> specification,
			CRUDCredential credential,
			Session session) throws Exception {
		// @formatter:on
		return readAll(type, properties, specification, DEFAULT_PAGEABLE, credential, session);
	}

	@Override
	public <E extends DomainResource> List<Map<String, Object>> readAll(
	// @formatter:off
			Class<E> type,
			Collection<String> properties,
			Specification<E> specification,
			Pageable pageable,
			CRUDCredential credential,
			Session session) throws Exception {
		// @formatter:on
		List<String> checkedProperties = readSecurityManager.check(type, properties, credential);
		List<Tuple> tuples = genericRepository.findAll(type, HibernateHelper.toSelector(checkedProperties),
				specification, pageable, session);

		return resolveRows(type, tuples, checkedProperties);
	}

	@Override
	public <E extends DomainResource> Map<String, Object> readById(
	// @formatter:off
			Class<E> type,
			Serializable id,
			Collection<String> properties,
			CRUDCredential credential,
			Session entityManager) throws Exception {
		// @formatter:on
		return readOne(type, properties, SpecificationHelper.hasId(type, id), credential, entityManager);
	}

	@Override
	public <E extends DomainResource> Map<String, Object> readOne(
	// @formatter:off
			Class<E> type,
			Collection<String> properties,
			Specification<E> specification,
			CRUDCredential credential,
			Session session) throws Exception {
		// @formatter:on
		List<String> checkedProperties = readSecurityManager.check(type, properties, credential);
		Optional<Tuple> optionalTuple = genericRepository.findOne(type, HibernateHelper.toSelector(checkedProperties),
				specification, session);

		if (optionalTuple.isEmpty()) {
			return null;
		}

		return resolveRows(type, List.of(optionalTuple.get()), checkedProperties).get(0);
	}

	@Override
	public <D extends DomainResource> List<Map<String, Object>> readAll(
	// @formatter:off
			RestQuery<D> restQuery,
			CRUDCredential credential,
			Session session) throws CredentialException, UnknownAttributesException {
		// @formatter:on
		Class<D> resourceType = restQuery.getResourceType();
		List<String> checkedColumns = readSecurityManager.check(resourceType, restQuery.getColumns(), credential);

		return null;
	}

}
