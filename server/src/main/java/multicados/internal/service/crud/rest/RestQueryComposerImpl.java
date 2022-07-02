/**
 *
 */
package multicados.internal.service.crud.rest;

import static multicados.internal.helper.Utils.declare;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;

import multicados.internal.config.Settings;
import multicados.internal.domain.DomainResource;
import multicados.internal.domain.DomainResourceContext;
import multicados.internal.domain.DomainResourceGraphCollectors;
import multicados.internal.domain.annotation.For;
import multicados.internal.domain.metadata.AssociationType;
import multicados.internal.domain.metadata.DomainResourceAttributesMetadata;
import multicados.internal.domain.tuplizer.AccessorFactory;
import multicados.internal.domain.tuplizer.AccessorFactory.Accessor;
import multicados.internal.helper.CollectionHelper;
import multicados.internal.helper.TypeHelper;
import multicados.internal.helper.Utils;
import multicados.internal.helper.Utils.BiDeclaration;
import multicados.internal.helper.Utils.TriDeclaration;
import multicados.internal.service.crud.rest.filter.Filter;
import multicados.internal.service.crud.security.read.ReadSecurityManager;

/**
 * @author Ngoc Huy
 *
 */
public class RestQueryComposerImpl implements RestQueryComposer {

	private static final Logger logger = LoggerFactory.getLogger(RestQueryComposerImpl.class);

	private final DomainResourceContext resourceContext;

	private final ReadSecurityManager readSecurityManager;
	private final Map<Class<? extends DomainResource>, QueryMetadata> queryMetadatasMap;
	private final Map<Class<? extends DomainResource>, Map<String, Accessor>> filtersAccessors;

	private static final int NON_BATCHING_COLLECTION_INDEX = 0;
	private static final int BATCHING_COLLECTION_INDEX = 1;

	public RestQueryComposerImpl(DomainResourceContext resourceContext, ReadSecurityManager readSecurityManager)
			throws Exception {
		if (logger.isTraceEnabled()) {
			logger.trace("Instantiating {}", RestQueryComposerImpl.class.getName());
		}

		this.resourceContext = resourceContext;
		this.readSecurityManager = readSecurityManager;
		// @formatter:off
		final Map<Class<? extends DomainResource>, Class<? extends RestQuery<?>>> queryClasses = scan();

		queryMetadatasMap = Utils
				.declare(resourceContext)
					.second(queryClasses)
				.then(this::constructQueryMetadatas)
				.then(Collections::unmodifiableMap)
				.get();
		filtersAccessors = Utils
				.declare(resourceContext)
					.second(queryClasses)
				.then(this::constructFiltersAccessors)
				.then(Collections::unmodifiableMap)
				.get();
		// @formatter:on
	}

	@SuppressWarnings("unchecked")
	private Map<Class<? extends DomainResource>, Class<? extends RestQuery<?>>> scan() throws ClassNotFoundException {
		if (logger.isTraceEnabled()) {
			logger.trace("Scanning for {}", RestQuery.class.getName());
		}

		final ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(
				false);

		scanner.addIncludeFilter(new AssignableTypeFilter(RestQuery.class));
		scanner.addExcludeFilter(new AssignableTypeFilter(ComposedRestQuery.class));

		final Map<Class<? extends DomainResource>, Class<? extends RestQuery<?>>> queryClassesMap = new HashMap<>();

		for (final BeanDefinition beanDefinition : scanner.findCandidateComponents(Settings.BASE_PACKAGE)) {
			final Class<? extends RestQuery<?>> type = (Class<? extends RestQuery<?>>) Class
					.forName(beanDefinition.getBeanClassName());

			if (!type.isAnnotationPresent(For.class)) {
				throw new IllegalArgumentException(For.Message.getMissingMessage(type));
			}

			queryClassesMap.put(type.getDeclaredAnnotation(For.class).value(), type);
		}

		return queryClassesMap;
	}

