/**
 * 
 */
package multicados.internal.domain.tuplizer;

/**
 * @author Ngoc Huy
 *
 */
public interface Setter extends Access {

	void set(Object source, Object val) throws Exception;
	
}
