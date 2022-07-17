/**
 *
 */
package multicados.internal.domain.tuplizer;

import static multicados.internal.helper.Utils.declare;

import java.io.Serializable;
import java.lang.reflect.Member;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.UniqueKeyLoadable;
import org.hibernate.tuple.entity.EntityTuplizer;
import org.hibernate.type.ComponentType;
import org.hibernate.type.Type;
import org.springframework.util.Assert;

import multicados.internal.domain.Entity;
import multicados.internal.domain.metadata.ComponentPath;
import multicados.internal.domain.metadata.DomainResourceAttributesMetadata;
import multicados.internal.domain.metadata.DomainResourceMetadata;
import multicados.internal.domain.tuplizer.AccessorFactory.Accessor;
import multicados.internal.helper.CollectionHelper;
import multicados.internal.helper.Utils;
import multicados.internal.helper.Utils.TriConsummer;

/**
 * @author Ngoc Huy
 *
 */
public class HibernateResourceTuplizer<I extends Serializable, E extends Entity<I>>
		extends AbstractDomainResourceTuplizer<E> {

	private static final String NULL_ACCESSOR_TEMPLATE = String.format("Unable to locate %s with property name [%s]",
			Accessor.class.getSimpleName(), "%s");

	private final Map<String, Accessor> accessors;

	@SuppressWarnings("unchecked")
	public HibernateResourceTuplizer(
	// @formatter:off
			DomainResourceMetadata<E> metadata,
			SessionFactoryImplementor sfi,
			BiFunction<Class<?>, String, Accessor> cachedAccessorProvider,
			Utils.TriConsummer<Class<?>, String, Accessor> accessorEntryConsumer)
			throws Exception {
		super(metadata.getResourceType());
		this.accessors = declare(resolveRootAccessors(sfi.getMetamodel().entityPersister(metadata.getResourceType()),
				metadata.unwrap(DomainResourceAttributesMetadata.class).getComponentPaths(), cachedAccessorProvider,
				accessorEntryConsumer))
			.<Map<String, Accessor>>then(Collections::unmodifiableMap)
			.get();
		// @formatter:on
	}

	@Override
	protected Map<String, Accessor> getAccessors() {
		return accessors;
	}

	private Map<String, Accessor> resolveRootAccessors(
	// @formatter:off
			EntityPersister persister,
			Map<String, ComponentPath> componentPaths,
			BiFunction<Class<?>, String, Accessor> cachedAccessorProvider,
			TriConsummer<Class<?>, String, Accessor> accessorEntryConsumer) throws Exception {
		// @formatter:on
		final Map<String, Accessor> accessors = new HashMap<>();

		declare(resolveIdentifierAccessors(persister, componentPaths, cachedAccessorProvider, accessorEntryConsumer))
				.consume(accessors::putAll);

		for (final String attributeName : persister.getPropertyNames()) {
			// @formatter:off
			final Accessor ownerAccessor = locateOnGoingAccessor(
					attributeName,
					() -> buildBasicTypeAccessor(attributeName, persister),
					cachedAccessorProvider,
					accessorEntryConsumer);
			// @formatter:on
			accessors.put(attributeName, ownerAccessor);

			final Type attributeType = persister.getPropertyType(attributeName);

			if (!attributeType.isComponentType()) {
				continue;
			}

			final ComponentType componentType = (ComponentType) attributeType;

			// @formatter:off
			accessors.putAll(
					resolveComponentAccessors(
							ownerAccessor,
							componentType,
							componentPaths,
							cachedAccessorProvider,
							accessorEntryConsumer));
			// @formatter:on
		}

		return CollectionHelper.getOrEmpty(accessors);
	}

	private Map<String, Accessor> resolveIdentifierAccessors(
	// @formatter:off
			EntityPersister persister,
			Map<String, ComponentPath> componentPaths,
			BiFunction<Class<?>, String, Accessor> cachedAccessorProvider,
			TriConsummer<Class<?>, String, Accessor> accessorEntryConsumer) throws Exception {
		// @formatter:on
		final HashMap<String, Accessor> accessors = new HashMap<>();
		final String identifierPropertyName = persister.getIdentifierPropertyName();
		// @formatter:off
		final Accessor identifierAccessor = locateOnGoingAccessor(
				persister.getIdentifierPropertyName(),
				() -> buildBasicTypeAccessor(identifierPropertyName, persister),
				cachedAccessorProvider,
				accessorEntryConsumer);
		// @formatter:on
		if (!persister.getIdentifierType().isComponentType()) {
			return accessors;
		}

		final ComponentType identifierType = (ComponentType) persister.getIdentifierType();
		// @formatter:off
		accessors.putAll(resolveComponentAccessors(
				identifierAccessor,
				identifierType,
				componentPaths,
				cachedAccessorProvider,
				accessorEntryConsumer));
		// @formatter:on
		return accessors;
	}

	private Map<String, Accessor> resolveComponentAccessors(
	// @formatter:off
			Accessor ownerAccessor,
			ComponentType type,
			Map<String, ComponentPath> componentPaths,
			BiFunction<Class<?>, String, Accessor> cachedAccessorProvider,
			TriConsummer<Class<?>, String, Accessor> accessorEntryConsumer)
			throws Exception {
		final Map<String, Accessor> accessors = new HashMap<>();

		for (final String componentAttributeName : type.getPropertyNames()) {
			final Type componentAttributeType = type.getSubtypes()[type.getPropertyIndex(componentAttributeName)];
			// @formatter:off
			final Accessor nextOwnerAccessor = locateOnGoingAccessor(
					componentPaths.get(componentAttributeName).toString(),
					() -> AccessorFactory.delegate(new ComponentGetter(ownerAccessor.getGetter(), type, componentAttributeName), new ComponentSetter(ownerAccessor, type, componentAttributeName)),
					cachedAccessorProvider,
					accessorEntryConsumer);

			if (!componentAttributeType.isComponentType()) {
				continue;
			}

			accessors.putAll(resolveComponentAccessors(
					nextOwnerAccessor,
					(ComponentType) componentAttributeType,
					componentPaths,
					cachedAccessorProvider,
					accessorEntryConsumer));
			// @formatter:on
		}

		return accessors;
	}

	private Accessor buildBasicTypeAccessor(String attributeName, EntityPersister persister) {
		final EntityTuplizer entityTuplizer = persister.getEntityTuplizer();
		final UniqueKeyLoadable loadable = (UniqueKeyLoadable) persister;

		return AccessorFactory.delegate(new HibernateGetter(entityTuplizer, loadable, attributeName),
				new HibernateSetter(entityTuplizer, loadable, attributeName));
	}

	@Override
	protected Getter getGetter(String propName) {
		final Accessor accessor = accessors.get(propName);

		Assert.notNull(accessor, String.format(NULL_ACCESSOR_TEMPLATE, propName));

		return accessor.getGetter();
	}

	@Override
	protected Setter getSetter(String propName) {
		final Accessor accessor = accessors.get(propName);

		Assert.notNull(accessor, String.format(NULL_ACCESSOR_TEMPLATE, propName));

		return accessor.getSetter();
	}

	private class HibernateGetter implements Getter {

		private final org.hibernate.property.access.spi.Getter getter;

		private HibernateGetter(EntityTuplizer hbmTuplizer, UniqueKeyLoadable loadable, String attributeName) {
			// @formatter:off
			getter = loadable.getIdentifierPropertyName().equals(attributeName) ?
					hbmTuplizer.getIdentifierGetter() :
						hbmTuplizer.getGetter(loadable.getPropertyIndex(attributeName));
			// @formatter:on
		}

		private HibernateGetter(org.hibernate.property.access.spi.Getter getter) {
			this.getter = getter;
		}

		@Override
		public Member getMember() {
			return getter.getMember();
		}

		@Override
		public Object get(Object source) throws Exception {
			return getter.get(source);
		}

		@Override
		public Class<?> getReturnedType() {
			return getter.getReturnType();
		}

	}

	private class HibernateSetter implements Setter {

		private final BiConsumer<Object, Object> setter;

		public HibernateSetter(EntityTuplizer tuplizer, UniqueKeyLoadable loadable, String attributeName) {
			// @formatter:off
			setter = loadable.getIdentifierPropertyName().equals(attributeName) ?
					(source, val) -> tuplizer.setIdentifier(source, (Serializable) val, null) :
						(source, val) -> tuplizer.setPropertyValue(source, attributeName, val);
			// @formatter:on
		}

		@Override
		public Member getMember() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void set(Object source, Object val) throws Exception {
			setter.accept(source, val);
		}

	}

	private class ComponentGetter implements Getter {

		private final Getter ownerGetter;
		private final ComponentType ownerType;
		private final int index;

		private final Member member;

		private ComponentGetter(Getter ownerGetter, ComponentType ownerType, String attributeName)
				throws NoSuchMethodException, SecurityException {
			this.ownerGetter = ownerGetter;
			this.ownerType = ownerType;
			this.index = ownerType.getPropertyIndex(attributeName);
			member = this.getClass().getDeclaredMethod("get", Object.class);
		}

		@Override
		public Member getMember() {
			return member;
		}

		@Override
		public Object get(Object source) throws Exception {
			return ownerType.getPropertyValue(ownerGetter.get(source), index);
		}

		@Override
		public Class<?> getReturnedType() {
			return ownerType.getSubtypes()[index].getReturnedClass();
		}

	}

	private class ComponentSetter implements Setter {

		private final Accessor ownerAccessor;
		private final ComponentType ownerType;
		private final Setter setter;

		private final Member member;

		public ComponentSetter(Accessor ownerAccessor, ComponentType type, String attributeName)
				throws NoSuchFieldException, SecurityException, NoSuchMethodException {
			this.ownerType = type;
			this.ownerAccessor = ownerAccessor;
			setter = AccessorFactory.standard(type.getReturnedClass(), attributeName).getSetter();
			member = this.getClass().getDeclaredMethod("set", Object.class, Object.class);
		}

		@Override
		public Member getMember() {
			return member;
		}

		@Override
		public void set(Object source, Object val) throws Exception {
			Object owner = ownerAccessor.get(source);

			if (owner == null) {
				owner = ownerType.instantiate(null);
				ownerAccessor.set(source, owner);
			}

			setter.set(owner, val);
		}

	}

}
