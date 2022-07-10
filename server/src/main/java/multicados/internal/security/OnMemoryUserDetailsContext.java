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

	boolean contains(String username);
	
	void put(UserDetails userDetails);

	UserDetails get(String username);

	void remove(String username);
	
}
