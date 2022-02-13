/**
 * 
 */
package multicados.internal.domain.tuplizer;

/**
 * @author Ngoc Huy
 *
 */
public interface Getter extends Access {

	Object get(Object source) throws Exception;
	
	Class<?> getReturnedType();

}
