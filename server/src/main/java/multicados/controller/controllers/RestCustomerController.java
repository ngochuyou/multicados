/**
 * 
 */
package multicados.controller.controllers;

import javax.transaction.Transactional;

import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import multicados.domain.entity.entities.Customer;
import multicados.internal.service.crud.GenericCRUDServiceImpl;

/**
 * @author Ngoc Huy
 *
 */
@Controller
@RequestMapping("/rest/customer")
public class RestCustomerController extends AbstractController {

	private final GenericCRUDServiceImpl crudService;
	private final SessionFactory sessionFactory;

	@Autowired
	public RestCustomerController(GenericCRUDServiceImpl crudService, SessionFactory sessionFactory) {
		this.crudService = crudService;
		this.sessionFactory = sessionFactory;
	}

	@PostMapping
	@Transactional
	public ResponseEntity<?> createCustomer(@RequestBody Customer newCustomer) throws HibernateException, Exception {
		// @formatter:off
		return sendResult(
				crudService.create(
						newCustomer.getId(),
						newCustomer,
						Customer.class,
						sessionFactory.getCurrentSession(),
						true),
				newCustomer);
		// @formatter:on
	}

}
