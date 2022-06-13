/**
 * 
 */
package multicados.internal.domain.metadata;

import static java.util.Collections.unmodifiableList;
import static multicados.internal.helper.Utils.declare;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Queue;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.tuple.IdentifierProperty;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.hibernate.type.AssociationType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.ComponentType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import multicados.internal.context.ContextManager;
import multicados.internal.domain.DomainComponentType;
import multicados.internal.domain.DomainResource;
import multicados.internal.domain.DomainResourceContext;
import multicados.internal.domain.DomainResourceGraph;
import multicados.internal.domain.Entity;
import multicados.internal.domain.FileResource;
import multicados.internal.domain.metadata.DomainResourceMetadataImpl.DomainAssociation.MandatoryAssociation;
import multicados.internal.domain.metadata.DomainResourceMetadataImpl.DomainAssociation.OptionalAssociation;
import multicados.internal.file.engine.FileManagement;
import multicados.internal.helper.CollectionHelper;
import multicados.internal.helper.StringHelper;
import multicados.internal.helper.TypeHelper;
import multicados.internal.helper.Utils;

/**
 * @author Ngoc Huy
 *
 */
public class DomainResourceMetadataImpl<T extends DomainResource> implements DomainResourceMetadata<T> {

	private final Class<T> resourceType;

	private final List<String> attributeNames;
	private final List<String> enclosedAttributeNames;
	private final List<String> nonLazyAttributeNames;
	private final Map<String, Class<?>> attributeTypes;

	private final Map<String, DomainAssociation> associationAttributes;
	private final Map<String, ComponentPath> componentPaths;

	public DomainResourceMetadataImpl(Class<T> resourceType, DomainResourceContext resourceContextProvider,
			Map<Class<? extends DomainResource>, DomainResourceMetadata<? extends DomainResource>> metadatasMap)
			throws Exception {
		this.resourceType = resourceType;

		Builder<T> builder = isHbmManaged(resourceType) ? new HibernateResourceMetadataBuilder<>(resourceType)
				: new NonHibernateResourceMetadataBuilder<>(resourceType, resourceContextProvider, metadatasMap);

		attributeNames = unmodifiableList(builder.locateAttributeNames());
		enclosedAttributeNames = unmodifiableList(builder.locateEnclosedAttributeNames());
		nonLazyAttributeNames = unmodifiableList(builder.locateNonLazyAttributeNames());
		attributeTypes = Collections.unmodifiableMap(builder.locateAttributeTypes());
		associationAttributes = Collections.unmodifiableMap(builder.locateAssociations());
		componentPaths = Collections.unmodifiableMap(builder.resolveComponentPaths());
	}

	private boolean isHbmManaged(Class<T> resourceType) {
		return (Entity.class.isAssignableFrom(resourceType) || FileResource.class.isAssignableFrom(resourceType))
				&& !Modifier.isAbstract(resourceType.getModifiers());
	}

	@Override
	public Class<T> getResourceType() {
		return resourceType;
	}

	@Override
	public List<String> getAttributeNames() {
		return attributeNames;
	}

	@Override
	public List<String> getEnclosedAttributeNames() {
		return enclosedAttributeNames;
	}

	@Override
	public Class<?> getAttributeType(String attributeName) {
		return attributeTypes.get(attributeName);
	}

	@Override
	public List<String> getNonLazyAttributeNames() {
		return nonLazyAttributeNames;
	}

	@Override
	public boolean isAssociation(String attributeName) {
		return associationAttributes.containsKey(attributeName);
	}

	/**
	 * Assumes association exists
	 */
	@Override
	public boolean isAssociationOptional(String associationName) {
		return OptionalAssociation.class.isAssignableFrom(associationAttributes.get(associationName).getClass());
	}

	@Override
	public boolean isAssociationInComponent(String associationName) {
		return associationAttributes.get(associationName).isComponent();
	}

	@Override
	public Map<String, Class<?>> getAttributeTypes() {
		return attributeTypes;
	}

	@Override
	public Map<String, ComponentPath> getComponentPaths() {
		return componentPaths;
	}

	@Override
	public multicados.internal.domain.metadata.AssociationType getAssociationType(String attributeName) {
		return associationAttributes.get(attributeName).getType();
	}

	@Override
	public boolean isComponent(String attributeName) {
		return componentPaths.containsKey(attributeName);
	}

	private interface Builder<D extends DomainResource> {

		List<String> locateAttributeNames() throws Exception;

		List<String> locateEnclosedAttributeNames() throws Exception;

