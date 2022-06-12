/**
 * 
 */
package multicados.internal.security;

import java.time.LocalDateTime;

import org.springframework.security.core.userdetails.UserDetails;

/**
 * @author Ngoc Huy
 *
 */
public interface DomainUserDetails extends UserDetails {

	LocalDateTime getVersion();

}
