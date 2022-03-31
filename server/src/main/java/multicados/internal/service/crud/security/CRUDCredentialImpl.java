/**
 * 
 */
package multicados.internal.service.crud.security;

import multicados.internal.helper.FunctionHelper.HandledBiFunction;
import multicados.internal.security.AbstractCompositeCredential;
import multicados.internal.security.Credential;
import multicados.internal.security.CredentialException;

/**
 * @author Ngoc Huy
 *
 */
public class CRUDCredentialImpl extends AbstractCompositeCredential<String> implements CRUDCredential {

	private static final String CREDENTIAL_TEMPLATE = "%s%s%s";
	private static final String DELIMITER = "-";
	private static final HandledBiFunction<Credential<String>, Credential<String>, String, CredentialException> combiner = (
			left, right) -> String.format(CREDENTIAL_TEMPLATE, left.evaluate(), DELIMITER, right.evaluate());

	private final String value;

	public CRUDCredentialImpl(String value) {
		this.value = value;
	}

	@Override
	public String getDelimiter() {
		return DELIMITER;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof CRUDCredential)) {
			return false;
		}

		CRUDCredential other = (CRUDCredential) obj;

		try {
			return this.evaluate().equals(other.evaluate());
		} catch (CredentialException any) {
			any.printStackTrace();
			return false;
		}
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}

	@Override
	protected HandledBiFunction<Credential<String>, Credential<String>, String, CredentialException> getCombiner() {
		return combiner;
	}

	@Override
	public boolean has(Credential<String> candidate) {
		try {
			return evaluate().contains(candidate.evaluate());
		} catch (Exception any) {
			any.printStackTrace();
			return false;
		}
	}

	@Override
	public String evaluate() throws CredentialException {
		return value;
	}

}