		List<String> locateNonLazyAttributeNames() throws Exception;

		Map<String, Class<?>> locateAttributeTypes() throws Exception;

		Map<String, DomainAssociation> locateAssociations() throws Exception;

		Map<String, ComponentPath> resolveComponentPaths() throws Exception;

	}

	@SuppressWarnings({ "rawtypes" })
	private class HibernateResourceMetadataBuilder<D extends DomainResource> implements Builder<D> {

		private static final Logger logger = LoggerFactory
				.getLogger(DomainResourceMetadataImpl.HibernateResourceMetadataBuilder.class);

		private final EntityPersister persister;
		private final EntityMetamodel metamodel;

		private HibernateResourceMetadataBuilder(Class<D> resourceType) {
			logger.trace("Building {} for Hibernate entity of type [{}]", DomainResourceMetadata.class.getSimpleName(),
					resourceType.getName());
			persister = locatePersister(resourceType);
			metamodel = persister.getEntityMetamodel();
		}

		private EntityPersister locatePersister(Class<D> resourceType) {
			if (Entity.class.isAssignableFrom(resourceType)) {
				return ContextManager.getBean(SessionFactoryImplementor.class).getMetamodel()
						.entityPersister(resourceType);
			}

			return ContextManager.getBean(FileManagement.class).getSessionFactory().getMetamodel()
					.entityPersister(resourceType);
		}

		@Override
		public Map<String, ComponentPath> resolveComponentPaths() throws Exception {
			// @formatter:off
			return declare(getAttributeNames())
					.then(this::addEnclosedIdentifierAttributeName)
					.then(this::doLocateComponentPaths)
					.get();
			// @formatter:on
		}

		private Map<String, ComponentPath> doLocateComponentPaths(String[] attributeNames) throws Exception {
			int span = attributeNames.length;
			Map<String, ComponentPath> paths = new HashMap<>();

			for (int i = 0; i < span; i++) {
				String attributeName = attributeNames[i];
				Type type = metamodel.getIdentifierProperty().getName().equals(attributeName)
						? metamodel.getIdentifierProperty().getType()
						: metamodel.getPropertyTypes()[metamodel.getPropertyIndex(attributeName)];

				if (!type.getClass().isAssignableFrom(ComponentType.class)) {
					continue;
				}

				List<Entry<String, ComponentPathImpl>> pathEntries = individuallyResolveComponentPath(
						new ComponentPathImpl(), attributeName, type);

				for (Map.Entry<String, ComponentPathImpl> entry : pathEntries) {
					paths.put(entry.getKey(), entry.getValue());
				}
			}

			return paths;
		}

		private List<Map.Entry<String, ComponentPathImpl>> individuallyResolveComponentPath(
				ComponentPathImpl currentPath, String attributeName, Type type) throws Exception {
			ComponentPathImpl root = declare(new ComponentPathImpl(currentPath))
					.consume(self -> self.add(attributeName)).get();
			List<Map.Entry<String, ComponentPathImpl>> paths = declare(
					new ArrayList<Map.Entry<String, ComponentPathImpl>>())
							.consume(self -> self.add(Map.entry(attributeName, root))).get();

			if (!(type instanceof ComponentType)) {
				return paths;
			}

			ComponentType componentType = (ComponentType) type;
			int span = componentType.getPropertyNames().length;

			for (int i = 0; i < span; i++) {
				// @formatter:off
				declare(new ComponentPathImpl(root))
						.second(componentType.getPropertyNames()[i])
						.third(componentType.getSubtypes()[i])
					.then(this::individuallyResolveComponentPath)
					.consume(paths::addAll);
				// @formatter:on
			}

			return paths;
		}

		@Override
		public Map<String, DomainAssociation> locateAssociations() throws Exception {
			// @formatter:off
			return declare(getAttributeNames())
					.then(this::doLocateAssociations)
					.get();
			// @formatter:on
		}

		private Map<String, DomainAssociation> doLocateAssociations(String[] attributeNames) throws Exception {
			Type[] types = persister.getPropertyTypes();
			Map<String, DomainAssociation> associationAttributes = new HashMap<>(16);
			int span = attributeNames.length;

			for (int i = 0; i < span; i++) {
				int propertyIndex = metamodel.getPropertyIndex(attributeNames[i]);
				// @formatter:off
				associationAttributes.putAll(
					individuallyLocateAssociations(
						attributeNames[i],
						propertyIndex,
						types[propertyIndex],
						metamodel.getPropertyNullability()));
				// @formatter:on
			}

			return associationAttributes;
		}

