/**
 * 
 */
package multicados.internal.file.engine;

import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.internal.SessionImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * @author Ngoc Huy
 *
 */
@Component
@RequestScope
@Lazy
public class FileResourceSession extends SessionImpl {

	private static final long serialVersionUID = 1L;

	private static final Logger logger = LoggerFactory.getLogger(FileResourceSession.class);

	@Autowired
	public FileResourceSession(FileManagement fileManagement) {
		this(fileManagement.getSessionFactory());
	}

	public FileResourceSession(FileResourceSessionFactory sessionFactory) {
		super(SessionFactoryImpl.class.cast(sessionFactory), sessionFactory.getSessionCreationOptions());

		if (logger.isDebugEnabled()) {
			logger.debug("Creating a new instance of {}", this.getClass().getName());
		}

		beginTransaction();
	}

}
