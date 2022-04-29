/**
 * 
 */
package multicados.internal.service.crud;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.persistence.Tuple;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
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
import multicados.internal.domain.metadata.AssociationType;
import multicados.internal.domain.metadata.ComponentPath;
import multicados.internal.domain.metadata.DomainResourceMetadata;
import multicados.internal.domain.repository.GenericRepository;
import multicados.internal.domain.repository.Selector;
import multicados.internal.domain.validation.DomainResourceValidatorFactory;
import multicados.internal.helper.CollectionHelper;
import multicados.internal.helper.HibernateHelper;
import multicados.internal.helper.SpecificationHelper;
import multicados.internal.helper.Utils;
import multicados.internal.security.CredentialException;
import multicados.internal.service.crud.rest.ReadMetadata;
import multicados.internal.service.crud.rest.ReadMetadataComposer;
import multicados.internal.service.crud.rest.ReadMetadataComposerImpl;
import multicados.internal.service.crud.rest.RestQuery;
import multicados.internal.service.crud.rest.TupleImpl;
import multicados.internal.service.crud.security.CRUDCredential;
import multicados.internal.service.crud.security.read.ReadSecurityManager;
import multicados.internal.service.crud.security.read.UnknownAttributesException;

/**
 * @author Ngoc Huy
 *
 */
public class GenericCRUDServiceImpl extends AbstractGenericRestHibernateCRUDService<Map<String, Object>> {

	private static final Logger logger = LoggerFactory.getLogger(GenericCRUDServiceImpl.class);

	private final DomainResourceContext resourceContext;

	private final ReadSecurityManager readSecurityManager;
	private final GenericRepository genericRepository;
	private final ReadMetadataComposer readMetadataComposer;

	private final Map<Class<? extends DomainResource>, Function<List<String>, Selector<? extends DomainResource, Tuple>>> selectorsMap;
	private static final Pageable DEFAULT_PAGEABLE = Pageable.ofSize(10);

	public GenericCRUDServiceImpl(DomainResourceContext resourceContext, DomainResourceBuilderFactory builderFactory,
			DomainResourceValidatorFactory validatorFactory, ReadSecurityManager readSecurityManager,
			GenericRepository genericRepository) throws Exception {
		super(resourceContext, builderFactory, validatorFactory);
		this.resourceContext = resourceContext;
		this.readSecurityManager = readSecurityManager;
		this.genericRepository = genericRepository;
		selectorsMap = Collections.unmodifiableMap(resolveSelectorsMap(resourceContext));
		readMetadataComposer = new ReadMetadataComposerImpl(resourceContext, readSecurityManager);
	}

	private Map<Class<? extends DomainResource>, Function<List<String>, Selector<? extends DomainResource, Tuple>>> resolveSelectorsMap(
			DomainResourceContext resourceContext) throws Exception {
		logger.debug("Resolving selectors map");

		Map<Class<? extends DomainResource>, Function<List<String>, Selector<? extends DomainResource, Tuple>>> selectorsMap = new HashMap<>();

		for (Class<DomainResource> type : resourceContext.getResourceGraph()
				.collect(DomainResourceGraphCollectors.toTypesSet())) {
			DomainResourceMetadata<DomainResource> metadata = resourceContext.getMetadata(type);

			if (metadata == null) {
				logger.trace("Skipping type {}", type.getName());
				continue;
			}

			List<String> attributes = metadata.getAttributeNames();
			Map<String, Function<Root<? extends DomainResource>, Path<?>>> pathResolvers = new HashMap<>();

			for (String attribute : attributes) {
				if (metadata.isComponent(attribute)) {
					// @formatter:off
					pathResolvers.put(attribute,
							Utils.declare(metadata)
									.second(attribute)
									.third(metadata.getComponentPaths().get(attribute))
								.then(this::makeComponentPathResolver)
								.get());
					continue;
					// @formatter:on
				}

				if (metadata.isAssociation(attribute)) {
					// we do not allow indirect association fetching
					// @formatter:off
//					pathResolvers.put(attribute,
//							metadata.isAssociationOptional(attribute)
//								? root -> root.join(attribute, JoinType.LEFT)
//								: root -> root.join(attribute));
					continue;
					// @formatter:on
				}

				pathResolvers.put(attribute, root -> root.get(attribute));
			}

			selectorsMap.put(type,
					(checkedAttributes) -> (root, query, builder) -> checkedAttributes.stream()
							.map(checkedAttribute -> pathResolvers.get(checkedAttribute).apply(root))
							.collect(Collectors.toList()));
		}

		return selectorsMap;
	}