		private multicados.internal.domain.metadata.AssociationType resolveAssociationType(AssociationType type) {
			return type instanceof CollectionType ? multicados.internal.domain.metadata.AssociationType.COLLECTION
					: type instanceof EntityType ? multicados.internal.domain.metadata.AssociationType.ENTITY
							: Objects.requireNonNull(null,
									String.format("Unknown association type [%s]", type.getName()));
		}

		private Map<String, DomainAssociation> individuallyLocateAssociations(String attributeName, int attributeIndex,
				Type type, boolean[] nullabilities) throws Exception {
			Map<String, DomainAssociation> associationAttributes = new HashMap<>(8);

			if (type instanceof AssociationType) {
				multicados.internal.domain.metadata.AssociationType associationType = resolveAssociationType(
						(AssociationType) type);
				// @formatter:off
				associationAttributes.put(attributeName,
						nullabilities[attributeIndex] ?
							new OptionalAssociation(attributeName, associationType, true) :
								new MandatoryAssociation(attributeName, associationType, true));
				return associationAttributes;
				// @formatter:on
			}

			if (type instanceof ComponentType) {
				ComponentType componentType = (ComponentType) type;
				String[] subAttributes = componentType.getPropertyNames();
				Type[] subtypes = componentType.getSubtypes();
				int span = subAttributes.length;

				for (int i = 0; i < span; i++) {
					int propertyIndex = componentType.getPropertyIndex(subAttributes[i]);
					// @formatter:off
					associationAttributes.putAll(
						individuallyLocateAssociations(
							subAttributes[i],
							propertyIndex,
							subtypes[propertyIndex],
							componentType.getPropertyNullability()));
					// @formatter:on
				}

				return associationAttributes;
			}

			return Collections.emptyMap();
		}

		@Override
		public List<String> locateAttributeNames() throws Exception {
			// @formatter:off
			return declare(getAttributeNames())
					.then(this::unwrapComponentsAttributes)
					.then(this::addIdentifierAttributeNames)
					.then(Arrays::asList)
					.get();
			// @formatter:on
		}

		@Override
		public List<String> locateNonLazyAttributeNames() throws Exception {
			// @formatter:off
			return declare(getAttributeNames())
					.then(Arrays::asList)
					.then(this::resolveNonLazyAttributes)
					.then(this::resolveIdentifierLaziness)
					.get();
			// @formatter:on
		}

		private List<String> resolveIdentifierLaziness(List<String> nonLazyAttributes) {
			IdentifierProperty identifier = metamodel.getIdentifierProperty();

			if (identifier.isVirtual()) {
				return nonLazyAttributes;
			}

			if (!ComponentType.class.isAssignableFrom(identifier.getType().getClass())) {
				nonLazyAttributes.add(identifier.getName());

				return nonLazyAttributes;
			}

			nonLazyAttributes.addAll(List.of(((ComponentType) identifier.getType()).getPropertyNames()));

			return nonLazyAttributes;
		}

		private List<String> resolveNonLazyAttributes(List<String> attributes) {
			// @formatter:off
			return attributes
					.stream()
					.map(this::doResolveNonLazyAttributes)
					.flatMap(List::stream)
					.collect(Collectors.toList());
			// @formatter:on
		}

		private List<String> doResolveNonLazyAttributes(String attributeName) {
			try {
				int index = metamodel.getPropertyIndex(attributeName);
				Type type = metamodel.getPropertyTypes()[index];

				if (!ComponentType.class.isAssignableFrom(type.getClass())) {
					// @formatter:off
					return isLazy(
							attributeName,
							type,
							index,
							metamodel.getPropertyLaziness()) ?
									Collections.emptyList() :
										List.of(attributeName);
					// @formatter:on
				}

				ComponentType componentType = (ComponentType) type;
				List<String> nonLazyProperties = new ArrayList<>();
				int componentPropertyIndex;
				boolean[] componentPropertyLaziness = new boolean[componentType.getPropertyNames().length];
				// assumes all properties in a component is non-lazy
				Arrays.fill(componentPropertyLaziness, false);

				for (String componentProperty : componentType.getPropertyNames()) {
					componentPropertyIndex = componentType.getPropertyIndex(componentProperty);
					// @formatter:off
					if (!isLazy(
							componentProperty,
							componentType.getSubtypes()[componentPropertyIndex],
							componentPropertyIndex,
							componentPropertyLaziness)) {
						nonLazyProperties.add(componentProperty);
					}
					// @formatter:on					
				}

				return nonLazyProperties.isEmpty() ? Collections.emptyList() : nonLazyProperties;
			} catch (NoSuchFieldException | SecurityException any) {
				return Collections.emptyList();
			}
		}

