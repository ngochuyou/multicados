/**
 * 
 */
package multicados.domain.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import multicados.domain.entity.entities.Operator;
import multicados.domain.entity.entities.User_;
import multicados.internal.domain.DomainComponentType;

/**
 * @author Ngoc Huy
 *
 */
@Embeddable
public class AuditInformations implements DomainComponentType, Serializable {

	private static final long serialVersionUID = 1L;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "creator", referencedColumnName = User_.ID, updatable = false)
	private Operator creator;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "updater", referencedColumnName = User_.ID)
	private Operator updater;

	@Column(nullable = false, updatable = false)
	private LocalDateTime createdTimestamp;

	@Column(nullable = false)
	private LocalDateTime updatedTimestamp;

	public AuditInformations() {}

	public AuditInformations(Operator creator, Operator updater, LocalDateTime createdTimestamp,
			LocalDateTime updatedTimestamp) {
		this.creator = creator;
		this.updater = updater;
		this.createdTimestamp = createdTimestamp;
		this.updatedTimestamp = updatedTimestamp;
	}

	public Operator getCreator() {
		return creator;
	}

	public void setCreator(Operator creator) {
		this.creator = creator;
	}

	public Operator getUpdater() {
		return updater;
	}

	public void setUpdater(Operator updater) {
		this.updater = updater;
	}

	public LocalDateTime getCreatedTimestamp() {
		return createdTimestamp;
	}

	public void setCreatedTimestamp(LocalDateTime createdTimestamp) {
		this.createdTimestamp = createdTimestamp;
	}

	public LocalDateTime getUpdatedTimestamp() {
		return updatedTimestamp;
	}

	public void setUpdatedTimestamp(LocalDateTime updatedTimestamp) {
		this.updatedTimestamp = updatedTimestamp;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((createdTimestamp == null) ? 0 : createdTimestamp.hashCode());
		result = prime * result + ((creator == null) ? 0 : creator.hashCode());
		result = prime * result + ((updatedTimestamp == null) ? 0 : updatedTimestamp.hashCode());
		result = prime * result + ((updater == null) ? 0 : updater.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AuditInformations other = (AuditInformations) obj;
		if (createdTimestamp == null) {
			if (other.createdTimestamp != null)
				return false;
		} else if (!createdTimestamp.equals(other.createdTimestamp))
			return false;
		if (creator == null) {
			if (other.creator != null)
				return false;
		} else if (!creator.getId().equals(other.creator.getId()))
			return false;
		if (updatedTimestamp == null) {
			if (other.updatedTimestamp != null)
				return false;
		} else if (!updatedTimestamp.equals(other.updatedTimestamp))
			return false;
		if (updater == null) {
			if (other.updater != null)
				return false;
		} else if (!updater.getId().equals(other.updater.getId()))
			return false;
		return true;
	}

}
