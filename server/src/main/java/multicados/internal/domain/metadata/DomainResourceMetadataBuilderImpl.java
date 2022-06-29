/**
 *
 */
package multicados.internal.domain.metadata;

import static multicados.internal.helper.Utils.declare;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;

import multicados.internal.domain.DomainComponent;
import multicados.internal.domain.DomainResource;
import multicados.internal.domain.metadata.DomainResourceMetadataImpl.DomainAssociation;
import multicados.internal.helper.CollectionHelper;

/**
 * @author Ngoc Huy
 *
 */
public class DomainResourceMetadataBuilderImpl implements DomainResourceMetadataBuilder {

	private static final Logger logger = LoggerFactory.getLogger(DomainResourceMetadataImpl.class);

	@Override
	public <D extends DomainResource> DomainResourceMetadata<D> build(Class<D> resourceType,
			Map<Class<? extends DomainResource>, DomainResourceMetadata<? extends DomainResource>> onGoingMetadatas)
			throws Exception {
		if (logger.isTraceEnabled()) {
			logger.trace("Building {} for resource type {}", DomainResourceMetadata.class.getName(),
					resourceType.getName());
		}

		final List<String> declaredAttributeNames = getDeclaredAttributeNames(resourceType);
		final List<String> wrappedAttributes = locateWrappedAttributeNames(resourceType, declaredAttributeNames,
				onGoingMetadatas);
		final Map<String, Class<?>> attributeTypes = resolveAttributeTypes(resourceType, wrappedAttributes,
				onGoingMetadatas);
		final List<String> attributesToBeUnwrapped = new ArrayList<>(wrappedAttributes);

		unwrapAttributes(attributesToBeUnwrapped, attributeTypes);

		final Map<String, ComponentPath> componentPaths = resolveComponentPaths(resourceType, wrappedAttributes,
				attributeTypes, onGoingMetadatas);
		final Map<String, DomainAssociation> associations = locateAssociations(resourceType, wrappedAttributes,
				attributeTypes, componentPaths, onGoingMetadatas);

		return new DomainResourceMetadataImpl<>(resourceType, declaredAttributeNames, wrappedAttributes,
				attributesToBeUnwrapped, attributeTypes, wrappedAttributes, componentPaths, associations);
	}

	private <D extends DomainResource> Map<String, DomainAssociation> locateAssociations(Class<D> resourceType,
			List<String> unwrappedAttributeNames, Map<String, Class<?>> attributeTypes,
			Map<String, ComponentPath> componentPaths,
			Map<Class<? extends DomainResource>, DomainResourceMetadata<? extends DomainResource>> onGoingMetadatas)
			throws Exception {
		if (logger.isTraceEnabled()) {
			logger.trace("Locating associations");
		}
		// @formatter:off
		return declare(locateDeclaredAssociations(resourceType, unwrappedAttributeNames, attributeTypes, componentPaths))
					.prepend(resourceType)
					.third(onGoingMetadatas)
				.then(this::appendAssociationsFromParent)
				.get();
		// @formatter:on
	}

	@SuppressWarnings("unchecked")
	private <D extends DomainResource> Map<String, DomainAssociation> appendAssociationsFromParent(
			Class<D> resourceType, Map<String, DomainAssociation> declaredAssociations,
			Map<Class<? extends DomainResource>, DomainResourceMetadata<? extends DomainResource>> onGoingMetadatas)
			throws Exception {
		final DomainResourceMetadataImpl<?> parentMetadata = locateParentMetadata(resourceType, onGoingMetadatas);

		if (parentMetadata == null) {
			return Collections.emptyMap();
		}
		// @formatter:off
		declare(parentMetadata)
			.then(DomainResourceMetadataImpl.class::cast)
			.then(DomainResourceMetadataImpl::getAssociations)
			.consume(parentAssociations -> declaredAssociations.putAll(parentAssociations));
		// @formatter:on
		return declaredAssociations;
	}

	private <D extends DomainResource> Map<String, DomainAssociation> locateDeclaredAssociations(Class<D> resourceType,
			List<String> unwrappedAttributeNames, Map<String, Class<?>> attributeTypes,
			Map<String, ComponentPath> componentPaths) throws NoSuchFieldException, SecurityException {
		final Map<String, DomainAssociation> associations = new HashMap<>(0);

		for (final String attributeName : unwrappedAttributeNames) {
			if (!DomainResource.class.isAssignableFrom(attributeTypes.get(attributeName))) {
				continue;
			}

			final AssociationType associationType = DomainResource.class.isAssignableFrom(
					attributeTypes.get(attributeName)) ? AssociationType.ENTITY : AssociationType.COLLECTION;

			if (isComponentAssociationOptional(resourceType, componentPaths.get(attributeName).getPath())) {
				associations.put(attributeName,
						new DomainAssociation.OptionalAssociation(attributeName, associationType));
				continue;
			}

			associations.put(attributeName, new DomainAssociation.MandatoryAssociation(attributeName, associationType));
		}

		return associations;
	}

