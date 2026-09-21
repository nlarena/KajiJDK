package java.text;

import java.io.Serializable;
import java.util.Currency;
import java.util.Locale;

/**
 * The characters a {@link DecimalFormat} pattern is rendered with, for one locale.
 *
 * <p>A pattern like {@code #,##0.00} says <em>structure</em> — group the integer part, keep two
 * fraction digits — and says nothing about which characters draw it. That separation is the whole
 * design: the same pattern renders {@code 1,234.50} in the United States and {@code 1.234,50} in
 * Germany, because the pattern is shared and only these symbols change.
 *
 * <p>The symbols are less predictable than they look. French groups with {@code U+202F}, a NARROW
 * NO-BREAK SPACE — not a comma, not an ordinary space — and the per-mille sign is {@code U+2030}
 * everywhere. Guessing them is how a formatter ends up subtly wrong in exactly the locales its
 * author does not read.
 *
 * @implNote The table was extracted by running the JDK's own {@code DecimalFormatSymbols}, not
 *           transcribed, and every non-ASCII character is written as a {@code \\uXXXX} escape so the
 *           source stays ASCII and cannot be corrupted by an encoding mishap.
 *
 * @implNote The surface is complete. What is still a subset is the DATA: the table covers six
 *           locales and not the JDK's hundreds, and an unknown locale falls back to ROOT -- which is
 *           the same as what the JDK does with a locale it has no data for. Widening it is extracting
 *           more rows, not writing new code.
 */
public class DecimalFormatSymbols implements Cloneable, Serializable {

    // The supported locales, and the symbol table in parallel rows. Index 0 is ROOT, which is also
    // the fallback. They were written as methods, and not as `static final` fields, back when finding
    // #112 made a primitive constant read as 0 at run time. #112 is closed; the shape is kept because
    // returning a fresh array per call also stops a caller mutating the shared table, which is the
    // reason it is worth it now.
    private static String[] tags() {
        return new String[] {"und", "en-US", "es-AR", "de-DE", "fr-FR", "ja-JP"};
    }

    private static char[] zeroDigits() {
        return new char[] {'0', '0', '0', '0', '0', '0'};
    }

    private static char[] decimalSeparators() {
        return new char[] {'.', '.', ',', ',', ',', '.'};
    }

    private static char[] groupingSeparators() {
        // French groups with U+202F NARROW NO-BREAK SPACE - not a comma, not an ordinary space.
        return new char[] {',', ',', '.', '.', '\u202f', ','};
    }

    private static char[] minusSigns() {
        return new char[] {'-', '-', '-', '-', '-', '-'};
    }

    private static char[] percents() {
        return new char[] {'%', '%', '%', '%', '%', '%'};
    }

    private static char[] perMills() {
        return new char[] {'\u2030', '\u2030', '\u2030', '\u2030', '\u2030', '\u2030'};
    }

    private static char[] digits() {
        return new char[] {'#', '#', '#', '#', '#', '#'};
    }

    private static char[] patternSeparators() {
        return new char[] {';', ';', ';', ';', ';', ';'};
    }

    private static char[] monetaryDecimals() {
        return new char[] {'.', '.', ',', ',', ',', '.'};
    }

    private static char[] monetaryGroupings() {
        return new char[] {',', ',', '.', '.', '\u202f', ','};
    }

    private static String[] exponents() {
        return new String[] {"E", "E", "E", "E", "E", "E"};
    }

    private static String[] infinities() {
        return new String[] {"\u221e", "\u221e", "\u221e", "\u221e", "\u221e", "\u221e"};
    }

    private static String[] nans() {
        return new String[] {"NaN", "NaN", "NaN", "NaN", "NaN", "NaN"};
    }

    private static String[] currencySymbols() {
        return new String[] {"\u00a4", "$", "$", "\u20ac", "\u20ac", "\uffe5"};
    }

