/**
 * 
 */
package multicados.internal.file.engine;

import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.StatelessSessionBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.engine.query.spi.QueryPlanCache.QueryPlanCreator;
import org.hibernate.engine.spi.SessionBuilderImplementor;
import org.hibernate.internal.SessionCreationOptions;
import org.hibernate.internal.SessionFactoryImpl;

/**
 * @author Ngoc Huy
 *
 */
public class FileResourceSessionFactoryImpl extends SessionFactoryImpl implements FileResourceSessionFactory {

	private static final long serialVersionUID = 1L;

	@SuppressWarnings("rawtypes")
	private final SessionBuilderImplementor sessionCreationOptions;
	@SuppressWarnings("rawtypes")
	private final StatelessSessionBuilder statelessSessionBuilder;

	@SuppressWarnings("rawtypes")
	public FileResourceSessionFactoryImpl(
	// @formatter:off
			MetadataImplementor metadataImplementor,
			SessionFactoryOptions factoryOptions,
			QueryPlanCreator planCreator) throws Exception {
		// @formatter:on
		super(metadataImplementor, factoryOptions, planCreator);
		sessionCreationOptions = new SessionFactoryImpl.SessionBuilderImpl<SessionBuilderImplementor>(this) {

			@Override
			public Session openSession() throws HibernateException {
				return new FileResourceSession(FileResourceSessionFactoryImpl.this);
			}

		}.flushMode(FlushMode.MANUAL);
		statelessSessionBuilder = new SessionFactoryImpl.StatelessSessionBuilderImpl(this) {

			@Override
			public FlushMode getInitialSessionFlushMode() {
				return FlushMode.MANUAL;
			};

		};

		registerEventListeners();
	}

	private void registerEventListeners() {
//		EventListenerRegistry listenerRegistry = getServiceRegistry().requireService(EventListenerRegistry.class);
//
//		listenerRegistry.prependListeners(eventType, listener);
	}

	@Override
	@SuppressWarnings("rawtypes")
	public SessionBuilderImplementor withOptions() {
		return sessionCreationOptions;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public StatelessSessionBuilder withStatelessOptions() {
		return statelessSessionBuilder;
	}

	@Override
	public SessionCreationOptions getSessionCreationOptions() {
		return SessionCreationOptions.class.cast(sessionCreationOptions);
	}

}
