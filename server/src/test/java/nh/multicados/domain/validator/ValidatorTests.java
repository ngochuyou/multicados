/**
 * 
 */
package nh.multicados.domain.validator;

import static org.junit.Assert.assertTrue;

import org.junit.jupiter.api.Test;

import multicados.domain.entity.entities.Category;
import multicados.domain.entity.entities.Category_;
import multicados.domain.validator.CategoryValidator;
import multicados.internal.domain.validation.Validation;

/**
 * @author Ngoc Huy
 *
 */
public class ValidatorTests {

	private Category getCategoryWithScriptInDescription() {
		final Category category = new Category();

		category.setDescription("<script>document.cookies['_abcdef']</script>");

		return category;
	}
	
	private Category getCategoryWithAcceptableCharactersInDescription() {
		final Category category = new Category();

//		category.setDescription("In fashion, an accessory is an item used to contribute, in a secondary manner, to an individual's outfit. Accessories are often chosen to complete an outfit and complement the wearer's look.");
//		category.setDescription("Formal wear, formal attire or full dress is the traditional Western dress code category applicable for the most formal occasions, such as weddings, christenings...");
//		category.setDescription("Sportswear or activewear is clothing, including footwear, worn for sport or physical exercise. Sport-specific clothing is worn for most sports and physical exercise, for practical, comfort or safety reasons.");
//		category.setDescription("A shirt is a cloth garment for the upper body. Many terms are used to describe and differentiate types of shirts (and upper-body garments in general) and their construction.");
//		category.setDescription("Trousers (British English), slacks, or pants are an item of clothing that might have originated in Central Asia, worn from the waist to the ankles, covering both legs separately.");
		category.setDescription("Eyewear consists of items and accessories worn on or over the eyes, for fashion or adornment, protection against the environment, and to improve or enhance visual acuity.");
		
		return category;
	}

	@Test
	void testCategoryValidator() throws Exception {
		final CategoryValidator validator = new CategoryValidator();
		Validation result = validator.isSatisfiedBy(getCategoryWithScriptInDescription());

		assertTrue("Injection attack breached", !result.isOk());
		System.out.println(result.getErrors().get(Category_.DESCRIPTION).getMessage());
		result = validator.isSatisfiedBy(getCategoryWithAcceptableCharactersInDescription());
		
		assertTrue("Failed to accept", result.isOk());
	}

}