	private Function<Root<? extends DomainResource>, Path<?>> makeComponentPathResolver(
	// @formatter:off
			DomainResourceMetadata<? extends DomainResource> metadata,
			String attributeName,
			ComponentPath componentPath) {
		List<Function<Path<?>, Path<?>>> pathNodes = makePathProducers(metadata, attributeName, componentPath);
		// this produces a chain of functions, eventually invoke the final product (a function chain) with the root arg
		Function<Path<?>, Path<?>> resolver = pathNodes.stream().reduce(
				(leadingFunction, followingFunction) ->
					(currentPath) -> followingFunction.apply(leadingFunction.apply(currentPath)))
				.get();
		return root -> resolver.apply(root);
		// @formatter:on
	}

	@SuppressWarnings("rawtypes")
	private List<Function<Path<?>, Path<?>>> makePathProducers(
			DomainResourceMetadata<? extends DomainResource> metadata, String attributeName,
			ComponentPath componentPath) {
		Queue<String> path = componentPath.getComponents();
		List<Function<Path<?>, Path<?>>> pathNodes = new ArrayList<>();

		if (metadata.isAssociation(attributeName)) {
			Queue<String> copiedPath = new ArrayDeque<>(path);

			pathNodes.add(new Function<String, Function<Path<?>, Path<?>>>() {
				@Override
				public Function<Path<?>, Path<?>> apply(String name) {
					return root -> ((Root) root).join(name);
				}
			}.apply(copiedPath.poll()));

			while (copiedPath.size() > 1) {
				pathNodes.add(new Function<String, Function<Path<?>, Path<?>>>() {
					@Override
					public Function<Path<?>, Path<?>> apply(String name) {
						return root -> ((Join) root).join(name);
					}
				}.apply(copiedPath.poll()));
			}

			String lastNode = copiedPath.poll();
			// @formatter:off
			pathNodes.add((root) -> metadata.isAssociationOptional(lastNode)
							? ((Join) root).join(lastNode, JoinType.LEFT)
							: ((Join) root).join(lastNode));
			return pathNodes;
			// @formatter:on
		}

		for (String node : path) {
			pathNodes.add((root) -> root.get(node));
		}

		return pathNodes;
	}

