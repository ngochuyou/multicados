/**
 *
 */
package multicados.internal.service.crud;

import javax.persistence.EntityManager;

import multicados.internal.service.crud.rest.RestQueryFulfiller;

/**
 * @author Ngoc Huy
 *
 */
public interface GenericRestCRUDService<TUPLE, EM extends EntityManager>
		extends GenericCRUDService<TUPLE, EM>, RestQueryFulfiller<TUPLE, EM> {

}
