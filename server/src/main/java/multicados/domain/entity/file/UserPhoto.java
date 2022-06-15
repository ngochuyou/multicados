/**
 * 
 */
package multicados.domain.entity.file;

import javax.persistence.Entity;

import org.hibernate.annotations.Persister;

import multicados.internal.domain.Image;
import multicados.internal.file.engine.FileResourcePersisterImpl;
import multicados.internal.file.model.AbstractFileResource;
import multicados.internal.file.model.Directory;

/**
 * @author Ngoc Huy
 *
 */
@Entity
@Directory("user\\")
@Persister(impl = FileResourcePersisterImpl.class)
public class UserPhoto extends AbstractFileResource implements Image {

}
