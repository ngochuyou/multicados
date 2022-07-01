/**
 *
 */
package multicados.internal.domain.validation;

import multicados.internal.service.crud.DomainResourceAttributeTranslator;

/**
 * @author Ngoc Huy
 *
 */
public abstract class AbstractAttributeTranslatorAware {

	private final DomainResourceAttributeTranslator translator;

	public AbstractAttributeTranslatorAware(DomainResourceAttributeTranslator translator) {
		this.translator = translator;
	}

	/**
	 * @return the translator
	 */
	public DomainResourceAttributeTranslator getTranslator() {
		return translator;
	}

}
