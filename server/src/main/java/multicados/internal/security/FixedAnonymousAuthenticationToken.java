/**
 * 
 */
package multicados.internal.security;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import multicados.security.userdetails.UserDetailsServiceImpl.DomainUser;

/**
 * @author Ngoc Huy
 *
 */
@Component
public class FixedAnonymousAuthenticationToken extends AnonymousAuthenticationToken {

	private static final long serialVersionUID = 1L;

	private static final DomainUser ANONYMOUS_DETAILS = new DomainUser("ANONYMOUS", "ANONYMOUS", false, false,
			LocalDateTime.now(), List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));

	public FixedAnonymousAuthenticationToken() {
		super(ANONYMOUS_DETAILS.getUsername(), ANONYMOUS_DETAILS, ANONYMOUS_DETAILS.getAuthorities());
	}

	@Override
	public DomainUserDetails getPrincipal() {
		return (DomainUserDetails) super.getPrincipal();
	}

}
