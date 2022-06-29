/**
 *
 */
package multicados.internal.domain.tuplizer;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.function.BiFunction;

import multicados.internal.domain.DomainResource;
import multicados.internal.domain.tuplizer.AccessorFactory.Accessor;
import multicados.internal.helper.TypeHelper;
import multicados.internal.helper.Utils.HandledSupplier;
import multicados.internal.helper.Utils.TriConsummer;

/**
 * @author Ngoc Huy
 *
 */
public abstract class AbstractDomainResourceTuplizer<D extends DomainResource> implements DomainResourceTuplizer<D> {

	private final Class<D> resourceType;

	public AbstractDomainResourceTuplizer(Class<D> resourceType) {
		this.resourceType = resourceType;
	}

	@Override
	public D instantiate(Object... args) throws TuplizerException {
		try {
			return TypeHelper.constructFromNonArgs(resourceType);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			throw new TuplizerException(e);
		}
	}

	@Override
	public void setProperty(Object source, String propName, Object value) throws TuplizerException {
		try {
			getSetter(propName).set(source, value);
		} catch (Exception e) {
			throw new TuplizerException(e);
		}
	}

	@Override
	public Object getProperty(Object source, String propName) throws TuplizerException {
		try {
			return getGetter(propName).get(source);
		} catch (Exception e) {
			throw new TuplizerException(e);
		}
	}

	protected abstract Getter getGetter(String propName);

	protected abstract Setter getSetter(String propName);

	protected abstract Map<String, Accessor> getAccessors();

	@Override
	public Class<D> getResourceType() {
		return resourceType;
	}

	protected Accessor locateOnGoingAccessor(
	// @formatter:off
			Class<?> ownerType,
			String attributeName,
			HandledSupplier<Accessor, Exception> accessorSupplier,
			BiFunction<Class<?>, String, Accessor> cachedAccessorProvider,
			TriConsummer<Class<?>, String, Accessor> accessorEntryConsumer) throws Exception {
		// @formatter:on
		final Accessor accessor = cachedAccessorProvider.apply(ownerType, attributeName);

		if (accessor != null) {
			return accessor;
		}

		final Accessor newCachedAccessorEntry = accessorSupplier.get();

		accessorEntryConsumer.accept(ownerType, attributeName, newCachedAccessorEntry);

		return newCachedAccessorEntry;
	}

}
