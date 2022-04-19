/**
 * 
 */
package multicados.internal.domain.tuplizer;

import java.io.Serializable;
import java.lang.reflect.Member;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.UniqueKeyLoadable;
import org.hibernate.tuple.entity.EntityTuplizer;
import org.hibernate.type.ComponentType;
import org.springframework.util.Assert;

import multicados.internal.domain.Entity;
import multicados.internal.domain.metadata.DomainResourceMetadata;
import multicados.internal.domain.tuplizer.AccessorFactory.Accessor;
import multicados.internal.helper.Utils;

/**
 * @author Ngoc Huy
 *
 */
public class DomainEntityTuplizer<I extends Serializable, E extends Entity<I>>
		extends AbstractDomainResourceTuplizer<E> {

	private static final String NULL_ACCESSOR_TEMPLATE = String.format("Unable to locate %s with property name [%s]",
			Accessor.class.getSimpleName(), "%s");

	private final Map<String, Accessor> accessors;

	public DomainEntityTuplizer(Class<E> resourceType, DomainResourceMetadata<E> metadata,
			SessionFactoryImplementor sfi) throws Exception {
		super(resourceType);
		// @formatter:off
		this.accessors = Utils.declare(metadata.getEnclosedAttributeNames())
					.second(sfi)
					.third(resourceType)
					.triInverse()
				.then(this::mapToAccessors)
				.then(Collections::unmodifiableMap)
				.get();
		// @formatter:on
	}

	private Map<String, Accessor> mapToAccessors(Class<E> resourceType, SessionFactoryImplementor sfi,
			Collection<String> propertyNames) throws NoSuchFieldException, SecurityException {
		EntityPersister persister = sfi.getMetamodel().entityPersister(resourceType);
		Map<String, Accessor> accessorsMap = new HashMap<>();

		for (String attribute : propertyNames) {
			Accessor ownerAccessor = buildBasicTypeAccessor(resourceType, attribute, persister);

			accessorsMap.put(attribute, ownerAccessor);

			if (!persister.getPropertyType(attribute).isComponentType()) {
				continue;
			}

			ComponentType componentType = (ComponentType) persister.getPropertyType(attribute);

			for (String subAttribute : componentType.getPropertyNames()) {
				accessorsMap.put(subAttribute,
						AccessorFactory.delegate(
								new ComponentGetter(ownerAccessor.getGetter(), componentType, subAttribute),
								new ComponentSetter(ownerAccessor.getGetter(), ownerAccessor.getSetter(), componentType,
										subAttribute)));
			}
		}

		return accessorsMap;
	}

	private Accessor buildBasicTypeAccessor(Class<E> resourceType, String attributeName, EntityPersister persister) {
		EntityTuplizer entityTuplizer = persister.getEntityTuplizer();
		UniqueKeyLoadable loadable = (UniqueKeyLoadable) persister;

		return AccessorFactory.delegate(new HibernateGetter(entityTuplizer, loadable, attributeName),
				new HibernateSetter(entityTuplizer, loadable, attributeName));
	}

	@Override
	protected Getter getGetter(String propName) {
		Accessor accessor = accessors.get(propName);

		Assert.notNull(accessor, String.format(NULL_ACCESSOR_TEMPLATE, propName));

		return accessor.getGetter();
	}

	@Override
	protected Setter getSetter(String propName) {
		Accessor accessor = accessors.get(propName);

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

		private final EntityTuplizer tuplizer;
		private final BiConsumer<Object, Object> setter;

		public HibernateSetter(EntityTuplizer hbmTuplizer, UniqueKeyLoadable loadable, String attributeName) {
			tuplizer = hbmTuplizer;
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
		private final ComponentType type;
		private final int index;

		private ComponentGetter(Getter ownerGetter, ComponentType type, String attributeName) {
			Assert.notNull(ownerGetter, String.format("%s was null", Getter.class.getName()));
			Assert.notNull(type, String.format("%s was null", ComponentType.class.getName()));
			Assert.notNull(attributeName, "attributeName was null");
			this.ownerGetter = ownerGetter;
			this.type = type;
			this.index = type.getPropertyIndex(attributeName);
		}

		@Override
		public Member getMember() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object get(Object source) throws Exception {
			return type.getPropertyValue(ownerGetter.get(source), index);
		}

		@Override
		public Class<?> getReturnedType() {
			return type.getSubtypes()[index].getReturnedClass();
		}

	}

	private class ComponentSetter implements Setter {

		private final Getter ownerGetter;
		private final Setter ownerSetter;
		private final ComponentType type;
		private final Setter setter;

		public ComponentSetter(Getter ownerGetter, Setter ownerSetter, ComponentType type, String attributeName)
				throws NoSuchFieldException, SecurityException {
			Assert.notNull(ownerGetter, String.format("%s was null", Getter.class.getName()));
			Assert.notNull(ownerSetter, String.format("%s was null", Setter.class.getName()));
			Assert.notNull(type, String.format("%s was null", ComponentType.class.getName()));
			Assert.notNull(type, "attributeName was null");
			this.ownerGetter = ownerGetter;
			this.ownerSetter = ownerSetter;
			this.type = type;
			setter = AccessorFactory.standard(type.getReturnedClass(), attributeName).getSetter();
		}

		@Override
		public Member getMember() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void set(Object source, Object val) throws Exception {
			Object owner = ownerGetter.get(source);

			if (owner == null) {
				owner = type.instantiate(null);
				ownerSetter.set(source, owner);
			}

			setter.set(owner, val);
		}

	}

}
