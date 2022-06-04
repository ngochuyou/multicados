/**
 * 
 */
package multicados.domain;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import multicados.domain.entity.Role;
import multicados.domain.entity.entities.Personnel;
import multicados.domain.entity.entities.Province;
import multicados.domain.entity.entities.Province_;
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
		builder
			.type(Province.class)
			.credentials(make(Role.CUSTOMER.toString()))
				.attributes(Province_.ACTIVE).mask()
				.others().publish()
			.credentials(make(Role.HEAD.toString()))
				.publish();

		builder
			.type(Personnel.class)
			.credentials(make(Role.HEAD.toString())).publish();
		// @formatter:on
	}

	private GrantedAuthority make(String val) {
		return new SimpleGrantedAuthority(val);
	}

}
