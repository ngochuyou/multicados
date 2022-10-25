/**
 * 
 */
package multicados.service.domain.customer;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.mail.MessagingException;
import javax.persistence.EntityNotFoundException;
import javax.persistence.LockModeType;
import javax.persistence.Tuple;
import javax.servlet.http.Cookie;

import org.hibernate.Session;
import org.hibernate.SharedSessionContract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import multicados.domain.entity.PermanentEntity_;
import multicados.domain.entity.entities.Customer;
import multicados.domain.entity.entities.User;
import multicados.domain.entity.entities.User_;
import multicados.domain.entity.entities.customer.CredentialReset;
import multicados.domain.entity.entities.customer.CredentialReset_;
import multicados.internal.config.ExecutorNames;
import multicados.internal.config.Settings;
import multicados.internal.domain.repository.GenericRepository;
import multicados.internal.helper.HibernateHelper;
import multicados.internal.helper.HttpHelper;
import multicados.internal.helper.StringHelper;
import multicados.internal.helper.Utils;
import multicados.internal.helper.Utils.BiDeclaration;
import multicados.internal.helper.oauth.HOTP;
import multicados.internal.security.OnMemoryUserDetailsContext;
import multicados.internal.security.rsa.RSAContext;
import multicados.internal.service.ServicePayload;
import multicados.internal.service.ServiceResult;
import multicados.internal.service.crud.GenericCRUDServiceImpl;
import multicados.service.mail.MailProvider;

/**
 * @author Ngoc Huy
 *
 */
@Service
public class CredentialResetService {

	private static final Logger logger = LoggerFactory.getLogger(CredentialResetService.class);

	private static final String RSA_CIPHER_INSTANCE_NAME = "RSA/ECB/OAEPWITHSHA-256ANDMGF1PADDING";

	private final GenericCRUDServiceImpl crudService;
	private final GenericRepository genericRepository;
	private final OnMemoryUserDetailsContext onMemUserDetailsContext;

	private final RSAContext rsaContext;
	private final PasswordEncoder passwordEncoder;

	private final String requestIdCookieName;
	private final Duration publicKeyExpiration;

	private final int hotpMovingLowerFactor;
	private final int hotpMovingFactorRange;
	private final AtomicLong hotpMovingFactor;
	private final int hotpLength;

	private final JavaMailSender mailSender;
	private final MailProvider mailProvider;

	@Autowired
	public CredentialResetService(
	// @formatter:off
			Environment env,
			OnMemoryUserDetailsContext onMemUserDetailsContext,
			GenericCRUDServiceImpl crudService,
			RSAContext rsaContext,
			GenericRepository genericRepository,
			MailProvider mailProvider,
			JavaMailSender mailSender, 
			PasswordEncoder passwordEncoder) {
		// @formatter:on
		this.crudService = crudService;
		this.genericRepository = genericRepository;
		this.onMemUserDetailsContext = onMemUserDetailsContext;

		this.rsaContext = rsaContext;
		this.passwordEncoder = passwordEncoder;

		requestIdCookieName = env.getRequiredProperty(Settings.CUSTOMER_CREDENTIAL_RESET_REQUEST_COOKIE_NAME);
		publicKeyExpiration = Duration.ofMinutes(
				env.getRequiredProperty(Settings.CUSTOMER_CREDENTIAL_RESET_REQUEST_COOKIE_EXPIRATION, Integer.class));
		hotpMovingLowerFactor = env.getRequiredProperty(Settings.CUSTOMER_CREDENTIAL_RESET_HOTP_LOWER_FACTOR,
				Integer.class);
		hotpMovingFactor = new AtomicLong(hotpMovingLowerFactor);
		hotpMovingFactorRange = env.getRequiredProperty(Settings.CUSTOMER_CREDENTIAL_RESET_HOTP_UPPER_FACTOR,
				Integer.class) - hotpMovingLowerFactor;
		hotpLength = env.getRequiredProperty(Settings.CUSTOMER_CREDENTIAL_RESET_HOTP_LENGTH, Integer.class);

		this.mailSender = mailSender;
		this.mailProvider = mailProvider;
	}

	@Async(ExecutorNames.CREDENTIAL_RESET_EXECUTOR)
	public CompletableFuture<Void> sendCredentialResetEmail(String customerEmail, int resetCode) {
		if (logger.isDebugEnabled()) {
			logger.debug("Sending credential reset email to customer {}, code {}", customerEmail, resetCode);
		}

		try {
			mailSender.send(mailProvider.createCustomerPasswordResetEmail(customerEmail, resetCode,
					mailSender::createMimeMessage));
		} catch (MailException | MessagingException any) {
			if (logger.isErrorEnabled()) {
				logger.error("Error encountered while trying to send email {}", any.getMessage());
				any.printStackTrace();
			}

			return null;
		}

		return new CompletableFuture<>();
	}

