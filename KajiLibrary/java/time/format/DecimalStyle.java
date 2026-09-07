package java.time.format;

import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

// KajiLibrary's java.time.format.DecimalStyle — the symbols a DateTimeFormatter uses for numbers:
// the zero digit, the positive/negative signs, and the decimal separator.
//
// The four symbols the class keeps are **the caller's data**: `withZeroDigit`,
// `withDecimalSeparator` and the other two do exactly what they say, and the formatter uses them.
// That half is complete.
//
// The other half --which locale they come from-- is CLDR. `of(Locale)` and `ofDefaultLocale()`
// return `STANDARD` for any locale, and that is right for the vast majority but **not for all**: a
// locale with Indo-Arabic digits has a different zero, and it will not be given one here. It stands
// as a written approximation and not as an omission because removing `of(Locale)` --which the JDK
// uses to build any `DateTimeFormatter`-- would leave the class with no way in.
//
// `getAvailableLocales()` is here, and returns the same as
// `DecimalFormatSymbols.getAvailableLocales()` --which is literally what the JDK does, with the
// same delegation. The list is short because `java.text`'s is short: they are the locales this
// library ships data for. That invents nothing; it reports how many there are, which is different
// from saying there are more.
public final class DecimalStyle {

    public static final DecimalStyle STANDARD = new DecimalStyle('0', '+', '-', '.');

    private final char zeroDigit;
    private final char positiveSign;
    private final char negativeSign;
    private final char decimalSeparator;

    private DecimalStyle(char zeroDigit, char positiveSign, char negativeSign, char decimalSeparator) {
        this.zeroDigit = zeroDigit;
        this.positiveSign = positiveSign;
        this.negativeSign = negativeSign;
        this.decimalSeparator = decimalSeparator;
    }

    public static DecimalStyle of(Locale locale) {
        return STANDARD;
    }

    public static DecimalStyle ofDefaultLocale() {
        return STANDARD;
    }

    /**
     * The locales there is data for, which are {@link DecimalFormatSymbols}'s.
     *
     * <p>The JDK makes this same delegation. What changes is how many there are.
     */
    public static Set<Locale> getAvailableLocales() {
        Locale[] l = DecimalFormatSymbols.getAvailableLocales();
        return new HashSet<Locale>(Arrays.asList(l));
    }

    public char getZeroDigit() {
        return this.zeroDigit;
    }

    public DecimalStyle withZeroDigit(char zeroDigit) {
        if (zeroDigit == this.zeroDigit) {
            return this;
        }
        return new DecimalStyle(zeroDigit, this.positiveSign, this.negativeSign, this.decimalSeparator);
    }

    public char getPositiveSign() {
        return this.positiveSign;
    }

    public DecimalStyle withPositiveSign(char positiveSign) {
        if (positiveSign == this.positiveSign) {
            return this;
        }
        return new DecimalStyle(this.zeroDigit, positiveSign, this.negativeSign, this.decimalSeparator);
    }

    public char getNegativeSign() {
        return this.negativeSign;
    }

    public DecimalStyle withNegativeSign(char negativeSign) {
        if (negativeSign == this.negativeSign) {
            return this;
        }
        return new DecimalStyle(this.zeroDigit, this.positiveSign, negativeSign, this.decimalSeparator);
    }

    public char getDecimalSeparator() {
        return this.decimalSeparator;
    }

    public DecimalStyle withDecimalSeparator(char decimalSeparator) {
        if (decimalSeparator == this.decimalSeparator) {
            return this;
        }
        return new DecimalStyle(this.zeroDigit, this.positiveSign, this.negativeSign, decimalSeparator);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DecimalStyle) {
            DecimalStyle other = (DecimalStyle) obj;
            return this.zeroDigit == other.zeroDigit && this.positiveSign == other.positiveSign
                && this.negativeSign == other.negativeSign && this.decimalSeparator == other.decimalSeparator;
        }
        return false;
    }

    public int hashCode() {
        return this.zeroDigit + this.positiveSign + this.negativeSign + this.decimalSeparator;
    }

    public String toString() {
        return "DecimalStyle[" + this.zeroDigit + this.positiveSign + this.negativeSign
            + this.decimalSeparator + "]";
    }
}
