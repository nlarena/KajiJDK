package java.time.format;

import java.util.Locale;

// The format templates --`yMMMd`, `Hm`-- and the pattern each one gets in each language.
//
// ===============================================================================================
// WHAT A TEMPLATE IS AND HOW IT DIFFERS FROM A PATTERN
// ===============================================================================================
//
// A template says WHICH fields are wanted and in how much detail: `yMMMd` is "the year, the month
// with a short name, and the day". A pattern says ALSO in what order and with which separators, and
// that changes from language to language: the same template gives `MMM d, y` in English and `d. MMM
// y` in German. The template is what a program can write without knowing which language it will
// come out in.
//
// ===============================================================================================
// HOW IT IS RESOLVED
// ===============================================================================================
//
// By table, not by algorithm. The template is split into its date half and its time half --the
// order of the symbols is fixed, so the cut is unique-- each half is looked up, and they are joined
// with the language's glue. The combinations that do not come out of that joining live apart, in
// the exceptions table: they are the ones starting with the day of the week alone, where the
// language uses the context form (`E`) instead of the standalone one (`ccc`).
//
// A template that is not in the table is not invented: it is answered with `DateTimeException`,
// which is what the JDK does. The table was extracted by running JDK 25 --asking it for the 38,880
// date templates and the 8,910 time templates the grammar allows, and writing down the ones it
// resolved along with each language's 2,600 to 3,550 combinations-- which is the same methodology
// as the library's other data tables.
//
// The six rows are `DecimalFormatSymbols`'s, in the same order, and are resolved by the same rule
// as `DayPeriods`.
final class LocaleTemplates {

    private LocaleTemplates() {
    }

    /** Each row's glue: what goes between the date and the time. */
    private static final String[] GLUE = {" ", ", ", ", ", ", ", " ", " "};

    // ---- und ----

    private static String[] dateKeys0() {
        return new String[] {
            "E", "EEE", "EEEE", "EEEEE", "EEEEEd", "EEEEd", "EEEd", "Ed", "Gy", "GyM",
            "GyMEEEEEd", "GyMEEEEd", "GyMEEEd", "GyMEd", "GyMM", "GyMMEEEEEd", "GyMMEEEEd",
            "GyMMEEEd", "GyMMEd", "GyMMM", "GyMMMEEEEEd", "GyMMMEEEEd", "GyMMMEEEd", "GyMMMEd",
            "GyMMMM", "GyMMMMEEEEEd", "GyMMMMEEEEd", "GyMMMMEEEd", "GyMMMMEd", "GyMMMMM",
            "GyMMMMMEEEEEd", "GyMMMMMEEEEd", "GyMMMMMEEEd", "GyMMMMMEd", "GyMMMMMd", "GyMMMMd",
            "GyMMMd", "GyMMd", "GyMd", "M", "MEEEEEd", "MEEEEd", "MEEEd", "MEd", "MM",
            "MMEEEEEd", "MMEEEEd", "MMEEEd", "MMEd", "MMM", "MMMEEEEEd", "MMMEEEEd", "MMMEEEd",
            "MMMEd", "MMMM", "MMMMEEEEEd", "MMMMEEEEd", "MMMMEEEd", "MMMMEd", "MMMMM",
            "MMMMMEEEEEd", "MMMMMEEEEd", "MMMMMEEEd", "MMMMMEd", "MMMMMd", "MMMMd", "MMMd",
            "MMd", "Md", "d", "y", "yM", "yMEEEEEd", "yMEEEEd", "yMEEEd", "yMEd", "yMM",
            "yMMEEEEEd", "yMMEEEEd", "yMMEEEd", "yMMEd", "yMMM", "yMMMEEEEEd", "yMMMEEEEd",
            "yMMMEEEd", "yMMMEd", "yMMMM", "yMMMMEEEEEd", "yMMMMEEEEd", "yMMMMEEEd", "yMMMMEd",
            "yMMMMM", "yMMMMMEEEEEd", "yMMMMMEEEEd", "yMMMMMEEEd", "yMMMMMEd", "yMMMMMd",
            "yMMMMd", "yMMMd", "yMMd", "yMd", "yQQQ", "yQQQQ", "yw", "yyyy", "yyyyM",
            "yyyyMEEEEEd", "yyyyMEEEEd", "yyyyMEEEd", "yyyyMEd", "yyyyMM", "yyyyMMEEEEEd",
            "yyyyMMEEEEd", "yyyyMMEEEd", "yyyyMMEd", "yyyyMMM", "yyyyMMMEEEEEd", "yyyyMMMEEEEd",
            "yyyyMMMEEEd", "yyyyMMMEd", "yyyyMMMM", "yyyyMMMMEEEEEd", "yyyyMMMMEEEEd",
            "yyyyMMMMEEEd", "yyyyMMMMEd", "yyyyMMMMM", "yyyyMMMMMEEEEEd", "yyyyMMMMMEEEEd",
            "yyyyMMMMMEEEd", "yyyyMMMMMEd", "yyyyMMMMMd", "yyyyMMMMd", "yyyyMMMd", "yyyyMMd",
            "yyyyMd", "yyyyQQQ", "yyyyQQQQ",
        };
    }

    private static String[] datePatterns0() {
        return new String[] {
            "ccc", "ccc", "ccc", "ccc", "d, E", "d, E", "d, E", "d, E", "G y", "G y MMM",
            "G y MMM d, E", "G y MMM d, E", "G y MMM d, E", "G y MMM d, E", "G y MMM",
            "G y MMM d, E", "G y MMM d, E", "G y MMM d, E", "G y MMM d, E", "G y MMM",
            "G y MMM d, E", "G y MMM d, E", "G y MMM d, E", "G y MMM d, E", "G y MMM",
            "G y MMM d, E", "G y MMM d, E", "G y MMM d, E", "G y MMM d, E", "G y MMM",
            "G y MMM d, E", "G y MMM d, E", "G y MMM d, E", "G y MMM d, E", "G y MMM d",
            "G y MMM d", "G y MMM d", "GGGGG y-MM-dd", "GGGGG y-MM-dd", "L", "MM-dd, E",
            "MM-dd, E", "MM-dd, E", "MM-dd, E", "L", "MM-dd, E", "MM-dd, E", "MM-dd, E",
            "MM-dd, E", "LLL", "MMM d, E", "MMM d, E", "MMM d, E", "MMM d, E", "LLL",
            "MMM d, E", "MMM d, E", "MMM d, E", "MMM d, E", "LLL", "MMM d, E", "MMM d, E",
            "MMM d, E", "MMM d, E", "MMMM d", "MMMM d", "MMM d", "MM-dd", "MM-dd", "d", "y",
            "y-MM", "y-MM-dd, E", "y-MM-dd, E", "y-MM-dd, E", "y-MM-dd, E", "y-MM",
            "y-MM-dd, E", "y-MM-dd, E", "y-MM-dd, E", "y-MM-dd, E", "y MMM", "y MMM d, E",
            "y MMM d, E", "y MMM d, E", "y MMM d, E", "y MMMM", "y MMM d, E", "y MMM d, E",
            "y MMM d, E", "y MMM d, E", "y MMMM", "y MMM d, E", "y MMM d, E", "y MMM d, E",
            "y MMM d, E", "y MMM d", "y MMM d", "y MMM d", "y-MM-dd", "y-MM-dd", "y QQQ",
            "y QQQQ", "'week' w 'of' Y", "G y", "GGGGG y-MM", "GGGGG y-MM-dd, E",
            "GGGGG y-MM-dd, E", "GGGGG y-MM-dd, E", "GGGGG y-MM-dd, E", "GGGGG y-MM",
            "GGGGG y-MM-dd, E", "GGGGG y-MM-dd, E", "GGGGG y-MM-dd, E", "GGGGG y-MM-dd, E",
            "G y MMM", "G y MMM d, E", "G y MMM d, E", "G y MMM d, E", "G y MMM d, E",
            "G y MMMM", "G y MMM d, E", "G y MMM d, E", "G y MMM d, E", "G y MMM d, E",
            "G y MMMM", "G y MMM d, E", "G y MMM d, E", "G y MMM d, E", "G y MMM d, E",
            "G y MMM d", "G y MMM d", "G y MMM d", "GGGGG y-MM-dd", "GGGGG y-MM-dd", "G y QQQ",
            "G y QQQQ",
        };
    }

