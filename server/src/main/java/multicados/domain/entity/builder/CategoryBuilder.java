/**
 *
 */
package multicados.domain.entity.builder;

import javax.persistence.EntityManager;

import multicados.domain.entity.entities.Category;
import multicados.internal.domain.annotation.For;
import multicados.internal.domain.builder.AbstractDomainResourceBuilder;
import multicados.internal.helper.StringHelper;

/**
 * @author Ngoc Huy
 *
 */
@For(Category.class)
public class CategoryBuilder extends AbstractDomainResourceBuilder<Category> {

	private Category mandatoryBuild(Category model, Category persistence) {
		persistence.setDescription(StringHelper.normalizeString(model.getDescription()));
		
		return persistence;
	}

	@Override
	public Category buildInsertion(Category model, EntityManager entityManager) throws Exception {
		return mandatoryBuild(model, model);
	}

	@Override
	public Category buildUpdate(Category model, Category persistence, EntityManager entityManger) {
		return mandatoryBuild(model, persistence);
	}

}
