/**
 *
 */
package multicados.internal.domain.metadata;

import static multicados.internal.helper.Utils.declare;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.persistence.metamodel.Attribute;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.id.Assigned;
import org.hibernate.id.CompositeNestedGeneratedValueGenerator;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.hibernate.type.CollectionType;
import org.hibernate.type.ComponentType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import multicados.internal.domain.DomainResource;
import multicados.internal.domain.Entity;
import multicados.internal.domain.IdentifiableResource;
import multicados.internal.domain.NamedResource;
import multicados.internal.domain.annotation.Name;
import multicados.internal.domain.metadata.DomainResourceAttributesMetadataImpl.DomainAssociation;
import multicados.internal.file.domain.FileResource;
import multicados.internal.helper.TypeHelper;
import multicados.internal.helper.Utils;
import multicados.internal.helper.Utils.BiDeclaration;
import multicados.internal.helper.Utils.HandledBiFunction;

/**
 * @author Ngoc Huy
 *
 */
public class HibernateDomainResourceMetadataBuilder implements DomainResourceMetadataBuilder {

	private static final Logger logger = LoggerFactory.getLogger(HibernateDomainResourceMetadataBuilder.class);

	private final SessionFactoryImplementor sfi;

	public HibernateDomainResourceMetadataBuilder(SessionFactoryImplementor sfi) {
		this.sfi = sfi;
	}

	@Override
	public <D extends DomainResource> DomainResourceMetadata<D> build(Class<D> resourceType,
			Map<Class<? extends DomainResource>, DomainResourceMetadata<? extends DomainResource>> onGoingMetadatas)
			throws Exception {
		final EntityPersister persister = sfi.getMetamodel().entityPersister(resourceType);
		final EntityMetamodel metamodel = persister.getEntityMetamodel();
		final List<String> declaredAttributeNames = locateDeclaredAttributeNames(resourceType, metamodel);
		final List<String> wrappedAttributes = locateWrappedAttributeNames(metamodel);
		final Map<String, Class<?>> attributeTypes = locateAttributeTypes(metamodel, wrappedAttributes);
		final List<String> attributesToBeUnwrapped = new ArrayList<>(wrappedAttributes);

		unwrapAttributes(metamodel, attributesToBeUnwrapped, attributeTypes);

		final List<String> nonLazyAttributes = locateNonLazyAttributes(resourceType, metamodel, wrappedAttributes);
		final Map<String, DomainAssociation> associations = locateAssociations(metamodel, wrappedAttributes);
		final Map<String, ComponentPath> componentPaths = resolveComponentPaths(metamodel, wrappedAttributes);

		return new DomainResourceAttributesMetadataImpl<>(resourceType, declaredAttributeNames, wrappedAttributes,
				attributesToBeUnwrapped, attributeTypes, nonLazyAttributes, componentPaths, associations,
				MetadataDecorator.INSTANCE.getDecorations(resourceType, persister));
	}

	public Map<String, ComponentPath> resolveComponentPaths(EntityMetamodel metamodel, List<String> wrappedAttributes)
			throws Exception {
		if (logger.isTraceEnabled()) {
			logger.trace("Resolving component paths");
		}

		final int span = wrappedAttributes.size();
		final Map<String, ComponentPath> paths = new HashMap<>();

		for (int i = 0; i < span; i++) {
			final String attributeName = wrappedAttributes.get(i);
			final Type type = metamodel.getIdentifierProperty().getName().equals(attributeName)
					? metamodel.getIdentifierProperty().getType()
					: metamodel.getPropertyTypes()[metamodel.getPropertyIndex(attributeName)];

			if (!type.getClass().isAssignableFrom(ComponentType.class)) {
				continue;
			}

			paths.putAll(individuallyResolveComponentPath(new ComponentPathImpl(), attributeName, type));
		}

		return paths;
	}

	private Map<String, ComponentPathImpl> individuallyResolveComponentPath(ComponentPathImpl currentPath,
			String attributeName, Type type) throws Exception {
		final ComponentPathImpl root = declare(new ComponentPathImpl(currentPath))
				.consume(self -> self.add(attributeName)).get();
		final Map<String, ComponentPathImpl> paths = new HashMap<>();

		paths.put(attributeName, root);

		if (!(type instanceof ComponentType)) {
			return paths;
		}

		final ComponentType componentType = (ComponentType) type;
		final int span = componentType.getPropertyNames().length;

		for (int i = 0; i < span; i++) {
			// @formatter:off
			declare(new ComponentPathImpl(root))
					.second(componentType.getPropertyNames()[i])
					.third(componentType.getSubtypes()[i])
				.then(this::individuallyResolveComponentPath)
				.consume(paths::putAll);
			// @formatter:on
		}

		return paths;
	}

