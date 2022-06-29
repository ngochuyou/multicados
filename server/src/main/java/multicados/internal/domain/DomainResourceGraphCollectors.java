/**
 *
 */
package multicados.internal.domain;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.function.Function;

import multicados.internal.domain.DomainResourceGraph.AbstractCollector;

/**
 * @author Ngoc Huy
 *
 */
public class DomainResourceGraphCollectors {

	private DomainResourceGraphCollectors() {}

	public static AbstractCollector<Class<? extends DomainResource>, LinkedHashSet<Class<? extends DomainResource>>> toTypesSet() {
		return new AbstractCollector<>(LinkedHashSet::new, DomainResourceGraph::getResourceType) {};
	}

	public static AbstractCollector<Class<? extends DomainResource>, LinkedList<Class<? extends DomainResource>>> toTypesList() {
		return new AbstractCollector<>(LinkedList::new, DomainResourceGraph::getResourceType) {};
	}

	public static AbstractCollector<DomainResourceGraph<?>, LinkedHashSet<DomainResourceGraph<?>>> toGraphsSet() {
		return new AbstractCollector<>(LinkedHashSet::new, Function.identity()) {};
	}

	public static AbstractCollector<DomainResourceGraph<?>, LinkedList<DomainResourceGraph<?>>> toGraphsList() {
		return new AbstractCollector<>(LinkedList::new, Function.identity()) {};
	}

}
