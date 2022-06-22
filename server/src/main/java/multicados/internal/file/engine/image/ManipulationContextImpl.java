/**
 * 
 */
package multicados.internal.file.engine.image;

import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.HashMap;
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
import multicados.internal.helper.Utils;
import multicados.internal.helper.Utils.HandledFunction;

/**
 * @author Ngoc Huy
 *
 */
public class ManipulationContextImpl implements ManipulationContext {

	private static final long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory.getLogger(ManipulationContextImpl.class);

	private final Map<String, Standard> standardsMap;
	private final Standard[] standardsArray;
	private final String identifierDelimiter;

	private final int maximumIdentifierOccupancy;

	public ManipulationContextImpl(Environment env, String identifierDelimiter) throws Exception {
		standardsMap = locateStandardsConfiguration(env);
		standardsArray = standardsMap.values().stream().sorted((one, two) -> one.compareRatio(two.getRatio()))
				.toArray(Standard[]::new);
		maximumIdentifierOccupancy = standardsMap.values().stream().map(Standard::getName).map(String::length)
				.max(Integer::compare).get();
		this.identifierDelimiter = identifierDelimiter;

		if (logger.isDebugEnabled()) {
			logger.debug("Ordered {}(s) are\n\t{}", Standard.class.getSimpleName(),
					StringHelper.join("\n\t", List.of(standardsArray)));
			logger.debug("maximumIdentifierOccupancy is {}", maximumIdentifierOccupancy);
			logger.debug("identifierDelimiter is {}", identifierDelimiter);
		}
	}

	private Map<String, Standard> locateStandardsConfiguration(Environment env) throws Exception {
		if (logger.isTraceEnabled()) {
			logger.trace("Locating manipulation standards");
		}

		final Map<String, Standard> standards = new HashMap<>();

		for (String configuration : SpringHelper.getOrThrow(env, Settings.FILE_RESOURCE_IMAGE_STANDARD,
				HandledFunction.identity(),
				() -> new IllegalArgumentException(
						String.format("Unable to locate any %s configuration", Settings.FILE_RESOURCE_IMAGE_STANDARD)))
				.split(StringHelper.SEMI_COLON)) {
			// @formatter:off
			Utils.declare(resolveStandard(configuration))
				.flat(Standard::getName, HandledFunction.identity())
				.consume(standards::put);
			// @formatter:on
		}

		return Collections.unmodifiableMap(standards);
	}

	private Standard resolveStandard(String configuration) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("Constructing an instance of {} with configuration {}", Standard.class.getName(),
					configuration);
		}

		String[] components = configuration.split(StringHelper.VERTICAL_BAR);
		String name = components[0];
		String[] ratioPair = components[1].split(StringHelper.COLON);

		if (ratioPair.length < 2) {
			throw new IllegalArgumentException("Expect ratio parts to be 2 in length");
		}
		// @formatter:off
		return new Standard(
				name,
				Fraction.getFraction(Integer.valueOf(ratioPair[0]), Integer.valueOf(ratioPair[1])),
				Integer.valueOf(components[2]),
				ArrayUtils.toPrimitive(Stream.of(components[3].split(StringHelper.COMMA)).map(Float::parseFloat)
						.toArray(Float[]::new)),
				ArrayUtils.toPrimitive(Stream.of(components[4].split(StringHelper.COMMA)).map(Float::parseFloat)
						.toArray(Float[]::new)),
				components[5].split(StringHelper.COMMA));
		// @formatter:on
	}

	@Override
	public Standard resolveStandard(BufferedImage bufferedImage) {
		final int size = standardsMap.size();
		final float ratio = Double.valueOf((bufferedImage.getWidth() * 1.0) / (bufferedImage.getHeight() * 1.0))
				.floatValue();

		if (standardsArray[0].greaterThan(ratio) || standardsArray[0].equalsTo(ratio)) {
			return standardsArray[0];
		}

		for (int i = 1; i < size - 1; i++) {
			if (standardsArray[i - 1].lessThan(ratio)
					&& (standardsArray[i].greaterThan(ratio) || standardsArray[i].equalsTo(ratio))) {
				return standardsArray[i];
			}
		}

		return standardsArray[size - 1];
	}

	@Override
	public String resolveCompressionName(String filename, String prefix) {
		return prefix + identifierDelimiter + filename;
	}

	@Override
	public int getMaximumIdentifierOccupancy() {
		return maximumIdentifierOccupancy;
	}

	@Override
	public List<Standard> getStandards() {
		return List.of(standardsArray);
	}

	@Override
	public Standard locateStandard(String filename) {
		return standardsMap.get(filename.split(identifierDelimiter)[0]);
	}

}
