/**
 * 
 */
package multicados.internal.domain.repository;

import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;

import multicados.internal.domain.DomainResource;

/**
 * @author Ngoc Huy
 *
 */
@FunctionalInterface
public interface Selector<T extends DomainResource, E> {

	List<Selection<?>> select(Root<T> root, CriteriaQuery<E> query, CriteriaBuilder builder);

}
