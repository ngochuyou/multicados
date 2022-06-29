/**
 *
 */
package multicados.internal.file.engine;

import static multicados.internal.helper.Utils.declare;

import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.LockOptions;
import org.hibernate.SessionFactory;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.persister.entity.SingleTableEntityPersister;
import org.hibernate.persister.spi.PersisterCreationContext;
import org.hibernate.proxy.HibernateProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import multicados.internal.config.Settings;
import multicados.internal.file.domain.Directory;
import multicados.internal.file.domain.FileResource;

/**
 * @author Ngoc Huy
 *
 */
public class FileResourcePersisterImpl extends SingleTableEntityPersister implements FileResourcePersister {

	private static final long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory.getLogger(FileResourcePersisterImpl.class);

	private static final String MESSAGE = String.format("Insertions on %s must always contain every property values",
			FileResource.class.getSimpleName());

	private final String directoryPath;

	private final SaveStrategy saveStrategy;

	@SuppressWarnings("unchecked")
	public FileResourcePersisterImpl(
	// @formatter:off
			PersistentClass persistentClass,
			EntityDataAccess cacheAccessStrategy,
			NaturalIdDataAccess naturalIdRegionAccessStrategy,
			PersisterCreationContext creationContext)
			throws Exception {
		// @formatter:on
		super(persistentClass, cacheAccessStrategy, naturalIdRegionAccessStrategy, creationContext);
		FileResourceSessionFactory sfi = FileResourceSessionFactory.class.cast(creationContext.getSessionFactory());

		sfi.addObserver(this);
		// @formatter:off
		directoryPath = String.format("%s%s",
				sfi.getServiceRegistry().requireService(ConfigurationService.class).getSettings()
					.get(Settings.FILE_RESOURCE_ROOT_DIRECTORY).toString(),
				declare(persistentClass)
					.then(PersistentClass::getMappedClass)
					.then(type -> type.getDeclaredAnnotation(Directory.class))
					.then(Directory.class::cast)
					.then(Directory::value)
					.get());
		// @formatter:on
		saveStrategy = sfi.getServiceRegistry().requireService(SaveStrategyResolver.class)
				.getSaveStrategy(getMappedClass());
	}

	@Override
	public void delete(Serializable id, Object version, Object object, SharedSessionContractImplementor session)
			throws HibernateException {}

	@Override
	public void delete(Serializable id, Object version, int j, Object object, String sql,
			SharedSessionContractImplementor session, Object[] loadedState) throws HibernateException {
		delete(id, version, object, session);
	}

	@Override
	public Object load(Serializable id, Object optionalObject, LockOptions lockOptions,
			SharedSessionContractImplementor session) throws HibernateException {
		return super.load(id, optionalObject, LockOptions.NONE, session);
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

	@Override
	public void insert(Serializable id, Object[] fields, boolean[] notNull, int j, String sql, Object object,
			SharedSessionContractImplementor session) throws HibernateException {
		throw new UnsupportedOperationException("Multiple insert operation is unnecessary");
	}

	@Override
	public void insert(Serializable id, Object[] fields, Object object, SharedSessionContractImplementor session) {
		// @formatter:off
		try {
			if (object instanceof HibernateProxy) {
				Hibernate.initialize(object);
			}

			String manipulatedIdentifier = saveStrategy.save(this, id.toString(), FileResource.class.cast(object), FileResourceSession.class.cast(session));

			setIdentifier(object, manipulatedIdentifier, session);
		} catch (Exception any) {
			any.printStackTrace();
			throw new HibernateException(any);
		}
		// @formatter:on
	}

	@Override
	public String resolvePath(String id) {
		return directoryPath + id;
	}

	@Override
	public void sessionFactoryCreated(SessionFactory factory) {
		createDirectory();
	}

	private void createDirectory() {
		Path path = Paths.get(directoryPath);

		if (!Files.exists(path)) {
			logger.debug("Creating new directory with path [{}]", path);
			path.toFile().mkdir();
			return;
		}

		if (Files.isDirectory(path)) {
			return;
		}

		throw new IllegalArgumentException(String.format("%s has already existed but it's not a directory", path));
	}

	@Override
	public String getDirectoryPath() {
		return directoryPath;
	}

}
