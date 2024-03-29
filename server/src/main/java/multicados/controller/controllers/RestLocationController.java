/**
 *
 */
package multicados.controller.controllers;

import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import multicados.controller.query.DistrictQuery;
import multicados.controller.query.ProvinceQuery;
import multicados.internal.helper.SpringHelper;
import multicados.internal.security.FixedAnonymousAuthenticationToken;
import multicados.internal.service.crud.GenericCRUDServiceImpl;

/**
 * @author Ngoc Huy
 *
 */
@RestController
@RequestMapping("/rest/location")
public class RestLocationController extends AbstractController {

	private final SessionFactory sessionFactory;
	private final GenericCRUDServiceImpl crudService;

	private final FixedAnonymousAuthenticationToken anonymousToken;

	public RestLocationController(SessionFactory sessionFactory, GenericCRUDServiceImpl crudService,
			FixedAnonymousAuthenticationToken anonymousToken) {
		this.sessionFactory = sessionFactory;
		this.crudService = crudService;
		this.anonymousToken = anonymousToken;
	}

	@GetMapping("/district")
	@Transactional(readOnly = true)
	public ResponseEntity<?> getDistricts(DistrictQuery districtQuery, Authentication authentication)
			throws HibernateException, Exception {
		return ResponseEntity.ok(crudService.readAll(districtQuery,
				SpringHelper.getUserDetails(authentication, anonymousToken.getPrincipal()).getCRUDAuthority(),
				sessionFactory.getCurrentSession()));
	}

	@GetMapping("/province")
	@Transactional(readOnly = true)
	public ResponseEntity<?> getProvices(ProvinceQuery provinceQuery, Authentication authentication)
			throws HibernateException, Exception {
		return ResponseEntity.ok(crudService.readAll(provinceQuery,
				SpringHelper.getUserDetails(authentication, anonymousToken.getPrincipal()).getCRUDAuthority(),
				sessionFactory.getCurrentSession()));
	}

}
