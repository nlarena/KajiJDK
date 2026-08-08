package java.text;

// KajiLibrary's java.text.NumberFormat — the abstract base for number formatters. A subset:
// it declares the public `format(double)` (matching the JDK) and delegates to a package-private
// `formatImpl` that concrete subclasses (DecimalFormat) provide. The JDK's StringBuffer/
// FieldPosition-based methods, the locale factory methods and parsing are out of subset.
public abstract class NumberFormat {

    protected NumberFormat() {
    }

    public String format(double number) {
        return this.formatImpl(number);
    }

    // The seam a concrete formatter fills in (package-private, so it isn't part of the public
    // API surface the gate checks).
    abstract String formatImpl(double number);
}
