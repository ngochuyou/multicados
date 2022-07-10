/**
 * 
 */
package multicados.internal.security.rsa;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import multicados.internal.config.Settings;

/**
 * Resolves the keys regarding the following specifications:
 * <ul>
 * <li>1. Keys are in .der format</li>
 * <li>2. Key paths are resolves from ClassPath using system
 * {@link ClassLoader}</li>
 * </ul>
 * 
 * @author Ngoc Huy
 *
 */
@Component
public class RSAContextImpl implements RSAContext {

	private final PrivateKey privateKey;
	private final PublicKey publicKey;

	public RSAContextImpl(Environment env)
			throws NoSuchAlgorithmException, InvalidKeySpecException, URISyntaxException, IOException {
		final KeyFactory rsaKeyFactory = KeyFactory.getInstance("RSA");

		privateKey = resolvePrivate(env, rsaKeyFactory);
		publicKey = resolvePublic(env, rsaKeyFactory);
	}

	private PrivateKey resolvePrivate(Environment env, KeyFactory rsaKeyFactory)
			throws URISyntaxException, IOException, InvalidKeySpecException {
		final String keyPath = env.getRequiredProperty(Settings.SECURITY_RSA_PRIVATE_KEY_PATH);
		final URL url = ClassLoader.getSystemResource(keyPath);

		if (url == null) {
			throw new IllegalArgumentException(String.format("Unable to locate private key with path %s", keyPath));
		}

		final File file = new File(url.toURI());
		final byte[] privateKeyBytes = Files.readAllBytes(file.toPath());
		final PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(privateKeyBytes);

		return rsaKeyFactory.generatePrivate(spec);
	}

	private PublicKey resolvePublic(Environment env, KeyFactory rsaKeyFactory)
			throws IOException, URISyntaxException, InvalidKeySpecException {
		final String keyPath = env.getRequiredProperty(Settings.SECURITY_RSA_PUBLIC_KEY_PATH);
		final URL url = ClassLoader.getSystemResource(keyPath);

		if (url == null) {
			throw new IllegalArgumentException(String.format("Unable to locate private key with path %s", keyPath));
		}

		final File file = new File(url.toURI());
		final byte[] publicKeyBytes = Files.readAllBytes(file.toPath());
		final X509EncodedKeySpec spec = new X509EncodedKeySpec(publicKeyBytes);

		return rsaKeyFactory.generatePublic(spec);
	}

	@Override
	public PrivateKey getPrivateKey() {
		return privateKey;
	}

	@Override
	public PublicKey getPublicKey() {
		return publicKey;
	}

}
