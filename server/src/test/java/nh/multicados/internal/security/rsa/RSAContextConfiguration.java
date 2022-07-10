/**
 * 
 */
package nh.multicados.internal.security.rsa;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import multicados.internal.security.rsa.RSAContext;
import multicados.internal.security.rsa.RSAContextImpl;

/**
 * @author Ngoc Huy
 *
 */
@Configurable
public class RSAContextConfiguration {

	@Bean
	public RSAContext rsaContext(Environment env)
			throws NoSuchAlgorithmException, InvalidKeySpecException, URISyntaxException, IOException {
		return new RSAContextImpl(env);
	}

}