    private static String[] currencyCodes() {
        return new String[] {"XXX", "USD", "ARS", "EUR", "EUR", "JPY"};
    }

    private final Locale locale;
    private char zeroDigit;
    private char groupingSeparator;
    private char decimalSeparator;
    private char perMill;
    private char percent;
    private char digit;
    private char patternSeparator;
    private char minusSign;
    private char monetaryDecimalSeparator;
    private char monetaryGroupingSeparator;
    private String infinity;
    private String nan;
    private String currencySymbol;
    private String internationalCurrencySymbol;
    private String exponentSeparator;
    private Currency currency;

    /**
     * Creates symbols for the default locale.
     */
    public DecimalFormatSymbols() {
        this(Locale.getDefault());
    }

    /**
     * Creates symbols for the given locale.
     *
     * @param locale the locale whose symbols to use
     */
    public DecimalFormatSymbols(Locale locale) {
        this.locale = locale;
        int i = DecimalFormatSymbols.indexOf(locale);
        this.zeroDigit = DecimalFormatSymbols.zeroDigits()[i];
        this.groupingSeparator = DecimalFormatSymbols.groupingSeparators()[i];
        this.decimalSeparator = DecimalFormatSymbols.decimalSeparators()[i];
        this.perMill = DecimalFormatSymbols.perMills()[i];
        this.percent = DecimalFormatSymbols.percents()[i];
        this.digit = DecimalFormatSymbols.digits()[i];
        this.patternSeparator = DecimalFormatSymbols.patternSeparators()[i];
        this.minusSign = DecimalFormatSymbols.minusSigns()[i];
        this.monetaryDecimalSeparator = DecimalFormatSymbols.monetaryDecimals()[i];
        this.monetaryGroupingSeparator = DecimalFormatSymbols.monetaryGroupings()[i];
        this.infinity = DecimalFormatSymbols.infinities()[i];
        this.nan = DecimalFormatSymbols.nans()[i];
        this.currencySymbol = DecimalFormatSymbols.currencySymbols()[i];
        this.internationalCurrencySymbol = DecimalFormatSymbols.currencyCodes()[i];
        this.exponentSeparator = DecimalFormatSymbols.exponents()[i];
        // The symbol is NOT recomputed from the currency: the table already carries the one this
        // locale takes, and Currency.getSymbol() of a currency foreign to the locale would return the
        // ISO code. The currency is derived from the code, and "XXX" (ROOT's) leaves the field null
        // on purpose: ROOT has no currency, and saying it has one would be inventing it.
        this.currency = null;
        try {
            this.currency = Currency.getInstance(this.internationalCurrencySymbol);
        } catch (IllegalArgumentException e) {
            this.currency = null;
        }
    }

    // Matches on "lang-COUNTRY" first, then on the language alone, then falls back to ROOT. That
    // ordering is what makes an unlisted country of a listed language (es-MX) still get Spanish
    // symbols rather than the root ones.
    //
    // Package-access and not private because LocalePatterns resolves the locale with THIS very
    // function: if the two tables were indexed differently, a locale could end up with one's symbols
    // and another's pattern, which is exactly the kind of mix nobody finds by looking.
    static int indexOf(Locale locale) {
        String lang = locale.getLanguage();
        String country = locale.getCountry();
        String full = lang;
        if (country.length() > 0) {
            full = lang + "-" + country;
        }
        String[] all = DecimalFormatSymbols.tags();
        int found = -1;
        int i = 0;
        while (i < all.length) {
            if (all[i].equals(full)) {
                found = i;
                i = all.length;
            } else {
                i = i + 1;
            }
        }
        if (found < 0) {
            i = 0;
            while (i < all.length) {
                String tag = all[i];
                int dash = -1;
                int k = 0;
                while (k < tag.length()) {
                    if (tag.charAt(k) == '-' && dash < 0) {
                        dash = k;
                    }
                    k = k + 1;
                }
                String tagLang = tag;
                if (dash > 0) {
                    tagLang = tag.substring(0, dash);
                }
                if (tagLang.equals(lang) && lang.length() > 0) {
                    found = i;
                    i = all.length;
                } else {
                    i = i + 1;
                }
            }
        }
        if (found < 0) {
            found = 0;
        }
        return found;
    }

