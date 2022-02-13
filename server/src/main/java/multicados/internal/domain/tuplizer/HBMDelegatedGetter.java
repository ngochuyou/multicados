/**
 * 
 */
package multicados.internal.domain.tuplizer;

import java.lang.reflect.Member;

/**
 * @author Ngoc Huy
 *
 */
class HBMDelegatedGetter implements Getter {

	private final org.hibernate.property.access.spi.Getter hbmGetter;

	HBMDelegatedGetter(org.hibernate.property.access.spi.Getter hbmGetter) {
		super();
		this.hbmGetter = hbmGetter;
	}

	@Override
	public Member getMember() {
		return hbmGetter.getMember();
	}

	@Override
	public Object get(Object source) throws Exception {
		return hbmGetter.get(source);
	}

	@Override
	public Class<?> getReturnedType() {
		return hbmGetter.getReturnType();
	}

}
