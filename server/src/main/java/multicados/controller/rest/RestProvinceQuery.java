/**
 * 
 */
package multicados.controller.rest;

import multicados.domain.entity.entities.Province;
import multicados.internal.domain.For;
import multicados.internal.service.crud.rest.AbstractRestQuery;
import multicados.internal.service.crud.rest.filter.Filters.StringFilter;

/**
 * @author Ngoc Huy
 *
 */
@For(Province.class)
public class RestProvinceQuery extends AbstractRestQuery<Province> {

	private StringFilter name;

	public RestProvinceQuery() {
		super(Province.class);
	}

	public StringFilter getName() {
		return name;
	}

	public void setName(StringFilter name) {
		this.name = name;
	}

}
