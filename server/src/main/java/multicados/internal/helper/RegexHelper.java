/**
 * 
 */
package multicados.internal.helper;

import java.util.Collection;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.RegExUtils;

/**
 * @author Ngoc Huy
 *
 */
public class RegexHelper extends RegExUtils {

	public interface RegexBuilder {

		RegexBuilder start();

		RegexBuilder end();

		RegexBuilder and(RegexBuilder other);

		RegexBuilder or(RegexBuilder other);

		RegexGroupBuilder group();

		RegexBuilder literal(String fragment);

		RangedRegexBuilder withLength();

		String build();

		default RegexBuilder naturalAlphabet() {
			return new RegexBuilderImpl("\\p{L}");
		}

		default RegexBuilder naturalNumeric() {
			return new RegexBuilderImpl("\\p{N}");
		}

	}

	public interface RegexGroupBuilder extends RegexBuilder {

		RegexBuilder start();

		RegexBuilder end();

		RegexGroupBuilder literal(String fragment);

		RegexGroupBuilder literal(Collection<Character> characters);

		RegexGroupBuilder and(RegexBuilder other);

		RegexGroupBuilder or(RegexBuilder other);

		RegexGroupBuilder naturalAlphabet();

		RegexGroupBuilder naturalNumeric();

	}

	public interface RangedRegexBuilder extends RegexBuilder {

		RangedRegexBuilder max(int max);

		RangedRegexBuilder min(int min);

		RangedRegexBuilder atLeastOne();

	}

	private static class RegexGroupBuilderImpl extends RegexBuilderImpl implements RegexGroupBuilder {

		private final RegexBuilder owner;

		public RegexGroupBuilderImpl(RegexBuilder owner) {
			super();
			this.owner = owner;
		}

		public RegexGroupBuilderImpl(RegexBuilder owner, String regex) {
			super(regex);
			this.owner = owner;
		}

		@Override
		public RegexBuilder start() {
			return owner;
		}

		@Override
		public RegexBuilder end() {
			return owner.and(this);
		}

		@Override
		public RegexGroupBuilder literal(String fragment) {
			return new RegexGroupBuilderImpl(owner, super.build() + fragment);
		}

		@Override
		public RegexGroupBuilder literal(Collection<Character> characters) {
			return literal(new String(ArrayUtils.toPrimitive(characters.stream().toArray(Character[]::new))));
		}

		@Override
		public RegexGroupBuilder naturalAlphabet() {
			return and(super.naturalAlphabet());
		}

		@Override
		public RegexGroupBuilder naturalNumeric() {
			return and(super.naturalNumeric());
		}

		@Override
		public RegexGroupBuilder and(RegexBuilder other) {
			return new RegexGroupBuilderImpl(owner, super.build() + other.build());
		}

		@Override
		public RegexGroupBuilder or(RegexBuilder other) {
			return new RegexGroupBuilderImpl(owner, String.format("%s|%s", super.build(), other.build()));
		}

		@Override
		public String build() {
			// @formatter:off
			String regex = super
					.build()
					.replaceAll("\\[", "\\\\[")
					.replaceAll("\\]", "\\\\]")
					.replaceAll("\\.", "\\\\.")
					.replaceAll("\\-", "\\\\-")
					.replaceAll("\\^", "\\\\^");
			// @formatter:on
			return String.format("[%s]", regex);
		}

	}

	private static class RegexBuilderImpl implements RegexBuilder {

		private String regex = StringHelper.EMPTY_STRING;

		public RegexBuilderImpl(String regex) {
			this.regex = regex;
		}

		public RegexBuilderImpl() {}

		@Override
		public RegexBuilder start() {
			this.regex = "^";
			return this;
		}

		@Override
		public RegexBuilder literal(String fragment) {
			return new RegexBuilderImpl(fragment);
		}

		@Override
		public RegexBuilder end() {
			return new RegexBuilderImpl(this.build() + "$");
		}

		@Override
		public RegexGroupBuilder group() {
			return new RegexGroupBuilderImpl(this);
		}

		@Override
		public RegexBuilder and(RegexBuilder other) {
			return new RegexBuilderImpl(this.build() + other.build());
		}

		@Override
		public RegexBuilder or(RegexBuilder other) {
			return new RegexBuilderImpl(String.format("%s|%s", this.build(), other.build()));
		}

		@Override
		public String build() {
			return regex;
		}

		@Override
		public RangedRegexBuilder withLength() {
			return new RangedRegexBuilderImpl(regex);
		}

	}

	private static class RangedRegexBuilderImpl extends RegexBuilderImpl implements RangedRegexBuilder {

		private String max = StringHelper.EMPTY_STRING;
		private String min = String.valueOf(0);

		public RangedRegexBuilderImpl(String regex) {
			super(regex);
		}

		@Override
		public String build() {
			return String.format("%s{%s,%s}", super.build(), min, max);
		}

		@Override
		public RangedRegexBuilder max(int max) {
			this.max = String.valueOf(max);
			return this;
		}

		@Override
		public RangedRegexBuilder min(int min) {
			this.min = String.valueOf(min);
			return this;
		}

		@Override
		public RangedRegexBuilder atLeastOne() {
			min = String.valueOf(1);
			max = StringHelper.EMPTY_STRING;
			return this;
		}

	}

	public static RegexBuilder start() {
		return new RegexBuilderImpl().start();
	}

}