	private AssociationType resolveAssociationType(org.hibernate.type.AssociationType type) {
		return type instanceof CollectionType ? multicados.internal.domain.metadata.AssociationType.COLLECTION
				: type instanceof EntityType ? multicados.internal.domain.metadata.AssociationType.ENTITY
						: Objects.requireNonNull(null, String.format("Unknown association type [%s]", type.getName()));
	}

	private Map<String, DomainAssociation> locateAssociations(EntityMetamodel metamodel,
			List<String> wrappedAttributeNames) throws Exception {
		if (logger.isTraceEnabled()) {
			logger.trace("Locating association");
		}

		final Map<String, DomainAssociation> associations = new HashMap<>();

		for (final String attributeName : wrappedAttributeNames) {
			if (attributeName.equals(metamodel.getIdentifierProperty().getName())) {
				continue;
			}

			final int attributeIndex = metamodel.getPropertyIndex(attributeName);
			// @formatter:off
			associations.putAll(
				individuallyLocateAssociations(
						attributeName,
						attributeIndex,
						metamodel.getPropertyTypes()[attributeIndex],
						metamodel.getPropertyNullability()));
			// @formatter:on
		}

		return associations.isEmpty() ? Collections.emptyMap() : associations;
	}

	private Map<String, DomainAssociation> individuallyLocateAssociations(String attributeName, int attributeIndex,
			Type type, boolean[] nullabilities) throws Exception {
		final Map<String, DomainAssociation> associations = new HashMap<>();

		if (type instanceof org.hibernate.type.AssociationType) {
			final AssociationType associationType = resolveAssociationType((org.hibernate.type.AssociationType) type);
			// @formatter:off
			if (nullabilities[attributeIndex]) {
				associations.put(attributeName,
						new DomainResourceAttributesMetadataImpl.DomainAssociation.OptionalAssociation(attributeName,
								associationType));

				return associations;
			}

			associations.put(attributeName, new DomainResourceAttributesMetadataImpl.DomainAssociation.MandatoryAssociation(
					attributeName, associationType));

			return associations;
			// @formatter:on
		}

		if (type instanceof ComponentType) {
			final ComponentType componentType = (ComponentType) type;
			final String[] subAttributes = componentType.getPropertyNames();
			final Type[] subtypes = componentType.getSubtypes();
			final int span = subAttributes.length;

			for (int i = 0; i < span; i++) {
				int propertyIndex = componentType.getPropertyIndex(subAttributes[i]);
				// @formatter:off
				associations.putAll(
					individuallyLocateAssociations(
						subAttributes[i],
						propertyIndex,
						subtypes[propertyIndex],
						componentType.getPropertyNullability()));
				// @formatter:on
			}

			return associations;
		}

		return Collections.emptyMap();
	}

	private <D extends DomainResource> List<String> locateNonLazyAttributes(Class<D> resourceType,
			EntityMetamodel metamodel, List<String> wrappedAttributeNames) throws Exception {
		if (logger.isTraceEnabled()) {
			logger.trace("Locating non-lazy attributes");
		}

		final List<String> nonLazyAttributes = new ArrayList<>();
		final int span = wrappedAttributeNames.size();
		final Type identifierType = metamodel.getIdentifierProperty().getType();
		final Type[] attributeHBMTypes = metamodel.getPropertyTypes();

		for (int i = 0; i < span; i++) {
			final String attributeName = wrappedAttributeNames.get(i);

			if (attributeName.equals(metamodel.getIdentifierProperty().getName())) {
				nonLazyAttributes.add(attributeName);

				if (isComponent(identifierType)) {
					nonLazyAttributes
							.addAll(locateNonLazyAttributesOfComponent(resourceType, (ComponentType) identifierType));
				}

				continue;
			}

			final Type attributeType = attributeHBMTypes[metamodel.getPropertyIndex(attributeName)];

			if (!isComponent(attributeType)) {
				if (isLazy(resourceType, attributeName, attributeType, i, metamodel.getPropertyLaziness())) {
					continue;
				}

				nonLazyAttributes.add(attributeName);
				continue;
			}

			nonLazyAttributes.addAll(locateNonLazyAttributesOfComponent(resourceType, (ComponentType) attributeType));
		}

		return nonLazyAttributes.isEmpty() ? Collections.emptyList() : nonLazyAttributes;
	}

