/**
 * 
 */
package multicados.internal.domain.tuplizer;

import java.lang.reflect.InvocationTargetException;

import multicados.internal.context.Loggable;
import multicados.internal.domain.DomainResource;

/**
 * @author Ngoc Huy
 *
 */
public interface DomainResourceTuplizer<T extends DomainResource> extends Loggable {

	T instantiate(Object... args)
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException;

	void setProperty(String propName, Object value)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException;

	Object getProperty(String propName)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException;

}
