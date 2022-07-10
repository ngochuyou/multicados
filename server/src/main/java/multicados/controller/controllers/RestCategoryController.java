/**
 *
 */
package multicados.controller.controllers;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import multicados.controller.query.CategoryQuery;
import multicados.domain.entity.entities.Category;
import multicados.internal.helper.SpringHelper;
import multicados.internal.security.FixedAnonymousAuthenticationToken;
import multicados.internal.service.crud.GenericCRUDServiceImpl;

/**
 * @author Ngoc Huy
 *
 */
@RestController
@RequestMapping("/rest/category")
public class RestCategoryController extends AbstractController {

	private final GenericCRUDServiceImpl crudService;
	private final SessionFactory sessionFactory;

	private final FixedAnonymousAuthenticationToken anonymousToken;

	@Autowired
	public RestCategoryController(GenericCRUDServiceImpl crudService, SessionFactory sessionFactory,
			FixedAnonymousAuthenticationToken anonymousToken) {
		this.crudService = crudService;
		this.sessionFactory = sessionFactory;
		this.anonymousToken = anonymousToken;
	}

	@GetMapping
	@Transactional(readOnly = true)
	public ResponseEntity<?> getCategories(CategoryQuery query, Authentication authentication) throws Exception {
		final Session session = sessionFactory.getCurrentSession();

		return ResponseEntity.ok(crudService.readAll(query,
				SpringHelper.getUserDetails(authentication, anonymousToken.getPrincipal()).getCRUDAuthority(),
				session));
	}

	@PostMapping
	@Transactional
	public ResponseEntity<?> createCategory(@RequestBody Category category, HttpServletRequest request)
			throws Exception {
		return sendResult(crudService.create(Category.class, null, category, sessionFactory.getCurrentSession(), true),
				category, request);
	}

	@PatchMapping
	@Transactional
	public ResponseEntity<?> patchCategory(@RequestBody Category category, HttpServletRequest request)
			throws Exception {
		return sendResult(crudService.update(Category.class, null, category, sessionFactory.getCurrentSession(), true),
				category, request);
	}

}
