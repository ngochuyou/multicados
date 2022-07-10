/**
 * 
 */
package multicados.service.mail;

import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * @author Ngoc Huy
 *
 */
@Component
public class MailProvider {

	private static final Logger logger = LoggerFactory.getLogger(MailProvider.class);

	private static final ClassPathResource LOGO = new ClassPathResource("img/S_qan_1656913055482_UleNlw5K07lC.png");

	private final String mailUsername;

	public MailProvider(Environment env) {
		mailUsername = env.getRequiredProperty("spring.mail.username");

		if (logger.isDebugEnabled()) {
			logger.debug("Mail username {}", mailUsername);
		}
	}

	public MimeMessage createCustomerVerificationEmail(String customerEmail, int verificationCode,
			Supplier<MimeMessage> initializer) throws MessagingException {
		final MimeMessage mimeMessage = initializer.get();
		final MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());

		helper.setTo(customerEmail);
		helper.setSubject("[MULTICADOS]: Active your account");
		helper.setFrom(mailUsername);
		helper.setText(MailTemplates.getCustomerVerificationTemplate(verificationCode), true);
		helper.addInline(MailTemplates.CID_LOGO, LOGO);

		return mimeMessage;
	}
	
	public MimeMessage createCustomerPasswordResetEmail(String customerEmail, int resetCode,
			Supplier<MimeMessage> initializer) throws MessagingException {
		final MimeMessage mimeMessage = initializer.get();
		final MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());

		helper.setTo(customerEmail);
		helper.setSubject("[MULTICADOS]: Let's get you back in");
		helper.setFrom(mailUsername);
		helper.setText(MailTemplates.getCustomerCredentialResetTemplate(resetCode), true);
		helper.addInline(MailTemplates.CID_LOGO, LOGO);

		return mimeMessage;
	}

}