	private List<String> locateNonLazyAttributesOfComponent(Class<?> owningType, ComponentType componentType)
			throws Exception {
		final List<String> nonLazyAttributes = new ArrayList<>();
		// assumes all properties in a component is eager
		// @formatter:off
		final boolean[] componentAttributeLaziness = Utils
				.declare(new boolean[componentType.getPropertyNames().length])
					.second(false)
				.consume(Arrays::fill)
				.get();
		// @formatter:on
		for (final String componentAttributeName : componentType.getPropertyNames()) {
			final int componentAttributeIndex = componentType.getPropertyIndex(componentAttributeName);
			final Type componentAttributeType = componentType.getSubtypes()[componentAttributeIndex];

			if (isComponent(componentAttributeType)) {
				nonLazyAttributes.addAll(locateNonLazyAttributesOfComponent(componentType.getReturnedClass(),
						(ComponentType) componentAttributeType));
				continue;
			}

			if (!isLazy(owningType, componentAttributeName, componentAttributeType, componentAttributeIndex,
					componentAttributeLaziness)) {
				nonLazyAttributes.add(componentAttributeName);
			}
		}

		return nonLazyAttributes.isEmpty() ? Collections.emptyList() : nonLazyAttributes;
	}

	private boolean isLazy(Class<?> owningType, String name, Type type, int index, boolean[] propertyLaziness)
			throws NoSuchFieldException, SecurityException {
		if (!org.hibernate.type.AssociationType.class.isAssignableFrom(type.getClass())) {
			return propertyLaziness[index];
		}

		if (EntityType.class.isAssignableFrom(type.getClass())) {
			return !((EntityType) type).isEager(null);
		}

		if (CollectionType.class.isAssignableFrom(type.getClass())) {
			final Class<?> genericType = (Class<?>) TypeHelper.getGenericType(owningType.getDeclaredField(name));

			if (!Entity.class.isAssignableFrom(genericType) && !FileResource.class.isAssignableFrom(genericType)) {
				return propertyLaziness[index];
			}
			// this is how a CollectionPersister role is resolved by Hibernate
			// see org.hibernate.cfg.annotations.CollectionBinder.bind()
			String collectionRole = org.hibernate.internal.util.StringHelper.qualify(owningType.getName(), name);

			return sfi.getMetamodel().collectionPersister(collectionRole).isLazy();
		}

		return propertyLaziness[index];
	}

	private void unwrapAttributes(EntityMetamodel entityMetamodel, List<String> attributesToBeUnwrapped,
			Map<String, Class<?>> attributeTypes) {
		if (logger.isTraceEnabled()) {
			logger.trace("Unwrapping attributes");
		}

		final int span = attributesToBeUnwrapped.size();
		final Type identifierType = entityMetamodel.getIdentifierProperty().getType();
		final Type[] attributeHBMTypes = entityMetamodel.getPropertyTypes();

		for (int i = 0; i < span; i++) {
			final String attributeName = attributesToBeUnwrapped.get(i);

			if (entityMetamodel.getIdentifierProperty().getName().equals(attributeName)) {
				if (isComponent(identifierType)) {
					unwrapComponentAttribute((ComponentType) identifierType, attributesToBeUnwrapped, attributeTypes);
				}

				continue;
			}

			final Type attributeType = attributeHBMTypes[entityMetamodel.getPropertyIndex(attributeName)];

			if (!isComponent(attributeType)) {
				continue;
			}

			unwrapComponentAttribute((ComponentType) attributeType, attributesToBeUnwrapped, attributeTypes);
		}
	}

	private void unwrapComponentAttribute(ComponentType componentType, List<String> attributesToBeUnwrapped,
			Map<String, Class<?>> attributeTypes) {
		final Type[] componentAttributeTypes = componentType.getSubtypes();
		final List<String> nestedComponentAttributes = new ArrayList<>(0);

		for (final String componentAttribute : componentType.getPropertyNames()) {
			final Type componentAttributeType = componentAttributeTypes[componentType
					.getPropertyIndex(componentAttribute)];

			nestedComponentAttributes.add(componentAttribute);
			attributeTypes.put(componentAttribute, componentAttributeType.getReturnedClass());

			if (!isComponent(componentAttributeType)) {
				continue;
			}

			unwrapComponentAttribute((ComponentType) componentAttributeType, nestedComponentAttributes, attributeTypes);
		}

		attributesToBeUnwrapped.addAll(nestedComponentAttributes);
	}

	private boolean isComponent(Type type) {
		return ComponentType.class.isAssignableFrom(type.getClass());
	}

	private Map<String, Class<?>> locateAttributeTypes(EntityMetamodel metamodel, List<String> wrappedAttributeNames) {
		if (logger.isTraceEnabled()) {
			logger.trace("Locating attribute types");
		}

		final int span = wrappedAttributeNames.size();
		final Type[] propertyTypes = metamodel.getPropertyTypes();
		final Map<String, Class<?>> attributeTypes = new HashMap<>(0);

		for (int i = 0; i < span; i++) {
			final String attributeName = wrappedAttributeNames.get(i);

			if (metamodel.getIdentifierProperty().getName().equals(attributeName)) {
				attributeTypes.put(attributeName, metamodel.getIdentifierProperty().getType().getReturnedClass());
				continue;
			}

			attributeTypes.put(attributeName,
					propertyTypes[metamodel.getPropertyIndex(attributeName)].getReturnedClass());
		}

		return attributeTypes;
	}

