/**
 * 
 */
package multicados.domain.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

import com.fasterxml.jackson.annotation.JsonProperty;

import multicados.domain.AbstractEntity;
import multicados.internal.domain.PermanentResource;

/**
 * @author Ngoc Huy
 *
 */
@MappedSuperclass
public abstract class PermanentEntity<T extends Serializable> extends AbstractEntity<T> implements PermanentResource {

	@Column(nullable = false)
	private Boolean active;

	@Override
	@JsonProperty(value = PermanentEntity_.ACTIVE)
	public Boolean isActive() {
		return active;
	}

	@Override
	public void setActive(Boolean active) {
		this.active = active;
	}

}
