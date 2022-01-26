/**
 * 
 */
package multicados.internal.domain.metadata;

/**
 * @author Ngoc Huy
 *
 */
public interface Getter extends Access {

	Object get(Object source) throws Exception;
	
	Class<?> getReturnedType();

}
