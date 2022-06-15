/**
 * 
 */
package multicados.internal.file.engine.image;

import org.apache.commons.lang3.math.Fraction;
import org.springframework.util.Assert;

/**
 * @author Ngoc Huy
 *
 */
public class Standard {

	private final int numerator;
	private final int denominator;

	private final float ratio;
	private final float upperBoundRatio;
	private final float lowerBoundRatio;

	private final int originalWidth;
	private final int originalHeight;

	private final int batchSize;

	private final float[] compressionQualities;
	private final float[] compressionFactors;

	public Standard(Fraction ratio, int originalWidth, float[] compressionQualities, float[] compressionFactors) {
		numerator = ratio.getNumerator();
		denominator = ratio.getDenominator();

		this.ratio = ratio.floatValue();

		upperBoundRatio = ratio.add(Fraction.ONE_FIFTH).floatValue();
		lowerBoundRatio = ratio.subtract(Fraction.ONE_FIFTH).floatValue();

		this.originalWidth = originalWidth;
		originalHeight = (originalWidth * denominator) / numerator;

		Assert.isTrue(compressionQualities.length == compressionFactors.length,
				"compressionQualities and compressionFactors must have the same length");

		this.compressionQualities = compressionQualities;
		this.compressionFactors = compressionFactors;
		batchSize = compressionQualities.length + 1;
	}

	public int maintainHeight(int width) {
		return (width * denominator) / numerator;
	}

	public int maintainWidth(int height) {
		return (height * numerator) / denominator;
	}

	public int getOriginalWidth() {
		return originalWidth;
	}

	public int getOriginalHeight() {
		return originalHeight;
	}

	public int getBatchSize() {
		return batchSize;
	}

	public float[] getCompressionQualities() {
		return compressionQualities;
	}

	public float[] getCompressionFactors() {
		return compressionFactors;
	}

	public int compareRatio(float candiateRatio) {
		if (candiateRatio >= lowerBoundRatio && candiateRatio <= upperBoundRatio) {
			return 0;
		}

		return upperBoundRatio < candiateRatio ? -1 : 1;
	}

	public boolean lessThan(float candiateRatio) {
		return candiateRatio > upperBoundRatio;
	}

	public boolean equalsTo(float candiateRatio) {
		return candiateRatio >= lowerBoundRatio && candiateRatio <= upperBoundRatio;
	}

	public boolean greaterThan(float candiateRatio) {
		return candiateRatio < lowerBoundRatio;
	}

	public float getRatio() {
		return ratio;
	}

}
