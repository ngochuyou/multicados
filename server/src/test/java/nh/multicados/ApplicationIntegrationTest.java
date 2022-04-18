/**
 * 
 */
package nh.multicados;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import multicados.domain.entity.Role;
import multicados.domain.entity.entities.Province;
import multicados.domain.entity.entities.Province_;
import multicados.internal.config.WebConfiguration;
import multicados.internal.helper.HibernateHelper;
import multicados.internal.service.crud.GenericCRUDServiceImpl;
import multicados.internal.service.crud.security.CRUDCredentialImpl;

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

	private static final Logger logger = LoggerFactory.getLogger(ApplicationIntegrationTest.class);

	@Autowired
	private GenericCRUDServiceImpl crudService;

	@Test
	@Transactional
	public void test() throws Exception {
		Pageable page = PageRequest.of(0, 50, Sort.by(Province_.ID).descending());
		List<Map<String, Object>> rows = crudService.readAll(Province.class,
				List.of(Province_.ID, Province_.NAME, Province_.ACTIVE),
				(root, query, builder) -> builder.and(builder.like(root.get(Province_.name), "%Ha%"),
						builder.greaterThan(root.get(Province_.id), 20)),
				page, new CRUDCredentialImpl(Role.CUSTOMER.toString()), HibernateHelper.getCurrentSession());

		for (Map<String, Object> row : rows) {
			logger.info(row.entrySet().stream().map(Map.Entry::getValue).map(Object::toString)
					.collect(Collectors.joining("\t")));
		}
	}

}
