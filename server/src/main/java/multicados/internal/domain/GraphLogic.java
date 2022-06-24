/**
 * 
 */
package multicados.internal.domain;

import multicados.internal.context.Loggable;

/**
 * @author Ngoc Huy
 *
 */
public interface GraphLogic<T> extends Loggable {

	<E extends T> GraphLogic<E> and(GraphLogic<E> next);

}
