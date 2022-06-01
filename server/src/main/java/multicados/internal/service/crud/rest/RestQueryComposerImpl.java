/**
 * 
 */
package multicados.internal.service.crud.rest;

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

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.util.Assert;

import multicados.internal.config.Settings;
import multicados.internal.domain.DomainResource;
import multicados.internal.domain.DomainResourceContext;
import multicados.internal.domain.DomainResourceGraphCollectors;
import multicados.internal.domain.For;
import multicados.internal.domain.metadata.AssociationType;
import multicados.internal.domain.metadata.DomainResourceMetadata;
import multicados.internal.domain.tuplizer.AccessorFactory;
import multicados.internal.domain.tuplizer.AccessorFactory.Accessor;
import multicados.internal.helper.CollectionHelper;
import multicados.internal.helper.TypeHelper;
import multicados.internal.helper.Utils;
import multicados.internal.service.crud.rest.filter.Filter;
import multicados.internal.service.crud.security.CRUDCredential;
import multicados.internal.service.crud.security.read.ReadSecurityManager;

/**
 * @author Ngoc Huy
 *
 */
public class RestQueryComposerImpl implements RestQueryComposer {

	private final DomainResourceContext resourceContext;

	private final ReadSecurityManager readSecurityManager;
	private final Map<Class<? extends DomainResource>, QueryMetadata> queryMetadatasMap;
	private final Map<Class<? extends DomainResource>, Map<String, Accessor>> filtersAccessors;

	public RestQueryComposerImpl(DomainResourceContext resourceContext, ReadSecurityManager readSecurityManager)
			throws Exception {
		this.resourceContext = resourceContext;
		this.readSecurityManager = readSecurityManager;
		// @formatter:off
		Map<Class<? extends DomainResource>, Class<? extends RestQuery<?>>> queryClasses = scan();
		
		queryMetadatasMap = Utils
				.declare(resourceContext)
					.second(queryClasses)
				.then(this::resolveQueriesMetadatas)
				.then(Collections::unmodifiableMap)
				.get();
		filtersAccessors = Utils
				.declare(resourceContext)
					.second(queryClasses)
				.then(this::resolveFiltersAccessors)
				.then(Collections::unmodifiableMap)
				.get();
		// @formatter:on
	}

	@SuppressWarnings("unchecked")
	private Map<Class<? extends DomainResource>, Class<? extends RestQuery<?>>> scan() throws ClassNotFoundException {
		ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);

		scanner.addIncludeFilter(new AssignableTypeFilter(RestQuery.class));
		scanner.addExcludeFilter(new AssignableTypeFilter(ComposedRestQuery.class));

		Map<Class<? extends DomainResource>, Class<? extends RestQuery<?>>> queryClassesMap = new HashMap<>();

		for (BeanDefinition beanDefinition : scanner.findCandidateComponents(Settings.BASE_PACKAGE)) {
			Class<? extends RestQuery<?>> type = (Class<? extends RestQuery<?>>) Class
					.forName(beanDefinition.getBeanClassName());

			if (!type.isAnnotationPresent(For.class)) {
				throw new IllegalArgumentException(For.Message.getMissingMessage(type));
			}

			queryClassesMap.put(type.getDeclaredAnnotation(For.class).value(), type);
		}

