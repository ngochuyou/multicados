/**
 * 
 */
package multicados.internal.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import multicados.internal.domain.DomainResourceTree.AbstractCollector;

/**
 * @author Ngoc Huy
 *
 */
public class DomainResourceTreeCollectors {

	private DomainResourceTreeCollectors() {};

	public static <D extends DomainResource> AbstractCollector<Class<D>, Set<Class<D>>> toTypesSet() {
		return new AbstractCollector<>(HashSet::new, DomainResourceTree::getResourceType) {};
	}

	public static <D extends DomainResource> AbstractCollector<Class<D>, List<Class<D>>> toTypesList() {
		return new AbstractCollector<>(ArrayList::new, DomainResourceTree::getResourceType) {};
	}

	@SuppressWarnings("rawtypes")
	public static <D extends DomainResource> AbstractCollector<DomainResourceTree, Set<DomainResourceTree>> toTreesSet() {
		return new AbstractCollector<>(HashSet::new, tree -> tree) {};
	}

	@SuppressWarnings("rawtypes")
	public static <D extends DomainResource> AbstractCollector<DomainResourceTree, List<DomainResourceTree>> toTreesList() {
		return new AbstractCollector<>(ArrayList::new, tree -> tree) {};
	}

}
