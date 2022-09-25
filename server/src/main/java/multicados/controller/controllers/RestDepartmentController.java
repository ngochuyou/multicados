/**
 * 
 */
package multicados.controller.controllers;

import javax.transaction.Transactional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import multicados.controller.mapping.Mapping;

/**
 * @author Ngoc Huy
 *
 */
@RestController
@RequestMapping(Mapping.Endpoint.DEPARTMENT)
public class RestDepartmentController extends AbstractController {

	@PostMapping
	@Transactional
	public ResponseEntity<?> createDepartment() {
		return null;
	}

}
