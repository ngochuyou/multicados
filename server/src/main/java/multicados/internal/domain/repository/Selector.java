/**
 * 
 */
package multicados.internal.domain.repository;

import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;

import multicados.domain.entity.Entity;

/**
 * @author Ngoc Huy
 *
 */
public interface Selector<T extends Entity<?>, E> {

	List<Selection<?>> select(Root<T> root, CriteriaQuery<E> query, CriteriaBuilder builder);
	
}
