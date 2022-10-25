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

import multicados.domain.AbstractEntity;
import multicados.domain.entity.entities.Head;
import multicados.internal.domain.DomainComponent;

/**
 * @author Ngoc Huy
 *
 */
@Embeddable
public class ApprovalInformations implements DomainComponent, Serializable {

	private static final long serialVersionUID = 1L;

	public static final String APPROVED_BY_C = "approved_by";
	public static final String APPROVED_TIMESTAMP_C = "approved_timestamp";

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = APPROVED_BY_C, referencedColumnName = AbstractEntity.ID_C)
	private transient Head approvedBy;

	@Column(name = APPROVED_TIMESTAMP_C)
	private LocalDateTime approvedTimestamp;

	public ApprovalInformations() {}

	public ApprovalInformations(Head approvedBy, LocalDateTime approvedTimestamp) {
		this.approvedBy = approvedBy;
		this.approvedTimestamp = approvedTimestamp;
	}

	public Head getApprovedBy() {
		return approvedBy;
	}

	public void setApprovedBy(Head approvedBy) {
		this.approvedBy = approvedBy;
	}

	public LocalDateTime getApprovedTimestamp() {
		return approvedTimestamp;
	}

	public void setApprovedTimestamp(LocalDateTime approvedTimestamp) {
		this.approvedTimestamp = approvedTimestamp;
	}

}
