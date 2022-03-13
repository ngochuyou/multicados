/**
 * 
 */
package multicados.internal.service.credential;

import java.util.List;

import multicados.internal.domain.DomainResource;

/**
 * @author Ngoc Huy
 *
 */
public class ReadAccessManagerImpl implements ReadAccessManager {

	@Override
	public <D extends DomainResource> List<String> check(Class<D> type, List<String> requestedAttributes,
			CRUDCredential credential) {
		return null;
	}

}
