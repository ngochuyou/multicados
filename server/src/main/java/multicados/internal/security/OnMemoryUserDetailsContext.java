/**
 *
 */
package multicados.internal.security;

import org.springframework.security.core.userdetails.UserDetails;

/**
 * @author Ngoc Huy
 *
 */
public interface OnMemoryUserDetailsContext {

	void put(UserDetails userDetails);

	UserDetails get(String username);

}
