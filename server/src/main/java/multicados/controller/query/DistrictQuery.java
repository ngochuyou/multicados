/**
 * 
 */
package multicados.controller.query;

import multicados.domain.entity.entities.District;
import multicados.internal.domain.For;
import multicados.internal.service.crud.rest.AbstractRestQuery;

/**
 * @author Ngoc Huy
 *
 */
@For(District.class)
public class DistrictQuery extends AbstractRestQuery<District> {

	public DistrictQuery() {
		super(District.class);
	}

}
