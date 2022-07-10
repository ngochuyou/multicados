/**
 * 
 */
package multicados.internal.file.engine;

import org.hibernate.service.Service;

/**
 * @author Ngoc Huy
 *
 */
public interface DirectoryInitializer extends Service {

	void createDirectory(String directory);

}
