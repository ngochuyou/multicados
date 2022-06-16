/**
 * 
 */
package multicados.internal.file.engine;

import org.hibernate.SessionFactoryObserver;
import org.hibernate.persister.entity.EntityPersister;

/**
 * @author Ngoc Huy
 *
 */
public interface FileResourcePersister extends EntityPersister, SessionFactoryObserver {

	String getDirectoryPath();

	String resolvePath(String id);

}
