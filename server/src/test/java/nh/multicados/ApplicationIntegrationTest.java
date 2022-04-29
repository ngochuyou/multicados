/**
 * 
 */
package nh.multicados;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import multicados.domain.entity.Role;
import multicados.domain.entity.entities.District;
import multicados.domain.entity.entities.District_;
import multicados.domain.entity.entities.Province;
import multicados.domain.entity.entities.Province_;
import multicados.internal.config.WebConfiguration;
import multicados.internal.helper.HibernateHelper;
import multicados.internal.security.CredentialException;
import multicados.internal.service.crud.GenericCRUDServiceImpl;
import multicados.internal.service.crud.rest.RestQuery;
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

	@SuppressWarnings("unchecked")
	@Transactional
	@Test
	public void test() throws CredentialException, UnknownAttributesException, Exception {
		Map<String, Object> row = genericCRUDServiceImpl.read(new RestQuery<District>() {

			@Override
			public Class<District> getResourceType() {
				return District.class;
			}

			@Override
			public List<String> getProperties() {
				return List.of(District_.ID, District_.NAME, District_.ACTIVE);
			}

			@Override
			public List<RestQuery<?>> getQueries() {
				return List.of(new RestQuery<Province>() {

					@Override
					public Class<Province> getResourceType() {
						return Province.class;
					}

					@Override
					public String getName() {
						return District_.PROVINCE;
					}

					@Override
					public List<String> getProperties() {
						return List.of(Province_.ID, Province_.NAME, Province_.ACTIVE);
					}

					@Override
					public List<RestQuery<?>> getQueries() {
						return null;
					}

					@Override
					public Specification<Province> getSpecification() {
						return null;
					}

					@Override
					public Pageable getPageable() {
						return PageRequest.ofSize(5);
					}
				});
			}

			@Override
			public Specification<District> getSpecification() {
				return null;
			}

			@Override
			public Pageable getPageable() {
				return null;
			}

			@Override
			public String getName() {
				return null;
			}

		}, new CRUDCredentialImpl(Role.HEAD.toString()), HibernateHelper.getCurrentSession());

		for (Entry<String, Object> entry : row.entrySet()) {
			Object value = entry.getValue();

			if (!Map.class.isAssignableFrom(value.getClass())) {
				System.out.println(String.format("%s:\t%s", entry.getKey(), value));
				continue;
			}

			for (Entry<String, Object> innerEntry : ((Map<String, Object>) value).entrySet()) {
				System.out.println(String.format("\t\t%s:\t%s", innerEntry.getKey(), innerEntry.getValue()));
			}
		}
	}

}
