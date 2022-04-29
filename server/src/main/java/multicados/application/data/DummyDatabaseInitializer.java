/**
 * 
 */
package multicados.application.data;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import com.fasterxml.jackson.databind.ObjectMapper;

import multicados.domain.entity.entities.Category;
import multicados.domain.entity.entities.District;
import multicados.domain.entity.entities.Province;
import multicados.internal.domain.DomainResourceContext;
import multicados.internal.domain.repository.DatabaseInitializer.DatabaseInitializerContributor;
import multicados.internal.helper.HibernateHelper;
import multicados.internal.service.crud.GenericCRUDServiceImpl;

/**
 * @author Ngoc Huy
 *
 */
public class DummyDatabaseInitializer extends AbstractDummyDatabaseContributor
		implements DatabaseInitializerContributor {

	@Autowired
	public DummyDatabaseInitializer(ObjectMapper objectMapper, DomainResourceContext resourceContext,
			GenericCRUDServiceImpl crudService, Environment env) {
		super(objectMapper, resourceContext, crudService, env);
	}

	@Override
	public void contribute() {
		final Logger logger = LoggerFactory.getLogger(DummyDatabaseInitializer.class);
		Session session = HibernateHelper.getSessionFactory().openSession();

		session.setHibernateFlushMode(FlushMode.MANUAL);
		session.beginTransaction();
		// @formatter:off
		try {
			createBatch(Category.class, "dummy_categories.json", session, ex -> logger.error(ex.getMessage()));
			createBatch(Province.class, "dummy_provinces.json", session, ex -> logger.error(ex.getMessage()));
			createBatch(District.class, "dummy_districts.json", session, ex -> logger.error(ex.getMessage()));
			
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

}
