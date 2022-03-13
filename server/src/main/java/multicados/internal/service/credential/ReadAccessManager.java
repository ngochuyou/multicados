/**
 * 
 */
package multicados.internal.service.credential;

import java.util.List;

import multicados.internal.context.ContextBuilder;
import multicados.internal.domain.DomainResource;

/**
 * @author Ngoc Huy
 *
 */
public interface ReadAccessManager extends ContextBuilder {

	<D extends DomainResource> List<String> check(Class<D> type, List<String> requestedAttributes,
			CRUDCredential credential);

}
