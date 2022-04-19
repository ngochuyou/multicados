/**
 * 
 */
package nh.multicados;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.hibernate.Session;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import multicados.domain.entity.AuditInformations_;
import multicados.domain.entity.Role;
import multicados.domain.entity.entities.Personnel;
import multicados.domain.entity.entities.Personnel_;
import multicados.internal.config.WebConfiguration;
import multicados.internal.helper.HibernateHelper;
import multicados.internal.service.crud.rest.RestQuery;
import multicados.internal.service.crud.rest.RestQueryFulfiller;
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
	private RestQueryFulfiller<Map<String, Object>, Session> fulfiller;

	@Test
	@Transactional
	public void test() throws Exception {
		RestQuery<Personnel> query = new RestQuery<Personnel>() {

			@Override
			public Class<Personnel> getResourceType() {
				return Personnel.class;
			}

			@Override
			public boolean isEmpty() {
				return false;
			}

			@Override
			public List<String> getColumns() {
				return List.of(Personnel_.ID, AuditInformations_.CREATED_TIMESTAMP,
						AuditInformations_.UPDATED_TIMESTAMP, AuditInformations_.CREATOR, AuditInformations_.UPDATER);
			}

			@Override
			public List<RestQuery<?>> getAssociations() {
				return null;
			}

			@Override
			public Pageable getPageable() {
				return Pageable.ofSize(10);
			}
		};

		for (Map<String, Object> row : fulfiller.readAll(query, new CRUDCredentialImpl(Role.HEAD.toString()),
				HibernateHelper.getCurrentSession())) {
			logger.info(row.entrySet().stream().map(Map.Entry::getValue).map(Object::toString)
					.collect(Collectors.joining("\t")));
		}
	}

}