	private <E extends DomainResource> List<Map<String, Object>> resolveRows(Class<E> type, List<Tuple> tuples,
			List<String> checkedProperties) {
		Map<String, String> translatedAttributes = readSecurityManager.translate(type, checkedProperties);
		int span = checkedProperties.size();
		// @formatter:off
		return tuples.stream()
				.map(tuple -> IntStream.range(0, span)
					.mapToObj(j -> Map.entry(translatedAttributes.get(checkedProperties.get(j)), tuple.get(j)))
					.collect(CollectionHelper.toMap()))
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

	@SuppressWarnings("unchecked")
	private <D extends DomainResource> Selector<D, Tuple> resolveSelector(Class<D> resourceType,
			Collection<String> requestedAttributes, CRUDCredential credential)
			throws CredentialException, UnknownAttributesException {
		List<String> checkedAttributes = readSecurityManager.check(resourceType, requestedAttributes, credential);

		return (Selector<D, Tuple>) selectorsMap.get(resourceType).apply(checkedAttributes);
	}

	@Override
	public <D extends DomainResource> List<Map<String, Object>> readAll(RestQuery<D> restQuery,
			CRUDCredential credential, Session entityManager) throws Exception {
		return doReadAll(readMetadataComposer.compose(restQuery, credential), entityManager);
	}

	private <D extends DomainResource> List<Map<String, Object>> doReadAll(ReadMetadata<D> readMetadata,
			Session session) throws Exception {
		return List.of();
	}

	@Override
	public <D extends DomainResource> Map<String, Object> read(RestQuery<D> restQuery, CRUDCredential credential,
			Session entityManager) throws Exception {
		return doRead(readMetadataComposer.compose(restQuery, credential), entityManager);
	}

	private <D extends DomainResource> Map<String, Object> doRead(ReadMetadata<D> readMetadata, Session session)
			throws Exception {
		Class<D> resourceType = readMetadata.getResourceType();
		List<String> checkedAttributes = readMetadata.getAttributes();
		Optional<Tuple> optionalTuple = genericRepository.findOne(resourceType,
				resolveSelector(resourceType, checkedAttributes, readMetadata.getCredential()),
				readMetadata.getSpecification(), session);

		if (optionalTuple.isEmpty()) {
			return null;
		}

		List<Object> associationTuples = new ArrayList<>();
		DomainResourceMetadata<D> metadata = resourceContext.getMetadata(resourceType);

		for (ReadMetadata<?> associationMetadata : readMetadata.getMetadatas()) {
			String associationName = associationMetadata.getName();

			checkedAttributes.add(associationName);

			if (metadata.getAssociationType(associationName) == AssociationType.ENTITY) {
				associationTuples.add(doRead(associationMetadata, session));
				continue;
			}

			associationTuples.add(doReadAll(associationMetadata, session));
		}

		return resolveRows(resourceType, List.of(new TupleImpl(optionalTuple.get(), associationTuples)),
				checkedAttributes).get(0);
	}

//	@SuppressWarnings("unchecked")
//	private <D extends DomainResource> Selector<D, Tuple> getSelector(Class<D> resourceType,
//			List<String> checkedAttributes) {
//		return (Selector<D, Tuple>) selectorsMap.get(resourceType).apply(checkedAttributes);
//	}

//	private <D extends DomainResource> Selector<D, Tuple> getSelector(RestQuery<D> restQuery, CRUDCredential credential)
//			throws CredentialException, UnknownAttributesException {
//		Class<D> resourceType = restQuery.getResourceType();
//
//		return getSelector(resourceType,
//				readSecurityManager.check(resourceType, restQuery.getProperties(), credential));
//	}
//
//	@Override
//	public <D extends DomainResource> Map<String, Object> read(
//	// @formatter:off
//			NonBatchingRestQuery<D> restQuery,
//			CRUDCredential credential,
//			Session session)
//			throws CredentialException, UnknownAttributesException, Exception {
//		// @formatter:on
//		Selector<D, Tuple> selector = getSelector(restQuery, credential);
//		Optional<Tuple> optionalRootTuple = genericRepository.findOne(restQuery.getResourceType(), selector, null,
//				session);
//
//		if (optionalRootTuple.isEmpty()) {
//			return null;
//		}
//
//		Tuple rootTuple = optionalRootTuple.get();
//		int finalSize = restQuery.getProperties().size();
//		List<Object> indirectValues = new ArrayList<>();
//
//		for (NonBatchingRestQuery<?> nonBatchingQuery : Optional.ofNullable(restQuery.getNonBatchingQueries())
//				.orElseGet(Collections::emptyList)) {
//			indirectValues.add(read(nonBatchingQuery, credential, session));
//			finalSize++;
//		}
//
//		for (BatchingRestQuery<?> batchingQuery : Optional.ofNullable(restQuery.getBatchingQueries())
//				.orElseGet(Collections::emptyList)) {
//			indirectValues.add(readAll(batchingQuery, credential, session));
//			finalSize++;
//		}
//
//		return null;
//	}
//
//	@Override
//	public <D extends DomainResource> List<Map<String, Object>> readAll(
//	// @formatter:off
//			BatchingRestQuery<D> restQuery,
//			CRUDCredential credential,
//			Session session) throws Exception {
//		// @formatter:on
//		Class<D> resourceType = restQuery.getResourceType();
//		List<String> checkedAttributes = readSecurityManager.check(resourceType, restQuery.getProperties(), credential);
//		List<Tuple> tuples = genericRepository.findAll(resourceType, getSelector(resourceType, checkedAttributes),
//				restQuery.getPageable(), session);
//
//		return resolveRows(resourceType, tuples, checkedAttributes);
//	}

}
