/**
 * 
 */
package nh.multicados;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import multicados.domain.entity.entities.Personnel;
import multicados.internal.config.WebConfiguration;
import multicados.internal.domain.DomainResourceContext;
import multicados.internal.domain.tuplizer.DomainResourceTuplizer;
import multicados.internal.domain.tuplizer.TuplizerException;

/**
 * @author Ngoc Huy
 *
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = ApplicationIntegrationTest.class)
//@TestPropertySource(locations = "classpath:application-test.properties")
@ContextConfiguration(classes = { WebConfiguration.class })
@AutoConfigureMockMvc
public class ApplicationIntegrationTest {

	private final DomainResourceContext resourceContext;

	@Autowired
	public ApplicationIntegrationTest(DomainResourceContext resourceContext) {
		this.resourceContext = resourceContext;
	}

	@Test
	public void testTuplizer() throws TuplizerException {
		DomainResourceTuplizer<Personnel> tuplizer = resourceContext.getTuplizer(Personnel.class);
		LocalDateTime now = LocalDateTime.now();
		Personnel personnel = new Personnel();

		tuplizer.setProperty(personnel, "createdTimestamp", now);

		assertTrue(tuplizer.getProperty(personnel, "createdTimestamp").equals(now), "You suck");
	}

}
