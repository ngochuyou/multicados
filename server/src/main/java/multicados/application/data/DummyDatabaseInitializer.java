/**
 * 
 */
package multicados.application.data;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.LockModeType;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;

import multicados.domain.entity.entities.Category;
import multicados.domain.entity.entities.Category_;
import multicados.internal.domain.DomainResourceContext;
import multicados.internal.domain.repository.DatabaseInitializer.DatabaseInitializerContributor;
import multicados.internal.domain.repository.GenericRepository;
import multicados.internal.helper.HibernateHelper;
import multicados.internal.helper.Utils;
import multicados.internal.service.crud.GenericCRUDService;

/**
 * @author Ngoc Huy
 *
 */
public class DummyDatabaseInitializer extends AbstractDummyDatabaseContributor
		implements DatabaseInitializerContributor {

	private final GenericRepository genericRepository;

	@Autowired
	public DummyDatabaseInitializer(ObjectMapper objectMapper, DomainResourceContext resourceContext,
			GenericCRUDService crudService, GenericRepository genericRepository) {
		super(objectMapper, resourceContext, crudService);
		this.genericRepository = genericRepository;
	}

	@Override
	public void contribute() {
		Session session = HibernateHelper.getSessionFactory().openSession();

		session.setHibernateFlushMode(FlushMode.MANUAL);
		session.beginTransaction();
		// @formatter:off
		try {
			contributeCategories(genericRepository, session);
			
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

	private void contributeCategories(GenericRepository repository, Session session) throws Exception {
		final Logger logger = LoggerFactory.getLogger(this.getClass());
		// @formatter:off
		List<Category> categories = Utils.declare("data\\dummy\\dummy_categories.json")
			.then(this::getArray)
				.second(Category.class)
			.then(this::toInstances)
			.get();
		Set<String> exsitingCategories = repository
				.findAll(
						Category.class,
						(root, query, builder) -> List.of(root.get(Category_.name)),
						(root, query, builder) -> builder.in(root.get(Category_.NAME))
								.value(categories.stream().map(Category::getName).collect(Collectors.toList())),
						LockModeType.PESSIMISTIC_WRITE,
						session)
				.stream().map(tuple -> tuple.get(0, String.class))
				.collect(HashSet::new, (set, value) -> set.add(value), Set::addAll);
		// @formatter:on
		create(categories.stream().filter(category -> {
			if (exsitingCategories.contains(category.getName())) {
				logger.debug("Skipping exsiting entity of type {} with name {}", Category.class, category.getName());
				return false;
			}

			return true;
		}).collect(Collectors.toList()), Category.class, session);
	}

}