	private <D extends DomainResource> boolean isComponentAssociationOptional(Class<D> resourceType, Queue<String> path)
			throws NoSuchFieldException, SecurityException {
		final Queue<String> copiedPath = new ArrayDeque<>(path);
		Class<?> currentType = resourceType;
		Field field = currentType.getDeclaredField(copiedPath.poll());

		while (!copiedPath.isEmpty()) {
			currentType = field.getType();
			field = currentType.getDeclaredField(copiedPath.poll());
		}

		return field.isAnnotationPresent(Nullable.class);
	}

	private <D extends DomainResource> Map<String, ComponentPath> resolveComponentPaths(
	// @formatter:off
			Class<D> resourceType,
			List<String> attributeNames,
			Map<String, Class<?>> attributeTypes,
			Map<Class<? extends DomainResource>, DomainResourceMetadata<? extends DomainResource>> onGoingMetadatas) throws Exception {
		// @formatter:on
		if (logger.isTraceEnabled()) {
			logger.trace("Resolving component paths");
		}
		// @formatter:off
		return declare(locateDeclaredComponentPaths(attributeNames, attributeTypes, null))
				.prepend(resourceType)
				.third(onGoingMetadatas)
				.then(this::appendComponentPathsFromParent)
				.get();
		// @formatter:on
	}

	@SuppressWarnings("unchecked")
	private <D extends DomainResource> Map<String, ComponentPath> appendComponentPathsFromParent(Class<D> resourceType,
			Map<String, ComponentPath> declaredComponentPaths,
			Map<Class<? extends DomainResource>, DomainResourceMetadata<? extends DomainResource>> onGoingMetadatas)
			throws Exception {
		final DomainResourceMetadataImpl<?> parentMetadata = locateParentMetadata(resourceType, onGoingMetadatas);

		if (parentMetadata == null) {
			return Collections.emptyMap();
		}
		// @formatter:off
		declare(parentMetadata)
			.then(DomainResourceMetadataImpl.class::cast)
			.then(DomainResourceMetadataImpl::getComponentPaths)
			.consume(parentComponentPaths -> declaredComponentPaths.putAll(parentComponentPaths));
		// @formatter:on
		return declaredComponentPaths;
	}

	private Map<String, ComponentPath> locateDeclaredComponentPaths(List<String> attributeNames,
			Map<String, Class<?>> attributeTypes, ComponentPathImpl parentPath) throws Exception {
		final Map<String, ComponentPath> componentPaths = new HashMap<>();

		for (final String attributeName : attributeNames) {
			final Class<?> possibleComponentType = attributeTypes.get(attributeName);
			final ComponentPathImpl componentPath = parentPath == null ? new ComponentPathImpl(attributeName)
					: declare(parentPath).then(ComponentPathImpl::new).consume(self -> self.add(attributeName)).get();

			if (!DomainComponent.class.isAssignableFrom(possibleComponentType)) {
				if (parentPath != null) {
					componentPaths.put(attributeName, componentPath);
				}

				continue;
			}
			// @formatter:off
			declare(possibleComponentType)
				.then(this::getDeclaredAttributeNames)
					.second(attributeTypes)
					.third(componentPath)
				.then(this::locateDeclaredComponentPaths)
				.consume(componentPaths::putAll);
			// @formatter:on
		}

		return componentPaths;
	}

	/**
	 * Thoroughly unwrap component attributes, collect attribute names and types
	 *
	 *
	 * @param attributeNames reflect collected attribute names
	 * @param attributeTypes reflect collected attribute types
	 * @throws Exception
	 */
	private void unwrapAttributes(List<String> attributeNames, Map<String, Class<?>> attributeTypes) throws Exception {
		if (logger.isTraceEnabled()) {
			logger.trace("Unwrapping attributes");
		}

		final List<String> finalComponentAttributeNames = new ArrayList<>(0);
		final Map<String, Class<?>> finalComponentAttributeTypes = new HashMap<>(0);

		for (final String attributeName : attributeNames) {
			final Class<?> attributeType = attributeTypes.get(attributeName);

			if (!DomainComponent.class.isAssignableFrom(attributeType)) {
				continue;
			}

			final List<String> componentAttributeNames = new ArrayList<>(0);
			final Map<String, Class<?>> componentAttributeTypes = new HashMap<>(0);

			for (final Field componentField : Stream.of(attributeType.getDeclaredFields()).filter(this::isFieldManaged)
					.toList()) {
				final String componentAttributeName = componentField.getName();

				componentAttributeNames.add(componentAttributeName);
				componentAttributeTypes.put(componentAttributeName, componentField.getType());
				unwrapAttributes(componentAttributeNames, componentAttributeTypes);
			}

			finalComponentAttributeNames.addAll(componentAttributeNames);
			finalComponentAttributeTypes.putAll(componentAttributeTypes);
		}

		attributeNames.addAll(finalComponentAttributeNames);
		attributeTypes.putAll(finalComponentAttributeTypes);
	}

