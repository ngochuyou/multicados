/**
 * 
 */
package multicados.service.mail;

/**
 * @author Ngoc Huy
 *
 */
public abstract class MailTemplates {

	public static final String CID_LOGO = "logo";
	public static final String CID_CODE = "cid:code";

	private static final String CUSTOMER_VERIFICATION_TEMPLATE;
	private static final String CUSTOMER_CREDENTIAL_RESET_TEMPLATE;

	static {
		CUSTOMER_VERIFICATION_TEMPLATE = """
				<body style="text-align: center; font-family: 'Comfortaa', cursive;">
					<div
						id="heading"
						style="font-size: large;
							display: flex;
							align-content: center;
							justify-content: center;
							background-color: #1a2942;
							color: whitesmoke;
							border-radius: 10px;
							padding: 10px;"
					>
						<img
							style="
								width: 25px;
								height: 25px;
								margin-right: 15px;"
							src="cid:logo"
						>
							<div
								style="position: relative;
								width: 400px;"
							>
								<span
									style="position: absolute;
										top: 50%;
										left: 50%;
										transform: translate(-50%, -50%);
										width: 100%;"
								>Welcome and thank you for joining us.</span>
							</div>
						</div>
						<h3>
							Enter the following code on the website to activate your account. This code will be invalidated within 12 hours.</h3>
							<p style="color: #1a2942; text-align: center; font-size: x-large;">cid:"""
				+ CID_CODE + "</p>" + "</body>";
		CUSTOMER_CREDENTIAL_RESET_TEMPLATE = """
				<body style="text-align: center; font-family: 'Comfortaa', cursive;">
					<div
						id="heading"
						style="font-size: large;
							display: flex;
							align-content: center;
							justify-content: center;
							background-color: #1a2942;
							color: whitesmoke;
							border-radius: 10px;
							padding: 10px;"
					>
						<img
							style="
								width: 50px;
								height: 50px;
								margin-right: 15px;"
							src="cid:logo"
						>
						<div
							style="position: relative;
							width: 400px;"
						>
							<span
								style="position: absolute;
									top: 50%;
									left: 50%;
									transform: translate(-50%, -50%);
									width: 100%;"
							>We'll help you with your account.</span>
						</div>
					</div>
					<h3>
						Enter the following code on the website to continue.
					</h3>
					<p style="color: #1a2942; text-align: center; font-size: x-large;">cid:code</p>
				</body>""";
	}

	public static String getCustomerVerificationTemplate(int verificationCode) {
		return CUSTOMER_VERIFICATION_TEMPLATE.replace(CID_CODE, String.valueOf(verificationCode));
	}

	public static String getCustomerCredentialResetTemplate(int resetCode) {
		return CUSTOMER_CREDENTIAL_RESET_TEMPLATE.replace(CID_CODE, String.valueOf(resetCode));
	}

}
