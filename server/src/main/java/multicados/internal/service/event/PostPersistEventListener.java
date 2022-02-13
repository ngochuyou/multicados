/**
 * 
 */
package multicados.internal.service.event;

import multicados.internal.domain.DomainResource;

/**
 * @author Ngoc Huy
 *
 */
public interface PostPersistEventListener<D extends DomainResource> extends ServiceEventListener {

	void onPostInsert(D resource) throws Exception;

}
