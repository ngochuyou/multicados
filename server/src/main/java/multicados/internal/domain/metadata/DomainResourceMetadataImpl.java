/**
 * 
 */
package multicados.internal.domain.metadata;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.tuple.IdentifierProperty;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.hibernate.type.ComponentType;
import org.hibernate.type.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import multicados.domain.AbstractEntity;
import multicados.internal.domain.DomainResource;
import multicados.internal.domain.DomainResourceContext;
import multicados.internal.domain.DomainResourceGraph;
import multicados.internal.helper.CollectionHelper;
import multicados.internal.helper.HibernateHelper;
import multicados.internal.helper.Utils;

/**
 * @author Ngoc Huy
 *
 */
public class DomainResourceMetadataImpl<T extends DomainResource> implements DomainResourceMetadata<T> {

	private final Class<T> resourceType;

	private final List<String> attributeNames;

	public DomainResourceMetadataImpl(Class<T> resourceType, DomainResourceContext resourceContextProvider)
			throws Exception {
		this.resourceType = resourceType;

		Builder<T> builder = AbstractEntity.class.isAssignableFrom(resourceType)
				&& !Modifier.isAbstract(resourceType.getModifiers())
						? new HibernateResourceMetadataBuilder<>(resourceType)
						: new NonHibernateResourceMetadataBuilder<>(resourceType, resourceContextProvider);

		attributeNames = builder.locateAttributeNames();
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
	public Class<?> getAttributeType(String attributeName) {
		return null;
	}

	@Override
	public List<String> getNonLazyAttributeNames() {
		return null;
	}

	@Override
	public boolean isAssociation(String attributeName) {
		return false;
	}

	private interface Builder<D extends DomainResource> {

		List<String> locateAttributeNames() throws Exception;

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
		public List<String> locateAttributeNames() throws Exception {
			// @formatter:off
			return Utils.declare(getAttributeNames())
					.then(this::unwrapComponentsAttributes)
					.then(this::addIdentifierAttributeNames)
					.then(Arrays::asList)
					.get();
			// @formatter:on
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

		public NonHibernateResourceMetadataBuilder(Class<D> resourceType,
				DomainResourceContext resourceContextProvider) {
			logger.trace("Building {} for resource of type [{}]", DomainResourceMetadata.class.getSimpleName(),
					resourceType.getName());
			this.resourceType = resourceType;
			this.resourceContextProvider = resourceContextProvider;
		}

		@Override
		public List<String> locateAttributeNames() throws Exception {
			// @formatter:off
			return Utils.declare(resourceType)
					.then(this::getDeclaredAttributeNames)
					.then(this::joinWithParentAttributeNames)
					.then(Arrays::asList)
					.get();
			// @formatter:on
		}

		private String[] getDeclaredAttributeNames(Class<? extends DomainResource> resourceType) {
			// @formatter:off
			return Stream.of(resourceType.getDeclaredFields())
					.filter(field -> !Modifier.isStatic(field.getModifiers()))
					.map(Field::getName)
					.toArray(String[]::new);
			// @formatter:on
		}

		@SuppressWarnings("unchecked")
		private String[] joinWithParentAttributeNames(String[] declaredAttributes) throws Exception {
			// @formatter:off
			return Utils
					.declare(resourceContextProvider.getResourceGraph().locate((Class<DomainResource>) resourceType))
					.identical(tree -> Assert.notNull(tree, String.format("Unable to locate %s for resource [%s]", DomainResourceGraph.class, resourceType.getName())))
					.then(DomainResourceGraph::getParent)
					.then(DomainResourceGraph::getResourceType)
					.then(this::getDeclaredAttributeNames)
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
				+ ")",
				this.getClass().getSimpleName(), resourceType.getName(),
				attributeNames.size() == 0 ? "<<empty>>" : attributeNames.stream().collect(Collectors.joining("\n\t\t")));
		// @formatter:on
	}

}
