/**
 * 
 */
package nh.multicados;

import java.util.List;
import java.util.Map;

import javax.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import multicados.controller.rest.DistrictRestQuery;
import multicados.controller.rest.ProvinceRestQuery;
import multicados.domain.entity.Role;
import multicados.domain.entity.entities.District_;
import multicados.domain.entity.entities.Province_;
import multicados.internal.config.WebConfiguration;
import multicados.internal.helper.HibernateHelper;
import multicados.internal.security.CredentialException;
import multicados.internal.service.crud.GenericCRUDServiceImpl;
import multicados.internal.service.crud.security.CRUDCredentialImpl;
import multicados.internal.service.crud.security.read.UnknownAttributesException;

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

	@Autowired
	private GenericCRUDServiceImpl genericCRUDServiceImpl;

	@Transactional
	@Test
	public void test() throws CredentialException, UnknownAttributesException, Exception {
		DistrictRestQuery restQuery = new DistrictRestQuery();

		restQuery.setAttributes(List.of(District_.ID, District_.NAME, District_.ACTIVE));

		ProvinceRestQuery provinceRestQuery = new ProvinceRestQuery();

		provinceRestQuery.setAttributes(List.of(Province_.ID, District_.NAME));
		restQuery.setProvince(provinceRestQuery);

		List<Map<String, Object>> map = genericCRUDServiceImpl.readAll(restQuery,
				new CRUDCredentialImpl(Role.HEAD.toString()), HibernateHelper.getCurrentSession());
		map.get(0);
		return;
//
//			for (Entry<String, Object> entry : row.entrySet()) {
//				Object value = entry.getValue();
//
//				if (!Map.class.isAssignableFrom(value.getClass())) {
//					System.out.println(String.format("%s:\t%s", entry.getKey(), value));
//					continue;
//				}
//
//				for (Entry<String, Object> innerEntry : ((Map<String, Object>) value).entrySet()) {
//					System.out.println(String.format("\t\t%s:\t%s", innerEntry.getKey(), innerEntry.getValue()));
//				}
//			}
//		}
	}

}
