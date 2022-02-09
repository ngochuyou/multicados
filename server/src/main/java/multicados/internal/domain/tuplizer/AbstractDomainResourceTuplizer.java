/**
 * 
 */
package multicados.internal.domain.tuplizer;

import java.lang.reflect.InvocationTargetException;

import multicados.internal.domain.DomainResource;
import multicados.internal.domain.metadata.Getter;
import multicados.internal.domain.metadata.Setter;
import multicados.internal.helper.TypeHelper;

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

}
