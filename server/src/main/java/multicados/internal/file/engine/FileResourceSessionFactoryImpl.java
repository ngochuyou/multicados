/**
 * 
 */
package multicados.internal.file.engine;

import org.hibernate.FlushMode;
import org.hibernate.StatelessSessionBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.engine.query.spi.QueryPlanCache.QueryPlanCreator;
import org.hibernate.engine.spi.SessionBuilderImplementor;
import org.hibernate.internal.SessionFactoryImpl;
import org.springframework.core.env.Environment;

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
			Environment env,
			MetadataImplementor metadataImplementor,
			SessionFactoryOptions factoryOptions,
			QueryPlanCreator planCreator) throws Exception {
		// @formatter:on
		super(metadataImplementor, factoryOptions, planCreator);
		sessionCreationOptions = new SessionFactoryImpl.SessionBuilderImpl<SessionBuilderImplementor>(this)
				.flushMode(FlushMode.MANUAL);
		statelessSessionBuilder = new SessionFactoryImpl.StatelessSessionBuilderImpl(this) {

			@Override
			public FlushMode getInitialSessionFlushMode() {
				return FlushMode.MANUAL;
			};

		};
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

}
