/**
 *
 */
package multicados.domain.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

import multicados.domain.AbstractEntity;
import multicados.internal.domain.PermanentResource;

/**
 * @author Ngoc Huy
 *
 */
@MappedSuperclass
public abstract class PermanentEntity<T extends Serializable> extends AbstractEntity<T> implements PermanentResource {

	@Column(nullable = false)
	private boolean active;

	@Override
	public boolean isActive() {
		return active;
	}

	@Override
	public void setActive(boolean active) {
		this.active = active;
	}

}