    private static String[] timeKeys0() {
        return new String[] {
            "Bh", "Bhm", "Bhms", "H", "Hm", "Hms", "Hmsv", "Hmv", "h", "hm", "hms", "hmsv",
            "hmv", "j", "jm", "jms", "jmsv", "jmv", "ms",
        };
    }

    private static String[] timePatterns0() {
        return new String[] {
            "h B", "h:mm B", "h:mm:ss B", "HH", "HH:mm", "HH:mm:ss", "HH:mm:ss v", "HH:mm v",
            "h\u202fa", "h:mm a", "h:mm:ss a", "h:mm:ss a v", "h:mm a v", "HH", "HH:mm",
            "HH:mm:ss", "HH:mm:ss v", "HH:mm v", "mm:ss",
        };
    }

    private static String[] excKeys0() {
        return new String[] {
            "E+Bhm", "E+Bhms", "E+Hm", "E+Hms", "E+hm", "E+hms", "E+jm", "E+jms", "EEE+Bhm",
            "EEE+Bhms", "EEE+Hm", "EEE+Hms", "EEE+hm", "EEE+hms", "EEE+jm", "EEE+jms",
            "EEEE+Bhm", "EEEE+Bhms", "EEEE+Hm", "EEEE+Hms", "EEEE+hm", "EEEE+hms", "EEEE+jm",
            "EEEE+jms", "EEEEE+Bhm", "EEEEE+Bhms", "EEEEE+Hm", "EEEEE+Hms", "EEEEE+hm",
            "EEEEE+hms", "EEEEE+jm", "EEEEE+jms",
        };
    }

    private static String[] excPatterns0() {
        return new String[] {
            "E h:mm B", "E h:mm:ss B", "E HH:mm", "E HH:mm:ss", "E h:mm a", "E h:mm:ss a",
            "E HH:mm", "E HH:mm:ss", "E h:mm B", "E h:mm:ss B", "E HH:mm", "E HH:mm:ss",
            "E h:mm a", "E h:mm:ss a", "E HH:mm", "E HH:mm:ss", "E h:mm B", "E h:mm:ss B",
            "E HH:mm", "E HH:mm:ss", "E h:mm a", "E h:mm:ss a", "E HH:mm", "E HH:mm:ss",
            "E h:mm B", "E h:mm:ss B", "E HH:mm", "E HH:mm:ss", "E h:mm a", "E h:mm:ss a",
            "E HH:mm", "E HH:mm:ss",
        };
    }

    // ---- en-US ----

    private static String[] dateKeys1() {
        return new String[] {
            "E", "EEE", "EEEE", "EEEEE", "EEEEEd", "EEEEd", "EEEd", "Ed", "Gy", "GyM",
            "GyMEEEEEd", "GyMEEEEd", "GyMEEEd", "GyMEd", "GyMM", "GyMMEEEEEd", "GyMMEEEEd",
            "GyMMEEEd", "GyMMEd", "GyMMM", "GyMMMEEEEEd", "GyMMMEEEEd", "GyMMMEEEd", "GyMMMEd",
            "GyMMMM", "GyMMMMEEEEEd", "GyMMMMEEEEd", "GyMMMMEEEd", "GyMMMMEd", "GyMMMMM",
            "GyMMMMMEEEEEd", "GyMMMMMEEEEd", "GyMMMMMEEEd", "GyMMMMMEd", "GyMMMMMd", "GyMMMMd",
            "GyMMMd", "GyMMd", "GyMd", "M", "MEEEEEd", "MEEEEd", "MEEEd", "MEd", "MM",
            "MMEEEEEd", "MMEEEEd", "MMEEEd", "MMEd", "MMM", "MMMEEEEEd", "MMMEEEEd", "MMMEEEd",
            "MMMEd", "MMMM", "MMMMEEEEEd", "MMMMEEEEd", "MMMMEEEd", "MMMMEd", "MMMMM",
            "MMMMMEEEEEd", "MMMMMEEEEd", "MMMMMEEEd", "MMMMMEd", "MMMMMd", "MMMMd", "MMMd",
            "MMd", "Md", "d", "y", "yM", "yMEEEEEd", "yMEEEEd", "yMEEEd", "yMEd", "yMM",
            "yMMEEEEEd", "yMMEEEEd", "yMMEEEd", "yMMEd", "yMMM", "yMMMEEEEEd", "yMMMEEEEd",
            "yMMMEEEd", "yMMMEd", "yMMMM", "yMMMMEEEEEd", "yMMMMEEEEd", "yMMMMEEEd", "yMMMMEd",
            "yMMMMM", "yMMMMMEEEEEd", "yMMMMMEEEEd", "yMMMMMEEEd", "yMMMMMEd", "yMMMMMd",
            "yMMMMd", "yMMMd", "yMMd", "yMd", "yQQQ", "yQQQQ", "yw", "yyyy", "yyyyM",
            "yyyyMEEEEEd", "yyyyMEEEEd", "yyyyMEEEd", "yyyyMEd", "yyyyMM", "yyyyMMEEEEEd",
            "yyyyMMEEEEd", "yyyyMMEEEd", "yyyyMMEd", "yyyyMMM", "yyyyMMMEEEEEd", "yyyyMMMEEEEd",
            "yyyyMMMEEEd", "yyyyMMMEd", "yyyyMMMM", "yyyyMMMMEEEEEd", "yyyyMMMMEEEEd",
            "yyyyMMMMEEEd", "yyyyMMMMEd", "yyyyMMMMM", "yyyyMMMMMEEEEEd", "yyyyMMMMMEEEEd",
            "yyyyMMMMMEEEd", "yyyyMMMMMEd", "yyyyMMMMMd", "yyyyMMMMd", "yyyyMMMd", "yyyyMMd",
            "yyyyMd", "yyyyQQQ", "yyyyQQQQ",
        };
    }

    private static String[] datePatterns1() {
        return new String[] {
            "ccc", "ccc", "ccc", "ccc", "d E", "d E", "d E", "d E", "y G", "MMM y G",
            "E, MMM d, y G", "E, MMM d, y G", "E, MMM d, y G", "E, MMM d, y G", "MMM y G",
            "E, MMM d, y G", "E, MMM d, y G", "E, MMM d, y G", "E, MMM d, y G", "MMM y G",
            "E, MMM d, y G", "E, MMM d, y G", "E, MMM d, y G", "E, MMM d, y G", "MMM y G",
            "E, MMM d, y G", "E, MMM d, y G", "E, MMM d, y G", "E, MMM d, y G", "MMM y G",
            "E, MMM d, y G", "E, MMM d, y G", "E, MMM d, y G", "E, MMM d, y G", "MMM d, y G",
            "MMM d, y G", "MMM d, y G", "M/d/y G", "M/d/y G", "L", "E, M/d", "E, M/d", "E, M/d",
            "E, M/d", "L", "E, M/d", "E, M/d", "E, M/d", "E, M/d", "LLL", "E, MMM d",
            "E, MMM d", "E, MMM d", "E, MMM d", "LLL", "E, MMM d", "E, MMM d", "E, MMM d",
            "E, MMM d", "LLL", "E, MMM d", "E, MMM d", "E, MMM d", "E, MMM d", "MMMM d",
            "MMMM d", "MMM d", "M/d", "M/d", "d", "y", "M/y", "E, M/d/y", "E, M/d/y",
            "E, M/d/y", "E, M/d/y", "M/y", "E, M/d/y", "E, M/d/y", "E, M/d/y", "E, M/d/y",
            "MMM y", "E, MMM d, y", "E, MMM d, y", "E, MMM d, y", "E, MMM d, y", "MMMM y",
            "E, MMM d, y", "E, MMM d, y", "E, MMM d, y", "E, MMM d, y", "MMMM y", "E, MMM d, y",
            "E, MMM d, y", "E, MMM d, y", "E, MMM d, y", "MMM d, y", "MMM d, y", "MMM d, y",
            "M/d/y", "M/d/y", "QQQ y", "QQQQ y", "'week' w 'of' Y", "y G", "M/y GGGGG",
            "E, M/d/y GGGGG", "E, M/d/y GGGGG", "E, M/d/y GGGGG", "E, M/d/y GGGGG", "M/y GGGGG",
            "E, M/d/y GGGGG", "E, M/d/y GGGGG", "E, M/d/y GGGGG", "E, M/d/y GGGGG", "MMM y G",
            "E, MMM d, y G", "E, MMM d, y G", "E, MMM d, y G", "E, MMM d, y G", "MMMM y G",
            "E, MMM d, y G", "E, MMM d, y G", "E, MMM d, y G", "E, MMM d, y G", "MMMM y G",
            "E, MMM d, y G", "E, MMM d, y G", "E, MMM d, y G", "E, MMM d, y G", "MMM d, y G",
            "MMM d, y G", "MMM d, y G", "M/d/y GGGGG", "M/d/y GGGGG", "QQQ y G", "QQQQ y G",
        };
    }

