/**
 *
 */
package nh.multicados;

import static nh.multicados.UnitTest.Bob.publicKey;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 * @author Ngoc Huy
 *
 */
public class UnitTest {

	private static final Bob BOB = new Bob();
	private static final Alice ALICE = new Alice();

	public static void main(String[] args)
			throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, SignatureException {
		BOB.send(ALICE, "Hello Alice");
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

		static {
			final KeyPairGenerator generator;

			try {
				generator = KeyPairGenerator.getInstance("RSA");
				generator.initialize(1024);

				final KeyPair keyPair = generator.generateKeyPair();

				privateKey = keyPair.getPrivate();
				publicKey = keyPair.getPublic();
			} catch (NoSuchAlgorithmException any) {
				throw new IllegalArgumentException();
			}
		}

		private void send(Alice alice, String message)
				throws IllegalBlockSizeException, BadPaddingException, InvalidKeyException, SignatureException {
			signature.initSign(privateKey);
			signature.update(message.getBytes());

			alice.receive(new String(signature.sign()));
		}

		private void receive(String base64Message) throws IllegalBlockSizeException, BadPaddingException {
			System.out.println(new String(Base64.getDecoder().decode(base64Message), StandardCharsets.UTF_8));
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

		private void receive(String message)
				throws IllegalBlockSizeException, BadPaddingException, InvalidKeyException, SignatureException {
			signature.initVerify(publicKey);
			
			System.out.println(String.format("%s\n%s", message, signature.verify(message.getBytes())));
		}

	}

}
