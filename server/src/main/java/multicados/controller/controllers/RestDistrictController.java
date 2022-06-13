/**
 * 
 */
package multicados.controller.controllers;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import multicados.controller.rest.RestDistrictQuery;
import multicados.domain.entity.Role;
import multicados.internal.context.ContextManager;
import multicados.internal.service.crud.GenericCRUDServiceImpl;

/**
 * @author Ngoc Huy
 *
 */
@RequestMapping("/rest/district")
@RestController
public class RestDistrictController {

	private final SessionFactory sessionFactory;

	@Autowired
	public RestDistrictController(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@GetMapping
	@Transactional
	public ResponseEntity<?> getDistrict(RestDistrictQuery query) throws Exception {
		GenericCRUDServiceImpl crudService = ContextManager.getBean(GenericCRUDServiceImpl.class);

		return ResponseEntity.ok(crudService.readAll(query, new SimpleGrantedAuthority(Role.HEAD.toString()),
				sessionFactory.getCurrentSession()));
	}

}
