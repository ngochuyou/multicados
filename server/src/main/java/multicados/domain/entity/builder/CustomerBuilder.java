/**
 * 
 */
package multicados.domain.entity.builder;

import java.util.Optional;

import javax.persistence.EntityManager;

import multicados.domain.entity.entities.Customer;
import multicados.internal.domain.annotation.For;
import multicados.internal.domain.builder.AbstractDomainResourceBuilder;

/**
 * @author Ngoc Huy
 *
 */
@For(Customer.class)
public class CustomerBuilder extends AbstractDomainResourceBuilder<Customer> {

	private Customer doMandatory(Customer model, Customer persistence) {
		persistence.setSubscribed(Optional.ofNullable(model.isSubscribed()).orElse(Boolean.FALSE));

		return persistence;
	}

	@Override
	public Customer buildInsertion(Customer persistence, EntityManager entityManager) throws Exception {
		return doMandatory(persistence, persistence);
	}

	@Override
	public Customer buildUpdate(Customer model, Customer persistence, EntityManager entityManger) {
		return doMandatory(model, persistence);
	}

}
