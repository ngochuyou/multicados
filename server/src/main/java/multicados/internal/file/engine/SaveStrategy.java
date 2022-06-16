/**
 * 
 */
package multicados.internal.file.engine;

import java.io.Serializable;

import multicados.internal.file.domain.FileResource;

/**
 * @author Ngoc Huy
 *
 */
public interface SaveStrategy {

	<T extends FileResource> void save(FileResourcePersister persister, Serializable id, T object,
			FileResourceSession session) throws Exception;

}
