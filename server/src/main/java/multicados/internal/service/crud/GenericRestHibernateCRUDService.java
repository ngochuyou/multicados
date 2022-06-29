/**
 *
 */
package multicados.internal.service.crud;

import org.hibernate.Session;

/**
 * @author Ngoc Huy
 *
 */
public interface GenericRestHibernateCRUDService<TUPLE> extends GenericRestCRUDService<TUPLE, Session> {

}