    private static String[] timeKeys1() {
        return new String[] {
            "Bh", "Bhm", "Bhms", "Bj", "Bjm", "Bjms", "H", "Hm", "Hms", "Hmsv", "Hmv", "h",
            "hm", "hms", "hmsv", "hmv", "j", "jm", "jms", "jmsv", "jmv", "ms",
        };
    }

    private static String[] timePatterns1() {
        return new String[] {
            "h B", "h:mm B", "h:mm:ss B", "h B", "h:mm B", "h:mm:ss B", "HH", "HH:mm",
            "HH:mm:ss", "HH:mm:ss v", "HH:mm v", "h\u202fa", "h:mm\u202fa", "h:mm:ss\u202fa",
            "h:mm:ss\u202fa v", "h:mm\u202fa v", "h\u202fa", "h:mm\u202fa", "h:mm:ss\u202fa",
            "h:mm:ss\u202fa v", "h:mm\u202fa v", "mm:ss",
        };
    }

    private static String[] excKeys1() {
        return new String[] {
            "E+Bhm", "E+Bhms", "E+Bjm", "E+Bjms", "E+Hm", "E+Hms", "E+hm", "E+hms", "E+jm",
            "E+jms", "EEE+Bhm", "EEE+Bhms", "EEE+Bjm", "EEE+Bjms", "EEE+Hm", "EEE+Hms",
            "EEE+hm", "EEE+hms", "EEE+jm", "EEE+jms", "EEEE+Bhm", "EEEE+Bhms", "EEEE+Bjm",
            "EEEE+Bjms", "EEEE+Hm", "EEEE+Hms", "EEEE+hm", "EEEE+hms", "EEEE+jm", "EEEE+jms",
            "EEEEE+Bhm", "EEEEE+Bhms", "EEEEE+Bjm", "EEEEE+Bjms", "EEEEE+Hm", "EEEEE+Hms",
            "EEEEE+hm", "EEEEE+hms", "EEEEE+jm", "EEEEE+jms",
        };
    }

    private static String[] excPatterns1() {
        return new String[] {
            "E h:mm B", "E h:mm:ss B", "E h:mm B", "E h:mm:ss B", "E HH:mm", "E HH:mm:ss",
            "E h:mm\u202fa", "E h:mm:ss\u202fa", "E h:mm\u202fa", "E h:mm:ss\u202fa",
            "E h:mm B", "E h:mm:ss B", "E h:mm B", "E h:mm:ss B", "E HH:mm", "E HH:mm:ss",
            "E h:mm\u202fa", "E h:mm:ss\u202fa", "E h:mm\u202fa", "E h:mm:ss\u202fa",
            "E h:mm B", "E h:mm:ss B", "E h:mm B", "E h:mm:ss B", "E HH:mm", "E HH:mm:ss",
            "E h:mm\u202fa", "E h:mm:ss\u202fa", "E h:mm\u202fa", "E h:mm:ss\u202fa",
            "E h:mm B", "E h:mm:ss B", "E h:mm B", "E h:mm:ss B", "E HH:mm", "E HH:mm:ss",
            "E h:mm\u202fa", "E h:mm:ss\u202fa", "E h:mm\u202fa", "E h:mm:ss\u202fa",
        };
    }

    // ---- es-AR ----

    private static String[] dateKeys2() {
        return new String[] {
            "E", "EEE", "EEEE", "EEEEE", "EEEEEd", "EEEEd", "EEEd", "Ed", "Gy", "GyM",
            "GyMEEEEEd", "GyMEEEEd", "GyMEEEd", "GyMEd", "GyMM", "GyMMEEEEEd", "GyMMEEEEd",
            "GyMMEEEd", "GyMMEd", "GyMMM", "GyMMMEEEEEd", "GyMMMEEEEd", "GyMMMEEEd", "GyMMMEd",
            "GyMMMM", "GyMMMMEEEEEd", "GyMMMMEEEEd", "GyMMMMEEEd", "GyMMMMEd", "GyMMMMM",
            "GyMMMMMEEEEEd", "GyMMMMMEEEEd", "GyMMMMMEEEd", "GyMMMMMEd", "GyMMMMMd", "GyMMMMd",
            "GyMMMd", "GyMMd", "GyMd", "M", "MEEEEEd", "MEEEEd", "MEEEd", "MEd", "MM",
            "MMEEEEEd", "MMEEEEd", "MMEEEd", "MMEd", "MMM", "MMMEEEEEd", "MMMEEEEd", "MMMEEEd",
            "MMMEd", "MMMM", "MMMMEEEEEd", "MMMMEEEEd", "MMMMEEEd", "MMMMEd", "MMMMM",
            "MMMMMEEEEEd", "MMMMMEEEEd", "MMMMMEEEd", "MMMMMEd", "MMMMMd", "MMMMMdd", "MMMMd",
            "MMMMdd", "MMMd", "MMMdd", "MMd", "MMdd", "Md", "Mdd", "d", "y", "yM", "yMEEEEEd",
            "yMEEEEd", "yMEEEd", "yMEd", "yMM", "yMMEEEEEd", "yMMEEEEd", "yMMEEEd", "yMMEd",
            "yMMM", "yMMMEEEEEd", "yMMMEEEEd", "yMMMEEEd", "yMMMEd", "yMMMM", "yMMMMEEEEEd",
            "yMMMMEEEEd", "yMMMMEEEd", "yMMMMEd", "yMMMMM", "yMMMMMEEEEEd", "yMMMMMEEEEd",
            "yMMMMMEEEd", "yMMMMMEd", "yMMMMMd", "yMMMMd", "yMMMd", "yMMd", "yMd", "yQQQ",
            "yQQQQ", "yw", "yyyy", "yyyyM", "yyyyMEEEEEd", "yyyyMEEEEd", "yyyyMEEEd", "yyyyMEd",
            "yyyyMM", "yyyyMMEEEEEd", "yyyyMMEEEEd", "yyyyMMEEEd", "yyyyMMEd", "yyyyMMM",
            "yyyyMMMEEEEEd", "yyyyMMMEEEEd", "yyyyMMMEEEd", "yyyyMMMEd", "yyyyMMMM",
            "yyyyMMMMEEEEEd", "yyyyMMMMEEEEd", "yyyyMMMMEEEd", "yyyyMMMMEd", "yyyyMMMMM",
            "yyyyMMMMMEEEEEd", "yyyyMMMMMEEEEd", "yyyyMMMMMEEEd", "yyyyMMMMMEd", "yyyyMMMMMd",
            "yyyyMMMMd", "yyyyMMMd", "yyyyMMd", "yyyyMd", "yyyyQQQ", "yyyyQQQQ",
        };
    }

