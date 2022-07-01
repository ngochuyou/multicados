/**
 *
 */
package multicados.internal.service.crud;

import java.util.Collection;
import java.util.Map;

import multicados.internal.domain.DomainResource;

/**
 * @author Ngoc Huy
 *
 */
public interface DomainResourceAttributeTranslator {

	<D extends DomainResource> Map<String, String> translate(Class<D> resourceType, Collection<String> attributes);

}
