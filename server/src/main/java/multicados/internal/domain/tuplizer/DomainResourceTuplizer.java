/**
 * 
 */
package multicados.internal.domain.tuplizer;

import multicados.internal.context.Loggable;
import multicados.internal.domain.DomainResource;

/**
 * @author Ngoc Huy
 *
 */
public interface DomainResourceTuplizer<T extends DomainResource> extends Loggable {

	Class<T> getResourceType();
	
	T instantiate(Object... args) throws TuplizerException;

	void setProperty(Object source, String propName, Object value) throws TuplizerException;

	Object getProperty(Object source, String propName) throws TuplizerException;

}
