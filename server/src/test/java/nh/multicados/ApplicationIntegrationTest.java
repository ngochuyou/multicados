/**
 * 
 */
package nh.multicados;

import javax.persistence.Tuple;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;

import org.hibernate.Session;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import multicados.domain.entity.entities.District;
import multicados.domain.entity.entities.District_;
import multicados.domain.entity.entities.Province;
import multicados.domain.entity.entities.Province_;
import multicados.internal.config.WebConfiguration;
import multicados.internal.domain.tuplizer.TuplizerException;
import multicados.internal.helper.HibernateHelper;
import multicados.internal.helper.StringHelper;

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

	@Test
	@Transactional
	public void test() throws TuplizerException {
		Session session = HibernateHelper.getCurrentSession();
		CriteriaBuilder builder = HibernateHelper.getSessionFactory().getCriteriaBuilder();
		CriteriaQuery<Tuple> cq = builder.createTupleQuery();
		Root<District> root = cq.from(District.class);
		Join<District, Province> join = root.join(District_.province);

		cq.multiselect(root.get(District_.id), root.get(District_.name), join.get(Province_.name));

		session.createQuery(cq).getResultStream().forEach(
				tuple -> System.out.println(String.format(StringHelper.COMMON_JOINER, tuple.get(0), tuple.get(1))));
	}

}
