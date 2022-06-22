/**
 * 
 */
package multicados.controller.controllers;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import multicados.controller.query.CategoryQuery;
import multicados.internal.helper.SpringHelper;
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

	@Autowired
	public RestCategoryController(GenericCRUDServiceImpl crudService, SessionFactory sessionFactory) {
		this.crudService = crudService;
		this.sessionFactory = sessionFactory;
	}

	@GetMapping
	@Transactional(readOnly = true)
	public ResponseEntity<?> getCategories(CategoryQuery query, Authentication authentication) throws Exception {
		return ResponseEntity.ok(crudService.readAll(query,
				SpringHelper.getUserDetails(authentication).getCRUDAuthority(), sessionFactory.getCurrentSession()));
	}

}
