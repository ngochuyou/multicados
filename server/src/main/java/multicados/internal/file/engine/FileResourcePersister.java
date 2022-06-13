/**
 * 
 */
package multicados.internal.file.engine;

import static multicados.internal.helper.Utils.declare;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;

import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.persister.entity.SingleTableEntityPersister;
import org.hibernate.persister.spi.PersisterCreationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import multicados.internal.config.Settings;
import multicados.internal.context.ContextManager;
import multicados.internal.domain.FileResource;
import multicados.internal.file.model.AbstractFileResource_;
import multicados.internal.file.model.Directory;
import multicados.internal.helper.SpringHelper;

/**
 * @author Ngoc Huy
 *
 */
public class FileResourcePersister extends SingleTableEntityPersister implements SessionFactoryObserver {

	private static final long serialVersionUID = 1L;

	private static final Logger logger = LoggerFactory.getLogger(FileResourcePersister.class);

	private static final String MESSAGE = String.format("Insertions on %s must always contain every property values",
			FileResource.class.getSimpleName());

	private final String resourcePath;
	private final int contentIndex;

	@SuppressWarnings("unchecked")
	public FileResourcePersister(PersistentClass persistentClass, EntityDataAccess cacheAccessStrategy,
			NaturalIdDataAccess naturalIdRegionAccessStrategy, PersisterCreationContext creationContext)
			throws Exception {
		super(persistentClass, cacheAccessStrategy, naturalIdRegionAccessStrategy, creationContext);
		creationContext.getSessionFactory().addObserver(this);
		// @formatter:off
		resourcePath = String.format("%s%s",
				SpringHelper.getOrDefault(
						ContextManager.getBean(Environment.class),
						Settings.FILE_RESOURCE_ROOT_DIRECTORY,
						Function.identity(),
						Settings.DEAULT_FILE_RESOURCE_ROOT_DIRECTORY),
				declare(persistentClass)
					.then(PersistentClass::getMappedClass)
					.then(type -> type.getDeclaredAnnotation(Directory.class))
					.then(Directory.class::cast)
					.then(Directory::value)
					.get());
		// @formatter:on
		contentIndex = getEntityMetamodel().getPropertyIndex(AbstractFileResource_.CONTENT);
	}

	@Override
	public Serializable insert(Object[] fields, boolean[] notNull, String sql, Object object,
			SharedSessionContractImplementor session) throws HibernateException {
		throw new UnsupportedOperationException(MESSAGE);
	}

	@Override
	public Serializable insert(Object[] fields, Object object, SharedSessionContractImplementor session)
			throws HibernateException {
		throw new UnsupportedOperationException(MESSAGE);
	}

	private String resolvePath(String id) {
		return resourcePath + id;
	}

	@Override
	public void insert(Serializable id, Object[] fields, boolean[] notNull, int j, String sql, Object object,
			SharedSessionContractImplementor session) throws HibernateException {
		// // @formatter:off
		try {
			declare(id.toString())
				.then(this::resolvePath)
				.then(File::new)
				.then(File::getPath)
				.then(Paths::get)
					.second(byte[].class.cast(fields[contentIndex]))
				.consume(this::logInsert)
				.consume(Files::write);
		} catch (Exception any) {
			any.printStackTrace();
			throw new HibernateException(any);
		}
		// @formatter:on
	}

	private void logInsert(Path path, byte[] content) {
		if (logger.isDebugEnabled()) {
			logger.debug("Writing file [{}] with content length {}", path, content.length);
		}
	}

	@Override
	public void sessionFactoryCreated(SessionFactory factory) {
		createDirectory();
	}

	private void createDirectory() {
		Path path = Paths.get(resourcePath);
		boolean doesExist = Files.exists(path);
		boolean isDirectory = Files.isDirectory(path);

		if (!doesExist) {
			logger.debug("Creating new directory with path [{}]", path);
			path.toFile().mkdir();
			return;
		}

		if (isDirectory) {
			return;
		}

		throw new IllegalArgumentException(String.format("%s has already existed but it's not a directory", path));
	}

}
