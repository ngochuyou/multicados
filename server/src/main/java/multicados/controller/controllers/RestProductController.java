/**
 * 
 */
package multicados.controller.controllers;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import multicados.domain.entity.entities.Product;

/**
 * @author Ngoc Huy
 *
 */
@RestController
@RequestMapping("/rest/product")
public class RestProductController extends AbstractController {

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Secured({ HEAD })
	public ResponseEntity<?> createProduct(@RequestPart("model") Product model,
			@RequestPart(name = "images", required = false) MultipartFile[] images) {
		return null;
	}

}
