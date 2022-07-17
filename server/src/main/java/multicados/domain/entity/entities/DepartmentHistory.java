/**
 * 
 */
package multicados.domain.entity.entities;

import java.time.LocalDateTime;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.Table;

import multicados.domain.AbstractEntity;
import multicados.domain.entity.PermanentEntity;
import multicados.domain.entity.id.DepartmentHistoryId;
import multicados.domain.entity.id.DepartmentHistoryId_;
import multicados.internal.domain.SpannedResource;

/**
 * @author Ngoc Huy
 *
 */
@Entity
@Table(name = "department_histories")
public class DepartmentHistory extends PermanentEntity<DepartmentHistoryId> implements SpannedResource<LocalDateTime> {

	@EmbeddedId
	private DepartmentHistoryId id;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "department_id", referencedColumnName = Department_.ID, columnDefinition = AbstractEntity.MYSQL_UUID_COLUMN_DEFINITION, nullable = false, updatable = false)
	@MapsId(DepartmentHistoryId_.DEPARTMENT_ID)
	private Department department;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "personnel_id", referencedColumnName = Personnel_.ID, nullable = false, updatable = false)
	@MapsId(DepartmentHistoryId_.PERSONNEL_ID)
	private Personnel personnel;

	private String note;

	@Override
	public DepartmentHistoryId getId() {
		return id;
	}

	@Override
	public void setId(DepartmentHistoryId id) {
		this.id = id;
	}

	@Override
	public LocalDateTime getStartTimestamp() {
		return id.getSpanInformation().getStartTimestamp();
	}

	@Override
	public LocalDateTime getEndTimestamp() {
		return id.getSpanInformation().getEndTimestamp();
	}

	@Override
	public void setStartTimestamp(LocalDateTime temporal) {
		id.getSpanInformation().setStartTimestamp(temporal);
	}

	@Override
	public void setEndTimestamp(LocalDateTime temporal) {
		id.getSpanInformation().setEndTimestamp(temporal);
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}

	public Department getDepartment() {
		return department;
	}

	public void setDepartment(Department department) {
		this.department = department;
	}

	public Personnel getPersonnel() {
		return personnel;
	}

	public void setPersonnel(Personnel personnel) {
		this.personnel = personnel;
	}

}
