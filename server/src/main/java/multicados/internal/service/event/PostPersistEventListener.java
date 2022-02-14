/**
 * 
 */
package multicados.internal.service.event;

import multicados.internal.domain.DomainResource;

/**
 * @author Ngoc Huy
 *
 */
public interface PostPersistEventListener extends ServiceEventListener {

	<D extends DomainResource> void onPostPersist(D resource) throws Exception;

}
