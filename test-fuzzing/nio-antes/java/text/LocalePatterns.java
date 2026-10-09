package java.text;

import java.util.Locale;

/**
 * The patterns --not the words-- each locale writes numbers and dates with.
 *
 * <p>It is the package's third data file, and the one that was missing.
 * {@link DecimalFormatSymbols} says which characters a number is drawn with,
 * {@link DateFormatSymbols} says what the months are called, and here is what neither of them keeps:
 * the <em>order</em>. That Germany writes {@code 1.234,50 EUR} and the United States
 * {@code $1,234.50} is not a matter of symbols --both use the same four-- but of where each thing
 * goes, and that lives in the pattern.
 *
 * <p>It is not public: the JDK does not expose this table either. It is reached through
 * {@code NumberFormat.getCurrencyInstance(locale)} and {@code DateFormat.getDateInstance(style,
 * locale)}, which is the way a caller asks for it.
 *
 * <p>The rows are {@code DecimalFormatSymbols}'s same six, in the same order, and resolve with its
 * same {@code indexOf} -- an unknown locale falls back to ROOT, which is what the JDK does with a
 * locale it has no data for. Every pattern was extracted by running JDK 25 (the same methodology as
 * the other two tables, not transcription), and each non-ASCII character is written as
 * {@code \\uXXXX}: several of them are spaces that cannot be seen -- French separates the percentage
 * with {@code U+00A0} and English separates the a.m./p.m. with {@code U+202F}. An ordinary space in
 * their place is no cosmetic detail: it changes the result of an {@code equals}.
 */
final class LocalePatterns {

    private LocalePatterns() {
    }

    static final int FULL = 0;
    static final int LONG = 1;
    static final int MEDIUM = 2;
    static final int SHORT = 3;

    // The decimal pattern and the integer one are the same in all six rows. They are left as a
    // table all the same, and not as a constant, because the row is this class's unit: collapsing
    // them would hide that they are locale data and not a universal rule.
    private static String[] number() {
        return new String[] {"#,##0.###", "#,##0.###", "#,##0.###", "#,##0.###", "#,##0.###", "#,##0.###"};
    }

    private static String[] integerPart() {
        return new String[] {"#,##0", "#,##0", "#,##0", "#,##0", "#,##0", "#,##0"};
    }

    // What tells these rows apart is which side the currency sign (¤) goes on and which hard space
    // separates it. The yen carries no decimals: the pattern says so, not a special case in the
    // code.
    private static String[] currency() {
        return new String[] {
            "\u00a4\u00a0#,##0.00",
            "\u00a4#,##0.00",
            "\u00a4\u00a0#,##0.00",
            "#,##0.00\u00a0\u00a4",
            "#,##0.00\u00a0\u00a4",
            "\u00a4#,##0",
        };
    }

    private static String[] percentSign() {
        return new String[] {
            "#,##0%", "#,##0%", "#,##0%", "#,##0\u00a0%", "#,##0\u00a0%", "#,##0%",
        };
    }

    private static String[] dateRow(int i) {
        if (i == 1) {
            return new String[] {"EEEE, MMMM d, y", "MMMM d, y", "MMM d, y", "M/d/yy"};
        }
        if (i == 2) {
            return new String[] {"EEEE, d 'de' MMMM 'de' y", "d 'de' MMMM 'de' y", "d MMM y", "d/M/yy"};
        }
        if (i == 3) {
            return new String[] {"EEEE, d. MMMM y", "d. MMMM y", "dd.MM.y", "dd.MM.yy"};
        }
        if (i == 4) {
            return new String[] {"EEEE d MMMM y", "d MMMM y", "d MMM y", "dd/MM/y"};
        }
        if (i == 5) {
            return new String[] {
                "y\u5e74M\u6708d\u65e5EEEE", "y\u5e74M\u6708d\u65e5", "y/MM/dd", "y/MM/dd",
            };
        }
        return new String[] {"y MMMM d, EEEE", "y MMMM d", "y MMM d", "y-MM-dd"};
    }

    private static String[] timeRow(int i) {
        if (i == 1 || i == 2) {
            // A 12-hour clock with U+202F (NARROW NO-BREAK SPACE) before the a.m./p.m., not an
            // ordinary space: whoever compares the result against an ASCII space will not find
            // it.
            return new String[] {
                "h:mm:ss\u202fa zzzz", "h:mm:ss\u202fa z", "h:mm:ss\u202fa", "h:mm\u202fa",
            };
        }
        if (i == 5) {
            return new String[] {
                "H\u6642mm\u5206ss\u79d2 zzzz", "H:mm:ss z", "H:mm:ss", "H:mm",
            };
        }
        return new String[] {"HH:mm:ss zzzz", "HH:mm:ss z", "HH:mm:ss", "HH:mm"};
    }

    // What goes between the date and the time when they are combined. It is per locale AND per
    // style: French uses a comma in the three long styles and only a space in the short one.
    private static String[] glueRow(int i) {
        if (i == 1 || i == 2 || i == 3) {
            return new String[] {", ", ", ", ", ", ", "};
        }
        if (i == 4) {
            return new String[] {", ", ", ", ", ", " "};
        }
        return new String[] {" ", " ", " ", " "};
    }

    static String number(Locale l) {
        return LocalePatterns.number()[DecimalFormatSymbols.indexOf(l)];
    }

    static String integerPart(Locale l) {
        return LocalePatterns.integerPart()[DecimalFormatSymbols.indexOf(l)];
    }

    static String currency(Locale l) {
        return LocalePatterns.currency()[DecimalFormatSymbols.indexOf(l)];
    }

    static String percentSign(Locale l) {
        return LocalePatterns.percentSign()[DecimalFormatSymbols.indexOf(l)];
    }

    static String date(int style, Locale l) {
        return LocalePatterns.dateRow(DecimalFormatSymbols.indexOf(l))[style];
    }

    static String hour(int style, Locale l) {
        return LocalePatterns.timeRow(DecimalFormatSymbols.indexOf(l))[style];
    }

    /**
     * It combines date and time choosing the separator by the GREATER of the two styles (bearing in
     * mind that FULL is 0 and SHORT is 3, so "the greater" is the shorter one).
     *
     * <p>The rule is not an invention: it was checked against the sixteen style combinations in the
     * six locales and reproduces exactly what JDK 25 returns. It matters only where the glue varies
     * by style, which among these rows is French.
     */
    static String dateTime(int dateStyle, int timeStyle, Locale l) {
        int i = DecimalFormatSymbols.indexOf(l);
        int greaterOf = dateStyle;
        if (timeStyle > greaterOf) {
            greaterOf = timeStyle;
        }
        return LocalePatterns.dateRow(i)[dateStyle]
                + LocalePatterns.glueRow(i)[greaterOf]
                + LocalePatterns.timeRow(i)[timeStyle];
    }
}
