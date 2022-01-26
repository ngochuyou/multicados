/**
 * 
 */
package multicados.internal.domain;

import java.util.Set;

import multicados.internal.context.ContextBuildListener;
import multicados.internal.helper.FunctionHelper.HandledConsumer;

/**
 * @author Ngoc Huy
 *
 */
public interface DomainResourceTree<T extends DomainResource> extends ContextBuildListener {

	DomainResourceTree<? super T> getParent();

	Class<T> getResourceType();

	Set<DomainResourceTree<? extends T>> getChildrens();

	void add(Class<? extends DomainResource> resourceType);

	void forEach(HandledConsumer<DomainResourceTree<? extends DomainResource>, Exception> consumer) throws Exception;

	DomainResourceTree<? extends T> locate(Class<DomainResource> resourceType);

}
