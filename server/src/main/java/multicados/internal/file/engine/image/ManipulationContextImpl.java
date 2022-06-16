/**
 * 
 */
package multicados.internal.file.engine.image;

import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.math.Fraction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import multicados.internal.config.Settings;
import multicados.internal.helper.SpringHelper;
import multicados.internal.helper.StringHelper;
import multicados.internal.helper.Utils.HandledFunction;

/**
 * @author Ngoc Huy
 *
 */
public class ManipulationContextImpl implements ManipulationContext {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final Logger logger = LoggerFactory.getLogger(ManipulationContextImpl.class);

	private final Map<Ratio, Standard> standardsMap;
	private final Standard[] standardArrays;

	public ManipulationContextImpl(Environment env) throws Exception {
		Standard portrait = resolveStandard(env, Settings.FILE_RESOURCE_IMAGE_STANDARD_PORTRAIT);
		Standard landscape = resolveStandard(env, Settings.FILE_RESOURCE_IMAGE_STANDARD_LANDSCAPE);
		Standard square = resolveStandard(env, Settings.FILE_RESOURCE_IMAGE_STANDARD_SQUARE);

		standardsMap = Collections
				.unmodifiableMap(Map.of(Ratio.PORTRAIT, portrait, Ratio.LANDSCAPE, landscape, Ratio.SQUARE, square));
		standardArrays = Stream.of(portrait, landscape, square).sorted((one, two) -> one.compareRatio(two.getRatio()))
				.toArray(Standard[]::new);

		if (logger.isDebugEnabled()) {
			logger.debug("Ordered {}(s) are {}", Standard.class.getSimpleName(),
					StringHelper.join(List.of(standardArrays)));
		}
	}

	private Standard resolveStandard(Environment env, String envPropName) throws Exception {
		String configuration = SpringHelper.getOrThrow(env, envPropName, HandledFunction.identity(),
				() -> new IllegalArgumentException(
						String.format("Unable to locate any %s configuration", envPropName)));

		String[] components = configuration.split(StringHelper.VERTICAL_BAR);
		String[] ratioPair = components[0].split(StringHelper.COLON);

		if (ratioPair.length < 2) {
			throw new IllegalArgumentException("Expect ratio parts to be 2 in length");
		}

		// @formatter:off
		return new Standard(
				Fraction.getFraction(Integer.valueOf(ratioPair[0]), Integer.valueOf(ratioPair[1])),
				Integer.valueOf(components[1]),
				ArrayUtils.toPrimitive(Stream.of(components[2].split(StringHelper.COMMA)).map(Float::parseFloat)
						.toArray(Float[]::new)),
				ArrayUtils.toPrimitive(Stream.of(components[3].split(StringHelper.COMMA)).map(Float::parseFloat)
						.toArray(Float[]::new)),
				components[4].split(StringHelper.COMMA));
		// @formatter:on
	}

	@Override
	public Standard getStandard(Ratio ratio) {
		return standardsMap.get(ratio);
	}

	@Override
	public Standard resolveStandard(BufferedImage bufferedImage) {
		final int size = standardsMap.size();
		final float ratio = Double.valueOf((bufferedImage.getWidth() * 1.0) / (bufferedImage.getHeight() * 1.0))
				.floatValue();

		if (standardArrays[0].greaterThan(ratio)) {
			return standardArrays[0];
		}

		for (int i = 1; i < size - 1; i++) {
			if (standardArrays[i - 1].lessThan(ratio)
					&& (standardArrays[i].greaterThan(ratio) || standardArrays[i].equalsTo(ratio))) {
				return standardArrays[i];
			}
		}

		return standardArrays[size - 1];
	}

	@Override
	public String resolveCompressionName(String filename, String prefix) {
		return prefix + StringHelper.UNDERSCORE + filename;
	}

}
