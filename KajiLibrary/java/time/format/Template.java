package java.time.format;

// The grammar of a format template: which symbols it allows, in what order and how many times.
//
// ===============================================================================================
// WHY THERE ARE TWO WAYS TO FAIL
// ===============================================================================================
//
// A badly written template --`abc`, `dE` the wrong way round, `ddd`-- is a mistake in the program
// and gives `IllegalArgumentException`. A well written template that the locale cannot format
// --`w`, `zzz`-- is not a mistake in the program: it gives `DateTimeException`, and it could
// resolve in another locale. The distinction is the JDK's and it has to be respected, because a
// program catching one of the two does not expect the other.
//
// The count limits come from measuring JDK 25 symbol by symbol, not from reading the documentation:
// `y` and `w` have no cap --`yyyyyyyyyyyyyy` is a valid template that does not resolve-- and `K`
// and `k` are never allowed, which is the least expected thing about either.
final class Template {

    // The symbols in the order they have to appear in, and how many times at most. A zero means
    // there is no cap. The ones that are not here --`K`, `k`, `a`, `L`, `c` and all the rest-- are
    // not allowed.
    private static final String ORDER = "GyQMwEdBhHjmsvz";
    private static final int[] CAP = {5, 0, 5, 5, 0, 5, 2, 5, 2, 2, 2, 2, 2, 4, 4};

    // Where the time half begins. The last eight symbols of the order --from `B` on-- are time
    // symbols; `B` is the day period, which is time even though it carries no numbers.
    private static final int FIRST_OF_TIME = 7;

    private Template() {
    }

    /**
     * Checks that the template is well written.
     *
     * @param template the template
     * @throws IllegalArgumentException if it is not
     */
    static void check(String template) {
        int expected = 0;
        int i = 0;
        while (i < template.length()) {
            char c = template.charAt(i);
            int which = ORDER.indexOf(c);
            if (which < 0 || which < expected) {
                throw new IllegalArgumentException(
                        "Requested template \"" + template + "\" is invalid.");
            }
            int howMany = 0;
            while (i < template.length() && template.charAt(i) == c) {
                howMany = howMany + 1;
                i = i + 1;
            }
            if (CAP[which] > 0 && howMany > CAP[which]) {
                throw new IllegalArgumentException(
                        "Requested template \"" + template + "\" is invalid.");
            }
            expected = which + 1;
        }
    }

    /**
     * Where the date half ends and the time half begins.
     *
     * <p>The cut is unique because the order of the symbols is fixed: everything of the date comes
     * before everything of the time.
     *
     * @param template the template, already checked
     * @return the position, which may be zero or the whole length
     */
    static int cut(String template) {
        for (int i = 0; i < template.length(); i++) {
            if (ORDER.indexOf(template.charAt(i)) >= FIRST_OF_TIME) {
                return i;
            }
        }
        return template.length();
    }
}
