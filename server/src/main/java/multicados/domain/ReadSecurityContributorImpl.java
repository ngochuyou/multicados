/**
 * 
 */
package multicados.domain;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import multicados.domain.entity.PermanentEntity;
import multicados.domain.entity.PermanentEntity_;
import multicados.domain.entity.Role;
import multicados.domain.entity.entities.Category;
import multicados.domain.entity.entities.Personnel;
import multicados.internal.service.crud.security.read.ReadSecurityContributor;
import multicados.internal.service.crud.security.read.ReadSecurityManagerImpl.CRUDSecurityManagerBuilder;

/**
 * @author Ngoc Huy
 *
 */
public class ReadSecurityContributorImpl implements ReadSecurityContributor {

	@Override
	public void contribute(CRUDSecurityManagerBuilder builder) {
		// @formatter:off
		builder.type(PermanentEntity.class)
			.credentials(of(Role.HEAD), of(Role.PERSONNEL), of(Role.CUSTOMER), of(Role.ANONYMOUS))
				.attributes(PermanentEntity_.ACTIVE).mask();

		builder.type(Personnel.class)
			.credentials(of(Role.HEAD))
				.publish()
			.credentials(of(Role.ANONYMOUS), of(Role.CUSTOMER), of(Role.PERSONNEL))
				.mask();
		
		builder.type(Category.class)
			.credentials(of(Role.HEAD), of(Role.PERSONNEL), of(Role.CUSTOMER), of(Role.ANONYMOUS))
				.but(PermanentEntity_.ACTIVE).publish();
		// @formatter:on
	}

	private GrantedAuthority of(Role role) {
		return of(role.name());
	}

	private GrantedAuthority of(String val) {
		return new SimpleGrantedAuthority(val);
	}

}