    private static String[] datePatterns2() {
        return new String[] {
            "ccc", "ccc", "ccc", "ccc", "E d", "E d", "E d", "E d", "y G", "MMMM 'de' y G",
            "E, d 'de' MMMM 'de' y G", "E, d 'de' MMMM 'de' y G", "E, d 'de' MMMM 'de' y G",
            "E, d 'de' MMMM 'de' y G", "MMM 'de' y G", "E, d 'de' MMM 'de' y G",
            "E, d 'de' MMM 'de' y G", "E, d 'de' MMM 'de' y G", "E, d 'de' MMM 'de' y G",
            "MMM 'de' y G", "E, d 'de' MMM 'de' y G", "E, d 'de' MMM 'de' y G",
            "E, d 'de' MMM 'de' y G", "E, d 'de' MMM 'de' y G", "MMMM 'de' y G",
            "E, d 'de' MMMM 'de' y G", "E, d 'de' MMMM 'de' y G", "E, d 'de' MMMM 'de' y G",
            "E, d 'de' MMMM 'de' y G", "MMMM 'de' y G", "E, d 'de' MMMM 'de' y G",
            "E, d 'de' MMMM 'de' y G", "E, d 'de' MMMM 'de' y G", "E, d 'de' MMMM 'de' y G",
            "d 'de' MMMM 'de' y G", "d 'de' MMMM 'de' y G", "d MMM y G", "d/M/y GGGGG",
            "d/M/y GGGGG", "L", "E d-M", "E d-M", "E d-M", "E d-M", "L", "E d-M", "E d-M",
            "E d-M", "E d-M", "LLL", "E, d 'de' MMM", "E, d 'de' MMM", "E, d 'de' MMM",
            "E, d 'de' MMM", "LLL", "E, d 'de' MMMM", "E, d 'de' MMMM", "E, d 'de' MMMM",
            "E, d 'de' MMMM", "LLL", "E, d 'de' MMMM", "E, d 'de' MMMM", "E, d 'de' MMMM",
            "E, d 'de' MMMM", "d 'de' MMMM", "dd-MMM", "d 'de' MMMM", "dd-MMM", "d 'de' MMM",
            "dd-MMM", "d/M", "d/M", "d/M", "dd-MMM", "d", "y G", "M-y", "E, d/M/y", "E, d/M/y",
            "E, d/M/y", "E, d/M/y", "M/y", "E, d/M/y", "E, d/M/y", "E, d/M/y", "E, d/M/y",
            "MMM y", "E, d MMM y", "E, d MMM y", "E, d MMM y", "E, d MMM y", "MMMM 'de' y",
            "EEE, d 'de' MMMM 'de' y", "EEE, d 'de' MMMM 'de' y", "EEE, d 'de' MMMM 'de' y",
            "EEE, d 'de' MMMM 'de' y", "MMMM 'de' y", "EEE, d 'de' MMMM 'de' y",
            "EEE, d 'de' MMMM 'de' y", "EEE, d 'de' MMMM 'de' y", "EEE, d 'de' MMMM 'de' y",
            "d 'de' MMMM 'de' y", "d 'de' MMMM 'de' y", "d 'de' MMM 'de' y", "d/M/y", "d/M/y",
            "QQQ 'de' y", "QQQQ 'de' y", "'semana' w 'de' Y", "y G", "M-y G", "E d/M/y GGGGG",
            "E d/M/y GGGGG", "E d/M/y GGGGG", "E d/M/y GGGGG", "M-y G", "E d/M/y GGGGG",
            "E d/M/y GGGGG", "E d/M/y GGGGG", "E d/M/y GGGGG", "MMM 'de' y G",
            "EEE, d 'de' MMM 'de' y G", "EEE, d 'de' MMM 'de' y G", "EEE, d 'de' MMM 'de' y G",
            "EEE, d 'de' MMM 'de' y G", "MMMM 'de' y G", "E, d 'de' MMMM 'de' y G",
            "E, d 'de' MMMM 'de' y G", "E, d 'de' MMMM 'de' y G", "E, d 'de' MMMM 'de' y G",
            "MMMM 'de' y G", "E, d 'de' MMMM 'de' y G", "E, d 'de' MMMM 'de' y G",
            "E, d 'de' MMMM 'de' y G", "E, d 'de' MMMM 'de' y G", "d 'de' MMMM 'de' y G",
            "d 'de' MMMM 'de' y G", "d 'de' MMM 'de' y G", "d/M/y GGGGG", "d/M/y GGGGG",
            "QQQ 'de' y G", "QQQQ 'de' y G",
        };
    }

    private static String[] timeKeys2() {
        return new String[] {
            "Bh", "Bhm", "Bhms", "Bj", "Bjm", "Bjms", "H", "Hm", "Hms", "Hmsv", "Hmsvvvv",
            "Hmv", "h", "hm", "hms", "hmsv", "hmsvvvv", "hmv", "j", "jm", "jms", "jmsv",
            "jmsvvvv", "jmv", "ms",
        };
    }

    private static String[] timePatterns2() {
        return new String[] {
            "h B", "h:mm B", "h:mm:ss B", "h B", "h:mm B", "h:mm:ss B", "H", "H:mm", "H:mm:ss",
            "HH:mm:ss v", "HH:mm:ss (vvvv)", "HH:mm v", "h\u202fa", "h:mm\u202fa", "hh:mm:ss",
            "h:mm:ss\u202fa v", "h:mm:ss\u202fa (vvvv)", "h:mm\u202fa v", "h\u202fa",
            "h:mm\u202fa", "hh:mm:ss", "h:mm:ss\u202fa v", "h:mm:ss\u202fa (vvvv)",
            "h:mm\u202fa v", "mm:ss",
        };
    }

    private static String[] excKeys2() {
        return new String[] {
            "E+Bhm", "E+Bhms", "E+Bjm", "E+Bjms", "E+Hm", "E+Hms", "E+hm", "E+hms", "E+jm",
            "E+jms", "EEE+Bhm", "EEE+Bhms", "EEE+Bjm", "EEE+Bjms", "EEE+Hm", "EEE+Hms",
            "EEE+hm", "EEE+hms", "EEE+jm", "EEE+jms", "EEEE+Bhm", "EEEE+Bhms", "EEEE+Bjm",
            "EEEE+Bjms", "EEEE+Hm", "EEEE+Hms", "EEEE+hm", "EEEE+hms", "EEEE+jm", "EEEE+jms",
            "EEEEE+Bhm", "EEEEE+Bhms", "EEEEE+Bjm", "EEEEE+Bjms", "EEEEE+Hm", "EEEEE+Hms",
            "EEEEE+hm", "EEEEE+hms", "EEEEE+jm", "EEEEE+jms",
        };
    }

    private static String[] excPatterns2() {
        return new String[] {
            "E h:mm B", "E h:mm:ss B", "E h:mm B", "E h:mm:ss B", "E, HH:mm", "E, HH:mm:ss",
            "E, h:mm\u202fa", "E, h:mm:ss\u202fa", "E, h:mm\u202fa", "E, h:mm:ss\u202fa",
            "E h:mm B", "E h:mm:ss B", "E h:mm B", "E h:mm:ss B", "E, HH:mm", "E, HH:mm:ss",
            "E, h:mm\u202fa", "E, h:mm:ss\u202fa", "E, h:mm\u202fa", "E, h:mm:ss\u202fa",
            "E h:mm B", "E h:mm:ss B", "E h:mm B", "E h:mm:ss B", "E, HH:mm", "E, HH:mm:ss",
            "E, h:mm\u202fa", "E, h:mm:ss\u202fa", "E, h:mm\u202fa", "E, h:mm:ss\u202fa",
            "E h:mm B", "E h:mm:ss B", "E h:mm B", "E h:mm:ss B", "E, HH:mm", "E, HH:mm:ss",
            "E, h:mm\u202fa", "E, h:mm:ss\u202fa", "E, h:mm\u202fa", "E, h:mm:ss\u202fa",
        };
    }

    // ---- de-DE ----

