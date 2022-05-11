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
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.persistence.Tuple;
import javax.persistence.TupleElement;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Selection;

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
import multicados.internal.helper.CollectionHelper;
import multicados.internal.helper.HibernateHelper;
import multicados.internal.helper.SpecificationHelper;
import multicados.internal.helper.Utils;
import multicados.internal.helper.Utils.LazySupplier;
import multicados.internal.service.crud.rest.ComposedNonBatchingRestQuery;
import multicados.internal.service.crud.rest.ComposedRestQuery;
import multicados.internal.service.crud.rest.RestQuery;
import multicados.internal.service.crud.rest.RestQueryComposer;
import multicados.internal.service.crud.rest.RestQueryComposerImpl;
import multicados.internal.service.crud.security.CRUDCredential;
import multicados.internal.service.crud.security.read.ReadSecurityManager;

/**
 * @author Ngoc Huy
 *
 */
public class GenericCRUDServiceImpl extends AbstractGenericRestHibernateCRUDService<Map<String, Object>> {

	private static final Logger logger = LoggerFactory.getLogger(GenericCRUDServiceImpl.class);

//	private final DomainResourceContext resourceContext;

	private final ReadSecurityManager readSecurityManager;
	private final GenericRepository genericRepository;
	private final RestQueryComposer restQueryComposer;

	private final Map<Class<? extends DomainResource>, BiFunction<Path<?>, List<String>, Selector<?, Tuple>>> selectorsMap;
	private static final Pageable DEFAULT_PAGEABLE = Pageable.ofSize(10);

	public GenericCRUDServiceImpl(DomainResourceContext resourceContext, DomainResourceBuilderFactory builderFactory,
			DomainResourceValidatorFactory validatorFactory, ReadSecurityManager readSecurityManager,
			GenericRepository genericRepository) throws Exception {
		super(resourceContext, builderFactory, validatorFactory);
		this.readSecurityManager = readSecurityManager;
		this.genericRepository = genericRepository;
		selectorsMap = Collections.unmodifiableMap(resolveSelectorsMap(resourceContext));
		restQueryComposer = new RestQueryComposerImpl(resourceContext, readSecurityManager);
	}

	private Map<Class<? extends DomainResource>, BiFunction<Path<?>, List<String>, Selector<?, Tuple>>> resolveSelectorsMap(
			DomainResourceContext resourceContext) throws Exception {
		logger.debug("Resolving selectors map");

		Map<Class<? extends DomainResource>, BiFunction<Path<?>, List<String>, Selector<?, Tuple>>> selectorsMap = new HashMap<>();

		for (Class<DomainResource> type : resourceContext.getResourceGraph()
				.collect(DomainResourceGraphCollectors.toTypesSet())) {
			DomainResourceMetadata<DomainResource> metadata = resourceContext.getMetadata(type);

			if (metadata == null) {
				logger.trace("Skipping type {}", type.getName());
				continue;
			}

			List<String> attributes = metadata.getAttributeNames();
			Map<String, Function<Path<?>, Path<?>>> pathProducers = new HashMap<>();

			for (String attribute : attributes) {
				if (metadata.isComponent(attribute)) {
					// @formatter:off
					pathProducers.put(attribute,
							Utils.declare(metadata)
									.second(attribute)
									.third(metadata.getComponentPaths().get(attribute))
								.then(this::resolveComponentPathProducers)
								.get());
					continue;
					// @formatter:on
				}

				if (metadata.isAssociation(attribute)) {
					// @formatter:off
					pathProducers.put(attribute,
							metadata.isAssociationOptional(attribute)
								? path -> ((From<?, ?>) path).join(attribute, JoinType.LEFT)
								: path -> ((From<?, ?>) path).join(attribute));
					continue;
					// @formatter:on
				}

				pathProducers.put(attribute, path -> path.get(attribute));
			}

			selectorsMap
					.put(type,
							(joiningPath, checkedAttributes) -> (joiningPath == null
									? (root, query, builder) -> checkedAttributes.stream()
											.map(checkedAttribute -> pathProducers.get(checkedAttribute).apply(root))
											.collect(Collectors.toList())
									: (root, query, builder) -> checkedAttributes.stream().map(
											checkedAttribute -> pathProducers.get(checkedAttribute).apply(joiningPath))
											.collect(Collectors.toList())));
		}

		return selectorsMap;
	}

