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
import org.hibernate.SessionFactory;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.persister.entity.SingleTableEntityPersister;
import org.hibernate.persister.spi.PersisterCreationContext;
import org.hibernate.proxy.HibernateProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import multicados.internal.config.Settings;
import multicados.internal.context.ContextManager;
import multicados.internal.file.domain.Directory;
import multicados.internal.file.domain.FileResource;
import multicados.internal.file.domain.Image;
import multicados.internal.helper.SpringHelper;
import multicados.internal.helper.Utils.HandledFunction;

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
	public FileResourcePersisterImpl(PersistentClass persistentClass, EntityDataAccess cacheAccessStrategy,
			NaturalIdDataAccess naturalIdRegionAccessStrategy, PersisterCreationContext creationContext)
			throws Exception {
		super(persistentClass, cacheAccessStrategy, naturalIdRegionAccessStrategy, creationContext);
		FileResourceSessionFactory sfi = FileResourceSessionFactory.class.cast(creationContext.getSessionFactory());

		sfi.addObserver(this);
		// @formatter:off
		directoryPath = String.format("%s%s",
				SpringHelper.getOrDefault(
						ContextManager.getBean(Environment.class),
						Settings.FILE_RESOURCE_ROOT_DIRECTORY,
						HandledFunction.identity(),
						Settings.DEAULT_FILE_RESOURCE_ROOT_DIRECTORY),
				declare(persistentClass)
					.then(PersistentClass::getMappedClass)
					.then(type -> type.getDeclaredAnnotation(Directory.class))
					.then(Directory.class::cast)
					.then(Directory::value)
					.get());
		
		SaveStrategyResolver saveStrategyResolver = sfi.getServiceRegistry().requireService(SaveStrategyResolver.class);
		
		saveStrategy = Image.class.isAssignableFrom(getMappedClass()) ? saveStrategyResolver.getSaveStrategy(Image.class) : saveStrategyResolver.getSaveStrategy(FileResource.class);
		// @formatter:on
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

			saveStrategy.save(this, id, FileResource.class.cast(object), FileResourceSession.class.cast(session));
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

	@Override
	public String getDirectoryPath() {
		return directoryPath;
	}

}