    /**
     * Returns the locales for which symbols are available.
     *
     * @return the supported locales
     */
    public static Locale[] getAvailableLocales() {
        String[] all = DecimalFormatSymbols.tags();
        Locale[] out = new Locale[all.length];
        int i = 0;
        while (i < all.length) {
            String tag = all[i];
            int dash = -1;
            int k = 0;
            while (k < tag.length()) {
                if (tag.charAt(k) == '-' && dash < 0) {
                    dash = k;
                }
                k = k + 1;
            }
            if (dash > 0) {
                out[i] = new Locale(tag.substring(0, dash), tag.substring(dash + 1, tag.length()));
            } else if (tag.equals("und")) {
                // `und` is BCP-47's tag for "undetermined", and the locale it corresponds to is
                // ROOT --not one whose language is literally called "und"--. It is what
                // `Locale.forLanguageTag("und")` returns, and what the JDK puts in this list.
                out[i] = Locale.ROOT;
            } else {
                out[i] = new Locale(tag);
            }
            i = i + 1;
        }
        return out;
    }

    /**
     * Returns symbols for the default locale.
     *
     * @return the symbols
     */
    public static final DecimalFormatSymbols getInstance() {
        return new DecimalFormatSymbols();
    }

    /**
     * Returns symbols for the given locale.
     *
     * @param locale the locale
     * @return the symbols
     */
    public static final DecimalFormatSymbols getInstance(Locale locale) {
        return new DecimalFormatSymbols(locale);
    }

    /**
     * Returns the locale these symbols came from.
     *
     * @return the locale
     */
    public Locale getLocale() {
        return this.locale;
    }

    /**
     * Returns the character for zero.
     *
     * @return the zero digit
     * @implSpec Digits one through nine are this character plus one through nine, which is why a
     *           single symbol is enough to describe a whole digit set.
     */
    public char getZeroDigit() {
        return this.zeroDigit;
    }

    /**
     * Sets the character for zero.
     *
     * @param zeroDigit the zero digit
     */
    public void setZeroDigit(char zeroDigit) {
        this.zeroDigit = zeroDigit;
    }

    /**
     * Returns the character that separates groups of digits.
     *
     * @return the grouping separator
     */
    public char getGroupingSeparator() {
        return this.groupingSeparator;
    }

    /**
     * Sets the character that separates groups of digits.
     *
     * @param groupingSeparator the grouping separator
     */
    public void setGroupingSeparator(char groupingSeparator) {
        this.groupingSeparator = groupingSeparator;
    }

    /**
     * Returns the character that separates the integer and fraction parts.
     *
     * @return the decimal separator
     */
    public char getDecimalSeparator() {
        return this.decimalSeparator;
    }

    /**
     * Sets the character that separates the integer and fraction parts.
     *
     * @param decimalSeparator the decimal separator
     */
    public void setDecimalSeparator(char decimalSeparator) {
        this.decimalSeparator = decimalSeparator;
    }

    /**
     * Returns the per-mille sign.
     *
     * @return the per-mille sign
     */
    public char getPerMill() {
        return this.perMill;
    }

    /**
     * Sets the per-mille sign.
     *
     * @param perMill the per-mille sign
     */
    public void setPerMill(char perMill) {
        this.perMill = perMill;
    }

    /**
     * Returns the percent sign.
     *
     * @return the percent sign
     */
    public char getPercent() {
        return this.percent;
    }

    /**
     * Sets the percent sign.
     *
     * @param percent the percent sign
     */
    public void setPercent(char percent) {
        this.percent = percent;
    }

