/**
 *
 */
package multicados.controller.query;

import multicados.domain.entity.entities.Category;
import multicados.internal.domain.For;
import multicados.internal.service.crud.rest.AbstractRestQuery;

/**
 * @author Ngoc Huy
 *
 */
@For(Category.class)
public class CategoryQuery extends AbstractRestQuery<Category> {

	public CategoryQuery() {
		super(Category.class);
	}

}
