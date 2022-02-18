/**
 * 
 */
package multicados.domain.entity.entities;

import java.time.LocalDateTime;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Table;

import multicados.domain.entity.AuditInformations;
import multicados.domain.entity.Role;
import multicados.internal.domain.AuditableResource;

/**
 * @author Ngoc Huy
 *
 */
@Entity
@Table(name = "personnels")
public class Personnel extends Operator implements AuditableResource<String, Operator, LocalDateTime> {

	@Embedded
	private AuditInformations auditInformations;

	public Personnel() {
		setRole(Role.PERSONNEL);
		auditInformations = new AuditInformations();
	}

	public AuditInformations getAuditInformations() {
		return auditInformations;
	}

	public void setAuditInformations(AuditInformations auditInformations) {
		this.auditInformations = auditInformations;
	}

	@Override
	public LocalDateTime getCreatedTimestamp() {
		return auditInformations.getCreatedTimestamp();
	}

	@Override
	public Operator getCreator() {
		return auditInformations.getCreator();
	}

	@Override
	public Operator getUpdater() {
		return auditInformations.getUpdater();
	}

	@Override
	public void setCreatedTimestamp(LocalDateTime timestamp) {
		auditInformations.setCreatedTimestamp(timestamp);
	}

	@Override
	public void setCreator(Operator creator) {
		auditInformations.setCreator(creator);
	}

	@Override
	public void setUpdater(Operator updater) {
		auditInformations.setUpdater(updater);
	}

}