	/**
	 * @param username
	 * @param session
	 * @return {@link ServicePayload} describes the result or null if there's no
	 *         existing request
	 * @throws Exception
	 */
	public ServicePayload<Integer> handleExistingCredentialResetRequest(String username, SharedSessionContract session)
			throws Exception {
		final Optional<Tuple> optionalExistingRequest = locateExisitingRequestId(username, session);

		if (!optionalExistingRequest.isEmpty()) {
			final UUID credentialResetId = optionalExistingRequest.get().get(CredentialReset_.ID, UUID.class);

			if (logger.isDebugEnabled()) {
				logger.debug("Invalidating an existing request {}", credentialResetId);
			}

			return disableExistingRequest(credentialResetId, session);
		}

		return null;
	}

	public ServicePayload<Integer> disableExistingRequest(UUID credentialResetId, SharedSessionContract session)
			throws Exception {
		try {
			final int rowMod = genericRepository.update(CredentialReset.class,
					(root, cu, builder) -> cu.set(root.get(PermanentEntity_.active), Boolean.FALSE),
					(root, cu, builder) -> builder.equal(root.get(CredentialReset_.id), credentialResetId), session);

			if (rowMod != 1) {
				return new ServicePayload<>(ServiceResult
						.failed(new IllegalStateException(String.format("Exptecing 1 row but got %d", rowMod))));
			}

			return new ServicePayload<>(ServiceResult.success());
		} catch (Exception any) {
			return new ServicePayload<>(ServiceResult.failed(any));
		}
	}

	public Optional<Tuple> locateExisitingRequestId(String username, SharedSessionContract session) throws Exception {
		return genericRepository.findOne(CredentialReset.class,
				(root, cq, builder) -> List.of(root.get(CredentialReset_.id).alias(CredentialReset_.ID)),
				(root, cq, builder) -> builder.equal(root.join(CredentialReset_.customer).get(User_.id), username),
				LockModeType.PESSIMISTIC_WRITE, session);
	}

	public Cookie createRequestIdCookie(CredentialReset credentialReset, String path) throws Exception {
		// encrypt the cookie request id and put it in a http-only cookie
		final String encryptedRequestId = encryptRequestId(credentialReset.getId());
		final Cookie cookie = HttpHelper.createHttpOnlyCookie(requestIdCookieName, encryptedRequestId);

		cookie.setPath(path);
		cookie.setMaxAge(Math.toIntExact(publicKeyExpiration.toSeconds()));

		return cookie;
	}

	private String encryptRequestId(UUID id) throws Exception {
		final String idAsString = id.toString();

		if (logger.isDebugEnabled()) {
			logger.debug("Encrypting request id {}", idAsString);
		}

		final Cipher cipher = Cipher.getInstance(RSA_CIPHER_INSTANCE_NAME);

		cipher.init(Cipher.ENCRYPT_MODE, rsaContext.getPublicKey());
		cipher.update(idAsString.getBytes());

		return Base64.getEncoder().encodeToString(cipher.doFinal());
	}

	public ServicePayload<BiDeclaration<CredentialReset, String>> createCredentialResetRequest(String username,
			Session session, boolean flushOnFinish) throws Exception {
		final CredentialReset credentialReset = new CredentialReset();
		// avoid fetching the Customer by loading a proxy
		credentialReset.setCustomer(HibernateHelper.loadProxyInternal(Customer.class, username, session));
		// @formatter:off
		final String plainOTP = HOTP.generateOTP(
				rsaContext.getPrivateKey().getEncoded(),
				hotpMovingFactor.getAndUpdate(this::increaseMovingFactor),
				hotpLength,
				false,
				-1);
		credentialReset.setCode(passwordEncoder.encode(plainOTP));
		// @formatter:on
		return new ServicePayload<>(crudService.create(CredentialReset.class, credentialReset.getId(), credentialReset,
				session, flushOnFinish), Utils.declare(credentialReset, plainOTP));
	}

	private long increaseMovingFactor(long current) {
		return ((current + 1) % hotpMovingFactorRange) + hotpMovingLowerFactor;
	}