	private Function<Path<?>, Path<?>> resolveComponentPathProducers(
	// @formatter:off
			DomainResourceMetadata<? extends DomainResource> metadata,
			String attributeName,
			ComponentPath componentPath) {
		List<Function<Path<?>, Path<?>>> pathNodes = individuallyResolveComponentPathProducers(metadata, attributeName, componentPath);
		// this produces a chain of functions, eventually invoke the final product (a function chain) with the root arg
		Function<Path<?>, Path<?>> resolver = pathNodes.stream().reduce(
				(leadingFunction, followingFunction) ->
					(currentPath) -> followingFunction.apply(leadingFunction.apply(currentPath)))
				.get();
		return path -> resolver.apply(path);
		// @formatter:on
	}

	@SuppressWarnings("rawtypes")
	private List<Function<Path<?>, Path<?>>> individuallyResolveComponentPathProducers(
			DomainResourceMetadata<? extends DomainResource> metadata, String attributeName,
			ComponentPath componentPath) {
		Queue<String> nodeNames = componentPath.getNodeNames();

		if (metadata.isAssociation(attributeName)) {
			List<Function<Path<?>, Path<?>>> pathNodes = new ArrayList<>();
			Queue<String> copiedNodeNames = new ArrayDeque<>(nodeNames);

			pathNodes.add((metadata.isAssociationOptional(attributeName)
					? new Function<String, Function<Path<?>, Path<?>>>() {
						@Override
						public Function<Path<?>, Path<?>> apply(String nodeName) {
							return path -> ((From<?, ?>) path).join(nodeName, JoinType.LEFT);
						}
					}
					: new Function<String, Function<Path<?>, Path<?>>>() {
						@Override
						public Function<Path<?>, Path<?>> apply(String nodeName) {
							return path -> ((From<?, ?>) path).join(nodeName);
						}
					}).apply(copiedNodeNames.poll()));

			while (copiedNodeNames.size() > 1) {
				pathNodes.add(new Function<String, Function<Path<?>, Path<?>>>() {
					@Override
					public Function<Path<?>, Path<?>> apply(String name) {
						return join -> ((Join) join).join(name);
					}
				}.apply(copiedNodeNames.poll()));
			}

			String lastNode = copiedNodeNames.poll();
			// @formatter:off
			pathNodes.add(path -> metadata.isAssociationOptional(lastNode)
							? ((Join) path).join(lastNode, JoinType.LEFT)
							: ((Join) path).join(lastNode));
			return pathNodes;
			// @formatter:on
		}

		return nodeNames.stream().map(nodeName -> new Function<Path<?>, Path<?>>() {
			@Override
			public Path<?> apply(Path<?> path) {
				return path.get(nodeName);
			}
		}).collect(Collectors.toList());
	}

	private <E extends DomainResource> List<Map<String, Object>> resolveRows(Class<E> type, List<Tuple> tuples,
			List<String> checkedProperties, int offset) {
		Map<String, String> translatedAttributes = readSecurityManager.translate(type, checkedProperties);
		int span = checkedProperties.size();
		// @formatter:off
		return tuples.stream()
				.map(tuple -> IntStream.range(offset, span + offset)
					.mapToObj(j -> Utils.Entry.entry(translatedAttributes.get(checkedProperties.get(j)), tuple.get(j)))
					.collect(CollectionHelper.toMap()))
				.collect(Collectors.toList());
		// @formatter:on
	}

