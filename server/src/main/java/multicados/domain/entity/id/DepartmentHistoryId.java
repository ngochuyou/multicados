/**
 * 
 */
package multicados.domain.entity.id;

import java.io.Serializable;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;

import multicados.domain.AbstractEntity;
import multicados.domain.entity.SpanInformation;
import multicados.domain.entity.entities.Department_;
import multicados.domain.entity.entities.Personnel_;

/**
 * @author Ngoc Huy
 *
 */
@Embeddable
public class DepartmentHistoryId implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Column(name = Department_.ID, nullable = false, columnDefinition = AbstractEntity.MYSQL_UUID_COLUMN_DEFINITION)
	private UUID departmentId;

	@Column(name = Personnel_.ID, nullable = false)
	private String personnelId;

	@Embedded
	private SpanInformation spanInformation;

	public DepartmentHistoryId() {
		super();
	}

	public DepartmentHistoryId(UUID departmentId, String personnelId) {
		this.departmentId = departmentId;
		this.personnelId = personnelId;
	}

	public UUID getDepartmentId() {
		return departmentId;
	}

	public void setDepartmentId(UUID departmentId) {
		this.departmentId = departmentId;
	}

	public String getPersonnelId() {
		return personnelId;
	}

	public void setPersonnelId(String personnelId) {
		this.personnelId = personnelId;
	}

	private void makeSureSpanInformationIsNotNull() {
		spanInformation = spanInformation == null ? new SpanInformation() : spanInformation;
	}

	public SpanInformation getSpanInformation() {
		makeSureSpanInformationIsNotNull();
		return spanInformation;
	}

	public void setSpanInformation(SpanInformation spanInformation) {
		this.spanInformation = spanInformation;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((departmentId == null) ? 0 : departmentId.hashCode());
		result = prime * result + ((personnelId == null) ? 0 : personnelId.hashCode());
		result = prime * result + ((spanInformation == null) ? 0 : spanInformation.hashCode());
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
		DepartmentHistoryId other = (DepartmentHistoryId) obj;
		if (departmentId == null) {
			if (other.departmentId != null)
				return false;
		} else if (!departmentId.equals(other.departmentId))
			return false;
		if (personnelId == null) {
			if (other.personnelId != null)
				return false;
		} else if (!personnelId.equals(other.personnelId))
			return false;
		if (spanInformation == null) {
			if (other.spanInformation != null)
				return false;
		} else if (!spanInformation.equals(other.spanInformation))
			return false;
		return true;
	}

}
