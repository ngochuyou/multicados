/**
 * 
 */
package multicados.internal.service.crud.rest;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.util.Assert;

import multicados.internal.config.Settings;
import multicados.internal.domain.DomainResource;
import multicados.internal.domain.DomainResourceContext;
import multicados.internal.domain.For;
import multicados.internal.domain.metadata.AssociationType;
import multicados.internal.domain.metadata.DomainResourceMetadata;
import multicados.internal.domain.tuplizer.AccessorFactory;
import multicados.internal.domain.tuplizer.AccessorFactory.Accessor;
import multicados.internal.helper.Utils;
import multicados.internal.service.crud.security.CRUDCredential;
import multicados.internal.service.crud.security.read.ReadSecurityManager;

/**
 * @author Ngoc Huy
 *
 */
public class RestQueryComposerImpl implements RestQueryComposer {

	private final DomainResourceContext resourceContext;

	private final ReadSecurityManager readSecurityManager;
	private final Map<Class<? extends DomainResource>, QueryMetadata> metadatasMap;

	public RestQueryComposerImpl(DomainResourceContext resourceContext, ReadSecurityManager readSecurityManager)
			throws Exception {
		this.resourceContext = resourceContext;
		this.readSecurityManager = readSecurityManager;
		// @formatter:off
		metadatasMap = Utils
				.declare(scan())
					.second(resourceContext)
					.biInverse()
				.then(this::resolveMetadatas)
				.get();
		// @formatter:on
	}

	@SuppressWarnings("unchecked")
	private List<Class<? extends RestQuery<?>>> scan() throws ClassNotFoundException {
		ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);

		scanner.addIncludeFilter(new AssignableTypeFilter(RestQuery.class));
		scanner.addExcludeFilter(new AssignableTypeFilter(ComposedRestQuery.class));

		List<Class<? extends RestQuery<?>>> queryClasses = new ArrayList<>();

		for (BeanDefinition beanDefinition : scanner.findCandidateComponents(Settings.BASE_PACKAGE)) {
			Class<? extends RestQuery<?>> type = (Class<? extends RestQuery<?>>) Class
					.forName(beanDefinition.getBeanClassName());

			if (!type.isAnnotationPresent(For.class)) {
				throw new IllegalArgumentException(For.Message.getMissingMessage(type));
			}

			queryClasses.add(type);
		}

		return queryClasses;
	}

	@SuppressWarnings("unchecked")
	private Map<Class<? extends DomainResource>, QueryMetadata> resolveMetadatas(DomainResourceContext resourceContext,
			List<Class<? extends RestQuery<?>>> queryTypes) throws NoSuchFieldException, SecurityException {
		Map<Class<? extends DomainResource>, QueryMetadata> metadatasMap = new HashMap<>();

		for (Class<? extends RestQuery<?>> queryType : queryTypes) {
			Class<? extends DomainResource> resourceType = queryType.getDeclaredAnnotation(For.class).value();
			DomainResourceMetadata<? extends DomainResource> metadata = resourceContext.getMetadata(resourceType);
			List<Entry<String, Accessor>> nonBatchingQueriesAccessors = new ArrayList<>();
			List<Entry<String, Accessor>> batchingQueriesAccessors = new ArrayList<>();

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
		return compose(null, null, restQuery, credential, isQueryBatched);
	}

	@SuppressWarnings("unchecked")
	private <D extends DomainResource> ComposedRestQuery<D> compose(String associatingName, Integer associatedPosition,
			RestQuery<D> restQuery, CRUDCredential credential, boolean isQueryBatched) throws Exception {
		Class<D> resourceType = restQuery.getResourceType();
		List<String> checkedAttributes = readSecurityManager.check(resourceType, restQuery.getAttributes(), credential);
		@SuppressWarnings("rawtypes")
		List[] associationQueries = resolveAssociationQueries(restQuery, credential, isQueryBatched);

		restQuery.setName(associatingName);
		restQuery.setAttributes(checkedAttributes);

		if (isQueryBatched) {
			return new ComposedRestQueryImpl<>(restQuery, associationQueries[0], associationQueries[1]);
		}

		return new ComposedNonBatchingRestQueryImpl<>(restQuery, associationQueries[0], associationQueries[1],
				associatedPosition);
	}

	@SuppressWarnings("rawtypes")
	private List[] resolveAssociationQueries(RestQuery<?> restQuery, CRUDCredential credential, boolean isQueryBatched)
			throws Exception {
		QueryMetadata queryMetadata = metadatasMap.get(restQuery.getResourceType());
		List<ComposedRestQuery<?>> nonBatchingQueries = new ArrayList<>();
		int index = restQuery.getAttributes().size();

		for (Entry<String, RestQuery<?>> entry : locateAssociationQueryEntries(restQuery,
				queryMetadata.getNonBatchingQueriesAccessors())) {
			ComposedNonBatchingRestQuery<?> composeAssociationQuery = (ComposedNonBatchingRestQuery<?>) individuallyComposeAssociationQuery(
					restQuery, index, entry, credential, isQueryBatched);

			nonBatchingQueries.add(composeAssociationQuery);
			index += composeAssociationQuery.getPropertySpan();
		}

		if (isQueryBatched) {
			return new List[] { nonBatchingQueries, Collections.emptyList() };
		}

		List<ComposedRestQuery<?>> batchingQueries = new ArrayList<>();

		for (Entry<String, RestQuery<?>> entry : locateAssociationQueryEntries(restQuery,
				queryMetadata.getBatchingQueriesAccessors())) {
			batchingQueries
					.add(individuallyComposeAssociationQuery(restQuery, null, entry, credential, isQueryBatched));
		}

		return new List[] { nonBatchingQueries, batchingQueries };
	}

	private boolean determineBatching(DomainResourceMetadata<?> associationOwnerMetadata, String associationName,
			boolean isQueryBatched) {
		return isQueryBatched
				|| associationOwnerMetadata.getAssociationType(associationName) == AssociationType.COLLECTION;
	}

	private ComposedRestQuery<?> individuallyComposeAssociationQuery(RestQuery<?> associationOwner,
			Integer associatedPosition, Entry<String, RestQuery<?>> entry, CRUDCredential credential,
			boolean isQueryBatched) throws Exception {
		String associationName = entry.getKey();
		RestQuery<?> associationQuery = entry.getValue();

		return compose(associationName, associatedPosition, associationQuery, credential, determineBatching(
				resourceContext.getMetadata(associationOwner.getResourceType()), associationName, isQueryBatched));
	}

	private List<Entry<String, RestQuery<?>>> locateAssociationQueryEntries(RestQuery<?> owningQuery,
			List<Entry<String, Accessor>> associationQueriesAccessors) throws Exception {
		List<Entry<String, RestQuery<?>>> queries = new ArrayList<>();

		for (Entry<String, Accessor> accessorEntry : associationQueriesAccessors) {
			RestQuery<?> associationQuery = (RestQuery<?>) accessorEntry.getValue().get(owningQuery);

			if (associationQuery == null) {
				continue;
			}

			queries.add(Map.entry(accessorEntry.getKey(), associationQuery));
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