    private static String[] dateKeys3() {
        return new String[] {
            "E", "EEE", "EEEE", "EEEEE", "EEEEEd", "EEEEd", "EEEd", "Ed", "Gy", "GyM",
            "GyMEEEEEd", "GyMEEEEd", "GyMEEEd", "GyMEd", "GyMM", "GyMMEEEEEd", "GyMMEEEEd",
            "GyMMEEEd", "GyMMEd", "GyMMM", "GyMMMEEEEEd", "GyMMMEEEEd", "GyMMMEEEd", "GyMMMEd",
            "GyMMMM", "GyMMMMEEEEEd", "GyMMMMEEEEd", "GyMMMMEEEd", "GyMMMMEd", "GyMMMMM",
            "GyMMMMMEEEEEd", "GyMMMMMEEEEd", "GyMMMMMEEEd", "GyMMMMMEd", "GyMMMMMd", "GyMMMMd",
            "GyMMMd", "GyMMd", "GyMd", "M", "MEEEEEd", "MEEEEd", "MEEEd", "MEd", "MM",
            "MMEEEEEd", "MMEEEEd", "MMEEEd", "MMEd", "MMM", "MMMEEEEEd", "MMMEEEEd", "MMMEEEd",
            "MMMEd", "MMMM", "MMMMEEEEEd", "MMMMEEEEd", "MMMMEEEd", "MMMMEd", "MMMMM",
            "MMMMMEEEEEd", "MMMMMEEEEd", "MMMMMEEEd", "MMMMMEd", "MMMMMd", "MMMMMdd", "MMMMd",
            "MMMMdd", "MMMd", "MMMdd", "MMd", "MMdd", "Md", "Mdd", "d", "y", "yM", "yMEEEEEd",
            "yMEEEEd", "yMEEEd", "yMEd", "yMM", "yMMEEEEEd", "yMMEEEEd", "yMMEEEd", "yMMEd",
            "yMMM", "yMMMEEEEEd", "yMMMEEEEd", "yMMMEEEd", "yMMMEd", "yMMMM", "yMMMMEEEEEd",
            "yMMMMEEEEd", "yMMMMEEEd", "yMMMMEd", "yMMMMM", "yMMMMMEEEEEd", "yMMMMMEEEEd",
            "yMMMMMEEEd", "yMMMMMEd", "yMMMMMd", "yMMMMMdd", "yMMMMd", "yMMMMdd", "yMMMd",
            "yMMMdd", "yMMd", "yMMdd", "yMd", "yMdd", "yQQQ", "yQQQQ", "yw", "yyyy", "yyyyM",
            "yyyyMEEEEEd", "yyyyMEEEEd", "yyyyMEEEd", "yyyyMEd", "yyyyMM", "yyyyMMEEEEEd",
            "yyyyMMEEEEd", "yyyyMMEEEd", "yyyyMMEd", "yyyyMMM", "yyyyMMMEEEEEd", "yyyyMMMEEEEd",
            "yyyyMMMEEEd", "yyyyMMMEd", "yyyyMMMM", "yyyyMMMMEEEEEd", "yyyyMMMMEEEEd",
            "yyyyMMMMEEEd", "yyyyMMMMEd", "yyyyMMMMM", "yyyyMMMMMEEEEEd", "yyyyMMMMMEEEEd",
            "yyyyMMMMMEEEd", "yyyyMMMMMEd", "yyyyMMMMMd", "yyyyMMMMd", "yyyyMMMd", "yyyyMMd",
            "yyyyMd", "yyyyQQQ", "yyyyQQQQ",
        };
    }

    private static String[] datePatterns3() {
        return new String[] {
            "ccc", "ccc", "ccc", "ccc", "E, d.", "E, d.", "E, d.", "E, d.", "y G", "MMM y G",
            "E, d. MMM y G", "E, d. MMM y G", "E, d. MMM y G", "E, d. MMM y G", "MMM y G",
            "E, d. MMM y G", "E, d. MMM y G", "E, d. MMM y G", "E, d. MMM y G", "MMM y G",
            "E, d. MMM y G", "E, d. MMM y G", "E, d. MMM y G", "E, d. MMM y G", "MMM y G",
            "E, d. MMM y G", "E, d. MMM y G", "E, d. MMM y G", "E, d. MMM y G", "MMM y G",
            "E, d. MMM y G", "E, d. MMM y G", "E, d. MMM y G", "E, d. MMM y G", "d. MMM y G",
            "d. MMM y G", "d. MMM y G", "dd.MM.y G", "dd.MM.y G", "L", "E, d.M.", "E, d.M.",
            "E, d.M.", "E, d.M.", "L", "E, d.M.", "E, d.M.", "E, d.M.", "E, d.M.", "LLL",
            "E, d. MMM", "E, d. MMM", "E, d. MMM", "E, d. MMM", "LLL", "E, d. MMMM",
            "E, d. MMMM", "E, d. MMMM", "E, d. MMMM", "LLL", "E, d. MMMM", "E, d. MMMM",
            "E, d. MMMM", "E, d. MMMM", "d. MMMM", "dd.MM.", "d. MMMM", "dd.MM.", "d. MMM",
            "dd.MM.", "d.MM.", "dd.MM.", "d.M.", "dd.MM.", "d", "y G", "M/y", "E, d.M.y",
            "E, d.M.y", "E, d.M.y", "E, d.M.y", "MM.y", "E, d.M.y", "E, d.M.y", "E, d.M.y",
            "E, d.M.y", "MMM y", "E, d. MMM y", "E, d. MMM y", "E, d. MMM y", "E, d. MMM y",
            "MMMM y", "E, d. MMM y", "E, d. MMM y", "E, d. MMM y", "E, d. MMM y", "MMMM y",
            "E, d. MMM y", "E, d. MMM y", "E, d. MMM y", "E, d. MMM y", "d. MMM y", "dd.MM.y",
            "d. MMM y", "dd.MM.y", "d. MMM y", "dd.MM.y", "d.M.y", "dd.MM.y", "d.M.y",
            "dd.MM.y", "QQQ y", "QQQQ y", "'Woche' w 'des' 'Jahres' Y", "y G", "M/y GGGGG",
            "E, d.M.y GGGGG", "E, d.M.y GGGGG", "E, d.M.y GGGGG", "E, d.M.y GGGGG", "M/y GGGGG",
            "E, d.M.y GGGGG", "E, d.M.y GGGGG", "E, d.M.y GGGGG", "E, d.M.y GGGGG", "MMM y G",
            "E, d. MMM y G", "E, d. MMM y G", "E, d. MMM y G", "E, d. MMM y G", "MMMM y G",
            "E, d. MMM y G", "E, d. MMM y G", "E, d. MMM y G", "E, d. MMM y G", "MMMM y G",
            "E, d. MMM y G", "E, d. MMM y G", "E, d. MMM y G", "E, d. MMM y G", "d. MMM y G",
            "d. MMM y G", "d. MMM y G", "d.M.y GGGGG", "d.M.y GGGGG", "QQQ y G", "QQQQ y G",
        };
    }

    private static String[] timeKeys3() {
        return new String[] {
            "Bh", "Bhm", "Bhms", "H", "Hm", "Hms", "Hmsv", "Hmv", "h", "hm", "hms", "hmsv",
            "hmv", "j", "jm", "jms", "jmsv", "jmv", "ms",
        };
    }

    private static String[] timePatterns3() {
        return new String[] {
            "h 'Uhr' B", "h:mm B", "h:mm:ss B", "HH 'Uhr'", "HH:mm", "HH:mm:ss", "HH:mm:ss v",
            "HH:mm v", "h 'Uhr' a", "h:mm\u202fa", "h:mm:ss\u202fa", "h:mm:ss\u202fa v",
            "h:mm\u202fa v", "HH 'Uhr'", "HH:mm", "HH:mm:ss", "HH:mm:ss v", "HH:mm v", "mm:ss",
        };
    }

    private static String[] excKeys3() {
        return new String[] {
            "E+Bhm", "E+Bhms", "E+Hm", "E+Hms", "E+hm", "E+hms", "E+jm", "E+jms", "EEE+Bhm",
            "EEE+Bhms", "EEE+Hm", "EEE+Hms", "EEE+hm", "EEE+hms", "EEE+jm", "EEE+jms",
            "EEEE+Bhm", "EEEE+Bhms", "EEEE+Hm", "EEEE+Hms", "EEEE+hm", "EEEE+hms", "EEEE+jm",
            "EEEE+jms", "EEEEE+Bhm", "EEEEE+Bhms", "EEEEE+Hm", "EEEEE+Hms", "EEEEE+hm",
            "EEEEE+hms", "EEEEE+jm", "EEEEE+jms",
        };
    }

