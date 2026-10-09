package java.time.format;

import java.util.Locale;

// The names of the periods of the day --"in the morning", "nachmittags", "madrugada"-- and the
// minute each one starts at.
//
// ===============================================================================================
// WHY IT IS A TABLE AND NOT A RULE
// ===============================================================================================
//
// Because the cuts are not universal and are not even regular. German splits the morning in two
// --`morgens` until ten, `vormittags` until noon-- and English does not; Spanish has `madrugada`
// from zero to six and French has nothing equivalent; Japanese goes back to `night` at 23. And
// `noon` lasts ONE minute in nearly every language, but in German it lasts an hour. Any rule
// written by eye is false in half the rows.
//
// The six rows are the same as `DecimalFormatSymbols`'s, in the same order, and are resolved with
// its same `indexOf`. They were extracted by running JDK 25 --asking it for all 1440 minutes of the
// day in each locale and each style, and writing down the cuts-- which is the same methodology as
// the library's other data tables, not a transcription.
//
// `SHORT` gives the same as `FULL` in all six rows, and the `_STANDALONE` forms give the same as
// their base: it was checked, and that is why there are two tables and not six.
//
// The locale resolution is written here --`index`-- and `DecimalFormatSymbols`'s is not called, it
// being package-private and in `java.text`. The rule is the same and it is short: exact tag, else
// the language alone, else row zero.
final class DayPeriods {

    private DayPeriods() {
    }

    // The long names; SHORT gives the same as FULL.
    private static final int[][] WIDE_END = {
        {720, 1440},   // und
        {1, 720, 721, 1080, 1260, 1440},   // en-US
        {360, 720, 721, 1200, 1440},   // es-AR
        {1, 300, 600, 720, 780, 1080, 1440},   // de-DE
        {1, 720, 721, 1080, 1440},   // fr-FR
        {1, 240, 720, 721, 960, 1140, 1380, 1440},   // ja-JP
    };

    private static final String[][] WIDE_NAME = {
        {"AM", "PM"},   // und
        {"midnight", "in the morning", "noon", "in the afternoon", "in the evening", "at night"},   // en-US
        {"madrugada", "ma\u00f1ana", "mediod\u00eda", "tarde", "noche"},   // es-AR
        {"Mitternacht", "nachts", "morgens", "vormittags", "mittags", "nachmittags", "abends"},   // de-DE
        {"minuit", "du matin", "midi", "de l\u2019apr\u00e8s-midi", "du soir"},   // fr-FR
        {"\u771f\u591c\u4e2d", "\u591c\u4e2d", "\u671d", "\u6b63\u5348", "\u663c", "\u5915\u65b9", "\u591c", "\u591c\u4e2d"},   // ja-JP
    };

    // The narrow names.
    private static final int[][] NARROW_END = {
        {720, 1440},   // und
        {1, 720, 721, 1080, 1260, 1440},   // en-US
        {360, 720, 721, 1200, 1440},   // es-AR
        {1, 300, 600, 720, 780, 1080, 1440},   // de-DE
        {1, 240, 720, 721, 1080, 1440},   // fr-FR
        {1, 240, 720, 721, 960, 1140, 1380, 1440},   // ja-JP
    };

    private static final String[][] NARROW_NAME = {
        {"AM", "PM"},   // und
        {"mi", "in the morning", "n", "in the afternoon", "in the evening", "at night"},   // en-US
        {"madrugada", "ma\u00f1ana", "mediod\u00eda", "tarde", "noche"},   // es-AR
        {"Mitternacht", "nachts", "morgens", "vorm.", "mittags", "nachm.", "abends"},   // de-DE
        {"minuit", "matin", "mat.", "midi", "ap.m.", "soir"},   // fr-FR
        {"\u771f\u591c\u4e2d", "\u591c\u4e2d", "\u671d", "\u6b63\u5348", "\u663c", "\u5915\u65b9", "\u591c", "\u591c\u4e2d"},   // ja-JP
    };

    /** The tags of the six rows, in the order they are in. */
    private static final String[] TAGS = {"und", "en-US", "es-AR", "de-DE", "fr-FR", "ja-JP"};

    /** Which row that locale gets; see the class note. */
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

    /**
     * The name of the period that minute of the day falls in.
     *
     * @param locale in which language
     * @param style in which style
     * @param minuteOfDay the minute, from 0 to 1439
     * @return the name
     */
    static String name(Locale locale, TextStyle style, int minuteOfDay) {
        int i = index(locale);
        boolean narrow = style == TextStyle.NARROW || style == TextStyle.NARROW_STANDALONE;
        int[] ends = narrow ? NARROW_END[i] : WIDE_END[i];
        String[] names = narrow ? NARROW_NAME[i] : WIDE_NAME[i];
        int k = 0;
        while (k < ends.length - 1 && minuteOfDay >= ends[k]) {
            k = k + 1;
        }
        return names[k];
    }

    /**
     * The names of that row, to recognize them while parsing.
     *
     * @param locale in which language
     * @param style in which style
     * @return the names, in the order of the stretches
     */
    static String[] names(Locale locale, TextStyle style) {
        int i = index(locale);
        boolean narrow = style == TextStyle.NARROW || style == TextStyle.NARROW_STANDALONE;
        return narrow ? NARROW_NAME[i] : WIDE_NAME[i];
    }

    /**
     * The middle minute of stretch number `k`, which is what a period resolves to when parsed.
     *
     * <p>It is what the JDK does: "in the morning" does not say what time it is, so the midpoint of
     * its stretch is taken. With integer division rounding down, just like over there.
     *
     * @param locale in which language
     * @param style in which style
     * @param k which stretch
     * @return the minute of the day
     */
    static int middle(Locale locale, TextStyle style, int k) {
        int i = index(locale);
        boolean narrow = style == TextStyle.NARROW || style == TextStyle.NARROW_STANDALONE;
        int[] ends = narrow ? NARROW_END[i] : WIDE_END[i];
        int from = k == 0 ? 0 : ends[k - 1];
        return (from + ends[k]) / 2;
    }
}
