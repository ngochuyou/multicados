/**
 *
 */
package multicados.domain.entity.file;

import javax.persistence.Entity;
import javax.persistence.Transient;

import org.hibernate.annotations.Persister;

import multicados.domain.entity.AuditInformations;
import multicados.internal.file.domain.Directory;
import multicados.internal.file.engine.FileResourcePersisterImpl;
import multicados.internal.file.engine.image.AbstractImage;

/**
 * @author Ngoc Huy
 *
 */
@Entity
@Directory("user\\")
@Persister(impl = FileResourcePersisterImpl.class)
public class UserPhoto extends AbstractImage {

	@Transient
	private AuditInformations auditInformations;

	public AuditInformations getAuditInformations() {
		return auditInformations;
	}

	public void setAuditInformations(AuditInformations auditInformations) {
		this.auditInformations = auditInformations;
	}

}
