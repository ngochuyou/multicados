/**
 * 
 */
package multicados.controller.controllers;

import static multicados.internal.helper.Utils.declare;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.util.WebUtils;

import multicados.controller.exception.AdvisedException;
import multicados.controller.exception.ExceptionAdvisor;
import multicados.domain.entity.entities.Customer;
import multicados.domain.entity.entities.User_;
import multicados.domain.entity.entities.customer.CredentialReset;
import multicados.internal.domain.repository.GenericRepository;
import multicados.internal.helper.Common;
import multicados.internal.helper.HibernateHelper;
import multicados.internal.helper.HttpHelper;
import multicados.internal.helper.StringHelper;
import multicados.internal.helper.Utils.BiDeclaration;
import multicados.internal.security.jwt.JwtSecurityContext;
import multicados.internal.service.ServicePayload;
import multicados.internal.service.crud.GenericCRUDServiceImpl;
import multicados.service.domain.customer.CredentialResetService;
import multicados.service.domain.customer.InvalidCredentialResetRequestIdException;
import multicados.service.domain.customer.StaleRequestException;

/**
 * @author Ngoc Huy
 *
 */
@Controller
@RequestMapping("/rest/customer")
public class RestCustomerController extends AbstractController {

	private static final Logger logger = LoggerFactory.getLogger(RestCustomerController.class);

	private final GenericCRUDServiceImpl crudService;
	private final GenericRepository genericRepository;
	private final SessionFactory sessionFactory;

	private final JwtSecurityContext jwtSecurityContext;
	private final CredentialResetService credentialResetService;

	private final String credentialResetCookiePath;

	@Autowired
	public RestCustomerController(
	// @formatter:off
			GenericCRUDServiceImpl crudService,
			SessionFactory sessionFactory,
			GenericRepository genericRepository,
			CredentialResetService credentialResetService,
			ExceptionAdvisor exceptionAdvisor,
			JwtSecurityContext jwtSecurityContext,
			@Value("${multicados.customer.password.reset.request.cookie.path}") String credentialResetCookiePath) throws IllegalAccessException {
		// @formatter:on
		this.crudService = crudService;
		this.genericRepository = genericRepository;
		this.sessionFactory = sessionFactory;
		this.jwtSecurityContext = jwtSecurityContext;
		this.credentialResetService = credentialResetService;
		this.credentialResetCookiePath = credentialResetCookiePath;

		contributeExceptionHandlers(exceptionAdvisor);
	}

	private void contributeExceptionHandlers(ExceptionAdvisor exceptionAdvisor) throws IllegalAccessException {
		exceptionAdvisor.register(this.getClass(), StaleRequestException.class,
				(sre, request) -> declare(HttpStatus.OK, "STALE"));
		exceptionAdvisor.register(this.getClass(), InvalidCredentialResetRequestIdException.class,
				(icrrie, request) -> declare(HttpStatus.BAD_REQUEST, icrrie.getMessage()));
	}

	@PostMapping
	@Transactional(readOnly = true)
	public ResponseEntity<Object> createCustomer(@RequestBody Customer newCustomer, HttpServletRequest request)
			throws HibernateException, Exception {
		// @formatter:off
		return sendResult(
				crudService.create(
						Customer.class,
						newCustomer.getId(),
						newCustomer,
						sessionFactory.getCurrentSession(),
						true),
				newCustomer,
				request);
		// @formatter:on
	}