    /**
     * Returns the character standing for an optional digit in a pattern.
     *
     * @return the digit placeholder
     */
    public char getDigit() {
        return this.digit;
    }

    /**
     * Sets the character standing for an optional digit in a pattern.
     *
     * @param digit the digit placeholder
     */
    public void setDigit(char digit) {
        this.digit = digit;
    }

    /**
     * Returns the character separating the positive and negative subpatterns.
     *
     * @return the pattern separator
     */
    public char getPatternSeparator() {
        return this.patternSeparator;
    }

    /**
     * Sets the character separating the positive and negative subpatterns.
     *
     * @param patternSeparator the pattern separator
     */
    public void setPatternSeparator(char patternSeparator) {
        this.patternSeparator = patternSeparator;
    }

    /**
     * Returns the string for infinity.
     *
     * @return the infinity string
     */
    public String getInfinity() {
        return this.infinity;
    }

    /**
     * Sets the string for infinity.
     *
     * @param infinity the infinity string
     */
    public void setInfinity(String infinity) {
        this.infinity = infinity;
    }

    /**
     * Returns the string for "not a number".
     *
     * @return the NaN string
     */
    public String getNaN() {
        return this.nan;
    }

    /**
     * Sets the string for "not a number".
     *
     * @param nan the NaN string
     */
    public void setNaN(String nan) {
        this.nan = nan;
    }

    /**
     * Returns the character for the minus sign.
     *
     * @return the minus sign
     */
    public char getMinusSign() {
        return this.minusSign;
    }

    /**
     * Sets the character for the minus sign.
     *
     * @param minusSign the minus sign
     */
    public void setMinusSign(char minusSign) {
        this.minusSign = minusSign;
    }

    /**
     * Returns the currency symbol.
     *
     * @return the currency symbol
     */
    public String getCurrencySymbol() {
        return this.currencySymbol;
    }

    /**
     * Sets the currency symbol.
     *
     * @param currency the currency symbol
     */
    public void setCurrencySymbol(String currency) {
        this.currencySymbol = currency;
    }

    /**
     * Returns the ISO 4217 currency code.
     *
     * @return the currency code
     */
    public String getInternationalCurrencySymbol() {
        return this.internationalCurrencySymbol;
    }

    /**
     * Sets the ISO 4217 currency code.
     *
     * @param currencyCode the currency code
     */
    public void setInternationalCurrencySymbol(String currencyCode) {
        this.internationalCurrencySymbol = currencyCode;
        // The ISO code rules: if it names a known currency, the currency and its symbol are
        // recomputed. If it names none --or is null-- the currency is left null and the symbol is NOT
        // touched, which is what the JDK does: an unknown code is no reason to erase a valid
        // symbol.
        this.currency = null;
        if (currencyCode != null) {
            try {
                Currency c = Currency.getInstance(currencyCode);
                this.currency = c;
                this.currencySymbol = c.getSymbol(this.locale);
            } catch (IllegalArgumentException e) {
                this.currency = null;
            }
        }
    }

    /**
     * These symbols' currency, or {@code null} if the international code names no known one.
     *
     * <p>It was left out while {@code java.util.Currency} did not exist. It does: today the symbols
     * and the currency are kept in step in both directions --{@link #setCurrency} rewrites both
     * symbols, {@link #setInternationalCurrencySymbol} rewrites the currency-- which is the part of
     * the contract that is lost if only the getter is added.
     *
     * @return the currency, or {@code null}
     */
    public Currency getCurrency() {
        return this.currency;
    }

    /**
     * It sets the currency and, with it, the ISO code and the symbol.
     *
     * @param currency the currency
     * @throws NullPointerException if {@code currency} is {@code null}
     */
    public void setCurrency(Currency currency) {
        if (currency == null) {
            throw new NullPointerException();
        }
        this.currency = currency;
        this.internationalCurrencySymbol = currency.getCurrencyCode();
        this.currencySymbol = currency.getSymbol(this.locale);
    }

