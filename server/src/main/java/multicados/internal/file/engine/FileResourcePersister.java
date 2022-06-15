/**
 * 
 */
package multicados.internal.file.engine;

import java.io.Serializable;

import org.hibernate.SessionFactoryObserver;

/**
 * @author Ngoc Huy
 *
 */
public interface FileResourcePersister extends SessionFactoryObserver {

	String getDirectoryPath();

	byte[] getContent(Serializable id, Object[] fields, FileResourceSession session) throws Exception;

}
