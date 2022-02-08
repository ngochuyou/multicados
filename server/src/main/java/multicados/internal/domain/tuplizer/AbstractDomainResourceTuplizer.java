/**
 * 
 */
package multicados.internal.domain.tuplizer;

import java.lang.reflect.InvocationTargetException;

import multicados.internal.domain.DomainResource;
import multicados.internal.domain.metadata.Getter;
import multicados.internal.domain.metadata.Setter;

/**
 * @author Ngoc Huy
 *
 */
public abstract class AbstractDomainResourceTuplizer<D extends DomainResource> implements DomainResourceTuplizer<D> {

	@Override
	public D instantiate(Object... args)
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		return null;
	}

	@Override
	public void setProperty(String propName, Object value)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

	}

	@Override
	public Object getProperty(String propName)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		return null;
	}

	protected abstract Getter getGetter(String propName);
	
	protected abstract Setter getSetter(String propName);
	
}