	@SuppressWarnings("unchecked")
	private Map<Class<? extends DomainResource>, Map<String, Accessor>> constructFiltersAccessors(
	// @formatter:off
				DomainResourceContext resourceContext,
				Map<Class<? extends DomainResource>, Class<? extends RestQuery<?>>> queryTypesMap)
				throws Exception {
		// @formatter:on
		if (logger.isTraceEnabled()) {
			logger.trace("Constructing {} for filters", Accessor.class.getSimpleName());
		}

		final Map<Class<? extends DomainResource>, Map<String, Accessor>> filterAccessors = new HashMap<>();

		for (final Class<? extends DomainResource> resourceType : resourceContext.getResourceGraph()
				.collect(DomainResourceGraphCollectors.toTypesSet())) {
			if (!queryTypesMap.containsKey(resourceType)) {
				continue;
			}

			final Queue<?> classQueue = TypeHelper.getClassQueue(resourceType);

			classQueue.poll();

			final Map<String, Accessor> scopedAccessors = new HashMap<>();

			while (!classQueue.isEmpty()) {
				final Class<? super DomainResource> superClass = (Class<? super DomainResource>) classQueue.poll();

				if (filterAccessors.containsKey(superClass)) {
					scopedAccessors.putAll(filterAccessors.get(superClass));
					break;
				}
			}

			final Class<? extends RestQuery<?>> queryType = queryTypesMap.get(resourceType);

			for (final Field field : queryType.getDeclaredFields()) {
				if (!Filter.class.isAssignableFrom(field.getType())) {
					continue;
				}

				scopedAccessors.put(field.getName(), AccessorFactory.standard(queryType, field.getName()));
			}

			filterAccessors.put(resourceType, scopedAccessors);
		}

		return filterAccessors;
	}

	@SuppressWarnings("unchecked")
	private Map<Class<? extends DomainResource>, QueryMetadata> constructQueryMetadatas(
	// @formatter:off
			DomainResourceContext resourceContext,
			Map<Class<? extends DomainResource>, Class<? extends RestQuery<?>>> queryTypesMap)
			throws Exception {
		// @formatter:on
		if (logger.isTraceEnabled()) {
			logger.trace("Constructing {}", QueryMetadata.class.getName());
		}

		final Map<Class<? extends DomainResource>, QueryMetadata> metadatasMap = new HashMap<>();

		for (final Class<? extends DomainResource> resourceType : resourceContext.getResourceGraph()
				.collect(DomainResourceGraphCollectors.toTypesSet())) {
			if (!queryTypesMap.containsKey(resourceType)) {
				continue;
			}

			final DomainResourceAttributesMetadata<? extends DomainResource> metadata = resourceContext.getMetadata(resourceType).unwrap(DomainResourceAttributesMetadata.class);
			final List<Entry<String, Accessor>> nonBatchingQueriesAccessors = new ArrayList<>();
			final List<Entry<String, Accessor>> batchingQueriesAccessors = new ArrayList<>();
			final Queue<?> classQueue = TypeHelper.getClassQueue(resourceType);

			classQueue.poll();

			while (!classQueue.isEmpty()) {
				final Class<? super DomainResource> superClass = (Class<? super DomainResource>) classQueue.poll();

				if (metadatasMap.containsKey(superClass)) {
					// @formatter:off
					Utils.declare(metadatasMap.get(superClass))
						.consume(queryMetadata -> nonBatchingQueriesAccessors.addAll(queryMetadata.getNonBatchingQueriesAccessors()))
						.consume(queryMetadata -> batchingQueriesAccessors.addAll(queryMetadata.getBatchingQueriesAccessors()));
					// @formatter:on
					break;
				}
			}

			final Class<? extends RestQuery<?>> queryType = queryTypesMap.get(resourceType);

			for (final Field field : queryType.getDeclaredFields()) {
				if (!RestQuery.class.isAssignableFrom(field.getType())) {
					continue;
				}

				final Class<? extends RestQuery<?>> associationQueryType = (Class<? extends RestQuery<?>>) field
						.getType();
				final String associationName = field.getName();

				Assert.isTrue(metadata.isAssociation(associationName),
						String.format("[%s.%s] is not an association", queryType.getSimpleName(), associationName));

				final Class<? extends DomainResource> associationType = associationQueryType
						.getDeclaredAnnotation(For.class).value();

				assertAssociationTypeEquality(metadata, queryType, associationName, associationType);

				final Entry<String, Accessor> associationAccessorEntry = Map.entry(associationName,
						AccessorFactory.standard(queryType, associationName));

				if (metadata.getAssociationType(associationName) == AssociationType.ENTITY) {
					nonBatchingQueriesAccessors.add(associationAccessorEntry);
					continue;
				}

				batchingQueriesAccessors.add(associationAccessorEntry);
			}

			metadatasMap.put(resourceType, new QueryMetadata(nonBatchingQueriesAccessors, batchingQueriesAccessors,
					TypeHelper.locateNoArgsConstructor(queryType)));
		}

		return metadatasMap;
	}

