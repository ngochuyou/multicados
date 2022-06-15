/**
 * 
 */
package multicados.domain.entity.file;

import javax.persistence.Entity;

import org.hibernate.annotations.Persister;

import multicados.internal.file.domain.AbstractFileResource;
import multicados.internal.file.domain.Directory;
import multicados.internal.file.domain.Image;
import multicados.internal.file.engine.FileResourcePersisterImpl;

/**
 * @author Ngoc Huy
 *
 */
@Entity
@Directory("user\\")
@Persister(impl = FileResourcePersisterImpl.class)
public class UserPhoto extends AbstractFileResource implements Image {

}
