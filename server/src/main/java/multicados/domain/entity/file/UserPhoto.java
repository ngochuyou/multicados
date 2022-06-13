/**
 * 
 */
package multicados.domain.entity.file;

import javax.persistence.Entity;

import org.hibernate.annotations.Persister;

import multicados.internal.file.engine.FileResourcePersister;
import multicados.internal.file.model.AbstractFileResource;
import multicados.internal.file.model.Directory;

/**
 * @author Ngoc Huy
 *
 */
@Entity
@Directory("user\\")
@Persister(impl = FileResourcePersister.class)
public class UserPhoto extends AbstractFileResource {
}
