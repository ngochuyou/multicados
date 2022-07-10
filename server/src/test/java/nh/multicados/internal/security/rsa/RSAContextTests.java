/**
 * 
 */
package nh.multicados.internal.security.rsa;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Base64;
import java.util.UUID;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import multicados.internal.security.rsa.RSAContext;

/**
 * @author Ngoc Huy
 *
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { RSAContextConfiguration.class })
@TestPropertySource("classpath:application.properties")
public class RSAContextTests {

	@Autowired
	private RSAContext rsaContext;

	@Test
	public void testRSASignature() throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
			IllegalBlockSizeException, BadPaddingException, SignatureException {
		final UUID uuid = UUID.randomUUID();

		final Cipher rsaEncryptCipher = getRSAEncryptCipher();
		final byte[] encryptedUUID = rsaEncryptCipher.doFinal(uuid.toString().getBytes());
		final Signature signatureForSigning = getRHA256WithRSASignatureForSigning();

		signatureForSigning.update(encryptedUUID);

		final String signedUUID = Base64.getEncoder().encodeToString(encryptedUUID);

		final Signature signatureForVerify = getRHA256WithRSASignatureForVerify();
		final byte[] toBeVerifiedUUID = Base64.getDecoder().decode(signedUUID);

		signatureForVerify.verify(toBeVerifiedUUID);

		final Cipher rsaDecryptCipher = getRSADecryptCipher();

		assertTrue(new String(rsaDecryptCipher.doFinal(toBeVerifiedUUID)).equals(uuid.toString()));
	}

	private Signature getRHA256WithRSASignature() throws NoSuchAlgorithmException {
		final Signature signature = Signature.getInstance("SHA256withRSA");

		return signature;
	}

	private Signature getRHA256WithRSASignatureForSigning() throws NoSuchAlgorithmException, InvalidKeyException {
		final Signature signature = getRHA256WithRSASignature();

		signature.initSign(rsaContext.getPrivateKey());
		
		return signature;
	}

	private Signature getRHA256WithRSASignatureForVerify() throws NoSuchAlgorithmException, InvalidKeyException {
		final Signature signature = getRHA256WithRSASignature();

		signature.initVerify(rsaContext.getPublicKey());

		return signature;
	}

	private Cipher getRSADecryptCipher() throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
		final Cipher cipher = getRSACipher();

		cipher.init(Cipher.DECRYPT_MODE, rsaContext.getPrivateKey());

		return cipher;
	}

	private Cipher getRSAEncryptCipher() throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
		final Cipher cipher = getRSACipher();

		cipher.init(Cipher.ENCRYPT_MODE, rsaContext.getPublicKey());

		return cipher;
	}

	private Cipher getRSACipher() throws NoSuchAlgorithmException, NoSuchPaddingException {
		final Cipher cipher = Cipher.getInstance("RSA");
		return cipher;
	}

}
