/**
 *
 */
package multicados.internal.file.engine;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;

/**
 * @author Ngoc Huy
 *
 */
public interface FileResourcePersister extends EntityPersister, SessionFactoryObserver {

	static final LockOptions LOCK_OPTIONS = new LockOptions().setLockMode(LockMode.NONE);

	String getDirectoryPath();

	String resolvePath(String id);

	@Override
	default Object load(Serializable id, Object optionalObject, LockMode lockMode,
			SharedSessionContractImplementor session) throws HibernateException {
		return load(id, optionalObject, LOCK_OPTIONS, session);
	}

	@Override
	default void lock(Serializable id, Object version, Object object, LockMode lockMode,
			SharedSessionContractImplementor session) throws HibernateException {
		throw new UnsupportedOperationException();
	}

	@Override
	default void lock(Serializable id, Object version, Object object, LockOptions lockOptions,
			SharedSessionContractImplementor session) throws HibernateException {
		throw new UnsupportedOperationException();
	}

	@Override
	default Serializable insert(Object[] fields, Object object, SharedSessionContractImplementor session)
			throws HibernateException {
		throw new UnsupportedOperationException();
	}

}
