/**
 * 
 */
package multicados.domain.entity.entities;

import javax.persistence.Entity;
import javax.persistence.Table;

import multicados.domain.entity.Role;

/**
 * @author Ngoc Huy
 *
 */
@Entity
@Table(name = "heads")
public class Head extends Operator {

	public Head() {
		setRole(Role.HEAD);
	}
	
}