    private static String[] excPatterns3() {
        return new String[] {
            "E h:mm B", "E h:mm:ss B", "E, HH:mm", "E, HH:mm:ss", "E h:mm\u202fa",
            "E, h:mm:ss\u202fa", "E, HH:mm", "E, HH:mm:ss", "E h:mm B", "E h:mm:ss B",
            "E, HH:mm", "E, HH:mm:ss", "E h:mm\u202fa", "E, h:mm:ss\u202fa", "E, HH:mm",
            "E, HH:mm:ss", "E h:mm B", "E h:mm:ss B", "E, HH:mm", "E, HH:mm:ss",
            "E h:mm\u202fa", "E, h:mm:ss\u202fa", "E, HH:mm", "E, HH:mm:ss", "E h:mm B",
            "E h:mm:ss B", "E, HH:mm", "E, HH:mm:ss", "E h:mm\u202fa", "E, h:mm:ss\u202fa",
            "E, HH:mm", "E, HH:mm:ss",
        };
    }

    // ---- fr-FR ----

    private static String[] dateKeys4() {
        return new String[] {
            "E", "EEE", "EEEE", "EEEEE", "EEEEEd", "EEEEd", "EEEd", "Ed", "Gy", "GyM",
            "GyMEEEEEd", "GyMEEEEd", "GyMEEEd", "GyMEd", "GyMM", "GyMMEEEEEd", "GyMMEEEEd",
            "GyMMEEEd", "GyMMEd", "GyMMM", "GyMMMEEEEEd", "GyMMMEEEEd", "GyMMMEEEd", "GyMMMEd",
            "GyMMMM", "GyMMMMEEEEEd", "GyMMMMEEEEd", "GyMMMMEEEd", "GyMMMMEd", "GyMMMMM",
            "GyMMMMMEEEEEd", "GyMMMMMEEEEd", "GyMMMMMEEEd", "GyMMMMMEd", "GyMMMMMd", "GyMMMMd",
            "GyMMMd", "GyMMd", "GyMd", "M", "MEEEEEd", "MEEEEd", "MEEEd", "MEd", "MM",
            "MMEEEEEd", "MMEEEEd", "MMEEEd", "MMEd", "MMM", "MMMEEEEEd", "MMMEEEEd", "MMMEEEd",
            "MMMEd", "MMMM", "MMMMEEEEEd", "MMMMEEEEd", "MMMMEEEd", "MMMMEd", "MMMMM",
            "MMMMMEEEEEd", "MMMMMEEEEd", "MMMMMEEEd", "MMMMMEd", "MMMMMd", "MMMMd", "MMMd",
            "MMd", "Md", "d", "y", "yM", "yMEEEEEd", "yMEEEEd", "yMEEEd", "yMEd", "yMM",
            "yMMEEEEEd", "yMMEEEEd", "yMMEEEd", "yMMEd", "yMMM", "yMMMEEEEEd", "yMMMEEEEd",
            "yMMMEEEd", "yMMMEd", "yMMMM", "yMMMMEEEEEd", "yMMMMEEEEd", "yMMMMEEEd", "yMMMMEd",
            "yMMMMM", "yMMMMMEEEEEd", "yMMMMMEEEEd", "yMMMMMEEEd", "yMMMMMEd", "yMMMMMd",
            "yMMMMd", "yMMMd", "yMMd", "yMd", "yQQQ", "yQQQQ", "yw", "yyyy", "yyyyM",
            "yyyyMEEEEEd", "yyyyMEEEEd", "yyyyMEEEd", "yyyyMEd", "yyyyMM", "yyyyMMEEEEEd",
            "yyyyMMEEEEd", "yyyyMMEEEd", "yyyyMMEd", "yyyyMMM", "yyyyMMMEEEEEd", "yyyyMMMEEEEd",
            "yyyyMMMEEEd", "yyyyMMMEd", "yyyyMMMM", "yyyyMMMMEEEEEd", "yyyyMMMMEEEEd",
            "yyyyMMMMEEEd", "yyyyMMMMEd", "yyyyMMMMM", "yyyyMMMMMEEEEEd", "yyyyMMMMMEEEEd",
            "yyyyMMMMMEEEd", "yyyyMMMMMEd", "yyyyMMMMMd", "yyyyMMMMd", "yyyyMMMd", "yyyyMMd",
            "yyyyMd", "yyyyQQQ", "yyyyQQQQ",
        };
    }

    private static String[] datePatterns4() {
        return new String[] {
            "E", "E", "E", "E", "E d", "E d", "E d", "E d", "y G", "MMM y G", "E d MMM y G",
            "E d MMM y G", "E d MMM y G", "E d MMM y G", "MMM y G", "E d MMM y G",
            "E d MMM y G", "E d MMM y G", "E d MMM y G", "MMM y G", "E d MMM y G",
            "E d MMM y G", "E d MMM y G", "E d MMM y G", "MMM y G", "E d MMM y G",
            "E d MMM y G", "E d MMM y G", "E d MMM y G", "MMM y G", "E d MMM y G",
            "E d MMM y G", "E d MMM y G", "E d MMM y G", "d MMM y G", "d MMM y G", "d MMM y G",
            "dd/MM/y GGGGG", "dd/MM/y GGGGG", "L", "E dd/MM", "E dd/MM", "E dd/MM", "E dd/MM",
            "L", "E dd/MM", "E dd/MM", "E dd/MM", "E dd/MM", "LLL", "E d MMM", "E d MMM",
            "E d MMM", "E d MMM", "LLL", "E d MMM", "E d MMM", "E d MMM", "E d MMM", "LLL",
            "E d MMM", "E d MMM", "E d MMM", "E d MMM", "d MMMM", "d MMMM", "d MMM", "dd/MM",
            "dd/MM", "d", "y G", "MM/y", "E dd/MM/y", "E dd/MM/y", "E dd/MM/y", "E dd/MM/y",
            "MM/y", "E dd/MM/y", "E dd/MM/y", "E dd/MM/y", "E dd/MM/y", "MMM y", "E d MMM y",
            "E d MMM y", "E d MMM y", "E d MMM y", "MMMM y", "E d MMM y", "E d MMM y",
            "E d MMM y", "E d MMM y", "MMMM y", "E d MMM y", "E d MMM y", "E d MMM y",
            "E d MMM y", "d MMM y", "d MMM y", "d MMM y", "dd/MM/y", "dd/MM/y", "QQQ y",
            "QQQQ y", "'semaine' w 'de' Y", "y G", "MM/y GGGGG", "E dd/MM/y GGGGG",
            "E dd/MM/y GGGGG", "E dd/MM/y GGGGG", "E dd/MM/y GGGGG", "MM/y GGGGG",
            "E dd/MM/y GGGGG", "E dd/MM/y GGGGG", "E dd/MM/y GGGGG", "E dd/MM/y GGGGG",
            "MMM y G", "E d MMM y G", "E d MMM y G", "E d MMM y G", "E d MMM y G", "MMMM y G",
            "E d MMM y G", "E d MMM y G", "E d MMM y G", "E d MMM y G", "MMMM y G",
            "E d MMM y G", "E d MMM y G", "E d MMM y G", "E d MMM y G", "d MMM y G",
            "d MMM y G", "d MMM y G", "dd/MM/y GGGGG", "dd/MM/y GGGGG", "QQQ y G", "QQQQ y G",
        };
    }

    private static String[] timeKeys4() {
        return new String[] {
            "Bh", "Bhm", "Bhms", "H", "Hm", "Hms", "Hmsv", "Hmv", "h", "hm", "hms", "hmsv",
            "hmv", "j", "jm", "jms", "jmsv", "jmv", "ms",
        };
    }

    private static String[] timePatterns4() {
        return new String[] {
            "h B", "h:mm B", "h:mm:ss B", "HH 'h'", "HH:mm", "HH:mm:ss", "HH:mm:ss v",
            "HH:mm v", "h\u202fa", "h:mm\u202fa", "h:mm:ss\u202fa", "h:mm:ss\u202fa v",
            "h:mm\u202fa v", "HH 'h'", "HH:mm", "HH:mm:ss", "HH:mm:ss v", "HH:mm v", "mm:ss",
        };
    }

