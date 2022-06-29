/**
 *
 */
package multicados.internal.file.engine;

import multicados.internal.file.domain.FileResource;

/**
 * @author Ngoc Huy
 *
 */
public interface SaveStrategy {

	<T extends FileResource> String save(FileResourcePersister persister, String id, T object,
			FileResourceSession session) throws Exception;

}