	private <E extends DomainResource> List<Map<String, Object>> resolveRows(Class<E> type, List<Tuple> tuples,
			List<String> checkedProperties) {
		return resolveRows(type, tuples, checkedProperties, 0);
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
	public <D extends DomainResource> List<Map<String, Object>> readAll(RestQuery<D> restQuery,
			CRUDCredential credential, Session entityManager) throws Exception {
		return readAll(restQueryComposer.compose(restQuery, credential, false), credential, entityManager);
	}

	private <D extends DomainResource> RestQueryProcessingUnit<D> createProcessingUnit(
			ComposedRestQuery<D> composedQuery, CRUDCredential credential) {
		return new RestQueryProcessingUnit<>(composedQuery, credential);
	}

	public <D extends DomainResource> List<Map<String, Object>> readAll(ComposedRestQuery<D> composedQuery,
			CRUDCredential credential, Session session) throws Exception {
		return createProcessingUnit(composedQuery, credential).doReadAll(session);
	}

	@Override
	public <D extends DomainResource> Map<String, Object> read(RestQuery<D> restQuery, CRUDCredential credential,
			Session entityManager) throws Exception {
		return read(restQueryComposer.compose(restQuery, credential, false), credential, entityManager);
	}

	private <D extends DomainResource> Map<String, Object> read(ComposedRestQuery<D> composedQuery,
			CRUDCredential credential, Session session) throws Exception {
		return createProcessingUnit(composedQuery, credential).doRead(session);
	}

	private class RestQueryProcessingUnit<D extends DomainResource> {

		private final ComposedRestQuery<D> query;
		private final CRUDCredential credential;

		private final Class<D> resourceType;
		private final List<String> attributes;
		private final List<ComposedNonBatchingRestQuery<?>> nonBatchingQueries;
		private final List<ComposedRestQuery<?>> batchingQueries;
		private final LazySupplier<Map<String, String>> translatedAttributesLoader;

		private final Pageable pageable;

		public RestQueryProcessingUnit(ComposedRestQuery<D> query, CRUDCredential credential) {
			this.query = query;
			this.credential = credential;

			resourceType = query.getResourceType();
			attributes = query.getAttributes();
			nonBatchingQueries = query.getNonBatchingAssociationQueries();
			batchingQueries = query.getBatchingAssociationQueries();
			translatedAttributesLoader = new LazySupplier<>(() -> translateAttributes(query, credential));

			pageable = Optional.ofNullable(query.getPageable()).orElse(DEFAULT_PAGEABLE);
		}

		@SuppressWarnings("unchecked")
		private Selector<D, Tuple> resolveSelections() {
			return (root, cq, builder) -> {
				List<Selection<?>> selections = new ArrayList<>(
						((Selector<D, Tuple>) selectorsMap.get(resourceType).apply(null, query.getAttributes()))
								.select(root, cq, builder));

				for (ComposedRestQuery<?> nonBatchingQuery : nonBatchingQueries) {
					selections.addAll(resolveJoinedSelections(root.join(nonBatchingQuery.getName()), cq, builder,
							nonBatchingQuery));
				}

				return selections;
			};
		}

		private List<Selection<?>> resolveJoinedSelections(Join<?, ?> join, CriteriaQuery<Tuple> cq,
				CriteriaBuilder builder, ComposedRestQuery<?> composedQuery) {
			List<Selection<?>> selections = new ArrayList<>(
					((Selector<?, Tuple>) selectorsMap.get(composedQuery.getResourceType()).apply(join,
							composedQuery.getAttributes())).select(null, cq, builder));

			for (ComposedRestQuery<?> nonBatchingAssociationQuery : composedQuery.getNonBatchingAssociationQueries()) {
				selections.addAll(resolveJoinedSelections(join.join(nonBatchingAssociationQuery.getName()), cq, builder,
						nonBatchingAssociationQuery));
			}

			return selections;
		}

		private List<Map<String, Object>> doReadAll(Session session) throws Exception {
			// @formatter:off
			List<Tuple> tuples = genericRepository.findAll(
					resourceType,
					resolveSelections(),
					query.getSpecification(),
					pageable,
					session);
			// @formatter:on
			if (tuples.isEmpty()) {
				return Collections.emptyList();
			}

			return transformRows(tuples);
		}

		private Map<String, Object> doRead(Session session) throws Exception {
			// @formatter:off
			Optional<Tuple> optionalTuple = genericRepository.findOne(
					resourceType,
					resolveSelections(),
					query.getSpecification(),
					session);
			// @formatter:on
			if (optionalTuple.isEmpty()) {
				return null;
			}

			Map<String, Object> record = transformRow(optionalTuple.get());
			Map<String, String> translatedAttributes = translatedAttributesLoader.get();

			for (ComposedRestQuery<?> batchingQuery : batchingQueries) {
				record.put(translatedAttributes.get(batchingQuery.getName()),
						readAll(batchingQuery, credential, session));
			}

			return record;
		}

		private List<Map<String, Object>> transformRows(List<Tuple> tuples) throws Exception {
			return tuples.stream().map(this::transformRow).collect(Collectors.toList());
		}

		private Map<String, Object> transformRow(Tuple tuple) {
			return transformRow(query, credential, attributes, translatedAttributesLoader.get(), tuple);
		}

		private Map<String, Object> transformRow(ComposedRestQuery<?> composedQuery, CRUDCredential credential,
				List<String> attributes, Map<String, String> translatedAttributes, Tuple tuple) {
			Map<String, Object> produce = new HashMap<>(translatedAttributes.size(), 1f);
			int basicAttributesSpan = attributes.size();

			for (int i = 0; i < basicAttributesSpan; i++) {
				produce.put(translatedAttributes.get(attributes.get(i)), tuple.get(i));
			}

			for (ComposedNonBatchingRestQuery<?> composedNonBatchingRestQuery : composedQuery
					.getNonBatchingAssociationQueries()) {
				// @formatter:off
				produce.put(translatedAttributes.get(composedNonBatchingRestQuery.getName()),
						transformRow(
								composedNonBatchingRestQuery,
								credential,
								composedNonBatchingRestQuery.getAttributes(),
								translateAttributes(composedNonBatchingRestQuery, credential),
								new AssociationTuple(composedNonBatchingRestQuery, tuple)));
				// @formatter:on
			}

			return produce;
		}

		private Map<String, String> translateAttributes(ComposedRestQuery<?> composedQuery, CRUDCredential credential) {
			// @formatter:off
			try {
				return Utils.declare(composedQuery.getResourceType())
						.second(Stream
								.of(composedQuery.getAttributes().stream(),
										composedQuery.getNonBatchingAssociationQueries().stream()
												.map(ComposedRestQuery::getName),
										composedQuery.getBatchingAssociationQueries().stream()
												.map(ComposedRestQuery::getName))
								.flatMap(Function.identity()).collect(Collectors.toList()))
						.then(readSecurityManager::translate).get();
			} catch (Exception any) {
				any.printStackTrace();
				return null;
			}
			// @formatter:on
		}

	}

	private class AssociationTuple implements Tuple {

		private ComposedNonBatchingRestQuery<?> composedQuery;
		private Tuple owningTuple;

		public AssociationTuple(ComposedNonBatchingRestQuery<?> composedQuery, Tuple owningTuple) {
			this.composedQuery = composedQuery;
			this.owningTuple = owningTuple;
		}

		@Override
		public <X> X get(TupleElement<X> tupleElement) {
			return owningTuple.get(tupleElement);
		}

		@Override
		public <X> X get(String alias, Class<X> type) {
			return owningTuple.get(alias, type);
		}

		@Override
		public Object get(String alias) {
			return owningTuple.get(alias);
		}

		private int getActualIndex(int i) {
			return i + composedQuery.getAssociatedPosition();
		}

		@Override
		public <X> X get(int i, Class<X> type) {
			return owningTuple.get(getActualIndex(i), type);
		}

		@Override
		public Object get(int i) {
			return owningTuple.get(getActualIndex(i));
		}

		@Override
		public Object[] toArray() {
			return IntStream.range(composedQuery.getAssociatedPosition(), composedQuery.getPropertySpan())
					.mapToObj(index -> owningTuple.get(index)).toArray();
		}

		@Override
		public List<TupleElement<?>> getElements() {
			List<TupleElement<?>> elements = owningTuple.getElements();

			return IntStream.range(composedQuery.getAssociatedPosition(), composedQuery.getPropertySpan())
					.mapToObj(index -> elements.get(index)).collect(Collectors.toList());
		}

	}
}