		return queryClassesMap;
	}

	private Map<Class<? extends DomainResource>, Map<String, Accessor>> resolveFiltersAccessors(
	// @formatter:off
				DomainResourceContext resourceContext,
				Map<Class<? extends DomainResource>, Class<? extends RestQuery<?>>> queryTypesMap)
				throws Exception {
		// @formatter:on
		Map<Class<? extends DomainResource>, Map<String, Accessor>> filterAccessors = new HashMap<>();

		for (Class<DomainResource> resourceType : resourceContext.getResourceGraph()
				.collect(DomainResourceGraphCollectors.toTypesSet())) {
			if (!queryTypesMap.containsKey(resourceType)) {
				continue;
			}

			Queue<Class<? super DomainResource>> classStack = Utils.declare(TypeHelper.getClassQueue(resourceType))
					.identical(Queue::poll).get();
			Map<String, Accessor> scopedAccessors = new HashMap<>();

			while (!classStack.isEmpty()) {
				Class<? super DomainResource> superClass = classStack.poll();

				if (filterAccessors.containsKey(superClass)) {
					scopedAccessors.putAll(filterAccessors.get(superClass));
					break;
				}
			}

			Class<? extends RestQuery<?>> queryType = queryTypesMap.get(resourceType);

			for (Field field : queryType.getDeclaredFields()) {
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
	private Map<Class<? extends DomainResource>, QueryMetadata> resolveQueriesMetadatas(
	// @formatter:off
			DomainResourceContext resourceContext,
			Map<Class<? extends DomainResource>, Class<? extends RestQuery<?>>> queryTypesMap)
			throws Exception {
		// @formatter:on
		Map<Class<? extends DomainResource>, QueryMetadata> metadatasMap = new HashMap<>();

		for (Class<DomainResource> resourceType : resourceContext.getResourceGraph()
				.collect(DomainResourceGraphCollectors.toTypesSet())) {
			if (!queryTypesMap.containsKey(resourceType)) {
				continue;
			}

			DomainResourceMetadata<? extends DomainResource> metadata = resourceContext.getMetadata(resourceType);
			List<Entry<String, Accessor>> nonBatchingQueriesAccessors = new ArrayList<>();
			List<Entry<String, Accessor>> batchingQueriesAccessors = new ArrayList<>();
			Queue<Class<? super DomainResource>> classStack = Utils.declare(TypeHelper.getClassQueue(resourceType))
					.identical(Queue::poll).get();

			while (!classStack.isEmpty()) {
				Class<? super DomainResource> superClass = classStack.poll();

				if (metadatasMap.containsKey(superClass)) {
					// @formatter:off
					Utils.declare(metadatasMap.get(superClass))
						.identical(queryMetadata -> nonBatchingQueriesAccessors.addAll(queryMetadata.getNonBatchingQueriesAccessors()))
						.identical(queryMetadata -> batchingQueriesAccessors.addAll(queryMetadata.getBatchingQueriesAccessors()));
					// @formatter:on
					break;
				}
			}

			Class<? extends RestQuery<?>> queryType = queryTypesMap.get(resourceType);

			for (Field field : queryType.getDeclaredFields()) {
				if (!RestQuery.class.isAssignableFrom(field.getType())) {
					continue;
				}

				Class<? extends RestQuery<?>> associationQueryType = (Class<? extends RestQuery<?>>) field.getType();
				String associationName = field.getName();

				Assert.isTrue(metadata.isAssociation(associationName),
						String.format("[%s.%s] is not an association", queryType.getSimpleName(), associationName));

				Class<? extends DomainResource> associationType = associationQueryType.getDeclaredAnnotation(For.class)
						.value();

				Assert.isTrue(associationType.equals(metadata.getAttributeType(associationName)), String.format(
						"[%s.%s] type mismatch between rest query generic type and registered resource type: [%s><%s]",
						queryType.getSimpleName(), associationName, associationType,
						metadata.getAttributeType(associationName)));

				Entry<String, Accessor> associationAccessorEntry = Map.entry(associationName,
						AccessorFactory.standard(queryType, associationName));

				if (metadata.getAssociationType(associationName) == AssociationType.ENTITY) {
					nonBatchingQueriesAccessors.add(associationAccessorEntry);
					continue;
				}

				batchingQueriesAccessors.add(associationAccessorEntry);
			}

			metadatasMap.put(resourceType, new QueryMetadata(nonBatchingQueriesAccessors, batchingQueriesAccessors));
		}

		return metadatasMap;
	}

	@Override
	public <D extends DomainResource> ComposedRestQuery<D> compose(RestQuery<D> restQuery, CRUDCredential credential,
			boolean isQueryBatched) throws Exception {
		return compose(null, restQuery, credential, isQueryBatched);
	}

	@SuppressWarnings("unchecked")
	private <D extends DomainResource> ComposedRestQuery<D> compose(Integer associatedPosition, RestQuery<D> owningQuery,
			CRUDCredential credential, boolean isQueryBatched) throws Exception {
		Class<D> resourceType = owningQuery.getResourceType();
		List<String> checkedAttributes = readSecurityManager.check(resourceType, owningQuery.getAttributes(), credential);

		owningQuery.setAttributes(checkedAttributes);

		@SuppressWarnings("rawtypes")
		List[] associationQueries = resolveAssociationQueries(owningQuery, credential, isQueryBatched);
		Map<String, Filter<?>> filters = resolveFilters(owningQuery);

		if (isQueryBatched) {
			return new ComposedRestQueryImpl<>(owningQuery, associationQueries[0], associationQueries[1], filters);
		}

		return new ComposedNonBatchingRestQueryImpl<>(owningQuery, associationQueries[0], associationQueries[1], filters,
				associatedPosition);
	}

	private <D extends DomainResource> Map<String, Filter<?>> resolveFilters(RestQuery<D> restQuery) throws Exception {
		Map<String, Filter<?>> filters = new HashMap<>();

		for (Entry<String, Accessor> accessorEntry : filtersAccessors.get(restQuery.getResourceType()).entrySet()) {
			Filter<?> filter = Optional.ofNullable(accessorEntry.getValue().get(restQuery)).map(Filter.class::cast)
					.orElse(null);

			if (filter == null || filter.getExpressionProducers().isEmpty()) {
				continue;
			}

			filters.put(accessorEntry.getKey(), filter);
		}

		return filters;
	}

	@SuppressWarnings("rawtypes")
	private List[] resolveAssociationQueries(RestQuery<?> owningQuery, CRUDCredential credential, boolean isQueryBatched)
			throws Exception {
		QueryMetadata queryMetadata = queryMetadatasMap.get(owningQuery.getResourceType());
		List<ComposedRestQuery<?>> nonBatchingQueries = new ArrayList<>();
		int index = owningQuery.getAttributes().size();
		Set<String> owningQueryAttributes = new HashSet<>(owningQuery.getAttributes());
		Set<String> collidedAttributes = new HashSet<>(owningQueryAttributes.size());

		for (Entry<String, RestQuery<?>> queryEntry : locateAssociationQueryEntries(owningQuery,
				queryMetadata.getNonBatchingQueriesAccessors())) {
			if (doCollide(owningQueryAttributes, queryEntry)) {
				collidedAttributes.add(queryEntry.getKey());
				continue;
			}

			ComposedNonBatchingRestQuery<?> composeAssociationQuery = (ComposedNonBatchingRestQuery<?>) individuallyComposeAssociationQuery(
					owningQuery, index, queryEntry, credential, isQueryBatched);

			nonBatchingQueries.add(composeAssociationQuery);
			index += composeAssociationQuery.getPropertySpan();
		}

		if (isQueryBatched) {
			return assertCollision(new List[] { nonBatchingQueries, Collections.emptyList() }, collidedAttributes);
		}
		// TODO: test this out
		List<ComposedRestQuery<?>> batchingQueries = new ArrayList<>();

		for (Entry<String, RestQuery<?>> entry : locateAssociationQueryEntries(owningQuery,
				queryMetadata.getBatchingQueriesAccessors())) {
			if (doCollide(owningQueryAttributes, entry)) {
				collidedAttributes.add(entry.getKey());
				continue;
			}

			batchingQueries
					.add(individuallyComposeAssociationQuery(owningQuery, index, entry, credential, isQueryBatched));
			index++;
		}

		return assertCollision(new List[] { nonBatchingQueries, batchingQueries }, collidedAttributes);
	}

	private boolean doCollide(Set<String> owningQueryAttributes, Entry<String, RestQuery<?>> queryEntry) {
		return owningQueryAttributes.contains(queryEntry.getKey())
				&& !CollectionHelper.isEmpty(queryEntry.getValue().getAttributes());
	}

	@SuppressWarnings("rawtypes")
	private List[] assertCollision(List[] product, Set<String> colliedAttributes)
			throws DuplicateRequestedAttributeException {
		if (colliedAttributes.isEmpty()) {
			return product;
		}

		throw new DuplicateRequestedAttributeException(colliedAttributes);
	}

	private boolean determineBatching(DomainResourceMetadata<?> associationOwnerMetadata, String associationName,
			boolean isQueryBatched) {
		return isQueryBatched
				&& associationOwnerMetadata.getAssociationType(associationName) == AssociationType.COLLECTION;
	}

	private ComposedRestQuery<?> individuallyComposeAssociationQuery(
	// @formatter:off
			RestQuery<?> associationOwner,
			Integer associatedPosition,
			Entry<String, RestQuery<?>> associationEntry,
			CRUDCredential credential,
			boolean isQueryBatched) throws Exception {
		// @formatter:on
		String associationName = associationEntry.getKey();
		RestQuery<?> associationQuery = associationEntry.getValue();
		// @formatter:off
		return compose(
				associatedPosition,
				associationQuery,
				credential,
				determineBatching(
						resourceContext.getMetadata(
								associationOwner.getResourceType()),
						associationName,
						isQueryBatched));
		// @formatter:on
	}

	private List<Entry<String, RestQuery<?>>> locateAssociationQueryEntries(RestQuery<?> owningQuery,
			List<Entry<String, Accessor>> associationQueriesAccessors) throws Exception {
		List<Entry<String, RestQuery<?>>> queries = new ArrayList<>();

		for (Entry<String, Accessor> accessorEntry : associationQueriesAccessors) {
			RestQuery<?> associationQuery = (RestQuery<?>) accessorEntry.getValue().get(owningQuery);

			if (associationQuery == null) {
				continue;
			}

			queries.add(Map.entry(accessorEntry.getKey(), Utils.declare(associationQuery)
					.identical(query -> query.setAssociationName(accessorEntry.getKey())).get()));
		}

		return queries;
	}

	private class QueryMetadata {

		private final List<Entry<String, Accessor>> nonBatchingQueriesAccessors;
		private final List<Entry<String, Accessor>> batchingQueriesAccessors;

		public QueryMetadata(List<Entry<String, Accessor>> nonBatchingQueriesAccessors,
				List<Entry<String, Accessor>> batchingQueriesAccessors) {
			this.nonBatchingQueriesAccessors = nonBatchingQueriesAccessors;
			this.batchingQueriesAccessors = batchingQueriesAccessors;
		}

		public List<Entry<String, Accessor>> getNonBatchingQueriesAccessors() {
			return nonBatchingQueriesAccessors;
		}

		public List<Entry<String, Accessor>> getBatchingQueriesAccessors() {
			return batchingQueriesAccessors;
		}

	}

}