    private static String[] excKeys4() {
        return new String[] {

        };
    }

    private static String[] excPatterns4() {
        return new String[] {

        };
    }

    // ---- ja-JP ----

    private static String[] dateKeys5() {
        return new String[] {
            "E", "EEE", "EEEE", "EEEEE", "EEEEEd", "EEEEd", "EEEd", "Ed", "Gy", "GyM",
            "GyMEEEEEd", "GyMEEEEd", "GyMEEEd", "GyMEd", "GyMM", "GyMMEEEEEd", "GyMMEEEEd",
            "GyMMEEEd", "GyMMEd", "GyMMM", "GyMMMEEEEEd", "GyMMMEEEEd", "GyMMMEEEd", "GyMMMEd",
            "GyMMMM", "GyMMMMEEEEEd", "GyMMMMEEEEd", "GyMMMMEEEd", "GyMMMMEd", "GyMMMMM",
            "GyMMMMMEEEEEd", "GyMMMMMEEEEd", "GyMMMMMEEEd", "GyMMMMMEd", "GyMMMMMd", "GyMMMMd",
            "GyMMMd", "GyMMd", "GyMd", "M", "MEEEEEd", "MEEEEd", "MEEEd", "MEd", "MM",
            "MMEEEEEd", "MMEEEEd", "MMEEEd", "MMEd", "MMM", "MMMEEEEEd", "MMMEEEEd", "MMMEEEd",
            "MMMEd", "MMMM", "MMMMEEEEEd", "MMMMEEEEd", "MMMMEEEd", "MMMMEd", "MMMMM",
            "MMMMMEEEEEd", "MMMMMEEEEd", "MMMMMEEEd", "MMMMMEd", "MMMMMd", "MMMMd", "MMMd",
            "MMd", "Md", "d", "y", "yM", "yMEEEEEd", "yMEEEEd", "yMEEEd", "yMEd", "yMM",
            "yMMEEEEEd", "yMMEEEEd", "yMMEEEd", "yMMEd", "yMMM", "yMMMEEEEEd", "yMMMEEEEd",
            "yMMMEEEd", "yMMMEd", "yMMMM", "yMMMMEEEEEd", "yMMMMEEEEd", "yMMMMEEEd", "yMMMMEd",
            "yMMMMM", "yMMMMMEEEEEd", "yMMMMMEEEEd", "yMMMMMEEEd", "yMMMMMEd", "yMMMMMd",
            "yMMMMd", "yMMMd", "yMMd", "yMd", "yQQQ", "yQQQQ", "yw", "yyyy", "yyyyM",
            "yyyyMEEEEEd", "yyyyMEEEEd", "yyyyMEEEd", "yyyyMEd", "yyyyMM", "yyyyMMEEEEEd",
            "yyyyMMEEEEd", "yyyyMMEEEd", "yyyyMMEd", "yyyyMMM", "yyyyMMMEEEEEd", "yyyyMMMEEEEd",
            "yyyyMMMEEEd", "yyyyMMMEd", "yyyyMMMM", "yyyyMMMMEEEEEd", "yyyyMMMMEEEEd",
            "yyyyMMMMEEEd", "yyyyMMMMEd", "yyyyMMMMM", "yyyyMMMMMEEEEEd", "yyyyMMMMMEEEEd",
            "yyyyMMMMMEEEd", "yyyyMMMMMEd", "yyyyMMMMMd", "yyyyMMMMd", "yyyyMMMd", "yyyyMMd",
            "yyyyMd", "yyyyQQQ", "yyyyQQQQ",
        };
    }

    private static String[] datePatterns5() {
        return new String[] {
            "ccc", "ccc", "ccc", "ccc", "d\u65e5EEEE", "d\u65e5EEEE", "d\u65e5(E)",
            "d\u65e5(E)", "Gy\u5e74", "Gy\u5e74M\u6708", "Gy\u5e74M\u6708d\u65e5EEEE",
            "Gy\u5e74M\u6708d\u65e5EEEE", "Gy\u5e74M\u6708d\u65e5(E)",
            "Gy\u5e74M\u6708d\u65e5(E)", "Gy\u5e74M\u6708", "Gy\u5e74M\u6708d\u65e5EEEE",
            "Gy\u5e74M\u6708d\u65e5EEEE", "Gy\u5e74M\u6708d\u65e5(E)",
            "Gy\u5e74M\u6708d\u65e5(E)", "Gy\u5e74M\u6708", "Gy\u5e74M\u6708d\u65e5EEEE",
            "Gy\u5e74M\u6708d\u65e5EEEE", "Gy\u5e74M\u6708d\u65e5(E)",
            "Gy\u5e74M\u6708d\u65e5(E)", "Gy\u5e74M\u6708", "Gy\u5e74M\u6708d\u65e5EEEE",
            "Gy\u5e74M\u6708d\u65e5EEEE", "Gy\u5e74M\u6708d\u65e5(E)",
            "Gy\u5e74M\u6708d\u65e5(E)", "Gy\u5e74M\u6708", "Gy\u5e74M\u6708d\u65e5EEEE",
            "Gy\u5e74M\u6708d\u65e5EEEE", "Gy\u5e74M\u6708d\u65e5(E)",
            "Gy\u5e74M\u6708d\u65e5(E)", "Gy\u5e74M\u6708d\u65e5", "Gy\u5e74M\u6708d\u65e5",
            "Gy\u5e74M\u6708d\u65e5", "Gy/M/d", "Gy/M/d", "M\u6708", "M/dEEEE", "M/dEEEE",
            "M/d(E)", "M/d(E)", "M\u6708", "M/dEEEE", "M/dEEEE", "M/d(E)", "M/d(E)", "M\u6708",
            "M\u6708d\u65e5EEEE", "M\u6708d\u65e5EEEE", "M\u6708d\u65e5(E)",
            "M\u6708d\u65e5(E)", "M\u6708", "M\u6708d\u65e5EEEE", "M\u6708d\u65e5EEEE",
            "M\u6708d\u65e5(E)", "M\u6708d\u65e5(E)", "M\u6708", "M\u6708d\u65e5EEEE",
            "M\u6708d\u65e5EEEE", "M\u6708d\u65e5(E)", "M\u6708d\u65e5(E)", "M\u6708d\u65e5",
            "M\u6708d\u65e5", "M\u6708d\u65e5", "M/d", "M/d", "d\u65e5", "y\u5e74", "y/M",
            "y/M/dEEEE", "y/M/dEEEE", "y/M/d(E)", "y/M/d(E)", "y/MM", "y/M/dEEEE", "y/M/dEEEE",
            "y/M/d(E)", "y/M/d(E)", "y\u5e74M\u6708", "y\u5e74M\u6708d\u65e5EEEE",
            "y\u5e74M\u6708d\u65e5EEEE", "y\u5e74M\u6708d\u65e5(E)", "y\u5e74M\u6708d\u65e5(E)",
            "y\u5e74M\u6708", "y\u5e74M\u6708d\u65e5EEEE", "y\u5e74M\u6708d\u65e5EEEE",
            "y\u5e74M\u6708d\u65e5(E)", "y\u5e74M\u6708d\u65e5(E)", "y\u5e74M\u6708",
            "y\u5e74M\u6708d\u65e5EEEE", "y\u5e74M\u6708d\u65e5EEEE",
            "y\u5e74M\u6708d\u65e5(E)", "y\u5e74M\u6708d\u65e5(E)", "y\u5e74M\u6708d\u65e5",
            "y\u5e74M\u6708d\u65e5", "y\u5e74M\u6708d\u65e5", "y/M/d", "y/M/d", "y/QQQ",
            "y\u5e74QQQQ", "Y\u5e74\u7b2cw\u9031", "Gy\u5e74", "GGGGGy/M", "GGGGGy/M/d(EEEE)",
            "GGGGGy/M/d(EEEE)", "GGGGGy/M/d(E)", "GGGGGy/M/d(E)", "GGGGGy/M",
            "GGGGGy/M/d(EEEE)", "GGGGGy/M/d(EEEE)", "GGGGGy/M/d(E)", "GGGGGy/M/d(E)",
            "Gy\u5e74M\u6708", "Gy\u5e74M\u6708d\u65e5(EEEE)", "Gy\u5e74M\u6708d\u65e5(EEEE)",
            "Gy\u5e74M\u6708d\u65e5(E)", "Gy\u5e74M\u6708d\u65e5(E)", "Gy\u5e74M\u6708",
            "Gy\u5e74M\u6708d\u65e5(EEEE)", "Gy\u5e74M\u6708d\u65e5(EEEE)",
            "Gy\u5e74M\u6708d\u65e5(E)", "Gy\u5e74M\u6708d\u65e5(E)", "Gy\u5e74M\u6708",
            "Gy\u5e74M\u6708d\u65e5(EEEE)", "Gy\u5e74M\u6708d\u65e5(EEEE)",
            "Gy\u5e74M\u6708d\u65e5(E)", "Gy\u5e74M\u6708d\u65e5(E)", "Gy\u5e74M\u6708d\u65e5",
            "Gy\u5e74M\u6708d\u65e5", "Gy\u5e74M\u6708d\u65e5", "GGGGGy/M/d", "GGGGGy/M/d",
            "Gy/QQQ", "Gy\u5e74QQQQ",
        };
    }

