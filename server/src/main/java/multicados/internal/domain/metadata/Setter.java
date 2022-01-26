/**
 * 
 */
package multicados.internal.domain.metadata;

/**
 * @author Ngoc Huy
 *
 */
public interface Setter extends Access {

	void set(Object source, Object val) throws Exception;
	
}
