/**
 *
 */
package multicados.controller.query;

import multicados.domain.entity.entities.Category;
import multicados.internal.domain.annotation.For;
import multicados.internal.service.crud.rest.AbstractRestQuery;
import multicados.internal.service.crud.rest.filter.Filters.StringFilter;

/**
 * @author Ngoc Huy
 *
 */
@For(Category.class)
public class CategoryQuery extends AbstractRestQuery<Category> {

	private StringFilter name;

	public CategoryQuery() {
		super(Category.class);
	}

	public StringFilter getName() {
		return name;
	}

	public void setName(StringFilter name) {
		this.name = name;
	}

}
