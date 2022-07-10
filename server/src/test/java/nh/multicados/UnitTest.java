/**
 *
 */
package nh.multicados;

import static nh.multicados.UnitTest.Bob.publicKey;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.UUID;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 * @author Ngoc Huy
 *
 */
public class UnitTest {

	public static void main(String[] args) throws InvalidKeyException, NoSuchAlgorithmException, URISyntaxException,
			IOException, InvalidKeySpecException {
		System.out.println(UUID.randomUUID().toString().matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"));
	}

	public static final Signature signature;

	static {
		try {
			signature = Signature.getInstance("SHA256withRSA");
		} catch (NoSuchAlgorithmException any) {
			throw new IllegalArgumentException(any);
		}
	}

	public static class Bob {

		public static final PrivateKey privateKey;
		public static final PublicKey publicKey;
		private static final Cipher cipher;

		static {
			final KeyPairGenerator generator;

			try {
				generator = KeyPairGenerator.getInstance("RSA");
				generator.initialize(1024);

				final KeyPair keyPair = generator.generateKeyPair();

				privateKey = keyPair.getPrivate();
				publicKey = keyPair.getPublic();

				cipher = Cipher.getInstance("RSA");

				cipher.init(Cipher.DECRYPT_MODE, privateKey);
			} catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException any) {
				throw new IllegalArgumentException();
			}
		}

		@SuppressWarnings("unused")
		private void send(Alice alice, String message)
				throws IllegalBlockSizeException, BadPaddingException, InvalidKeyException, SignatureException {
//			final byte[] cipherBytes = cipher.doFinal(message.getBytes());

			signature.initSign(privateKey);
			signature.update(message.getBytes());

			alice.receive(Base64.getEncoder().encodeToString(signature.sign()));
		}

		private void receive(String base64Message) throws IllegalBlockSizeException, BadPaddingException {
			final byte[] messageBytes = cipher.doFinal(Base64.getDecoder().decode(base64Message));

			System.out.println(new String(messageBytes, StandardCharsets.UTF_8));
		}

	}

	public static class Alice {

		private static final Cipher cipher;

		static {
			try {
				cipher = Cipher.getInstance("RSA");

				cipher.init(Cipher.ENCRYPT_MODE, publicKey);
			} catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException any) {
				throw new IllegalArgumentException();
			}
		}

		public void send(Bob bob, String message) throws IllegalBlockSizeException, BadPaddingException {
			final byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);

			bob.receive(Base64.getEncoder().encodeToString(cipher.doFinal(messageBytes)));
		}

		private void receive(String base64Message)
				throws IllegalBlockSizeException, BadPaddingException, InvalidKeyException, SignatureException {
			final byte[] encryptedBytes = Base64.getDecoder().decode(base64Message);

			signature.initVerify(publicKey);
			signature.update("can you catch me".getBytes());

			System.out.println(signature.verify(encryptedBytes));
		}

	}

}
