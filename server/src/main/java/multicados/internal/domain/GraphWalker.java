/**
 * 
 */
package multicados.internal.domain;

import multicados.internal.context.Loggable;

/**
 * @author Ngoc Huy
 *
 */
public interface GraphWalker<T> extends Loggable {

	<E extends T> GraphWalker<E> and(GraphWalker<E> next);
	
}
