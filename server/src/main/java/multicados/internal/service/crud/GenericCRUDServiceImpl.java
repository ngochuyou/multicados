/**
 *
 */
package multicados.internal.service.crud;

import static multicados.internal.helper.Utils.declare;

import java.io.Serializable;
import java.util.ArrayList;
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
import java.util.stream.Stream;

import javax.persistence.Tuple;
import javax.persistence.TupleElement;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;

import org.hibernate.Session;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.GrantedAuthority;

import multicados.internal.domain.DomainResource;
import multicados.internal.domain.DomainResourceContext;
import multicados.internal.domain.IdentifiableResource;
import multicados.internal.domain.builder.DomainResourceBuilderFactory;
import multicados.internal.domain.repository.GenericRepository;
import multicados.internal.domain.repository.Selector;
import multicados.internal.domain.validation.DomainResourceValidatorFactory;
import multicados.internal.helper.CollectionHelper;
import multicados.internal.helper.SpecificationHelper;
import multicados.internal.helper.StringHelper;
import multicados.internal.helper.Utils;
import multicados.internal.helper.Utils.Entry;
import multicados.internal.helper.Utils.HandledFunction;
import multicados.internal.helper.Utils.LazySupplier;
import multicados.internal.service.crud.rest.ComposedNonBatchingRestQuery;
import multicados.internal.service.crud.rest.ComposedRestQuery;
import multicados.internal.service.crud.rest.RestQuery;
import multicados.internal.service.crud.rest.RestQueryComposer;
import multicados.internal.service.crud.rest.RestQueryComposerImpl;
import multicados.internal.service.crud.rest.filter.Filter;
import multicados.internal.service.crud.security.read.ReadSecurityManager;

/**
 * @author Ngoc Huy
 *
 */
