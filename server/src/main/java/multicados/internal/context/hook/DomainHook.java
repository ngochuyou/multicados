/**
 * 
 */
package multicados.internal.context.hook;

import org.springframework.context.ApplicationContext;

/**
 * Marker objects for domain scoped hooks, executed after every other domain
 * logics have been executed
 * 
 * @author Ngoc Huy
 *
 */
public interface DomainHook {

	void hook(ApplicationContext context) throws Exception;

}