    /**
     * Returns the decimal separator used in currency amounts.
     *
     * @return the monetary decimal separator
     * @implSpec It is a separate symbol from {@link #getDecimalSeparator()} because some locales
     *           punctuate money differently from plain numbers.
     */
    public char getMonetaryDecimalSeparator() {
        return this.monetaryDecimalSeparator;
    }

    /**
     * Sets the decimal separator used in currency amounts.
     *
     * @param sep the monetary decimal separator
     */
    public void setMonetaryDecimalSeparator(char sep) {
        this.monetaryDecimalSeparator = sep;
    }

    /**
     * Returns the grouping separator used in currency amounts.
     *
     * @return the monetary grouping separator
     */
    public char getMonetaryGroupingSeparator() {
        return this.monetaryGroupingSeparator;
    }

    /**
     * Sets the grouping separator used in currency amounts.
     *
     * @param sep the monetary grouping separator
     */
    public void setMonetaryGroupingSeparator(char sep) {
        this.monetaryGroupingSeparator = sep;
    }

    /**
     * Returns the string separating the mantissa from the exponent.
     *
     * @return the exponent separator
     */
    public String getExponentSeparator() {
        return this.exponentSeparator;
    }

    /**
     * Sets the string separating the mantissa from the exponent.
     *
     * @param exp the exponent separator
     */
    public void setExponentSeparator(String exp) {
        this.exponentSeparator = exp;
    }

    /**
     * Returns a copy of these symbols.
     *
     * @return a copy
     */
    public Object clone() {
        DecimalFormatSymbols copy = new DecimalFormatSymbols(this.locale);
        copy.zeroDigit = this.zeroDigit;
        copy.groupingSeparator = this.groupingSeparator;
        copy.decimalSeparator = this.decimalSeparator;
        copy.perMill = this.perMill;
        copy.percent = this.percent;
        copy.digit = this.digit;
        copy.patternSeparator = this.patternSeparator;
        copy.minusSign = this.minusSign;
        copy.monetaryDecimalSeparator = this.monetaryDecimalSeparator;
        copy.monetaryGroupingSeparator = this.monetaryGroupingSeparator;
        copy.infinity = this.infinity;
        copy.nan = this.nan;
        copy.currencySymbol = this.currencySymbol;
        copy.internationalCurrencySymbol = this.internationalCurrencySymbol;
        copy.currency = this.currency;
        copy.exponentSeparator = this.exponentSeparator;
        return copy;
    }

    /**
     * Compares these symbols with another set.
     *
     * @param obj the object to compare with
     * @return {@code true} if every symbol matches
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DecimalFormatSymbols) {
            DecimalFormatSymbols other = (DecimalFormatSymbols) obj;
            return this.zeroDigit == other.zeroDigit
                    && this.groupingSeparator == other.groupingSeparator
                    && this.decimalSeparator == other.decimalSeparator
                    && this.perMill == other.perMill
                    && this.percent == other.percent
                    && this.digit == other.digit
                    && this.patternSeparator == other.patternSeparator
                    && this.minusSign == other.minusSign
                    && this.monetaryDecimalSeparator == other.monetaryDecimalSeparator
                    && this.monetaryGroupingSeparator == other.monetaryGroupingSeparator
                    && this.infinity.equals(other.infinity)
                    && this.nan.equals(other.nan)
                    && this.currencySymbol.equals(other.currencySymbol)
                    && this.internationalCurrencySymbol.equals(other.internationalCurrencySymbol)
                    && this.exponentSeparator.equals(other.exponentSeparator);
        }
        return false;
    }

    /**
     * Returns a hash code for these symbols.
     *
     * @return the hash code
     */
    public int hashCode() {
        int result = this.zeroDigit;
        result = result * 37 + this.groupingSeparator;
        result = result * 37 + this.decimalSeparator;
        return result;
    }
}
