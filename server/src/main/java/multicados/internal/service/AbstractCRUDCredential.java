/**
 * 
 */
package multicados.internal.service;

import multicados.internal.helper.FunctionHelper.HandledBiFunction;
import multicados.internal.security.AbstractCredential;
import multicados.internal.security.Credential;

/**
 * @author Ngoc Huy
 *
 */
public abstract class AbstractCRUDCredential extends AbstractCredential<String> implements CRUDCredential {

	private static final String DELIMITER = "-";
	private static final HandledBiFunction<Credential<String>, Credential<String>, String, Exception> combiner = (left,
			right) -> String.format("%s%s%s", left.evaluate(), DELIMITER, right.evaluate());

	@Override
	public Credential<String> and(Credential<String> next,
			HandledBiFunction<Credential<String>, Credential<String>, String, Exception> combiner) {
		return super.and(next, AbstractCRUDCredential.combiner);
	}

	@Override
	public String getDelimiter() {
		return DELIMITER;
	}

}
