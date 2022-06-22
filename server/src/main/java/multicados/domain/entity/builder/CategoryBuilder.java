/**
 * 
 */
package multicados.domain.entity.builder;

import java.io.Serializable;

import javax.persistence.EntityManager;

import multicados.domain.entity.entities.Category;
import multicados.internal.domain.For;
import multicados.internal.domain.builder.AbstractDomainResourceBuilder;

/**
 * @author Ngoc Huy
 *
 */
@For(Category.class)
public class CategoryBuilder extends AbstractDomainResourceBuilder<Category> {

	@Override
	public Category buildInsertion(Serializable id, Category resource, EntityManager entityManager) throws Exception {
		return null;
	}

	@Override
	public Category buildUpdate(Serializable id, Category model, Category resource, EntityManager entityManger) {
		return null;
	}

}