	private <D extends DomainResource> List<String> locateDeclaredAttributeNames(Class<D> resourceType,
			EntityMetamodel metamodel) {
		return new ArrayList<>(sfi.getMetamodel().entity(resourceType).getDeclaredAttributes().stream()
				.map(Attribute::getName).toList());
	}

	private List<String> locateWrappedAttributeNames(EntityMetamodel metamodel) throws Exception {
		if (logger.isTraceEnabled()) {
			logger.trace("Locating wrapped attributes");
		}
		// @formatter:off
		return Utils.declare(getNonIdentifierAttributes(metamodel))
					.prepend(metamodel)
				.then(this::addIdentifierAttribute)
				.get();
		// @formatter:on
	}

	private List<String> getNonIdentifierAttributes(EntityMetamodel metamodel) {
		return new ArrayList<>(List.of(metamodel.getPropertyNames()));
	}

	private List<String> addIdentifierAttribute(EntityMetamodel metamodel,
			List<String> attributesToBeJoinnedWithIdentifier) {
		attributesToBeJoinnedWithIdentifier.add(metamodel.getIdentifierProperty().getName());
		return attributesToBeJoinnedWithIdentifier;
	}

	@SuppressWarnings({ "rawtypes" })
	private static class MetadataDecorator {

		private static final MetadataDecorator INSTANCE = new MetadataDecorator();
		// @formatter:off
		private final List<HandledBiFunction<Class, EntityPersister, BiDeclaration<Class, DomainResourceMetadata>, Exception>> metadataContributors = List
				.of(
						this::resolveIdentifiableResourceLogics,
						this::resolveNamedResourceLogics);
		// @formatter:on
		@SuppressWarnings("unchecked")
		public <D extends DomainResource> Map<Class<? extends DomainResourceMetadata<D>>, DomainResourceMetadata<D>> getDecorations(
				Class<D> resourceType, EntityPersister persister) throws Exception {
			final Map<Class<? extends DomainResourceMetadata<D>>, DomainResourceMetadata<D>> metadatas = new HashMap<>(
					0);

			for (final HandledBiFunction<Class, EntityPersister, BiDeclaration<Class, DomainResourceMetadata>, Exception> filter : metadataContributors) {
				final BiDeclaration<Class, DomainResourceMetadata> filterEntry = filter.apply(resourceType, persister);

				if (filterEntry.getSecond() == null) {
					continue;
				}

				metadatas.put(filterEntry.getFirst(), filterEntry.getSecond());
			}

			return metadatas;
		}

		@SuppressWarnings("unchecked")
		private BiDeclaration<Class, DomainResourceMetadata> resolveNamedResourceLogics(Class resourceType,
				EntityPersister persister) {
			if (!NamedResource.class.isAssignableFrom(resourceType)) {
				return declare(NamedResourceMetadata.class, null);
			}

			final List<Field> scopedFields = new ArrayList<>(0);

			for (final Field field : resourceType.getDeclaredFields()) {
				if (!field.isAnnotationPresent(Name.class)) {
					continue;
				}

				scopedFields.add(field);
			}

			if (scopedFields.size() == 0) {
				throw new IllegalArgumentException(Name.Message.getMissingMessage(resourceType));
			}
			// @formatter:off
			return declare(
					NamedResourceMetadata.class,
					new NamedResourceMetadataImpl<>(resourceType, scopedFields));
			// @formatter:on
		}

		@SuppressWarnings("unchecked")
		private BiDeclaration<Class, DomainResourceMetadata> resolveIdentifiableResourceLogics(Class resourceType,
				EntityPersister persister) {
			if (!IdentifiableResource.class.isAssignableFrom(resourceType)) {
				return declare(IdentifiableResourceMetadata.class, null);
			}

			final IdentifierGenerator identifierGenerator = persister.getEntityMetamodel().getIdentifierProperty()
					.getIdentifierGenerator();
			final boolean isIdentifierAutoGenerated = !(identifierGenerator instanceof CompositeNestedGeneratedValueGenerator
					|| identifierGenerator instanceof Assigned);

			// @formatter:off
			return declare(IdentifiableResourceMetadata.class, new IdentifiableResourceMetadataImpl<>(resourceType, isIdentifierAutoGenerated));
			// @formatter:on
		}

	}

}