	@SuppressWarnings("unchecked")
	private void assertAssociationTypeEquality(
	// @formatter:off
			DomainResourceAttributesMetadata<? extends DomainResource> metadata,
			Class<? extends RestQuery<?>> queryType,
			String associationName,
			Class<? extends DomainResource> associationTypeInQuery) throws NoSuchFieldException, SecurityException {
		// @formatter:on
		if (metadata.getAssociationType(associationName) == AssociationType.ENTITY) {
			Assert.isTrue(associationTypeInQuery.equals(metadata.getAttributeType(associationName)), String.format(
					"[%s.%s] type mismatch between rest query generic type and registered resource type: [%s><%s]",
					queryType.getSimpleName(), associationName, associationTypeInQuery,
					metadata.getAttributeType(associationName)));
			return;
		}

		final Class<? extends DomainResource> associationTypeInResourceType = (Class<? extends DomainResource>) TypeHelper
				.getGenericType(metadata.getResourceType().getDeclaredField(associationName));

		Assert.isTrue(associationTypeInQuery.equals(associationTypeInResourceType), String.format(
				"[%s.%s] type mismatch between rest query generic type and registered resource type: [%s><%s]",
				queryType.getSimpleName(), associationName, associationTypeInQuery, associationTypeInResourceType));
	}

	@Override
	public <D extends DomainResource> ComposedRestQuery<D> compose(RestQuery<D> restQuery, GrantedAuthority credential,
			boolean isBatching) throws Exception {
		return compose(null, restQuery, credential, isBatching);
	}

	private <D extends DomainResource> ComposedRestQuery<D> compose(Integer associatedPosition,
			RestQuery<D> owningQuery, GrantedAuthority credential, boolean isBatching) throws Exception {
		// @formatter:off
		final TriDeclaration<List<String>, List<RestQuery<?>>, List<RestQuery<?>>> checkedResult = declare(owningQuery)
				.second(locateAssociationQueries(owningQuery, isBatching))
				.third(credential)
			.then(this::check)
			.consume(internal -> owningQuery.setAttributes(internal.getFirst()))
			.get();
		final BiDeclaration<List<ComposedNonBatchingRestQuery<?>>, List<ComposedRestQuery<?>>> composedResult =
			declare(checkedResult.getSecond())
				.second(checkedResult.getThird())
			.then((one, two) -> List.of(one, two))
			.then(Collections::unmodifiableList)
				.prepend(owningQuery)
				.third(credential)
			.then((query, rawQueries, cred) -> composeAssociationQuery(query, rawQueries, cred, isBatching))
			.get();
		final Map<String, Filter<?>> filters = resolveFilters(owningQuery);

		if (isBatching) {
			return new ComposedRestQueryImpl<>(
					owningQuery,
					composedResult.getFirst(),
					composedResult.getSecond(),
					filters);
		}

		return new ComposedNonBatchingRestQueryImpl<>(
				owningQuery,
				composedResult.getFirst(),
				composedResult.getSecond(),
				filters,
				associatedPosition);
		// @formatter:on
	}

	private boolean determineBatching(DomainResourceAttributesMetadata<?> associationOwnerMetadata, String associationName,
			boolean isQueryBatched) {
		return isQueryBatched
				&& associationOwnerMetadata.getAssociationType(associationName) == AssociationType.COLLECTION;
	}