public class GenericCRUDServiceImpl extends AbstractGenericHibernateCUDService<Map<String, Object>>
		implements GenericRestHibernateCRUDService<Map<String, Object>> {

	private static final Logger logger = LoggerFactory.getLogger(GenericCRUDServiceImpl.class);

	private final GenericRepository genericRepository;
	private final CriteriaBuilder criteriaBuilder;

	private final ReadSecurityManager readSecurityManager;
	private final RestQueryComposer restQueryComposer;
	private final SelectionProducersProvider selectionProducersProvider;

	private static final Pageable DEFAULT_PAGEABLE = Pageable.ofSize(10);

	@Autowired
	public GenericCRUDServiceImpl(
	// @formatter:off
			SessionFactoryImplementor sfi,
			DomainResourceContext resourceContext,
			DomainResourceBuilderFactory builderFactory,
			DomainResourceValidatorFactory validatorFactory,
			ReadSecurityManager readSecurityManager,
			GenericRepository genericRepository) throws Exception {
		// @formatter:on
		super(resourceContext, builderFactory, validatorFactory);
		this.readSecurityManager = readSecurityManager;
		this.genericRepository = genericRepository;

		restQueryComposer = new RestQueryComposerImpl(resourceContext, readSecurityManager);
		selectionProducersProvider = new SelectionProducersProvider(resourceContext);

		criteriaBuilder = sfi.getCriteriaBuilder();
	}

	private <E extends DomainResource> List<Map<String, Object>> resolveRows(Class<E> type, List<Tuple> tuples,
			List<String> checkedProperties, int offset) {
		final Map<String, String> translatedAttributes = readSecurityManager.translate(type, checkedProperties);
		final int span = checkedProperties.size();
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
	public <S extends Serializable, E extends IdentifiableResource<S>> List<Map<String, Object>> readAll(
	// @formatter:off
			Class<E> type,
			Collection<String> properties,
			Pageable pageable,
			GrantedAuthority credential,
			Session session) throws Exception {
		// @formatter:on
		return readAll(type, properties, null, pageable, credential, session);
	}

	@Override
	public <S extends Serializable, E extends IdentifiableResource<S>> List<Map<String, Object>> readAll(
	// @formatter:off
			Class<E> type,
			Collection<String> properties,
			GrantedAuthority credential,
			Session session) throws Exception {
		// @formatter:on
		return readAll(type, properties, DEFAULT_PAGEABLE, credential, session);
	}

	@Override
	public <S extends Serializable, E extends IdentifiableResource<S>> List<Map<String, Object>> readAll(
	// @formatter:off
			Class<E> type,
			Collection<String> properties,
			Specification<E> specification,
			GrantedAuthority credential,
			Session session) throws Exception {
		// @formatter:on
		return readAll(type, properties, specification, DEFAULT_PAGEABLE, credential, session);
	}

	@Override
	public <S extends Serializable, E extends IdentifiableResource<S>> List<Map<String, Object>> readAll(
	// @formatter:off
			Class<E> type,
			Collection<String> properties,
			Specification<E> specification,
			Pageable pageable,
			GrantedAuthority credential,
			Session session) throws Exception {
		// @formatter:on
		final List<String> checkedProperties = readSecurityManager.check(type, properties, credential);
		final List<Tuple> tuples = genericRepository.findAll(type, toSelector(checkedProperties), specification,
				pageable, session);

		return resolveRows(type, tuples, checkedProperties);
	}

	@Override
	public <S extends Serializable, E extends IdentifiableResource<S>> Map<String, Object> readById(
	// @formatter:off
			Class<E> type,
			Serializable id,
			Collection<String> properties,
			GrantedAuthority credential,
			Session entityManager) throws Exception {
		// @formatter:on
		return readOne(type, properties, SpecificationHelper.hasId(type, id, entityManager), credential, entityManager);
	}

	@Override
	public <S extends Serializable, E extends IdentifiableResource<S>> Map<String, Object> readOne(
	// @formatter:off
			Class<E> type,
			Collection<String> properties,
			Specification<E> specification,
			GrantedAuthority credential,
			Session session) throws Exception {
		// @formatter:on
		final List<String> checkedProperties = readSecurityManager.check(type, properties, credential);
		final Optional<Tuple> optionalTuple = genericRepository.findOne(type, toSelector(checkedProperties),
				specification, session);

		if (optionalTuple.isEmpty()) {
			return null;
		}

		return resolveRows(type, List.of(optionalTuple.get()), checkedProperties).get(0);
	}

	@Override
	public <D extends DomainResource> List<Map<String, Object>> readAll(RestQuery<D> restQuery,
			GrantedAuthority credential, Session entityManager) throws Exception {
		return readAll(restQueryComposer.compose(restQuery, credential, true), credential, entityManager);
	}

	private static <D extends DomainResource, E> Selector<D, E> toSelector(Collection<String> attributes) {
		return (root, query, builder) -> attributes.stream().map(root::get).collect(Collectors.toList());
	}

	private <D extends DomainResource> RestQueryProcessingUnit<D> createProcessingUnit(
			ComposedRestQuery<D> composedQuery, GrantedAuthority credential) {
		return new RestQueryProcessingUnit<>(composedQuery, credential);
	}

	public <D extends DomainResource> List<Map<String, Object>> readAll(ComposedRestQuery<D> composedQuery,
			GrantedAuthority credential, Session session) throws Exception {
		return createProcessingUnit(composedQuery, credential).doReadAll(session);
	}

	@Override
	public <D extends DomainResource> Map<String, Object> read(RestQuery<D> restQuery, GrantedAuthority credential,
			Session entityManager) throws Exception {
		return read(restQueryComposer.compose(restQuery, credential, false), credential, entityManager);
	}

	private <D extends DomainResource> Map<String, Object> read(ComposedRestQuery<D> composedQuery,
			GrantedAuthority credential, Session session) throws Exception {
		return createProcessingUnit(composedQuery, credential).doRead(session);
	}

	private class RestQueryProcessingUnit<D extends DomainResource> {

		private final ComposedRestQuery<D> query;

		private final Class<D> rootResourceType;
		private final List<String> basicAttributes;
		private final List<ComposedNonBatchingRestQuery<?>> nonBatchingQueries;
		private final List<ComposedRestQuery<?>> batchingQueries;
		private final LazySupplier<Map<String, String>> translatedAttributesLoader;
		private final Pageable pageable;
		private final GrantedAuthority credential;

		private final Map<String, From<?, ?>> fromsCache = new HashMap<>();

		private static final String ROOT_KEY_IN_CACHE = "<ROOT>";

		public RestQueryProcessingUnit(ComposedRestQuery<D> query, GrantedAuthority credential) {
			this.query = query;
			this.credential = credential;

			rootResourceType = query.getResourceType();

			if (logger.isDebugEnabled()) {
				logger.debug("Processing {}<{}>", ComposedRestQuery.class.getSimpleName(),
						rootResourceType.getSimpleName());
			}

			basicAttributes = query.getAttributes();
			nonBatchingQueries = query.getNonBatchingAssociationQueries();
			batchingQueries = query.getBatchingAssociationQueries();
			translatedAttributesLoader = new LazySupplier<>(() -> translateAttributes(query, credential));

			pageable = Optional.<Pageable>ofNullable(query.getPage()).orElse(DEFAULT_PAGEABLE);
		}

		private Selector<D, Tuple> resolveSelector() {
			// @formatter:off
			return (root, cq, builder) -> declare(root)
					.second(ROOT_KEY_IN_CACHE)
				.consume(this::cache)
					.second(selectionProducersProvider.getSelectionProducers(rootResourceType))
				.append(this::produceBasicSelections)
				.then(this::addAssociationSelections)
				.get();
			// @formatter:on
		}

		private <T extends DomainResource> List<Selection<?>> produceBasicSelections(Root<T> root,
				Map<String, Function<Path<?>, Path<?>>> selectionProducers) {
			// @formatter:off
			return basicAttributes
					.stream()
					.map(selectionProducers::get)
					.map(producer -> producer.apply(root))
					.collect(Collectors.toList());
			// @formatter:on
		}

		// @formatter:off
		private <T extends DomainResource> List<Selection<?>> addAssociationSelections(
				Root<T> root,
				Map<String, Function<Path<?>, Path<?>>> selectionProducers,
				List<Selection<?>> basicSelections) throws Exception {
			for (final ComposedRestQuery<?> nonBatchingQuery : nonBatchingQueries) {
				declare(nonBatchingQuery.getAssociationName())
						.flat(joinName -> (Join<?, ?>) selectionProducers.get(joinName).apply(root), HandledFunction.identity())
					.consume(this::cache)
						.third(nonBatchingQuery)
					.then(this::resolveJoinedSelections)
					.consume(basicSelections::addAll);
			}

			return basicSelections;
		}
		// @formatter:on
		private void cache(From<?, ?> from, String key) {
			fromsCache.put(key, from);
		}

		// @formatter:off
		private List<Selection<?>> resolveJoinedSelections(
				Join<?, ?> join,
				String joinRole,
				ComposedRestQuery<?> composedAssociationQuery) throws Exception {
			return declare(join)
						.second(composedAssociationQuery.getAttributes())
						.third(composedAssociationQuery.getNonBatchingAssociationQueries())
					.then(this::produceAssociationBasicSelections)
						.second(composedAssociationQuery.getNonBatchingAssociationQueries())
						.third(Utils.Entry.<Join<?, ?>, String>entry(join, joinRole))
						.triInverse()
					.then(this::addNestedAssociationSelections)
					.get();
		}
		// @formatter:on
		@SuppressWarnings("unchecked")
		private List<Selection<?>> produceAssociationBasicSelections(Join<?, ?> join, List<String> joinedAttributes,
				List<ComposedNonBatchingRestQuery<?>> joinedNonBatchingQueries) throws Exception {
			final List<Selection<?>> selections = new ArrayList<>(
					joinedAttributes.size() + joinedNonBatchingQueries.size());
			// @formatter:off
			for (String joinedAttribute : joinedAttributes) {
				declare(join.getJavaType())
					.then(type -> (Class<DomainResource>) type)
					.then(selectionProducersProvider::getSelectionProducers)
					.then(selectionsProducers -> selectionsProducers.get(joinedAttribute))
					.then(producer -> producer.apply(join))
					.consume(selections::add);
			}
			// @formatter:on
			return selections;
		}

		// @formatter:off
		private List<Selection<?>> addNestedAssociationSelections(
				Entry<Join<?, ?>, String> joinEntry,
				List<ComposedNonBatchingRestQuery<?>> joinedNonBatchingQueries,
				List<Selection<?>> associationBasicSelections) throws Exception {
			for (final ComposedRestQuery<?> nonBatchingAssociationQuery : joinedNonBatchingQueries) {
				declare(nonBatchingAssociationQuery.getAssociationName())
					.flat(
						associationName -> joinEntry.getKey().join(associationName),
						associationName -> resolveJoinRole(joinEntry.getValue(), associationName))
					.consume(this::cache)
						.third(nonBatchingAssociationQuery)
					.then(this::resolveJoinedSelections)
					.then(associationBasicSelections::addAll);
			}

			return associationBasicSelections;
		}
		// @formatter:on
		private String resolveJoinRole(String joinRole, String nextJoinName) {
			return joinRole != null ? StringHelper.join(StringHelper.DOT, List.of(joinRole, nextJoinName))
					: nextJoinName;
		}

		private Specification<D> resolveSpecification() {
			return (root, cq, builder) -> {
				try {
					return Optional.ofNullable(resolvePredicates(root, null, query)).orElse(builder.conjunction());
				} catch (Exception any) {
					any.printStackTrace();
					return null;
				}
			};
		}

		// @formatter:off
		private Predicate resolvePredicates(
				From<?, ?> from,
				String joinRole,
				ComposedRestQuery<?> composedQuery) throws Exception {
			return declare(composedQuery.getResourceType())
						.second(from)
						.third(composedQuery.getFilters())
					.then(this::resolveBasicPredicates)
						.second(composedQuery)
						.third(Entry.<From<?, ?>, String>entry(from, joinRole))
						.triInverse()
					.then(this::addAssociationPredicates)
					.then(predicates -> predicates.isEmpty() ? null : criteriaBuilder.and(predicates.toArray(Predicate[]::new)))
					.get();
		}
		// @formatter:on
		// @formatter:off
		private <E extends DomainResource> List<Predicate> addAssociationPredicates(
				Utils.Entry<From<?, ?>, String> fromEntry,
				ComposedRestQuery<?> composedAssocationQuery,
				List<Predicate> basicPredicates) throws Exception {
			for (final ComposedRestQuery<?> nonBatchingQuery : composedAssocationQuery.getNonBatchingAssociationQueries()) {
				final String nextJoinRole = resolveJoinRole(fromEntry.getValue(), nonBatchingQuery.getAssociationName());
				final Predicate associationPredicate = resolvePredicates(
						fromsCache.containsKey(nextJoinRole) ? fromsCache.get(nextJoinRole)
								: fromEntry.getKey().join(composedAssocationQuery.getAssociationName()),
						nextJoinRole,
						nonBatchingQuery);

				if (associationPredicate == null) {
					continue;
				}

				basicPredicates.add(associationPredicate);
			}

			return basicPredicates;
		}
		// @formatter:on
		// @formatter:off
		private <E extends DomainResource> List<Predicate> resolveBasicPredicates(
				Class<E> resourceType,
				From<?, ?> from,
				Map<String, Filter<?>> filters) {
			// @formatter:on
			if (filters.isEmpty()) {
				return new ArrayList<>();
			}

			final List<Predicate> predicates = new ArrayList<>();

			for (Map.Entry<String, Filter<?>> filterEntry : filters.entrySet()) {
				// @formatter:off
				final Predicate filterPredicate = extractFilterPredicate(
						resourceType,
						from,
						filterEntry.getKey(),
						filterEntry.getValue());
				// @formatter:on
				if (filterPredicate == null) {
					continue;
				}

				predicates.add(filterPredicate);
			}

			return predicates;
		}

		private <E extends DomainResource> Predicate extractFilterPredicate(
		// @formatter:off
				Class<E> resourceType,
				From<?, ?> from,
				String attributeName,
				Filter<?> filter) {
			// @formatter:on
			final List<BiFunction<Path<?>, CriteriaBuilder, Predicate>> expressionProducers = filter
					.getExpressionProducers();
			// @formatter:off
			final Path<?> attributePath = selectionProducersProvider
					.getSelectionProducers(resourceType)
					.get(attributeName)
					.apply(from);
			// @formatter:on
			final int size = expressionProducers.size();

			Predicate predicate = expressionProducers.get(0).apply(attributePath, criteriaBuilder);

			for (int i = 1; i < size; i++) {
				predicate = criteriaBuilder.or(predicate,
						expressionProducers.get(i).apply(attributePath, criteriaBuilder));
			}

			return predicate;
		}

		private List<Map<String, Object>> doReadAll(Session session) throws Exception {
			// @formatter:off
			final List<Tuple> tuples = genericRepository.findAll(
					rootResourceType,
					resolveSelector(),
					resolveSpecification(),
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
			final Optional<Tuple> optionalTuple = genericRepository.findOne(
					rootResourceType,
					resolveSelector(),
					resolveSpecification(),
					session);
			// @formatter:on
			if (optionalTuple.isEmpty()) {
				return null;
			}

			final Map<String, Object> record = transformRow(optionalTuple.get());
			final Map<String, String> translatedAttributes = translatedAttributesLoader.get();

			for (ComposedRestQuery<?> batchingQuery : batchingQueries) {
				record.put(translatedAttributes.get(batchingQuery.getAssociationName()),
						readAll(batchingQuery, credential, session));
			}

			return record;
		}

		private List<Map<String, Object>> transformRows(List<Tuple> tuples) throws Exception {
			return tuples.stream().map(this::transformRow).collect(Collectors.toList());
		}

		private Map<String, Object> transformRow(Tuple tuple) {
			return transformRow(query, credential, basicAttributes, translatedAttributesLoader.get(), tuple);
		}

		private Map<String, Object> transformRow(
		// @formatter:off
				ComposedRestQuery<?> composedQuery,
				GrantedAuthority credential,
				List<String> attributes,
				Map<String, String> translatedAttributes,
				Tuple tuple) {
			// @formatter:on
			final Map<String, Object> record = new HashMap<>(translatedAttributes.size(), 1f);
			final int basicAttributesSpan = attributes.size();

			for (int i = 0; i < basicAttributesSpan; i++) {
				record.put(translatedAttributes.get(attributes.get(i)), tuple.get(i));
			}

			for (final ComposedNonBatchingRestQuery<?> composedNonBatchingRestQuery : composedQuery
					.getNonBatchingAssociationQueries()) {
				// @formatter:off
				record.put(translatedAttributes.get(composedNonBatchingRestQuery.getAssociationName()),
						transformRow(
								composedNonBatchingRestQuery,
								credential,
								composedNonBatchingRestQuery.getAttributes(),
								translateAttributes(composedNonBatchingRestQuery, credential),
								new AssociationTuple(composedNonBatchingRestQuery, tuple)));
				// @formatter:on
			}

			return record;
		}

		private Map<String, String> translateAttributes(ComposedRestQuery<?> composedQuery,
				GrantedAuthority credential) {
			// should never be throwing exceptions
			// @formatter:off
			try {
				return Utils.declare(composedQuery.getResourceType())
						.second(Stream
								.of(composedQuery.getAttributes().stream(),
										composedQuery.getNonBatchingAssociationQueries().stream()
												.map(ComposedRestQuery::getAssociationName),
										composedQuery.getBatchingAssociationQueries().stream()
												.map(ComposedRestQuery::getAssociationName))
								.flatMap(Function.identity())
								.collect(Collectors.toList()))
						.then(readSecurityManager::translate)
						.get();
			} catch (Exception any) {
				any.printStackTrace();
				return null;
			}
			// @formatter:on
		}

	}

	private class AssociationTuple implements Tuple {

		private final ComposedNonBatchingRestQuery<?> composedQuery;
		private final Tuple owningTuple;

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
			return composedQuery.getAssociatedPosition() + i;
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
