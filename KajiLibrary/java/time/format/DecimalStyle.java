package java.time.format;

import java.util.Locale;

// KajiLibrary's java.time.format.DecimalStyle — the symbols a DateTimeFormatter uses for numbers: the
// zero digit, the positive/negative signs, and the decimal separator.
//
// Los cuatro simbolos que la clase guarda son **datos del llamador**: `withZeroDigit`,
// `withDecimalSeparator` y los otros dos hacen exactamente lo que dicen, y el formateador los usa.
// Esa mitad es completa.
//
// La otra mitad --de que locale salen-- es CLDR. `of(Locale)` y `ofDefaultLocale()` devuelven
// `STANDARD` para cualquier locale, y eso es correcto para la enorme mayoria pero **no para todos**:
// un locale con digitos indo-arabigos tiene otro cero, y aca no se lo va a dar. Queda como
// aproximacion escrita y no como omision porque quitar `of(Locale)` --que el JDK usa para construir
// cualquier `DateTimeFormatter`-- dejaria la clase sin punto de entrada.
//
// `getAvailableLocales()` si se omite, y la diferencia es la que gobierna el paquete: `of(locale)`
// devuelve simbolos que son ciertos casi siempre, pero `getAvailableLocales()` seria una **lista de
// locales para los que hay datos**, y no hay ninguno. Devolver todos los locales, o el conjunto
// vacio, serian las dos formas de mentir sobre lo mismo.
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
