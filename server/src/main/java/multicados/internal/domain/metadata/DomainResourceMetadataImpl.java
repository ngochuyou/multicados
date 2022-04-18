/**
 * 
 */
package multicados.internal.domain.metadata;

import static java.util.Collections.unmodifiableList;
import static multicados.internal.helper.Utils.declare;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.internal.util.StringHelper;
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
import org.springframework.util.Assert;

import multicados.domain.AbstractEntity;
import multicados.internal.domain.DomainComponentType;
import multicados.internal.domain.DomainResource;
import multicados.internal.domain.DomainResourceContext;
import multicados.internal.domain.DomainResourceGraph;
import multicados.internal.domain.Entity;
import multicados.internal.helper.CollectionHelper;
import multicados.internal.helper.HibernateHelper;
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

	private final Set<String> associationAttributeNames;

	public DomainResourceMetadataImpl(Class<T> resourceType, DomainResourceContext resourceContextProvider,
			Map<Class<? extends DomainResource>, DomainResourceMetadata<? extends DomainResource>> metadatasMap)
			throws Exception {
		this.resourceType = resourceType;

		Builder<T> builder = AbstractEntity.class.isAssignableFrom(resourceType)
				&& !Modifier.isAbstract(resourceType.getModifiers())
						? new HibernateResourceMetadataBuilder<>(resourceType)
						: new NonHibernateResourceMetadataBuilder<>(resourceType, resourceContextProvider,
								metadatasMap);

		attributeNames = unmodifiableList(builder.locateAttributeNames());
		enclosedAttributeNames = unmodifiableList(builder.locateEnclosedAttributeNames());
		nonLazyAttributeNames = unmodifiableList(builder.locateNonLazyAttributeNames());
		attributeTypes = Collections.unmodifiableMap(builder.locateAttributeTypes());
		associationAttributeNames = Collections.unmodifiableSet(builder.locateAssociationAttributeNames());
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
		return associationAttributeNames.contains(attributeName);
	}

	@Override
	public Map<String, Class<?>> getAttributeTypes() {
		return attributeTypes;
	}

	private interface Builder<D extends DomainResource> {

		List<String> locateAttributeNames() throws Exception;

		List<String> locateEnclosedAttributeNames() throws Exception;

		List<String> locateNonLazyAttributeNames() throws Exception;

		Map<String, Class<?>> locateAttributeTypes() throws Exception;

		Set<String> locateAssociationAttributeNames() throws Exception;

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private class HibernateResourceMetadataBuilder<D extends DomainResource> implements Builder<D> {

		private static final Logger logger = LoggerFactory
				.getLogger(DomainResourceMetadataImpl.HibernateResourceMetadataBuilder.class);

		private final Class<? extends AbstractEntity> entityType;
		private final EntityPersister persister;
		private final EntityMetamodel metamodel;

		private HibernateResourceMetadataBuilder(Class<D> resourceType) {
			logger.trace("Building {} for Hibernate entity of type [{}]", DomainResourceMetadata.class.getSimpleName(),
					resourceType.getName());
			entityType = (Class<? extends AbstractEntity>) resourceType;
			persister = HibernateHelper.getEntityPersister(entityType);
			metamodel = persister.getEntityMetamodel();
		}

		@Override
		public Set<String> locateAssociationAttributeNames() throws Exception {
			// @formatter:off
			return declare(getAttributeNames())
					.then(this::doLocateAssociations)
					.then(HashSet::new)
					.get();
			// @formatter:on
		}

		private List<String> doLocateAssociations(String[] attributeNames) throws Exception {
			Type[] types = persister.getPropertyTypes();
			List<String> associationAttributes = new ArrayList<>(16);
			int span = attributeNames.length;

			for (int i = 0; i < span; i++) {
				// @formatter:off
				declare(attributeNames[i])
						.second(types[metamodel.getPropertyIndex(attributeNames[i])])
					.then(this::individuallyLocateAssociations)
					.identical(associationAttributes::addAll);
				// @formatter:on
			}

			return associationAttributes;
		}

		private List<String> individuallyLocateAssociations(String attributeName, Type type) throws Exception {
			List<String> associationAttributes = new ArrayList<>(8);

			if (type instanceof AssociationType) {
				associationAttributes.add(attributeName);
				return associationAttributes;
			}

			if (type instanceof ComponentType) {
				ComponentType componentType = (ComponentType) type;
				String[] subAttributes = componentType.getPropertyNames();
				Type[] subtypes = componentType.getSubtypes();
				int span = subAttributes.length;

				for (int i = 0; i < span; i++) {
					// @formatter:off
					declare(subAttributes[i])
							.second(subtypes[componentType.getPropertyIndex(subAttributes[i])])
						.then(this::individuallyLocateAssociations)
						.identical(associationAttributes::addAll);
					// @formatter:on
				}

				return associationAttributes;
			}

			return Collections.emptyList();
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
				String collectionRole = StringHelper.qualify(owningType.getName(), name);

				return persister.getFactory().getMetamodel().collectionPersister(collectionRole).isLazy();
			}

			return propertyLaziness[index];
		}

		@Override
		public List<String> locateEnclosedAttributeNames() throws Exception {
			// @formatter:off
			return declare(getAttributeNames())
					.then(this::addIdentifierAttributeNames)
					.then(Arrays::asList)
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
					.identical(results::putAll);
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
					.identical(types::putAll);
				// @formatter:on
			}

			return types;
		}

		private String[] getAttributeNames() {
			return metamodel.getPropertyNames();
		}

		private String[] unwrapComponentsAttributes(String[] attributeNames) {
			List<String> wrappedAttributes = new ArrayList<>(0);
			Type attributeType;

			for (String attributeName : attributeNames) {
				attributeType = resolveAttributeType(attributeName);

				if (!ComponentType.class.isAssignableFrom(attributeType.getClass())) {
					continue;
				}

				ComponentType componentType = (ComponentType) attributeType;

				for (String componentAttributeName : componentType.getPropertyNames()) {
					wrappedAttributes.add(componentAttributeName);
				}
			}

			return CollectionHelper.join(String.class, attributeNames, wrappedAttributes.toArray(String[]::new));
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
				return StringHelper.EMPTY_STRINGS;
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
		public Set<String> locateAssociationAttributeNames() throws Exception {
			// @formatter:off
			return declare(getDeclaredAttributeNames())
					.then(this::joinWithParentEnclosedAttributeNames)
					.then(this::doLocateAssocations)
					.then(HashSet::new)
					.get();
			// @formatter:on
		}

		private List<String> doLocateAssocations(String[] attributeNames) throws Exception {
			int span = attributeNames.length;
			List<String> associations = new ArrayList<>();

			for (int i = 0; i < span; i++) {
				// @formatter:off
				declare(attributeNames[i])
						.second(attributeTypes.get(attributeNames[i]))
					.then(this::individuallyLocateAssocations)
					.identical(associations::addAll);
				// @formatter:on
			}

			return associations;
		}

		private List<String> individuallyLocateAssocations(String attributeName, Class<?> owningType) throws Exception {
			List<String> associations = new ArrayList<>();

			if (DomainResource.class.isAssignableFrom(owningType)) {
				associations.add(attributeName);
				return associations;
			}

			if (DomainComponentType.class.isAssignableFrom(owningType)) {
				Field[] subFields = Stream.of(owningType.getDeclaredFields())
						.filter(field -> !Modifier.isStatic(field.getModifiers())).toArray(Field[]::new);
				String[] subAttributes = Stream.of(subFields).map(Field::getName).toArray(String[]::new);
				Class<?>[] subTypes = Stream.of(subFields).map(Field::getType).toArray(Class[]::new);
				int span = subAttributes.length;

				for (int i = 0; i < span; i++) {
					// @formatter:off
					declare(subAttributes[i])
							.second(subTypes[i])
						.then(this::individuallyLocateAssocations)
						.identical(associations::addAll);
					// @formatter:on
				}

				return associations;
			}

			return Collections.emptyList();
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
					.identical(
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
					.then(metadata -> {
						if (metadata == null) {
							// happens when the current node is the root
							return Collections.<String, Class<?>>emptyMap();
						}
						
						return metadata.getAttributeTypes();
					})
					.then(parentAttributeTypes -> {
						typesMap.putAll(parentAttributeTypes);
						return typesMap;
					})
					.get();
			// @formatter:on
		}

		private String[] joinWithParentAttributeNames(String[] declaredAttributes) throws Exception {
			// @formatter:off
			return Utils
					.declare(locateParentGraph())
					.then(DomainResourceGraph::getResourceType)
					.then(metadatasMap::get)
					.then(metadata -> {
						if (metadata == null) {
							// happens when the current node is the root
							return Collections.emptyList();
						}
						
						return metadata.getAttributeNames();
					})
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
					.then(metadata -> {
						if (metadata == null) {
							// happens when the current node is the root
							return Collections.emptyList();
						}
						
						return metadata.getEnclosedAttributeNames();
					})
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
				+ "\tassociationAttributeNames=[\n"
				+ "\t\t%s\n"
				+ "\t],\n"
				+ ")",
				this.getClass().getSimpleName(), resourceType.getName(),
				collectList(attributeNames, Function.identity()),
				collectList(enclosedAttributeNames, Function.identity()),
				collectList(nonLazyAttributeNames, Function.identity()),
				collectMap(attributeTypes, entry -> String.format("%s: %s", entry.getKey(), entry.getValue().getName()), "\n\t\t"),
				collectList(associationAttributeNames.stream().collect(Collectors.toList()), Function.identity()));
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

}
