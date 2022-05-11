/**
 * 
 */
package multicados.controller.rest;

import org.springframework.data.jpa.domain.Specification;

import multicados.domain.entity.entities.District;
import multicados.internal.domain.For;
import multicados.internal.service.crud.rest.AbstractRestQuery;

/**
 * @author Ngoc Huy
 *
 */
@For(District.class)
public class DistrictRestQuery extends AbstractRestQuery<District> {

	private ProvinceRestQuery province;

	public DistrictRestQuery() {
		super(District.class);
	}

	@Override
	public Specification<District> getSpecification() {
		return null;
	}

	public ProvinceRestQuery getProvince() {
		return province;
	}

	public void setProvince(ProvinceRestQuery province) {
		this.province = province;
	}

}
