/**
 * 
 */
package multicados.application.data;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import com.fasterxml.jackson.databind.ObjectMapper;

import multicados.domain.entity.entities.User;
import multicados.internal.config.Settings;
import multicados.internal.domain.DomainResourceContext;
import multicados.internal.domain.repository.DatabaseInitializer.DatabaseInitializerContributor;
import multicados.internal.service.crud.GenericCRUDServiceImpl;

/**
 * @author Ngoc Huy
 *
 */
public class DummyDatabaseInitializer extends AbstractDummyDatabaseContributor
		implements DatabaseInitializerContributor {

	private final Environment env;
	private final SessionFactoryImplementor sfi;

	@Autowired
	public DummyDatabaseInitializer(ObjectMapper objectMapper, DomainResourceContext resourceContext,
			GenericCRUDServiceImpl crudService, Environment env, SessionFactoryImplementor sfi) {
		super(objectMapper, resourceContext, crudService, env);
		this.env = env;
		this.sfi = sfi;
	}

	@Override
	public void contribute() {
		final Logger logger = LoggerFactory.getLogger(DummyDatabaseInitializer.class);
		Session session = sfi.openSession();

		session.setHibernateFlushMode(FlushMode.MANUAL);
		session.beginTransaction();
		// @formatter:off
		try {
			createNonProductStrictDummies(logger, session);
			createProductStrictDummies(logger, session);
			
			session.getTransaction().commit();
		} catch (Exception e) {
			e.printStackTrace();
			session.clear();
			session.getTransaction().rollback();
		} finally {
			session.close();
		}
		// @formatter:on
	}

	private void createProductStrictDummies(Logger logger, Session session) throws Exception {
		if (env.getProperty(Settings.ACTIVE_PROFILES).equals("PROD")) {
			return;
		}

		createBatch(User.class, "dummy_users.json", session, ex -> logger.error(ex.getMessage()));
	}

	private void createNonProductStrictDummies(final Logger logger, Session session) throws Exception {
//		createBatch(Category.class, "dummy_categories.json", session, ex -> logger.error(ex.getMessage()));
//		createBatch(Province.class, "dummy_provinces.json", session, ex -> logger.error(ex.getMessage()));
//		createBatch(District.class, "dummy_districts.json", session, ex -> logger.error(ex.getMessage()));
	}

}
