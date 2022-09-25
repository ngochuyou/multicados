/**
 * 
 */
package multicados.application.context;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.transaction.annotation.Transactional;

import multicados.domain.entity.entities.Department;
import multicados.internal.context.hook.DomainContextHook;
import multicados.internal.domain.DomainResourceContext;
import multicados.internal.domain.metadata.DomainResourceMetadata;
import multicados.internal.domain.metadata.NamedResourceMetadata;
import multicados.internal.domain.repository.GenericRepository;
import multicados.internal.helper.StringHelper;
import multicados.internal.helper.Utils;
import multicados.internal.service.ServiceResult;
import multicados.internal.service.crud.GenericCRUDServiceImpl;

/**
 * @author Ngoc Huy
 *
 */
public class DepartmentScopeHook implements DomainContextHook {

	private static final Logger logger = LoggerFactory.getLogger(DepartmentScopeHook.class);

	private final SessionFactoryImplementor sessionFactory;
	private final GenericCRUDServiceImpl crudService;
	private final GenericRepository genericRepository;
	private final DomainResourceContext resourceContext;
	private final Environment env;

	public DepartmentScopeHook(Environment env, SessionFactory sessionFactory, GenericCRUDServiceImpl crudService,
			GenericRepository genericRepository, DomainResourceContext resourceContext) {
		this.sessionFactory = sessionFactory.unwrap(SessionFactoryImplementor.class);
		this.crudService = crudService;
		this.genericRepository = genericRepository;
		this.resourceContext = resourceContext;
		this.env = env;
	}

	/**
	 * From the configuredDepartmentNames, determine those to be created, saved to
	 * the database
	 * 
	 * @param configuredDepartmentNames
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	private List<String> locateToBeCreatedDepartmentNames(List<String> configuredDepartmentNames, Session session,
			DomainResourceMetadata<Department> metadata) throws Exception {
		final NamedResourceMetadata<Department> namedResourceMetadata = metadata.unwrap(NamedResourceMetadata.class);
		final List<String> toBeCreatedDepartmentNames = new ArrayList<>();
		final String scopedFieldName = namedResourceMetadata.getScopedAttributeNames().get(0).getName();

		for (final String departmentName : configuredDepartmentNames) {
			if (genericRepository.doesExist(Department.class,
					(root, cq, builder) -> builder.equal(root.get(scopedFieldName), departmentName), session)) {
				continue;
			}

			toBeCreatedDepartmentNames.add(departmentName);
		}

		return toBeCreatedDepartmentNames;
	}

	/**
	 * @return
	 */
	private List<String> getConfiguredDepartmentNames(Environment env) {
		return List.of(env.getRequiredProperty("multicados.department.scoped").split(StringHelper.COMMA));
	}

	@Override
	@Transactional
	public void hook(ApplicationContext context) throws Exception {
		final List<String> configuredDepartmentNames = getConfiguredDepartmentNames(env);

		if (logger.isDebugEnabled()) {
			logger.debug("Found {}", StringHelper.join(configuredDepartmentNames));
		}

		final Session session = sessionFactory.getCurrentSession();
		// @formatter:off
		Utils.declare(configuredDepartmentNames, session, resourceContext.getMetadata(Department.class))
			.then(this::locateToBeCreatedDepartmentNames)
				.second(session)
			.consume(this::createDepartments);
		// @formatter:on
		session.flush();
	}

	private void createDepartments(List<String> departmentNames, Session session) throws Exception {
		for (final String departmentName : departmentNames) {
			final Department newDepartment = new Department();

			newDepartment.setName(departmentName);

			final ServiceResult result = crudService.create(Department.class, null, newDepartment, session, false);

			if (!result.isOk()) {
				if (result.getException() != null) {
					throw result.getException();
				}

				throw new IllegalStateException(
						String.format("Unable to create department %s with unknown error", departmentName));
			}

			if (logger.isDebugEnabled()) {
				logger.debug("Creating department name {}", departmentName);
			}
		}
	}

}