		private boolean isLazy(String name, Type type, int index, boolean[] propertyLaziness)
				throws NoSuchFieldException, SecurityException {
			if (!AssociationType.class.isAssignableFrom(type.getClass())) {
				return propertyLaziness[index];
			}

			if (EntityType.class.isAssignableFrom(type.getClass())) {
				return !((EntityType) type).isEager(null);
			}

			Class owningType = type.getReturnedClass();

			if (CollectionType.class.isAssignableFrom(type.getClass())) {
				if (!Entity.class
						.isAssignableFrom((Class) TypeHelper.getGenericType(owningType.getDeclaredField(name)))) {
					return propertyLaziness[index];
				}
				// this is how a CollectionPersister role is resolved by Hibernate
				// see org.hibernate.cfg.annotations.CollectionBinder.bind()
				String collectionRole = org.hibernate.internal.util.StringHelper.qualify(owningType.getName(), name);

				return persister.getFactory().getMetamodel().collectionPersister(collectionRole).isLazy();
			}

			return propertyLaziness[index];
		}

		@Override
		public List<String> locateEnclosedAttributeNames() throws Exception {
			// @formatter:off
			return declare(getAttributeNames())
					.then(this::addEnclosedIdentifierAttributeName)
					.then(Arrays::asList)
					.get();
			// @formatter:on
		}

		private String[] addEnclosedIdentifierAttributeName(String[] enclosedAttributeNames) throws Exception {
			// @formatter:off
			return declare(enclosedAttributeNames)
					.second(CollectionHelper.toArray(metamodel.getIdentifierProperty().getName()))
				.then((enclosedNonIdentifier, enclosedIdentifier) -> CollectionHelper.join(String.class, enclosedNonIdentifier, enclosedIdentifier))
				.get();
			// @formatter:on
		}

		@Override
		public Map<String, Class<?>> locateAttributeTypes() throws Exception {
			// @formatter:off
			return declare(getAttributeNames())
					.then(this::mapTypes)
					.then(this::addIdentifierType)
					.then(this::unwrapTypes)
					.get();
			// @formatter:on
		}

		private Map<String, Type> mapTypes(String[] attributes) {
			return Stream.of(attributes)
					.map(attribute -> Map.entry(attribute,
							metamodel.getPropertyTypes()[metamodel.getPropertyIndex(attribute)]))
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		}

		private Map<String, Type> addIdentifierType(Map<String, Type> types) {
			IdentifierProperty identifierProp = metamodel.getIdentifierProperty();

			types.put(identifierProp.getName(), identifierProp.getType());

			return types;
		}

