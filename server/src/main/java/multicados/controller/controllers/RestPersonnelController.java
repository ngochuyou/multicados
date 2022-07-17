/**
 * 
 */
package multicados.controller.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import multicados.domain.entity.entities.Personnel;

/**
 * @author Ngoc Huy
 *
 */
@RestController
@RequestMapping("/personnel")
public class RestPersonnelController extends AbstractController {

	@PostMapping
	public ResponseEntity<?> createPersonnel(Personnel personnel) {
		return null;
	}

}
