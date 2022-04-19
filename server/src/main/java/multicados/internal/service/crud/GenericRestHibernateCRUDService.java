/**
 * 
 */
package multicados.internal.service.crud;

import org.hibernate.Session;

import multicados.internal.context.ContextBuilder;

/**
 * @author Ngoc Huy
 *
 */
public interface GenericRestHibernateCRUDService<TUPLE>
		extends GenericRestCRUDService<TUPLE, Session>, ContextBuilder {

}