		private Map<String, Class<?>> unwrapTypes(Map<String, Type> types) throws Exception {
			Map<String, Type> results = new HashMap<>();

			for (Map.Entry<String, Type> entry : types.entrySet()) {
				results.put(entry.getKey(), entry.getValue());

				if (!ComponentType.class.isAssignableFrom(entry.getValue().getClass())) {
					continue;
				}
				// @formatter:off
				declare(entry.getKey())
						.second((ComponentType) entry.getValue())
					.then(this::unwrapComponentType)
					.consume(results::putAll);
				// @formatter:on
			}
			// @formatter:off
			return results.entrySet().stream()
				.map(entry -> Map.entry(entry.getKey(), entry.getValue().getReturnedClass()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
			// @formatter:on
		}

		private Map<String, Type> unwrapComponentType(String attributeName, ComponentType componentType)
				throws Exception {
			Map<String, Type> types = new HashMap<>();

			for (String subAttr : componentType.getPropertyNames()) {
				Type subType = componentType.getSubtypes()[componentType.getPropertyIndex(subAttr)];

				types.put(subAttr, subType);

				if (!ComponentType.class.isAssignableFrom(subType.getClass())) {
					continue;
				}
				// @formatter:off
				declare(subAttr)
						.second((ComponentType) subType)
					.then(this::unwrapComponentType)
					.consume(types::putAll);
				// @formatter:on
			}

			return types;
		}

		private String[] getAttributeNames() {
			return metamodel.getPropertyNames();
		}

		private String[] unwrapComponentsAttributes(String[] attributeNames) {
			List<String> unwrappedAttributes = new ArrayList<>(0);
			Type attributeType;

			for (String attributeName : attributeNames) {
				attributeType = resolveAttributeType(attributeName);

				if (!ComponentType.class.isAssignableFrom(attributeType.getClass())) {
					continue;
				}

				ComponentType componentType = (ComponentType) attributeType;

				for (String componentAttributeName : componentType.getPropertyNames()) {
					unwrappedAttributes.add(componentAttributeName);
				}
			}

			return CollectionHelper.join(String.class, attributeNames, unwrappedAttributes.toArray(String[]::new));
		}

		private Type resolveAttributeType(String attributeName) {
			if (metamodel.getIdentifierProperty().getName().equals(attributeName)) {
				return metamodel.getIdentifierProperty().getType();
			}

			return metamodel.getPropertyTypes()[metamodel.getPropertyIndex(attributeName)];
		}

		private String[] addIdentifierAttributeNames(String[] unwrappedAttributeNames) throws Exception {
			IdentifierProperty identifier = metamodel.getIdentifierProperty();

			if (identifier.isVirtual()) {
				return org.hibernate.internal.util.StringHelper.EMPTY_STRINGS;
			}
			// @formatter:off
			return Utils
					.declare(identifier.getName())
					.then(CollectionHelper::toArray)
					.then(this::unwrapComponentsAttributes)
					.then(idAttributes -> CollectionHelper.join(String.class, idAttributes, unwrappedAttributeNames))
					.get();
			// @formatter:on
		}

	}

	private class NonHibernateResourceMetadataBuilder<D extends DomainResource> implements Builder<D> {

		private static final Logger logger = LoggerFactory
				.getLogger(DomainResourceMetadataImpl.NonHibernateResourceMetadataBuilder.class);

		private final Class<D> resourceType;

		private final DomainResourceContext resourceContextProvider;
		private final Map<Class<? extends DomainResource>, DomainResourceMetadata<? extends DomainResource>> metadatasMap;

//		private ObservableMetadataEntries observableMetadataEntries;

		public NonHibernateResourceMetadataBuilder(Class<D> resourceType, DomainResourceContext resourceContextProvider,
				Map<Class<? extends DomainResource>, DomainResourceMetadata<? extends DomainResource>> metadatasMap) {
			logger.trace("Building {} for resource of type [{}]", DomainResourceMetadata.class.getSimpleName(),
					resourceType.getName());
			this.resourceType = resourceType;
			this.resourceContextProvider = resourceContextProvider;
			this.metadatasMap = metadatasMap;
		}

		@Override
		public Map<String, ComponentPath> resolveComponentPaths() throws Exception {
			// @formatter:off
			return declare(getDeclaredAttributeNames())
						.then(this::doLocateComponentPaths)
						.then(this::joinWithParentComponentPaths)
					.get();
			// @formatter:on
		}

		private Map<String, ComponentPath> doLocateComponentPaths(String[] attributeNames) throws Exception {
			int span = attributeNames.length;
			Map<String, ComponentPath> paths = new HashMap<>();

			for (int i = 0; i < span; i++) {
				String attributeName = attributeNames[i];
				Class<?> type = resourceType.getDeclaredField(attributeName).getType();

				if (!DomainComponentType.class.isAssignableFrom(type)) {
					continue;
				}

				List<Entry<String, ComponentPathImpl>> pathEntries = individuallyResolveComponentPath(
						new ComponentPathImpl(), attributeName, type);

				for (Entry<String, ComponentPathImpl> entry : pathEntries) {
					paths.put(entry.getKey(), entry.getValue());
				}
			}

			return paths;
		}

		private List<Map.Entry<String, ComponentPathImpl>> individuallyResolveComponentPath(
				ComponentPathImpl currentPath, String attributeName, Class<?> attributeType) throws Exception {
			ComponentPathImpl root = declare(new ComponentPathImpl(currentPath))
					.consume(self -> self.add(attributeName)).get();
			List<Map.Entry<String, ComponentPathImpl>> paths = declare(
					new ArrayList<Map.Entry<String, ComponentPathImpl>>())
							.consume(self -> self.add(Map.entry(attributeName, root))).get();

			if (!DomainComponentType.class.isAssignableFrom(attributeType)) {
				return paths;
			}

			Field[] subFields = attributeType.getDeclaredFields();
			int span = subFields.length;
			String[] subAttributeNames = Stream.of(subFields).map(Field::getName).toArray(String[]::new);
			Class<?>[] subAttributeTypes = Stream.of(subFields).map(Field::getType).toArray(Class[]::new);

			for (int i = 0; i < span; i++) {
				// @formatter:off
				declare(new ComponentPathImpl(root))
						.second(subAttributeNames[i])
						.third(subAttributeTypes[i])
					.then(this::individuallyResolveComponentPath)
					.consume(paths::addAll);
				// @formatter:on
			}

			return paths;
		}

		private Map<String, ComponentPath> joinWithParentComponentPaths(Map<String, ComponentPath> declaredPaths)
				throws Exception {
			// @formatter:off
			return Utils.declare(locateParentGraph())
						.then(DomainResourceGraph::getResourceType)
						.then(metadatasMap::get)
						.then(metadata -> metadata == null ? Collections.<String, ComponentPath>emptyMap() : metadata.getComponentPaths())
						.then(parentComponentsPaths -> declare(declaredPaths).consume(self -> self.putAll(parentComponentsPaths)).get())
						.get();
			// @formatter:on
		}

		@Override
		public Map<String, DomainAssociation> locateAssociations() throws Exception {
			// @formatter:off
			return declare(getDeclaredAttributeNames())
					.then(this::joinWithParentEnclosedAttributeNames)
					.then(this::doLocateAssocations)
					.get();
			// @formatter:on
		}

		private Map<String, DomainAssociation> doLocateAssocations(String[] attributeNames) throws Exception {
			int span = attributeNames.length;
			Map<String, DomainAssociation> associations = new HashMap<>();

			for (int i = 0; i < span; i++) {
				// @formatter:off
				declare(attributeNames[i])
						.second(attributeTypes.get(attributeNames[i]))
						.third(false)
					.then(this::individuallyLocateAssocations)
					.consume(associations::putAll);
				// @formatter:on
			}

			return associations;
		}

		private Map<String, DomainAssociation> individuallyLocateAssocations(String attributeName,
				Class<?> attributeType, boolean isComponent) throws Exception {
			Map<String, DomainAssociation> associations = new HashMap<>();

			if (DomainComponentType.class.isAssignableFrom(attributeType)) {
				Field[] subFields = Stream.of(attributeType.getDeclaredFields())
						.filter(field -> !Modifier.isStatic(field.getModifiers())).toArray(Field[]::new);
				String[] subAttributes = Stream.of(subFields).map(Field::getName).toArray(String[]::new);
				Class<?>[] subTypes = Stream.of(subFields).map(Field::getType).toArray(Class[]::new);
				int span = subAttributes.length;

				for (int i = 0; i < span; i++) {
					// @formatter:off
					declare(subAttributes[i])
							.second(subTypes[i])
							.third(true)
						.then(this::individuallyLocateAssocations)
						.consume(associations::putAll);
					// @formatter:on
				}

				return associations;
			}

			BiFunction<String, multicados.internal.domain.metadata.AssociationType, DomainAssociation> resolver = resourceType
					.getDeclaredField(attributeName).isAnnotationPresent(Nullable.class)
							? (attributName, associationType) -> new OptionalAssociation(attributeName, associationType,
									isComponent)
							: (attributName, associationType) -> new MandatoryAssociation(attributeName,
									associationType, isComponent);

			if (DomainResource.class.isAssignableFrom(attributeType)) {
				associations.put(attributeName,
						resolver.apply(attributeName, multicados.internal.domain.metadata.AssociationType.ENTITY));
				return associations;
			}

			if (Collection.class.isAssignableFrom(attributeType)) {
				if (DomainResource.class.isAssignableFrom(
						Class.forName(TypeHelper.getGenericType(resourceType.getField(attributeName)).getTypeName()))) {
					// logically should never throw NoSuchFieldException
					// or else there's a fraud
					associations.put(attributeName, resolver.apply(attributeName,
							multicados.internal.domain.metadata.AssociationType.COLLECTION));
					return associations;
				}
			}

			return Collections.emptyMap();
		}

		@Override
		public List<String> locateAttributeNames() throws Exception {
			// @formatter:off
			return declare(getDeclaredAttributeNames())
					.then(this::unwrapAttributeNames)
					.then(this::joinWithParentAttributeNames)
					.then(Arrays::asList)
					.get();
			// @formatter:on
		}

		@Override
		public List<String> locateEnclosedAttributeNames() throws Exception {
			// @formatter:off
			return declare(getDeclaredAttributeNames())
					.then(this::joinWithParentEnclosedAttributeNames)
					.then(Arrays::asList)
					.get();
			// @formatter:on
		}

		@Override
		public List<String> locateNonLazyAttributeNames() throws Exception {
			return enclosedAttributeNames;
		}

		@Override
		public Map<String, Class<?>> locateAttributeTypes() throws Exception {
			// @formatter:off
			return declare(getDeclaredAttributeNames())
					.then(this::getAttributeTypes)
					.then(this::unwrapAttributeTypes)
					.then(this::joinWithParentAttributeTypes)
					.get();
			// @formatter:on
		}

		private Map<String, Class<?>> unwrapAttributeTypes(Map<String, Class<?>> typesMap) {
			Map<String, Class<?>> unwrappedTypesMap = new HashMap<>();

			for (Map.Entry<String, Class<?>> entry : typesMap.entrySet()) {
				Class<?> attributeType = entry.getValue();

				if (!DomainComponentType.class.isAssignableFrom(attributeType)) {
					unwrappedTypesMap.put(entry.getKey(), entry.getValue());
					continue;
				}

				for (Field componentField : attributeType.getDeclaredFields()) {
					unwrappedTypesMap.put(componentField.getName(), componentField.getType());
				}
			}

			return unwrappedTypesMap;
		}

		private String[] getDeclaredAttributeNames() {
			// @formatter:off
			return Stream.of(resourceType.getDeclaredFields())
					.filter(field -> !Modifier.isStatic(field.getModifiers()))
					.map(Field::getName)
					.toArray(String[]::new);
			// @formatter:on
		}

		private String[] unwrapAttributeNames(String[] attributeNames) throws NoSuchFieldException, SecurityException {
			Map<String, Class<?>> typesMap = getAttributeTypes(attributeNames);
			// @formatter:off
			return Stream.of(attributeNames)
					.map(attribute -> {
						if (!DomainComponentType.class.isAssignableFrom(typesMap.get(attribute))) {
							return Stream.of(attribute);
						}

						return Stream.concat(Stream.of(typesMap.get(attribute).getDeclaredFields())
								.map(Field::getName), Stream.of(attribute));
					})
					.flatMap(Function.identity())
					.toArray(String[]::new);
			// @formatter:on
		}

		private Map<String, Class<?>> getAttributeTypes(String[] attributeNames)
				throws NoSuchFieldException, SecurityException {
			Map<String, Class<?>> typesMap = new HashMap<>(attributeNames.length, 1.5f);

			for (String attribute : attributeNames) {
				typesMap.put(attribute, resourceType.getDeclaredField(attribute).getType());
			}

			return typesMap;
		}

		@SuppressWarnings("unchecked")
		private DomainResourceGraph<? super DomainResource> locateParentGraph() throws Exception {
			return (DomainResourceGraph<? super DomainResource>) Utils
					.declare(resourceContextProvider.getResourceGraph().locate((Class<DomainResource>) resourceType))
					.consume(
							tree -> Assert
									.notNull(tree,
											String.format("Unable to locate %s for resource [%s]",
													DomainResourceGraph.class, resourceType.getName())))
					.then(DomainResourceGraph::getParent).get();
		}

		private Map<String, Class<?>> joinWithParentAttributeTypes(Map<String, Class<?>> typesMap) throws Exception {
			// @formatter:off
			return Utils
					.declare(locateParentGraph())
					.then(DomainResourceGraph::getResourceType)
					.then(metadatasMap::get)
					.then(metadata -> metadata == null ? Collections.<String, Class<?>>emptyMap():metadata.getAttributeTypes())
					.then(parentAttributeTypes -> declare(typesMap).consume(self -> self.putAll(parentAttributeTypes)).get())
					.get();
			// @formatter:on
		}

		private String[] joinWithParentAttributeNames(String[] declaredAttributes) throws Exception {
			// @formatter:off
			return Utils
					.declare(locateParentGraph())
					.then(DomainResourceGraph::getResourceType)
					.then(metadatasMap::get)
					.then(metadata -> metadata == null ? Collections.emptyList() : metadata.getAttributeNames() )
					.then(List::stream)
					.then(stream -> stream.toArray(String[]::new))
					.then(parentAttributes -> CollectionHelper.join(String.class, declaredAttributes, parentAttributes))
					.get();
			// @formatter:on
		}

		private String[] joinWithParentEnclosedAttributeNames(String[] declaredAttributes) throws Exception {
			// @formatter:off
			return Utils
					.declare(locateParentGraph())
					.then(DomainResourceGraph::getResourceType)
					.then(metadatasMap::get)
					.then(metadata -> metadata == null ? Collections.emptyList() : metadata.getEnclosedAttributeNames())
					.then(List::stream)
					.then(stream -> stream.toArray(String[]::new))
					.then(parentAttributes -> CollectionHelper.join(String.class, declaredAttributes, parentAttributes))
					.get();
			// @formatter:on
		}
	}

	@Override
	public String toString() {
		// @formatter:off
		return String.format("%s<%s>(\n"
				+ "\tattributeNames=[\n"
				+ "\t\t%s\n"
				+ "\t],\n"
				+ "\tenclosedAttributeNames=[\n"
				+ "\t\t%s\n"
				+ "\t],\n"
				+ "\tnonLazyAttributes=[\n"
				+ "\t\t%s\n"
				+ "\t],\n"
				+ "\tattributeTypes=[\n"
				+ "\t\t%s\n"
				+ "\t],\n"
				+ "\tassociationAttributes=[\n"
				+ "\t\t%s\n"
				+ "\t],\n"
				+ "\tcomponentPaths=[\n"
				+ "\t\t%s\n"
				+ "\t],\n"
				+ ")",
				this.getClass().getSimpleName(), resourceType.getName(),
				collectList(attributeNames, Function.identity()),
				collectList(enclosedAttributeNames, Function.identity()),
				collectList(nonLazyAttributeNames, Function.identity()),
				collectMap(attributeTypes, entry -> String.format("%s: %s", entry.getKey(), entry.getValue().getName()), "\n\t\t"),
				collectMap(associationAttributes, entry -> String.format("%s|%s", entry.getKey(), entry.getValue().getType()), "\n\t\t"),
				collectMap(componentPaths, entry -> String.format("%s: %s", entry.getKey(), entry.getValue()), "\n\t\t"));
		// @formatter:on
	}

	private <E> String collectList(List<E> list, Function<E, String> toString) {
		return list.size() == 0 ? "<<empty>>"
				: list.stream().map(toString)
						.collect(Collectors.joining(multicados.internal.helper.StringHelper.COMMON_JOINER));
	}

	private <K, V> String collectMap(Map<K, V> map, Function<Map.Entry<K, V>, String> toString, CharSequence joiner) {
		return map.size() == 0 ? "<<empty>>"
				: map.entrySet().stream().map(toString).collect(Collectors.joining(joiner));
	}

	private class ComponentPathImpl implements ComponentPath {

		private final Queue<String> path;

		public ComponentPathImpl() {
			path = new ArrayDeque<>();
		}

		public ComponentPathImpl(ComponentPathImpl parent) {
			this();
			path.addAll(parent.path);
		}

		@Override
		public void add(String component) {
			path.add(component);
		}

		@Override
		public Queue<String> getNodeNames() {
			return path;
		}

		@Override
		public String toString() {
			return path.stream().collect(Collectors.joining(StringHelper.DOT));
		}

	}

	protected interface DomainAssociation {

		String getName();

		multicados.internal.domain.metadata.AssociationType getType();

		boolean isComponent();

		public abstract class AbstractAssociation implements DomainAssociation {

			private final String name;
			private final multicados.internal.domain.metadata.AssociationType type;
			private final boolean isComponent;

			public AbstractAssociation(String name, multicados.internal.domain.metadata.AssociationType type,
					boolean isComponent) {
				this.name = Objects.requireNonNull(name);
				this.type = Objects.requireNonNull(type);
				this.isComponent = isComponent;
			}

			@Override
			public multicados.internal.domain.metadata.AssociationType getType() {
				return type;
			}

			@Override
			public String getName() {
				return name;
			}

			@Override
			public boolean isComponent() {
				return isComponent;
			}

			@Override
			public int hashCode() {
				return name.hashCode();
			}

			@Override
			public boolean equals(Object obj) {
				if (this == obj) {
					return true;
				}

				if (obj == null) {
					return false;
				}

				return name.equals(((DomainAssociation) obj).getName());
			}

		}

		public class OptionalAssociation extends AbstractAssociation {

			public OptionalAssociation(String name, multicados.internal.domain.metadata.AssociationType type,
					boolean isComponent) {
				super(name, type, isComponent);
			}

		}

		public class MandatoryAssociation extends AbstractAssociation {

			public MandatoryAssociation(String name, multicados.internal.domain.metadata.AssociationType type,
					boolean isComponent) {
				super(name, type, isComponent);
			}

		}

	}

}