	@GetMapping("/reset/{username}")
	@Transactional
	public ResponseEntity<Object> requestPasswordReset(@PathVariable("username") String username,
			HttpServletRequest request, HttpServletResponse response) throws Exception {
		final Session session = useAutoSession(sessionFactory.getCurrentSession());
		final Optional<String> customerEmail = fetchCustomerEmail(username, session);
		// check if the user exists and find out whether they have provided an email
		// if not, early exits with the 200 code anyways without stating anything to
		// avoid user scanning
		if (customerEmail.isEmpty()) {
			if (logger.isDebugEnabled()) {
				logger.debug("Ignoring this request");
			}

			return ResponseEntity.ok(null);
		}
		// invalidate the existing request
		final ServicePayload<Integer> existingCredentialResetRequestHandling = credentialResetService
				.handleExistingCredentialResetRequest(username, session);

		if (existingCredentialResetRequestHandling != null && !existingCredentialResetRequestHandling.isOk()) {
			return sendResult(existingCredentialResetRequestHandling, null, request);
		}
		// create a new one
		final ServicePayload<BiDeclaration<CredentialReset, String>> payload = credentialResetService
				.createCredentialResetRequest(username, session, false);

		if (!payload.isOk()) {
			return sendResult(payload, null, request);
		}

		final CredentialReset credentialReset = payload.getBody().getFirst();
		// asynchronously send password reset email
		try {
			credentialResetService.sendCredentialResetEmail(customerEmail.get(),
					Integer.valueOf(payload.getBody().getSecond()));
		} catch (Exception any) {
			if (logger.isErrorEnabled()) {
				logger.error("Error while trying to send credential reset email: {}", any.getMessage());
				any.printStackTrace();
			}
		}

		final Cookie cookie = credentialResetService.createRequestIdCookie(credentialReset, credentialResetCookiePath);

		if (logger.isDebugEnabled()) {
			logger.debug("Attaching request id cookie");
		}

		HttpHelper.attachCookie(response, cookie);

		return sendOk(StringHelper.EMPTY_STRING, request);
	}

	private Optional<String> fetchCustomerEmail(String username, Session session) throws Exception {
		return genericRepository
				.findOne(Customer.class, (root, cq, builder) -> List.of(root.get(User_.email).alias(User_.EMAIL)),
						HibernateHelper.hasId(Customer.class, username, session), session)
				.map(tuple -> tuple.get(User_.EMAIL, String.class));
	}

	/**
	 * @param code
	 * @param newPassword
	 * @param request
	 * @param response
	 * @return
	 * @throws AdvisedException
	 * @throws Exception
	 */
	@PatchMapping(value = "/reset", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_PROBLEM_JSON_VALUE)
	@Transactional
	public ResponseEntity<Object> resetCustomerEmail(String code, String newPassword, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		final Session session = useAutoSession(sessionFactory.getCurrentSession());
		final Cookie passwordResetCookie = WebUtils.getCookie(request,
				credentialResetService.getPasswordResetRequestIdCookieName());

		if (passwordResetCookie == null) {
			return sendForbidden("Unable to locate request cookie", request);
		}

		try {
			// validate the request and extract the Customer's id from it
			final UUID requestId = credentialResetService.extractRequestIdFromCookie(passwordResetCookie);
			final String username = credentialResetService.validatePasswordResetRequest(requestId, code, session);

			if (username == null) {
				return sendBad("Invalid code", request);
			}
			// update the credential and do necessary purges
			final ServicePayload<Integer> payload = credentialResetService.updateCredential(username, newPassword,
					session);

			if (!payload.isOk()) {
				return sendResult(payload, null, request);
			}

			if (logger.isDebugEnabled()) {
				logger.debug("Updated {}'s credential. Purging user's tokens", username);
			}

			credentialResetService.disableExistingRequest(requestId, session);

			HttpHelper.attachCookie(response, createInvalidateCookie(
					credentialResetService.getPasswordResetRequestIdCookieName(), credentialResetCookiePath));
			HttpHelper.attachCookie(response, createInvalidateCookie(jwtSecurityContext.getCookieName(),
					jwtSecurityContext.getWholeDomainPath()));

			return ResponseEntity.ok(Common.payload("Successfully reset password"));
		} catch (StaleRequestException | InvalidCredentialResetRequestIdException ae) {
			// scope these two exceptions and let the ExceptionAdvisor handle the
			// other one using a generic method
			throw new AdvisedException(ae);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException
				| BadPaddingException cipherException) {
			if (logger.isDebugEnabled()) {
				logger.debug("Rejecting credential reset request due to: {}", cipherException.getMessage());
			}

			return sendForbidden("Invalid request", request);
		}
	}

	private Cookie createInvalidateCookie(String cookieName, String path) throws Exception {
		// @formatter:off
		return declare(HttpHelper
				.createInvalidateHttpOnlyCookie(cookieName))
				.consume(cookie -> cookie.setPath(path))
				.get();
		// @formatter:on
	}

}