	@SuppressWarnings("unchecked")
	private <D extends DomainResource> BiDeclaration<List<ComposedNonBatchingRestQuery<?>>, List<ComposedRestQuery<?>>> composeAssociationQuery(
	// @formatter:off
			RestQuery<?> owningQuery,
			List<List<RestQuery<?>>> associationQueries,
			GrantedAuthority credential,
			boolean isBatching) throws Exception {
		// @formatter:on
		final List<RestQuery<?>> rawNonBatchingQueries = associationQueries.get(NON_BATCHING_COLLECTION_INDEX);
		final List<ComposedNonBatchingRestQuery<?>> composedNonBatchingQueries = new ArrayList<>(
				rawNonBatchingQueries.size());
		int index = owningQuery.getAttributes().size();
		final DomainResourceAttributesMetadata<?> resourceMetadata = resourceContext.getMetadata(owningQuery.getResourceType()).unwrap(DomainResourceAttributesMetadata.class);

		for (RestQuery<?> rawNonBatchingQuery : rawNonBatchingQueries) {
			// @formatter:off
			final ComposedNonBatchingRestQuery<?> composedNonBatchingQuery = (ComposedNonBatchingRestQuery<?>) compose(
					index,
					rawNonBatchingQuery,
					credential,
					determineBatching(resourceMetadata, rawNonBatchingQuery.getAssociationName(), isBatching));
			// @formatter:on
			composedNonBatchingQueries.add(composedNonBatchingQuery);
			index += composedNonBatchingQuery.getPropertySpan();
		}

		if (isBatching) {
			return declare(composedNonBatchingQueries, Collections.emptyList());
		}

		final List<RestQuery<?>> rawBatchingQueries = associationQueries.get(BATCHING_COLLECTION_INDEX);
		final List<ComposedRestQuery<?>> composedBatchingQueries = new ArrayList<>(rawBatchingQueries.size());

		for (final RestQuery<?> rawBatchingQuery : rawBatchingQueries) {
			// @formatter:off
			declare(compose(
					index,
					rawBatchingQuery,
					credential,
					determineBatching(resourceMetadata, rawBatchingQuery.getAssociationName(), isBatching)))
				.consume(composedBatchingQueries::add);
			// @formatter:on
			index++;
		}

		return declare(composedNonBatchingQueries, Collections.unmodifiableList(composedBatchingQueries));
	}

	private <D extends DomainResource> TriDeclaration<List<String>, List<RestQuery<?>>, List<RestQuery<?>>> check(
			RestQuery<D> owningQuery, List<List<RestQuery<?>>> associationQueries, GrantedAuthority credential)
			throws Exception {
		// @formatter:off
		final Set<String> basicAttributes = declare(owningQuery.getResourceType())
				.second(owningQuery.getAttributes())
				.third(credential)
			.then(readSecurityManager::check)
			.then(HashSet::new)
			.get();

		declare(owningQuery.getResourceType())
				.second(Stream
						.of(
								associationQueries.get(NON_BATCHING_COLLECTION_INDEX).stream(),
								associationQueries.get(BATCHING_COLLECTION_INDEX).stream())
						.flatMap(stream -> stream.map(RestQuery::getAssociationName))
						.collect(Collectors.toSet()))
				.third(credential)
			.consume(readSecurityManager::check);
		// @formatter:on
		final Set<String> collisions = checkForCollisions(basicAttributes, associationQueries);

		if (!collisions.isEmpty()) {
			throw new DuplicateRequestedAttributeException(collisions);
		}

		return filterAssociationsFromBasicAttributes(owningQuery, basicAttributes, associationQueries);
	}

	@SuppressWarnings("unchecked")
	private <D extends DomainResource> TriDeclaration<List<String>, List<RestQuery<?>>, List<RestQuery<?>>> filterAssociationsFromBasicAttributes(
			RestQuery<?> owningQuery, Set<String> basicAttributes, List<List<RestQuery<?>>> associationQueries)
			throws Exception {
		final DomainResourceAttributesMetadata<?> metadata = resourceContext.getMetadata(owningQuery.getResourceType()).unwrap(DomainResourceAttributesMetadata.class);
		final List<String> filteredBasicAttributes = new ArrayList<>(basicAttributes.size());

		for (final String attribute : basicAttributes) {
			if (metadata.isAssociation(attribute)) {
				// @formatter:off
				associationQueries
					.get(
						metadata.getAssociationType(attribute) == AssociationType.ENTITY ?
								NON_BATCHING_COLLECTION_INDEX :
									BATCHING_COLLECTION_INDEX)
					.add(declare((Class<DomainResource>) metadata.getAttributeType(attribute))
							.then(queryMetadatasMap::get)
							.then(QueryMetadata::getConstructor)
							.then(Constructor::newInstance)
							.then(RestQuery.class::cast)
							.consume(emptyQuery -> emptyQuery.setAssociationName(attribute))
							.get());
				// @formatter:on
				continue;
			}

			filteredBasicAttributes.add(attribute);
		}

		return declare(Collections.unmodifiableList(filteredBasicAttributes))
				.second(Collections.unmodifiableList(associationQueries.get(NON_BATCHING_COLLECTION_INDEX)))
				.third(Collections.unmodifiableList(associationQueries.get(BATCHING_COLLECTION_INDEX)));
	}

