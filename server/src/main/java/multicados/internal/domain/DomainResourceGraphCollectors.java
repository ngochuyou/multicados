/**
 * 
 */
package multicados.internal.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import multicados.internal.domain.DomainResourceGraph.AbstractCollector;

/**
 * @author Ngoc Huy
 *
 */
public class DomainResourceGraphCollectors {

	private DomainResourceGraphCollectors() {};

	public static <D extends DomainResource> AbstractCollector<Class<D>, Set<Class<D>>> toTypesSet() {
		return new AbstractCollector<>(HashSet::new, DomainResourceGraph::getResourceType) {};
	}

	public static <D extends DomainResource> AbstractCollector<Class<D>, List<Class<D>>> toTypesList() {
		return new AbstractCollector<>(ArrayList::new, DomainResourceGraph::getResourceType) {};
	}

	@SuppressWarnings("rawtypes")
	public static <D extends DomainResource> AbstractCollector<DomainResourceGraph, Set<DomainResourceGraph>> toTreesSet() {
		return new AbstractCollector<>(HashSet::new, tree -> tree) {};
	}

	@SuppressWarnings("rawtypes")
	public static <D extends DomainResource> AbstractCollector<DomainResourceGraph, List<DomainResourceGraph>> toTreesList() {
		return new AbstractCollector<>(ArrayList::new, tree -> tree) {};
	}

}
