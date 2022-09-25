/**
 * 
 */
package integration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.hibernate.Session;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.test.context.junit4.SpringRunner;

import multicados.domain.entity.file.UserPhoto;
import multicados.internal.config.InternalWebConfiguration;
import multicados.internal.file.engine.FileManagement;
import multicados.internal.file.engine.FileResourcePersister;
import multicados.internal.file.engine.image.ManipulationContext;
import multicados.internal.file.engine.image.Standard;
import multicados.internal.helper.DNSUtils;

/**
 * @author Ngoc Huy
 *
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { InternalWebConfiguration.class })
public class ApplicationIntegrationTests {

	private final FileManagement fileManagement;
	private final JavaMailSender mailSender;
	private final DNSUtils dnsUtils;

	@Autowired
	public ApplicationIntegrationTests(FileManagement fileManagement, JavaMailSender mailSender, DNSUtils dnsUtils) {
		this.fileManagement = fileManagement;
		this.mailSender = mailSender;
		this.dnsUtils = dnsUtils;
	}

	@Test
	private void testUserPhotoSave() throws IOException {
		final SessionFactoryImplementor sfi = fileManagement.getSessionFactory();
		final Session session = sfi.openSession();
		final UserPhoto photo = new UserPhoto();

		photo.setContent(Files.readAllBytes(new File("C:\\Users\\Ngoc Huy\\Downloads\\Untitled_Artwork.png").toPath()));
		photo.setExtension("png");

		session.save(photo);
		session.flush();

		final FileResourcePersister persister = (FileResourcePersister) sfi.getMetamodel()
				.entityPersister(UserPhoto.class);
		final Standard standard = sfi.getServiceRegistry().requireService(ManipulationContext.class)
				.locateStandard(photo.getId());

		for (final String prefix : standard.getCompressionPrefixes()) {
			Files.delete(new File(persister.resolvePath(String.format("%s_%s", prefix, photo.getId()))).toPath());
		}
	}

	@Test
	public void testHTMLMail() throws MessagingException {
		final String html = """
								<head>
					<style type="text/css">
						@import url('https://fonts.googleapis.com/css2?family=Comfortaa:wght@700&display=swap');

						* {
							font-family: 'Comfortaa', cursive;
							text-align: center;
						}

						#heading {
							display: flex;
							align-content: center;
							justify-content: center;
							background-color: #1a2942;
							color: whitesmoke;
							border-radius: 10px;
							padding: 10px;
						}

						#heading > div {
							position: relative;
							width: 350px;
						}

						#heading > div > span {
							position: absolute;
							top: 50%;
							left: 50%;
							transform: translate(-50%, -50%);
							width: 100%;
						}

						#heading > img {
							width: 50px;
							height: 50px;
							margin-right: 15px;
						}

						a {
							display: inline-block;
							margin-top: 15px;
							padding: 10px 20px;
							background-color: white;
							color: #1a2942;
							border: 2px solid #1a2942;
							text-decoration: none;
							border-radius: 10px;
							transition: all .1s linear;
						}

						a:hover {
							background-color: #1a2942;
							color: white;
							border: 2px solid #1a2942;
							user-select: none;
						}
					</style>
				</head>
				<body>
					<h3 id="heading">
						<img
							src="""
				+ String.format("%s/file/public/S_qan_1656913055482_UleNlw5K07lC.png", dnsUtils.getHostEndpoint())
				+ """
								>
								<div>
									<span>Welcome and thank you for joining us.</span>
								</div>
							</h3>
							<p>Please use the following link to verify your account. This link will be invalidated within 24 hours.</p>
							<a href="">Verify your account</a>
						</body>
										""";
		final MimeMessage mimeMessage = mailSender.createMimeMessage();
		final MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "utf-8");

		mimeMessage.setContent(html, "text/html");

		helper.setTo("ngochuy.ou@gmail.com");
		helper.setSubject("This is the test message for testing gmail smtp server using spring mail");
		helper.setFrom("pi.sup.lot@gmail.com");

		mailSender.send(mimeMessage);
	}

}