	public String getPasswordResetRequestIdCookieName() {
		return requestIdCookieName;
	}

	private static final String JOINED_CUSTOMER_ID_PATH = CredentialReset_.CUSTOMER.concat(User_.ID);

	/**
	 * @param requestId
	 * @param code
	 * @param session
	 * @return username if codes match, otherwise {@code null} indicates given code
	 *         differ from the hased one
	 * @throws EntityNotFoundException                  if we can not find a
	 *                                                  {@link CredentialReset}
	 *                                                  regarding the request id
	 *                                                  extracted from the cookie
	 * @throws StaleRequestException                    if the cookie has expired
	 * @throws InvalidCredentialResetRequestIdException if the extracted request id
	 *                                                  does not meet the guideline
	 */
	public String validatePasswordResetRequest(UUID requestId, String code, SharedSessionContract session)
			throws Exception, StaleRequestException, InvalidCredentialResetRequestIdException {
		final LocalDateTime now = LocalDateTime.now();
		// @formatter:off
		// fetch version, code and username of the Customer, additionally lock the row
		final Optional<Tuple> optionalTuple = genericRepository.findOne(
				CredentialReset.class,
				(root, cq, builder) -> List.of(
						root.get(CredentialReset_.version).alias(CredentialReset_.VERSION),
						root.get(CredentialReset_.code).alias(CredentialReset_.CODE),
						root.join(CredentialReset_.customer).get(User_.id).alias(JOINED_CUSTOMER_ID_PATH)),
				(root, cq, builder) -> builder.equal(root.get(CredentialReset_.id), requestId),
				LockModeType.PESSIMISTIC_WRITE,
				session);
		// @formatter:on
		if (optionalTuple.isEmpty()) {
			throw new EntityNotFoundException("Unknown request");
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Found one request with id {}", requestId);
		}

		final Tuple tuple = optionalTuple.get();
		final LocalDateTime version = tuple.get(CredentialReset_.VERSION, LocalDateTime.class);
		// version check
		if (version.plus(publicKeyExpiration).isBefore(now)) {
			throw new StaleRequestException("This request is stale");
		}
		// check the reset code
		if (!passwordEncoder.matches(code, tuple.get(CredentialReset_.CODE, String.class))) {
			return null;
		}

		return tuple.get(JOINED_CUSTOMER_ID_PATH, String.class);
	}

	/**
	 * Decrypt and extract the {@link CredentialReset} id from cookie
	 * 
	 * @param cookie
	 * @return
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 * @throws InvalidKeyException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 * @throws InvalidCredentialResetRequestIdException
	 */
	public UUID extractRequestIdFromCookie(Cookie cookie)
			throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException,
			BadPaddingException, InvalidCredentialResetRequestIdException {
		// decrypt the cookie content
		final Cipher cipher = Cipher.getInstance(RSA_CIPHER_INSTANCE_NAME);

		cipher.init(Cipher.DECRYPT_MODE, rsaContext.getPrivateKey());
		cipher.update(Base64.getDecoder().decode(cookie.getValue()));
		// check if the content is an UUID and construct the actual UUID
		final String decryptedRequestIdAsString = new String(cipher.doFinal());

		if (!StringHelper.isUUID(decryptedRequestIdAsString)) {
			throw new InvalidCredentialResetRequestIdException("Invalid request id");
		}

		return UUID.fromString(decryptedRequestIdAsString);
	}

	public ServicePayload<Integer> updateCredential(String username, String newPassword,
			SharedSessionContract session) {
		final String encodedNewPassword = passwordEncoder.encode(newPassword);

		try {
			// @formatter:off
			final int rowMod = genericRepository.update(
					User.class,
					username,
					(root, cq, builder) -> cq
						.set(root.get(User_.password), encodedNewPassword)
						.set(root.get(User_.credentialVersion), LocalDateTime.now()),
					(root, cq, builder) -> builder.equal(root.get(User_.id), username),
					LockModeType.PESSIMISTIC_WRITE,
					session);
			// @formatter:on
			if (rowMod != 1) {
				return new ServicePayload<>(
						ServiceResult.failed(new IllegalStateException("Unable to update credential, unknown error")),
						0);
			}
			// remove user's entry from the OnMemoryUserDetailsContext
			if (onMemUserDetailsContext.contains(username)) {
				onMemUserDetailsContext.remove(username);
			}

			return new ServicePayload<>(ServiceResult.success(), 1);
		} catch (Exception any) {
			return new ServicePayload<>(ServiceResult.failed(any));
		}
	}

}