	private <D extends DomainResource> Set<String> checkForCollisions(Set<String> basicAttributes,
			List<List<RestQuery<?>>> associationQueries) {
		final Set<String> collidedAttributes = new HashSet<>();
		final List<RestQuery<?>> joinedQueries = CollectionHelper.join(Collectors.toList(),
				associationQueries.get(NON_BATCHING_COLLECTION_INDEX),
				associationQueries.get(BATCHING_COLLECTION_INDEX));

		for (final RestQuery<?> associationQuery : joinedQueries) {
			final String associationName = associationQuery.getAssociationName();

			if (basicAttributes.contains(associationName)) {
				if (!CollectionHelper.isEmpty(associationQuery.getAttributes())) {
					collidedAttributes.add(associationName);
					continue;
				}

				basicAttributes.remove(associationName);
			}
		}

		return collidedAttributes;
	}

	private <D extends DomainResource> List<List<RestQuery<?>>> locateAssociationQueries(RestQuery<D> restQuery,
			boolean isBatching) throws Exception {
		final QueryMetadata queryMetadata = queryMetadatasMap.get(restQuery.getResourceType());
		final List<RestQuery<?>> nonBatchingQueries = locateQueries(restQuery,
				queryMetadata.getNonBatchingQueriesAccessors());

		if (isBatching) {
			return List.of(nonBatchingQueries, Collections.emptyList());
		}

		final List<RestQuery<?>> batchingQueries = locateQueries(restQuery,
				queryMetadata.getBatchingQueriesAccessors());

		return List.of(nonBatchingQueries, batchingQueries);
	}

	private <D extends DomainResource> List<RestQuery<?>> locateQueries(RestQuery<D> restQuery,
			List<Entry<String, Accessor>> accessorsEntries) throws Exception {
		final List<RestQuery<?>> queries = new ArrayList<>();

		for (final Entry<String, Accessor> accessorEntry : accessorsEntries) {
			final RestQuery<?> associationQuery = (RestQuery<?>) accessorEntry.getValue().get(restQuery);

			if (associationQuery == null) {
				continue;
			}

			declare(associationQuery).consume(query -> query.setAssociationName(accessorEntry.getKey()))
					.consume(queries::add);
		}

		return queries;
	}

	private <D extends DomainResource> Map<String, Filter<?>> resolveFilters(RestQuery<D> restQuery) throws Exception {
		final Map<String, Filter<?>> filters = new HashMap<>();

		for (final Entry<String, Accessor> accessorEntry : filtersAccessors.get(restQuery.getResourceType())
				.entrySet()) {
			final Filter<?> filter = Optional.ofNullable(accessorEntry.getValue().get(restQuery))
					.map(Filter.class::cast).orElse(null);

			if (filter == null || filter.getExpressionProducers().isEmpty()) {
				continue;
			}

			filters.put(accessorEntry.getKey(), filter);
		}

		return filters;
	}

	private class QueryMetadata {

		private final Constructor<?> constructor;
		private final List<Entry<String, Accessor>> nonBatchingQueriesAccessors;
		private final List<Entry<String, Accessor>> batchingQueriesAccessors;

		public QueryMetadata(List<Entry<String, Accessor>> nonBatchingQueriesAccessors,
				List<Entry<String, Accessor>> batchingQueriesAccessors, Constructor<?> constructor) {
			this.constructor = constructor;
			this.nonBatchingQueriesAccessors = nonBatchingQueriesAccessors;
			this.batchingQueriesAccessors = batchingQueriesAccessors;
		}

		public List<Entry<String, Accessor>> getNonBatchingQueriesAccessors() {
			return nonBatchingQueriesAccessors;
		}

		public List<Entry<String, Accessor>> getBatchingQueriesAccessors() {
			return batchingQueriesAccessors;
		}

		public Constructor<?> getConstructor() {
			return constructor;
		}

	}

}