	private <D extends DomainResource> Map<String, Class<?>> resolveAttributeTypes(Class<D> resourceType,
			List<String> wrappedAttributes,
			Map<Class<? extends DomainResource>, DomainResourceMetadata<? extends DomainResource>> onGoingMetadatas)
			throws Exception {
		if (logger.isTraceEnabled()) {
			logger.trace("Resolving attribute types");
		}
		// @formatter:off
		return declare(locateParentMetadata(resourceType, onGoingMetadatas))
					.second(wrappedAttributes)
					.third(resourceType)
					.triInverse()
				.then(this::getAttributeTypes)
					.prepend(resourceType)
					.third(onGoingMetadatas)
				.then(this::appendAttributeTypesFromParent)
				.get();
		// @formatter:on
	}

	@SuppressWarnings("unchecked")
	private <D extends DomainResource> Map<String, Class<?>> appendAttributeTypesFromParent(Class<D> resourceType,
			Map<String, Class<?>> attributeTypes,
			Map<Class<? extends DomainResource>, DomainResourceMetadata<? extends DomainResource>> onGoingMetadatas)
			throws Exception {
		// @formatter:off
		return declare(locateParentMetadata(resourceType, onGoingMetadatas))
				.then(DomainResourceMetadataImpl.class::cast)
				.then(metadata -> Optional.ofNullable(metadata).map(self -> self.getAttributeTypes()).orElse(Collections.<String, Class<? extends DomainResource>>emptyMap()))
				.then(parentAttributeTypes -> declare(attributeTypes).consume(self -> self.putAll(parentAttributeTypes)).get())
				.get();
		// @formatter:on
	}

	private <D extends DomainResource> Map<String, Class<?>> getAttributeTypes(Class<D> resourceType,
			List<String> attributeNames, DomainResourceMetadataImpl<?> parentMetadata)
			throws NoSuchFieldException, SecurityException {
		final Map<String, Class<?>> typesMap = new HashMap<>(attributeNames.size(), 1.5f);

		for (final String attribute : attributeNames) {
			if (parentMetadata != null && parentMetadata.getAttributeTypes().containsKey(attribute)) {
				typesMap.put(attribute, parentMetadata.getAttributeTypes().get(attribute));
				continue;
			}

			Class<?> attributeType = resourceType.getDeclaredField(attribute).getType();

			typesMap.put(attribute, attributeType);

			if (!DomainComponent.class.isAssignableFrom(attributeType)) {
				continue;
			}

			typesMap.putAll(getComponentAttributeTypes(attributeType));
		}

		return typesMap;
	}

	private Map<String, Class<?>> getComponentAttributeTypes(Class<?> componentType) {
		final Map<String, Class<?>> types = new HashMap<>(0);

		for (Field componentField : Stream.of(componentType.getDeclaredFields()).filter(this::isFieldManaged)
				.toList()) {
			final Class<?> componentFieldType = componentField.getType();

			types.put(componentField.getName(), componentFieldType);

			if (!DomainComponent.class.isAssignableFrom(componentFieldType)) {
				continue;
			}

			types.putAll(getComponentAttributeTypes(componentFieldType));
		}

		return types;
	}

	private <D extends DomainResource> List<String> locateWrappedAttributeNames(Class<D> resourceType,
			List<String> declaredAttributeNames,
			Map<Class<? extends DomainResource>, DomainResourceMetadata<? extends DomainResource>> onGoingMetadatas)
			throws Exception {
		if (logger.isTraceEnabled()) {
			logger.trace("Locating wrapped attributes");
		}

		// @formatter:off
		return declare(declaredAttributeNames)
					.prepend(resourceType)
					.third(onGoingMetadatas)
				.then(this::appendWrappedAttributeNamesFromParent)
				.get();
		// @formatter:on
	}

	private List<String> getDeclaredAttributeNames(Class<?> type) {
		// @formatter:off
		return Stream.of(type.getDeclaredFields())
				.filter(this::isFieldManaged)
				.map(Field::getName)
				.toList();
		// @formatter:on
	}

	private boolean isFieldManaged(Field field) {
		final int modifiers = field.getModifiers();

		return !Modifier.isStatic(modifiers) && !Modifier.isTransient(modifiers);
	}

	private <D extends DomainResource> List<String> appendWrappedAttributeNamesFromParent(Class<D> resourceType,
			List<String> declaredAttributes,
			Map<Class<? extends DomainResource>, DomainResourceMetadata<? extends DomainResource>> onGoingMetadatas)
			throws Exception {
		// @formatter:off
		return declare(locateParentMetadata(resourceType, onGoingMetadatas))
				.then(parentMetadata -> parentMetadata != null ? parentMetadata.getWrappedAttributeNames() : List.<String>of())
				.then(parentAttributes -> CollectionHelper.join(Collectors.toList(), declaredAttributes, parentAttributes))
				.get();
		// @formatter:on
	}

	private <D extends DomainResource> DomainResourceMetadataImpl<?> locateParentMetadata(Class<D> resourceType,
			Map<Class<? extends DomainResource>, DomainResourceMetadata<? extends DomainResource>> onGoingMetadatas) {
		return (DomainResourceMetadataImpl<?>) onGoingMetadatas.get(resourceType.getSuperclass());
	}

}
