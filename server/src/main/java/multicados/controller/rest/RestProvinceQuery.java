/**
 * 
 */
package multicados.controller.rest;

import org.springframework.data.jpa.domain.Specification;

import multicados.domain.entity.entities.Province;
import multicados.internal.domain.For;
import multicados.internal.service.crud.rest.AbstractRestQuery;

/**
 * @author Ngoc Huy
 *
 */
@For(Province.class)
public class RestProvinceQuery extends AbstractRestQuery<Province> {

	public RestProvinceQuery() {
		super(Province.class);
	}

	@Override
	public Specification<Province> getSpecification() {
		return null;
	}

}
