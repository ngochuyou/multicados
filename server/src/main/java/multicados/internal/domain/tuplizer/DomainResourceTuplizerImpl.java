/**
 *
 */
package multicados.internal.domain.tuplizer;

import java.lang.reflect.Member;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.function.BiFunction;
import java.util.function.Function;

import multicados.internal.domain.DomainResource;
import multicados.internal.domain.metadata.ComponentPath;
import multicados.internal.domain.metadata.DomainResourceMetadata;
import multicados.internal.domain.tuplizer.AccessorFactory.Accessor;
import multicados.internal.helper.TypeHelper;
import multicados.internal.helper.Utils;
import multicados.internal.helper.Utils.HandledSupplier;
import multicados.internal.helper.Utils.TriConsummer;

/**
 * @author Ngoc Huy
 *
 */
public class DomainResourceTuplizerImpl<D extends DomainResource> extends AbstractDomainResourceTuplizer<D> {

	private final Map<String, Accessor> accessors;

	public DomainResourceTuplizerImpl(
	// @formatter:off
			DomainResourceMetadata<D> metadata,
			BiFunction<Class<?>, String, Accessor> cachedAccessorProvider,
			Utils.TriConsummer<Class<?>, String, Accessor> accessorEntryConsumer,
			Function<Class<? extends DomainResource>, AbstractDomainResourceTuplizer<?>> parentTuplizerProvider) throws Exception {
		// @formatter:on
		super(metadata.getResourceType());
		accessors = Collections.unmodifiableMap(
				resolveAccessors(metadata, cachedAccessorProvider, accessorEntryConsumer, parentTuplizerProvider));
	}

	@Override
	protected Map<String, Accessor> getAccessors() {
		return accessors;
	}

	private Map<String, Accessor> resolveAccessors(
	// @formatter:off
			DomainResourceMetadata<D> metadata,
			BiFunction<Class<?>, String, Accessor> cachedAccessorProvider,
			TriConsummer<Class<?>, String, Accessor> accessorEntryConsumer,
			Function<Class<? extends DomainResource>, AbstractDomainResourceTuplizer<?>> parentTuplizerProvider) throws Exception {
		// @formatter:on
		final Map<String, Accessor> accessors = new HashMap<>();
		final Class<D> resourceType = metadata.getResourceType();
		final AbstractDomainResourceTuplizer<?> parentTuplizer = parentTuplizerProvider.apply(resourceType);

		if (parentTuplizer != null) {
			accessors.putAll(parentTuplizer.getAccessors());
		}

		for (final String attributeName : metadata.getDeclaredAttributeNames()) {
			// @formatter:off
			final Accessor accessor = locateOnGoingAccessor(
					resourceType,
					attributeName,
					() -> AccessorFactory.standard(resourceType, attributeName),
					cachedAccessorProvider,
					accessorEntryConsumer);
			// @formatter:on
			accessors.put(attributeName, accessor);
		}

		if (metadata.getComponentPaths().isEmpty()) {
			return returnAccessorsOrEmpty(accessors);
		}

		for (final Entry<String, ComponentPath> componentEntry : metadata.getComponentPaths().entrySet()) {
			final String componentAttributeName = componentEntry.getKey();

			if (accessors.containsKey(componentAttributeName)) {
				continue;
			}

			final ComponentPath componentPath = componentEntry.getValue();
			final Queue<String> pathQueue = new ArrayDeque<>(componentPath.getPath());

			String currentOwnerName = pathQueue.poll();

			while (!pathQueue.isEmpty()) {
				final Class<?> currentOwnerType = metadata.getAttributeType(currentOwnerName);
				final Accessor currentOwnerAccessor = accessors.get(currentOwnerName);
				final String currentMemberName = pathQueue.poll();
				final Accessor memberAccessor = AccessorFactory.standard(currentOwnerType, currentMemberName);
				final Accessor componentAccessor = locateOnGoingAccessor(resourceType, componentAttributeName,
						() -> AccessorFactory.delegate(
								new ComponentGetter(currentOwnerAccessor.getGetter(), memberAccessor.getGetter()),
								new ComponentSetter(currentOwnerAccessor,
										() -> TypeHelper.locateNoArgsConstructor(currentOwnerType),
										memberAccessor.getSetter())),
						cachedAccessorProvider, accessorEntryConsumer);

				accessors.put(currentMemberName, componentAccessor);

				currentOwnerName = currentMemberName;
			}
		}

		return accessors;
	}

	private Map<String, Accessor> returnAccessorsOrEmpty(Map<String, Accessor> accessors) {
		return accessors.isEmpty() ? Collections.emptyMap() : accessors;
	}

	@Override
	protected Getter getGetter(String propName) {
		return accessors.get(propName).getGetter();
	}

	@Override
	protected Setter getSetter(String propName) {
		return accessors.get(propName).getSetter();
	}

	private static class ComponentSetter implements Setter {

		private final Accessor ownerAccessor;
		private final HandledSupplier<Object, Exception> ownerSupplier;
		private final Setter memberSetter;

		private static final Member MEMBER;

		static {
			try {
				MEMBER = ComponentSetter.class.getDeclaredMethod("set", Object.class, Object.class);
			} catch (NoSuchMethodException | SecurityException any) {
				throw new IllegalArgumentException(any);
			}
		}

		public ComponentSetter(Accessor ownerAccessor, HandledSupplier<Object, Exception> ownerSupplier,
				Setter memberSetter) {
			this.ownerAccessor = ownerAccessor;
			this.ownerSupplier = ownerSupplier;
			this.memberSetter = memberSetter;
		}

		@Override
		public Member getMember() {
			return MEMBER;
		}

		@Override
		public void set(Object source, Object val) throws Exception {
			Object owner = ownerAccessor.get(source);

			if (owner == null) {
				owner = ownerSupplier.get();
				ownerAccessor.set(source, owner);
			}

			memberSetter.set(owner, val);
		}

	}

	private static class ComponentGetter implements Getter {

		private final Getter ownerGetter;
		private final Getter memberGetter;

		private static final Member MEMBER;

		static {
			try {
				MEMBER = ComponentGetter.class.getDeclaredMethod("get", Object.class);
			} catch (NoSuchMethodException | SecurityException any) {
				throw new IllegalArgumentException(any);
			}
		}

		public ComponentGetter(Getter ownerGetter, Getter memberGetter) {
			this.ownerGetter = ownerGetter;
			this.memberGetter = memberGetter;
		}

		@Override
		public Member getMember() {
			return MEMBER;
		}

		@Override
		public Object get(Object source) throws Exception {
			final Object owner = ownerGetter.get(source);

			if (owner == null) {
				return null;
			}

			return memberGetter.get(owner);
		}

		@Override
		public Class<?> getReturnedType() {
			return memberGetter.getReturnedType();
		}

	}

}
