/**
 *
 */
package multicados.domain.entity.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import multicados.domain.entity.Role;

/**
 * @author Ngoc Huy
 *
 */
@Entity
@Table(name = "customers")
public class Customer extends User {

	@Column(nullable = false)
	private Boolean subscribed;

	public Customer() {
		setRole(Role.CUSTOMER);
	}

	public Boolean getSubscribed() {
		return subscribed;
	}

	public void setSubscribed(Boolean subscribed) {
		this.subscribed = subscribed;
	}

}
