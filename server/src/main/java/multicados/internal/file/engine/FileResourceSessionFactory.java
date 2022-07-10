/**
 *
 */
package multicados.internal.file.engine;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.SessionCreationOptions;

/**
 * @author Ngoc Huy
 *
 */
public interface FileResourceSessionFactory extends SessionFactoryImplementor {

	SessionCreationOptions getSessionCreationOptions();
	
}
