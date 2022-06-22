/**
 * 
 */
package multicados.controller.query;

import multicados.domain.entity.entities.Province;
import multicados.internal.domain.For;
import multicados.internal.service.crud.rest.AbstractRestQuery;

/**
 * @author Ngoc Huy
 *
 */
@For(Province.class)
public class ProvinceQuery extends AbstractRestQuery<Province> {

	private DistrictQuery districts;

	public ProvinceQuery() {
		super(Province.class);
	}

	public DistrictQuery getDistricts() {
		return districts;
	}

	public void setDistricts(DistrictQuery districts) {
		this.districts = districts;
	}

}