    private static String[] timeKeys5() {
        return new String[] {
            "Bh", "Bhm", "Bhms", "H", "Hm", "Hms", "Hmsv", "Hmv", "h", "hm", "hms", "hmsv",
            "hmv", "j", "jm", "jms", "jmsv", "jmv", "ms",
        };
    }

    private static String[] timePatterns5() {
        return new String[] {
            "BK\u6642", "BK:mm", "BK:mm:ss", "H\u6642", "H:mm", "H:mm:ss", "H:mm:ss v",
            "H:mm v", "aK\u6642", "aK:mm", "aK:mm:ss", "aK:mm:ss v", "aK:mm v", "H\u6642",
            "H:mm", "H:mm:ss", "H:mm:ss v", "H:mm v", "mm:ss",
        };
    }

    private static String[] excKeys5() {
        return new String[] {
            "E+Bhm", "E+Bhms", "E+Hm", "E+Hms", "E+hm", "E+hms", "E+jm", "E+jms", "EEE+Bhm",
            "EEE+Bhms", "EEE+Hm", "EEE+Hms", "EEE+hm", "EEE+hms", "EEE+jm", "EEE+jms",
            "EEEE+Bhm", "EEEE+Bhms", "EEEE+Hm", "EEEE+Hms", "EEEE+hm", "EEEE+hms", "EEEE+jm",
            "EEEE+jms", "EEEEE+Bhm", "EEEEE+Bhms", "EEEEE+Hm", "EEEEE+Hms", "EEEEE+hm",
            "EEEEE+hms", "EEEEE+jm", "EEEEE+jms",
        };
    }

    private static String[] excPatterns5() {
        return new String[] {
            "BK:mm (E)", "BK:mm:ss (E)", "H:mm (E)", "H:mm:ss (E)", "aK:mm (E)", "aK:mm:ss (E)",
            "H:mm (E)", "H:mm:ss (E)", "BK:mm (E)", "BK:mm:ss (E)", "H:mm (E)", "H:mm:ss (E)",
            "aK:mm (E)", "aK:mm:ss (E)", "H:mm (E)", "H:mm:ss (E)", "BK:mm (E)", "BK:mm:ss (E)",
            "H:mm (E)", "H:mm:ss (E)", "aK:mm (E)", "aK:mm:ss (E)", "H:mm (E)", "H:mm:ss (E)",
            "BK:mm (E)", "BK:mm:ss (E)", "H:mm (E)", "H:mm:ss (E)", "aK:mm (E)", "aK:mm:ss (E)",
            "H:mm (E)", "H:mm:ss (E)",
        };
    }

    private static String[] dateKeys(int i) {
        if (i == 0) {
            return dateKeys0();
        }
        if (i == 1) {
            return dateKeys1();
        }
        if (i == 2) {
            return dateKeys2();
        }
        if (i == 3) {
            return dateKeys3();
        }
        if (i == 4) {
            return dateKeys4();
        }
        return dateKeys5();
    }

    private static String[] datePatterns(int i) {
        if (i == 0) {
            return datePatterns0();
        }
        if (i == 1) {
            return datePatterns1();
        }
        if (i == 2) {
            return datePatterns2();
        }
        if (i == 3) {
            return datePatterns3();
        }
        if (i == 4) {
            return datePatterns4();
        }
        return datePatterns5();
    }

    private static String[] timeKeys(int i) {
        if (i == 0) {
            return timeKeys0();
        }
        if (i == 1) {
            return timeKeys1();
        }
        if (i == 2) {
            return timeKeys2();
        }
        if (i == 3) {
            return timeKeys3();
        }
        if (i == 4) {
            return timeKeys4();
        }
        return timeKeys5();
    }

    private static String[] timePatterns(int i) {
        if (i == 0) {
            return timePatterns0();
        }
        if (i == 1) {
            return timePatterns1();
        }
        if (i == 2) {
            return timePatterns2();
        }
        if (i == 3) {
            return timePatterns3();
        }
        if (i == 4) {
            return timePatterns4();
        }
        return timePatterns5();
    }

    private static String[] excKeys(int i) {
        if (i == 0) {
            return excKeys0();
        }
        if (i == 1) {
            return excKeys1();
        }
        if (i == 2) {
            return excKeys2();
        }
        if (i == 3) {
            return excKeys3();
        }
        if (i == 4) {
            return excKeys4();
        }
        return excKeys5();
    }

    private static String[] excPatterns(int i) {
        if (i == 0) {
            return excPatterns0();
        }
        if (i == 1) {
            return excPatterns1();
        }
        if (i == 2) {
            return excPatterns2();
        }
        if (i == 3) {
            return excPatterns3();
        }
        if (i == 4) {
            return excPatterns4();
        }
        return excPatterns5();
    }

    /** The tags of the six rows, in the order they are in. */
    private static final String[] TAGS = {"und", "en-US", "es-AR", "de-DE", "fr-FR", "ja-JP"};

    /** Which row that locale gets: exact tag, else the language alone, else row zero. */
    private static int index(Locale locale) {
        String lang = locale.getLanguage();
        String country = locale.getCountry();
        String full = country.length() > 0 ? lang + "-" + country : lang;
        for (int i = 0; i < TAGS.length; i++) {
            if (TAGS[i].equals(full)) {
                return i;
            }
        }
        if (lang.length() > 0) {
            for (int i = 0; i < TAGS.length; i++) {
                String e = TAGS[i];
                int dash = e.indexOf('-');
                String languageOnly = dash > 0 ? e.substring(0, dash) : e;
                if (languageOnly.equals(lang)) {
                    return i;
                }
            }
        }
        return 0;
    }

    private static String lookup(String[] keys, String[] patterns, String key) {
        for (int i = 0; i < keys.length; i++) {
            if (keys[i].equals(key)) {
                return patterns[i];
            }
        }
        return null;
    }

    /**
     * The pattern that language uses for that template.
     *
     * @param template the template, already checked by {@link Template#check}
     * @param locale in which language
     * @return the pattern, or {@code null} if that language has none for that template
     */
    static String pattern(String template, Locale locale) {
        int i = index(locale);
        int cut = Template.cut(template);
        String date = template.substring(0, cut);
        String time = template.substring(cut);
        if (time.isEmpty()) {
            return lookup(dateKeys(i), datePatterns(i), date);
        }
        if (date.isEmpty()) {
            return lookup(timeKeys(i), timePatterns(i), time);
        }
        String exception = lookup(excKeys(i), excPatterns(i), date + "+" + time);
        if (exception != null) {
            return exception;
        }
        String pf = lookup(dateKeys(i), datePatterns(i), date);
        String ph = lookup(timeKeys(i), timePatterns(i), time);
        if (pf == null || ph == null) {
            return null;
        }
        return pf + GLUE[i] + ph;
    }
}
