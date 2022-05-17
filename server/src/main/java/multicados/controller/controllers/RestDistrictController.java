/**
 * 
 */
package multicados.controller.controllers;

import org.hibernate.Session;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import multicados.controller.rest.RestDistrictQuery;
import multicados.domain.entity.Role;
import multicados.internal.context.ContextManager;
import multicados.internal.helper.HibernateHelper;
import multicados.internal.service.crud.GenericCRUDServiceImpl;
import multicados.internal.service.crud.security.CRUDCredentialImpl;

/**
 * @author Ngoc Huy
 *
 */
@RequestMapping("/rest/district")
@RestController
public class RestDistrictController {

	@GetMapping
	@Transactional
	public ResponseEntity<?> getDistrict(RestDistrictQuery query) throws Exception {
		GenericCRUDServiceImpl crudService = ContextManager.getBean(GenericCRUDServiceImpl.class);
		Session currentSession = HibernateHelper.getCurrentSession();
		
		currentSession.beginTransaction();
		
		return ResponseEntity.ok(crudService.readAll(query, new CRUDCredentialImpl(Role.HEAD.toString()),
				currentSession));
	}

}
