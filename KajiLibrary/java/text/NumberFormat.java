package java.text;

// KajiLibrary's java.text.NumberFormat — the abstract base for number formatters, and the layer
// that turns Format's Object-shaped contract into a numeric one.
//
// Its job in the hierarchy is dispatch: Format speaks Object, but a number formatter wants a
// primitive, and `double` and `long` are NOT interchangeable — a long past 2^53 cannot round-trip
// through a double. So NumberFormat declares two abstract seams, one per primitive, and routes
// Object to whichever one preserves the value.
//
// A KajiLibrary subset: the locale factories (getInstance/getNumberInstance/getCurrencyInstance/…)
// need CLDR data, the parsing half is absent package-wide, and INTEGER_FIELD/FRACTION_FIELD are
// omitted — they are `public static final int`, which our compiler leaves only in ConstantValue
// where the VM reads them back as 0 (finding #112), so shipping them would ship two constants that
// silently equal each other.
public abstract class NumberFormat extends Format {

    protected NumberFormat() {
    }

    // The Object entry point. Integral wrappers go through the long seam so their exact value
    // survives; everything else through the double one.
    public StringBuffer format(Object number, StringBuffer toAppendTo, FieldPosition pos) {
        if (number instanceof Long || number instanceof Integer
                || number instanceof Short || number instanceof Byte) {
            Number n = (Number) number;
            return this.format(n.longValue(), toAppendTo, pos);
        }
        if (number instanceof Number) {
            Number n = (Number) number;
            return this.format(n.doubleValue(), toAppendTo, pos);
        }
        throw new IllegalArgumentException("Cannot format given Object as a Number");
    }

    public final String format(double number) {
        return this.format(number, new StringBuffer(), new FieldPosition(0)).toString();
    }

    public final String format(long number) {
        return this.format(number, new StringBuffer(), new FieldPosition(0)).toString();
    }

    // The two seams a concrete formatter fills in.
    public abstract StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos);

    public abstract StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition pos);
}
