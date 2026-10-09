package java.util;

// The flags a Formattable receives in its formatTo call, as bit masks: they say whether the
// specifier carried `-` (left justify), `#` (alternate form) or an uppercase conversion.
// A holder of constants — never instantiated.
public class FormattableFlags {

    // The specifier had `-`: pad on the right instead of the left.
    public static final int LEFT_JUSTIFY = 1;
    // The specifier had `#`: use the conversion's alternate form.
    public static final int ALTERNATE = 4;
    // The conversion character was uppercase: uppercase the result.
    public static final int UPPERCASE = 2;

    private FormattableFlags() {
    }
}
