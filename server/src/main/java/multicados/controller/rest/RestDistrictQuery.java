/**
 * 
 */
package multicados.controller.rest;

import multicados.domain.entity.entities.District;
import multicados.internal.domain.For;
import multicados.internal.service.crud.rest.AbstractRestQuery;
import multicados.internal.service.crud.rest.filter.Filters.StringFilter;

/**
 * @author Ngoc Huy
 *
 */
@For(District.class)
public class RestDistrictQuery extends AbstractRestQuery<District> {

	private RestProvinceQuery province;

	private StringFilter name;

	public RestDistrictQuery() {
		super(District.class);
	}

	public RestProvinceQuery getProvince() {
		return province;
	}

	public void setProvince(RestProvinceQuery province) {
		this.province = province;
	}

	public StringFilter getName() {
		return name;
	}

	public void setName(StringFilter name) {
		this.name = name;
	}

}
