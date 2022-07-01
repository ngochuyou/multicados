/**
 *
 */
package multicados.controller.query;

import multicados.domain.entity.entities.District;
import multicados.internal.domain.annotation.For;
import multicados.internal.service.crud.rest.AbstractRestQuery;
import multicados.internal.service.crud.rest.filter.Filters.StringFilter;

/**
 * @author Ngoc Huy
 *
 */
@For(District.class)
public class DistrictQuery extends AbstractRestQuery<District> {

	private StringFilter name;

	public DistrictQuery() {
		super(District.class);
	}

	public StringFilter getName() {
		return name;
	}

	public void setName(StringFilter name) {
		this.name = name;
	}

}
