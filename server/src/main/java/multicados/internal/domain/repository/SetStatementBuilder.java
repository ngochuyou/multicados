/**
 * 
 */
package multicados.internal.domain.repository;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.Root;

/**
 * 
 * 
 * @author Ngoc Huy
 *
 */
public interface SetStatementBuilder<T> {

	CriteriaUpdate<?> build(Root<T> root, CriteriaUpdate<?> query, CriteriaBuilder builder);
	
}
