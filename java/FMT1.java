import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Checks the eight members of {@code java.time.format} that used to be missing, against JDK 25.
 *
 * <h2>What is compared</h2>
 *
 * <p>The three template ones --{@code getLocalizedDateTimePattern(String, ...)},
 * {@code appendLocalized(String)} and {@code ofLocalizedPattern}-- over forty-five templates in the
 * six languages the library ships, including the combinations that do not come out of simple gluing
 * and the ones that do not resolve. {@code appendDayPeriodText} over the twenty-four hours of the
 * day in the six languages and in the two name sets. And the four zone ones over offsets, which is
 * the only kind of zone this library can build.
 *
 * <p>What is really formatted uses templates of numeric fields: this library's month and day names
 * exist in English only, which is a gap on the text side and not on the template side. Resolving
 * the template to the pattern is compared in the six languages all the same.
 *
 * <p>The boundary between the two ways of failing is compared too: a malformed template gives
 * {@link IllegalArgumentException} and a well-formed one the language cannot format gives
 * {@code DateTimeException}. Confusing them would break anyone catching only one.
 *
 * <p>{@link #where()} returns the index of the first answer that does not match, or -1.
 */
public class FMT1 {

    static final String[] EXPECTED = {
        "pat|und|y|y",
        "pat|und|yy|DateTimeException",
        "pat|und|yyyy|G y",
        "pat|und|M|L",
        "pat|und|MM|L",
        "pat|und|MMM|LLL",
        "pat|und|MMMM|LLL",
        "pat|und|MMMMM|LLL",
        "pat|und|d|d",
        "pat|und|E|ccc",
        "pat|und|EEE|ccc",
        "pat|und|EEEE|ccc",
        "pat|und|EEEEE|ccc",
        "pat|und|Md|MM-dd",
        "pat|und|MMMd|MMM d",
        "pat|und|MEd|MM-dd, E",
        "pat|und|MMMEd|MMM d, E",
        "pat|und|yM|y-MM",
        "pat|und|yMd|y-MM-dd",
        "pat|und|yMMM|y MMM",
        "pat|und|yMMMd|y MMM d",
        "pat|und|yMMMEd|y MMM d, E",
        "pat|und|yMMMM|y MMMM",
        "pat|und|yQQQ|y QQQ",
        "pat|und|yQQQQ|y QQQQ",
        "pat|und|GyMMMd|G y MMM d",
        "pat|und|GyMMMEd|G y MMM d, E",
        "pat|und|Q|DateTimeException",
        "pat|und|QQQ|DateTimeException",
        "pat|und|h|h\u202fa",
        "pat|und|H|HH",
        "pat|und|j|HH",
        "pat|und|hm|h:mm a",
        "pat|und|Hm|HH:mm",
        "pat|und|jm|HH:mm",
        "pat|und|hms|h:mm:ss a",
        "pat|und|Hms|HH:mm:ss",
        "pat|und|jms|HH:mm:ss",
        "pat|und|Hmv|HH:mm v",
        "pat|und|jmv|HH:mm v",
        "pat|und|Bhm|h:mm B",
        "pat|und|Bhms|h:mm:ss B",
        "pat|und|yMMMdHm|y MMM d HH:mm",
        "pat|und|yMMMdjms|y MMM d HH:mm:ss",
        "pat|und|MdHm|MM-dd HH:mm",
        "pat|und|EHm|E HH:mm",
        "pat|und|Ehm|E h:mm a",
        "pat|und|EEEHms|E HH:mm:ss",
        "pat|und|EEEEjm|E HH:mm",
        "pat|und|w|DateTimeException",
        "pat|und|ww|DateTimeException",
        "pat|und|yy|DateTimeException",
        "pat|und|zzz|DateTimeException",
        "pat|und|B|DateTimeException",
        "pat|und|QQQQQ|DateTimeException",
        "pat|und|yyy|DateTimeException",
        "pat|und||DateTimeException",
        "pat|und|vvvv|DateTimeException",
        "pat|und|hH|DateTimeException",
        "pat|en-US|y|y",
        "pat|en-US|yy|DateTimeException",
        "pat|en-US|yyyy|y G",
        "pat|en-US|M|L",
        "pat|en-US|MM|L",
        "pat|en-US|MMM|LLL",
        "pat|en-US|MMMM|LLL",
        "pat|en-US|MMMMM|LLL",
        "pat|en-US|d|d",
        "pat|en-US|E|ccc",
        "pat|en-US|EEE|ccc",
        "pat|en-US|EEEE|ccc",
        "pat|en-US|EEEEE|ccc",
        "pat|en-US|Md|M/d",
        "pat|en-US|MMMd|MMM d",
        "pat|en-US|MEd|E, M/d",
        "pat|en-US|MMMEd|E, MMM d",
        "pat|en-US|yM|M/y",
        "pat|en-US|yMd|M/d/y",
        "pat|en-US|yMMM|MMM y",
        "pat|en-US|yMMMd|MMM d, y",
        "pat|en-US|yMMMEd|E, MMM d, y",
        "pat|en-US|yMMMM|MMMM y",
        "pat|en-US|yQQQ|QQQ y",
        "pat|en-US|yQQQQ|QQQQ y",
        "pat|en-US|GyMMMd|MMM d, y G",
        "pat|en-US|GyMMMEd|E, MMM d, y G",
        "pat|en-US|Q|DateTimeException",
        "pat|en-US|QQQ|DateTimeException",
        "pat|en-US|h|h\u202fa",
        "pat|en-US|H|HH",
        "pat|en-US|j|h\u202fa",
        "pat|en-US|hm|h:mm\u202fa",
        "pat|en-US|Hm|HH:mm",
        "pat|en-US|jm|h:mm\u202fa",
        "pat|en-US|hms|h:mm:ss\u202fa",
        "pat|en-US|Hms|HH:mm:ss",
        "pat|en-US|jms|h:mm:ss\u202fa",
        "pat|en-US|Hmv|HH:mm v",
        "pat|en-US|jmv|h:mm\u202fa v",
        "pat|en-US|Bhm|h:mm B",
        "pat|en-US|Bhms|h:mm:ss B",
        "pat|en-US|yMMMdHm|MMM d, y, HH:mm",
        "pat|en-US|yMMMdjms|MMM d, y, h:mm:ss\u202fa",
        "pat|en-US|MdHm|M/d, HH:mm",
        "pat|en-US|EHm|E HH:mm",
        "pat|en-US|Ehm|E h:mm\u202fa",
        "pat|en-US|EEEHms|E HH:mm:ss",
        "pat|en-US|EEEEjm|E h:mm\u202fa",
        "pat|en-US|w|DateTimeException",
        "pat|en-US|ww|DateTimeException",
        "pat|en-US|yy|DateTimeException",
        "pat|en-US|zzz|DateTimeException",
        "pat|en-US|B|DateTimeException",
        "pat|en-US|QQQQQ|DateTimeException",
        "pat|en-US|yyy|DateTimeException",
        "pat|en-US||DateTimeException",
        "pat|en-US|vvvv|DateTimeException",
        "pat|en-US|hH|DateTimeException",
        "pat|es-AR|y|y G",
        "pat|es-AR|yy|DateTimeException",
        "pat|es-AR|yyyy|y G",
        "pat|es-AR|M|L",
        "pat|es-AR|MM|L",
        "pat|es-AR|MMM|LLL",
        "pat|es-AR|MMMM|LLL",
        "pat|es-AR|MMMMM|LLL",
        "pat|es-AR|d|d",
        "pat|es-AR|E|ccc",
        "pat|es-AR|EEE|ccc",
        "pat|es-AR|EEEE|ccc",
        "pat|es-AR|EEEEE|ccc",
        "pat|es-AR|Md|d/M",
        "pat|es-AR|MMMd|d 'de' MMM",
        "pat|es-AR|MEd|E d-M",
        "pat|es-AR|MMMEd|E, d 'de' MMM",
        "pat|es-AR|yM|M-y",
        "pat|es-AR|yMd|d/M/y",
        "pat|es-AR|yMMM|MMM y",
        "pat|es-AR|yMMMd|d 'de' MMM 'de' y",
        "pat|es-AR|yMMMEd|E, d MMM y",
        "pat|es-AR|yMMMM|MMMM 'de' y",
        "pat|es-AR|yQQQ|QQQ 'de' y",
        "pat|es-AR|yQQQQ|QQQQ 'de' y",
        "pat|es-AR|GyMMMd|d MMM y G",
        "pat|es-AR|GyMMMEd|E, d 'de' MMM 'de' y G",
        "pat|es-AR|Q|DateTimeException",
        "pat|es-AR|QQQ|DateTimeException",
        "pat|es-AR|h|h\u202fa",
        "pat|es-AR|H|H",
        "pat|es-AR|j|h\u202fa",
        "pat|es-AR|hm|h:mm\u202fa",
        "pat|es-AR|Hm|H:mm",
        "pat|es-AR|jm|h:mm\u202fa",
        "pat|es-AR|hms|hh:mm:ss",
        "pat|es-AR|Hms|H:mm:ss",
        "pat|es-AR|jms|hh:mm:ss",
        "pat|es-AR|Hmv|HH:mm v",
        "pat|es-AR|jmv|h:mm\u202fa v",
        "pat|es-AR|Bhm|h:mm B",
        "pat|es-AR|Bhms|h:mm:ss B",
        "pat|es-AR|yMMMdHm|d 'de' MMM 'de' y, H:mm",
        "pat|es-AR|yMMMdjms|d 'de' MMM 'de' y, hh:mm:ss",
        "pat|es-AR|MdHm|d/M, H:mm",
        "pat|es-AR|EHm|E, HH:mm",
        "pat|es-AR|Ehm|E, h:mm\u202fa",
        "pat|es-AR|EEEHms|E, HH:mm:ss",
        "pat|es-AR|EEEEjm|E, h:mm\u202fa",
        "pat|es-AR|w|DateTimeException",
        "pat|es-AR|ww|DateTimeException",
        "pat|es-AR|yy|DateTimeException",
        "pat|es-AR|zzz|DateTimeException",
        "pat|es-AR|B|DateTimeException",
        "pat|es-AR|QQQQQ|DateTimeException",
        "pat|es-AR|yyy|DateTimeException",
        "pat|es-AR||DateTimeException",
        "pat|es-AR|vvvv|DateTimeException",
        "pat|es-AR|hH|DateTimeException",
        "pat|de-DE|y|y G",
        "pat|de-DE|yy|DateTimeException",
        "pat|de-DE|yyyy|y G",
        "pat|de-DE|M|L",
        "pat|de-DE|MM|L",
        "pat|de-DE|MMM|LLL",
        "pat|de-DE|MMMM|LLL",
        "pat|de-DE|MMMMM|LLL",
        "pat|de-DE|d|d",
        "pat|de-DE|E|ccc",
        "pat|de-DE|EEE|ccc",
        "pat|de-DE|EEEE|ccc",
        "pat|de-DE|EEEEE|ccc",
        "pat|de-DE|Md|d.M.",
        "pat|de-DE|MMMd|d. MMM",
        "pat|de-DE|MEd|E, d.M.",
        "pat|de-DE|MMMEd|E, d. MMM",
        "pat|de-DE|yM|M/y",
        "pat|de-DE|yMd|d.M.y",
        "pat|de-DE|yMMM|MMM y",
        "pat|de-DE|yMMMd|d. MMM y",
        "pat|de-DE|yMMMEd|E, d. MMM y",
        "pat|de-DE|yMMMM|MMMM y",
        "pat|de-DE|yQQQ|QQQ y",
        "pat|de-DE|yQQQQ|QQQQ y",
        "pat|de-DE|GyMMMd|d. MMM y G",
        "pat|de-DE|GyMMMEd|E, d. MMM y G",
        "pat|de-DE|Q|DateTimeException",
        "pat|de-DE|QQQ|DateTimeException",
        "pat|de-DE|h|h 'Uhr' a",
        "pat|de-DE|H|HH 'Uhr'",
        "pat|de-DE|j|HH 'Uhr'",
        "pat|de-DE|hm|h:mm\u202fa",
        "pat|de-DE|Hm|HH:mm",
        "pat|de-DE|jm|HH:mm",
        "pat|de-DE|hms|h:mm:ss\u202fa",
        "pat|de-DE|Hms|HH:mm:ss",
        "pat|de-DE|jms|HH:mm:ss",
        "pat|de-DE|Hmv|HH:mm v",
        "pat|de-DE|jmv|HH:mm v",
        "pat|de-DE|Bhm|h:mm B",
        "pat|de-DE|Bhms|h:mm:ss B",
        "pat|de-DE|yMMMdHm|d. MMM y, HH:mm",
        "pat|de-DE|yMMMdjms|d. MMM y, HH:mm:ss",
        "pat|de-DE|MdHm|d.M., HH:mm",
        "pat|de-DE|EHm|E, HH:mm",
        "pat|de-DE|Ehm|E h:mm\u202fa",
        "pat|de-DE|EEEHms|E, HH:mm:ss",
        "pat|de-DE|EEEEjm|E, HH:mm",
        "pat|de-DE|w|DateTimeException",
        "pat|de-DE|ww|DateTimeException",
        "pat|de-DE|yy|DateTimeException",
        "pat|de-DE|zzz|DateTimeException",
        "pat|de-DE|B|DateTimeException",
        "pat|de-DE|QQQQQ|DateTimeException",
        "pat|de-DE|yyy|DateTimeException",
        "pat|de-DE||DateTimeException",
        "pat|de-DE|vvvv|DateTimeException",
        "pat|de-DE|hH|DateTimeException",
        "pat|fr-FR|y|y G",
        "pat|fr-FR|yy|DateTimeException",
        "pat|fr-FR|yyyy|y G",
        "pat|fr-FR|M|L",
        "pat|fr-FR|MM|L",
        "pat|fr-FR|MMM|LLL",
        "pat|fr-FR|MMMM|LLL",
        "pat|fr-FR|MMMMM|LLL",
        "pat|fr-FR|d|d",
        "pat|fr-FR|E|E",
        "pat|fr-FR|EEE|E",
        "pat|fr-FR|EEEE|E",
        "pat|fr-FR|EEEEE|E",
        "pat|fr-FR|Md|dd/MM",
        "pat|fr-FR|MMMd|d MMM",
        "pat|fr-FR|MEd|E dd/MM",
        "pat|fr-FR|MMMEd|E d MMM",
        "pat|fr-FR|yM|MM/y",
        "pat|fr-FR|yMd|dd/MM/y",
        "pat|fr-FR|yMMM|MMM y",
        "pat|fr-FR|yMMMd|d MMM y",
        "pat|fr-FR|yMMMEd|E d MMM y",
        "pat|fr-FR|yMMMM|MMMM y",
        "pat|fr-FR|yQQQ|QQQ y",
        "pat|fr-FR|yQQQQ|QQQQ y",
        "pat|fr-FR|GyMMMd|d MMM y G",
        "pat|fr-FR|GyMMMEd|E d MMM y G",
        "pat|fr-FR|Q|DateTimeException",
        "pat|fr-FR|QQQ|DateTimeException",
        "pat|fr-FR|h|h\u202fa",
        "pat|fr-FR|H|HH 'h'",
        "pat|fr-FR|j|HH 'h'",
        "pat|fr-FR|hm|h:mm\u202fa",
        "pat|fr-FR|Hm|HH:mm",
        "pat|fr-FR|jm|HH:mm",
        "pat|fr-FR|hms|h:mm:ss\u202fa",
        "pat|fr-FR|Hms|HH:mm:ss",
        "pat|fr-FR|jms|HH:mm:ss",
        "pat|fr-FR|Hmv|HH:mm v",
        "pat|fr-FR|jmv|HH:mm v",
        "pat|fr-FR|Bhm|h:mm B",
        "pat|fr-FR|Bhms|h:mm:ss B",
        "pat|fr-FR|yMMMdHm|d MMM y HH:mm",
        "pat|fr-FR|yMMMdjms|d MMM y HH:mm:ss",
        "pat|fr-FR|MdHm|dd/MM HH:mm",
        "pat|fr-FR|EHm|E HH:mm",
        "pat|fr-FR|Ehm|E h:mm\u202fa",
        "pat|fr-FR|EEEHms|E HH:mm:ss",
        "pat|fr-FR|EEEEjm|E HH:mm",
        "pat|fr-FR|w|DateTimeException",
        "pat|fr-FR|ww|DateTimeException",
        "pat|fr-FR|yy|DateTimeException",
        "pat|fr-FR|zzz|DateTimeException",
        "pat|fr-FR|B|DateTimeException",
        "pat|fr-FR|QQQQQ|DateTimeException",
        "pat|fr-FR|yyy|DateTimeException",
        "pat|fr-FR||DateTimeException",
        "pat|fr-FR|vvvv|DateTimeException",
        "pat|fr-FR|hH|DateTimeException",
        "pat|ja-JP|y|y\u5e74",
        "pat|ja-JP|yy|DateTimeException",
        "pat|ja-JP|yyyy|Gy\u5e74",
        "pat|ja-JP|M|M\u6708",
        "pat|ja-JP|MM|M\u6708",
        "pat|ja-JP|MMM|M\u6708",
        "pat|ja-JP|MMMM|M\u6708",
        "pat|ja-JP|MMMMM|M\u6708",
        "pat|ja-JP|d|d\u65e5",
        "pat|ja-JP|E|ccc",
        "pat|ja-JP|EEE|ccc",
        "pat|ja-JP|EEEE|ccc",
        "pat|ja-JP|EEEEE|ccc",
        "pat|ja-JP|Md|M/d",
        "pat|ja-JP|MMMd|M\u6708d\u65e5",
        "pat|ja-JP|MEd|M/d(E)",
        "pat|ja-JP|MMMEd|M\u6708d\u65e5(E)",
        "pat|ja-JP|yM|y/M",
        "pat|ja-JP|yMd|y/M/d",
        "pat|ja-JP|yMMM|y\u5e74M\u6708",
        "pat|ja-JP|yMMMd|y\u5e74M\u6708d\u65e5",
        "pat|ja-JP|yMMMEd|y\u5e74M\u6708d\u65e5(E)",
        "pat|ja-JP|yMMMM|y\u5e74M\u6708",
        "pat|ja-JP|yQQQ|y/QQQ",
        "pat|ja-JP|yQQQQ|y\u5e74QQQQ",
        "pat|ja-JP|GyMMMd|Gy\u5e74M\u6708d\u65e5",
        "pat|ja-JP|GyMMMEd|Gy\u5e74M\u6708d\u65e5(E)",
        "pat|ja-JP|Q|DateTimeException",
        "pat|ja-JP|QQQ|DateTimeException",
        "pat|ja-JP|h|aK\u6642",
        "pat|ja-JP|H|H\u6642",
        "pat|ja-JP|j|H\u6642",
        "pat|ja-JP|hm|aK:mm",
        "pat|ja-JP|Hm|H:mm",
        "pat|ja-JP|jm|H:mm",
        "pat|ja-JP|hms|aK:mm:ss",
        "pat|ja-JP|Hms|H:mm:ss",
        "pat|ja-JP|jms|H:mm:ss",
        "pat|ja-JP|Hmv|H:mm v",
        "pat|ja-JP|jmv|H:mm v",
        "pat|ja-JP|Bhm|BK:mm",
        "pat|ja-JP|Bhms|BK:mm:ss",
        "pat|ja-JP|yMMMdHm|y\u5e74M\u6708d\u65e5 H:mm",
        "pat|ja-JP|yMMMdjms|y\u5e74M\u6708d\u65e5 H:mm:ss",
        "pat|ja-JP|MdHm|M/d H:mm",
        "pat|ja-JP|EHm|H:mm (E)",
        "pat|ja-JP|Ehm|aK:mm (E)",
        "pat|ja-JP|EEEHms|H:mm:ss (E)",
        "pat|ja-JP|EEEEjm|H:mm (E)",
        "pat|ja-JP|w|DateTimeException",
        "pat|ja-JP|ww|DateTimeException",
        "pat|ja-JP|yy|DateTimeException",
        "pat|ja-JP|zzz|DateTimeException",
        "pat|ja-JP|B|DateTimeException",
        "pat|ja-JP|QQQQQ|DateTimeException",
        "pat|ja-JP|yyy|DateTimeException",
        "pat|ja-JP||DateTimeException",
        "pat|ja-JP|vvvv|DateTimeException",
        "pat|ja-JP|hH|DateTimeException",
        "bad|IllegalArgumentException;IllegalArgumentException;IllegalArgumentException;IllegalArgumentException;IllegalArgumentException;IllegalArgumentException;IllegalArgumentException;IllegalArgumentException;IllegalArgumentException;IllegalArgumentException;IllegalArgumentException;IllegalArgumentException;IllegalArgumentException;IllegalArgumentException;IllegalArgumentException;IllegalArgumentException;IllegalArgumentException;IllegalArgumentException;IllegalArgumentException;IllegalArgumentException;",
        "nulls|NullPointerException|NullPointerException|NullPointerException|NullPointerException",
        "null-chrono|NullPointerException|MMM d, y",
        "fmt|und|2026-03-09|2026-03-09 14:05",
        "fmt|en-US|3/9/2026|3/9/2026, 14:05",
        "fmt|es-AR|9/3/2026|9/3/2026, 14:05",
        "fmt|de-DE|9.3.2026|9.3.2026, 14:05",
        "fmt|fr-FR|09/03/2026|09/03/2026 14:05",
        "fmt|ja-JP|2026/3/9|2026/3/9 14:05",
        "relocalize|3/9/2026|9.3.2026",
        "ofLocalizedPattern|3/9/2026|14:05",
        "day|und|FULL|AM;AM;AM;AM;AM;AM;AM;AM;AM;AM;AM;AM;PM;PM;PM;PM;PM;PM;PM;PM;PM;PM;PM;PM;",
        "day|und|NARROW|AM;AM;AM;AM;AM;AM;AM;AM;AM;AM;AM;AM;PM;PM;PM;PM;PM;PM;PM;PM;PM;PM;PM;PM;",
        "day|en-US|FULL|midnight;in the morning;in the morning;in the morning;in the morning;in the morning;in the morning;in the morning;in the morning;in the morning;in the morning;in the morning;noon;in the afternoon;in the afternoon;in the afternoon;in the afternoon;in the afternoon;in the evening;in the evening;in the evening;at night;at night;at night;",
        "day|en-US|NARROW|mi;in the morning;in the morning;in the morning;in the morning;in the morning;in the morning;in the morning;in the morning;in the morning;in the morning;in the morning;n;in the afternoon;in the afternoon;in the afternoon;in the afternoon;in the afternoon;in the evening;in the evening;in the evening;at night;at night;at night;",
        "day|es-AR|FULL|madrugada;madrugada;madrugada;madrugada;madrugada;madrugada;ma\u00f1ana;ma\u00f1ana;ma\u00f1ana;ma\u00f1ana;ma\u00f1ana;ma\u00f1ana;mediod\u00eda;tarde;tarde;tarde;tarde;tarde;tarde;tarde;noche;noche;noche;noche;",
        "day|es-AR|NARROW|madrugada;madrugada;madrugada;madrugada;madrugada;madrugada;ma\u00f1ana;ma\u00f1ana;ma\u00f1ana;ma\u00f1ana;ma\u00f1ana;ma\u00f1ana;mediod\u00eda;tarde;tarde;tarde;tarde;tarde;tarde;tarde;noche;noche;noche;noche;",
        "day|de-DE|FULL|Mitternacht;nachts;nachts;nachts;nachts;morgens;morgens;morgens;morgens;morgens;vormittags;vormittags;mittags;nachmittags;nachmittags;nachmittags;nachmittags;nachmittags;abends;abends;abends;abends;abends;abends;",
        "day|de-DE|NARROW|Mitternacht;nachts;nachts;nachts;nachts;morgens;morgens;morgens;morgens;morgens;vorm.;vorm.;mittags;nachm.;nachm.;nachm.;nachm.;nachm.;abends;abends;abends;abends;abends;abends;",
        "day|fr-FR|FULL|minuit;du matin;du matin;du matin;du matin;du matin;du matin;du matin;du matin;du matin;du matin;du matin;midi;de l\u2019apr\u00e8s-midi;de l\u2019apr\u00e8s-midi;de l\u2019apr\u00e8s-midi;de l\u2019apr\u00e8s-midi;de l\u2019apr\u00e8s-midi;du soir;du soir;du soir;du soir;du soir;du soir;",
        "day|fr-FR|NARROW|minuit;matin;matin;matin;mat.;mat.;mat.;mat.;mat.;mat.;mat.;mat.;midi;ap.m.;ap.m.;ap.m.;ap.m.;ap.m.;soir;soir;soir;soir;soir;soir;",
        "day|ja-JP|FULL|\u771f\u591c\u4e2d;\u591c\u4e2d;\u591c\u4e2d;\u591c\u4e2d;\u671d;\u671d;\u671d;\u671d;\u671d;\u671d;\u671d;\u671d;\u6b63\u5348;\u663c;\u663c;\u663c;\u5915\u65b9;\u5915\u65b9;\u5915\u65b9;\u591c;\u591c;\u591c;\u591c;\u591c\u4e2d;",
        "day|ja-JP|NARROW|\u771f\u591c\u4e2d;\u591c\u4e2d;\u591c\u4e2d;\u591c\u4e2d;\u671d;\u671d;\u671d;\u671d;\u671d;\u671d;\u671d;\u671d;\u6b63\u5348;\u663c;\u663c;\u663c;\u5915\u65b9;\u5915\u65b9;\u5915\u65b9;\u591c;\u591c;\u591c;\u591c;\u591c\u4e2d;",
        "cuts|midnight|in the morning|in the morning|noon|in the afternoon|at night",
        "day-null|NullPointerException",
        "zone|und|-03:00;-03:00;-03:00;-03:00;-03:00;-03:00;-03:00;-03:00;-03:00;-03:00;-03:00;-03:00;",
        "zone|en-US|-03:00;-03:00;-03:00;-03:00;-03:00;-03:00;-03:00;-03:00;-03:00;-03:00;-03:00;-03:00;",
        "zone|es-AR|-03:00;-03:00;-03:00;-03:00;-03:00;-03:00;-03:00;-03:00;-03:00;-03:00;-03:00;-03:00;",
        "zone|de-DE|-03:00;-03:00;-03:00;-03:00;-03:00;-03:00;-03:00;-03:00;-03:00;-03:00;-03:00;-03:00;",
        "zone|fr-FR|-03:00;-03:00;-03:00;-03:00;-03:00;-03:00;-03:00;-03:00;-03:00;-03:00;-03:00;-03:00;",
        "zone|ja-JP|-03:00;-03:00;-03:00;-03:00;-03:00;-03:00;-03:00;-03:00;-03:00;-03:00;-03:00;-03:00;",
        "zone-utc|Z|+05:30",
        "zone-nulls|NullPointerException|NullPointerException|NullPointerException|ok",
        "zone-without-zone|DateTimeException",
        "zone-parse|+05:30",
    };

    static final String[] TEMPLATES = {
        "y", "yy", "yyyy", "M", "MM", "MMM", "MMMM", "MMMMM", "d", "E", "EEE", "EEEE", "EEEEE",
        "Md", "MMMd", "MEd", "MMMEd", "yM", "yMd", "yMMM", "yMMMd", "yMMMEd", "yMMMM", "yQQQ",
        "yQQQQ", "GyMMMd", "GyMMMEd", "Q", "QQQ",
        "h", "H", "j", "hm", "Hm", "jm", "hms", "Hms", "jms", "Hmv", "jmv", "Bhm", "Bhms",
        "yMMMdHm", "yMMMdjms", "MdHm", "EHm", "Ehm", "EEEHms", "EEEEjm",
        "w", "ww", "yy", "zzz", "B", "QQQQQ", "yyy", "", "vvvv", "hH",
    };

    static final String[] BAD = {
        "abc", "dE", "My", "yMy", "mH", "ddd", "EEEEEE", "GGGGGG", "y M", "y2", "K", "k", "L",
        "a", "S", "u", "hhh", "vvvvv", "zzzzz", "BBBBBB",
    };

    /** What the eight members do, one line per check. */
    static String[] actual() throws Exception {
        final java.util.List<String> a = new java.util.ArrayList<String>();
        final Locale[] locales = {
            Locale.ROOT, Locale.US, new Locale("es", "AR"), Locale.GERMANY, Locale.FRANCE,
            Locale.JAPAN,
        };
        final String[] tags = {"und", "en-US", "es-AR", "de-DE", "fr-FR", "ja-JP"};

        // The pattern of each template in each language.
        for (int li = 0; li < locales.length; li++) {
            for (int i = 0; i < TEMPLATES.length; i++) {
                a.add("pat|" + tags[li] + "|" + TEMPLATES[i] + "|"
                        + pattern(TEMPLATES[i], locales[li]));
            }
        }

        // The malformed templates, which fail differently.
        final StringBuilder bad = new StringBuilder();
        for (int i = 0; i < BAD.length; i++) {
            bad.append(pattern(BAD[i], Locale.US)).append(';');
        }
        a.add("bad|" + bad);

        // The nulls.
        a.add("nulls|" + attempt(new NullTemplate()) + "|" + attempt(new NullLocale())
                + "|" + attempt(new NullAppend()) + "|" + attempt(new NullOf()));

        // A null calendar changes nothing: ISO is taken.
        a.add("null-chrono|" + patternChrono("yMMMd", null, Locale.US)
                + "|" + patternChrono("yMMMd", IsoChronology.INSTANCE, Locale.US));

        // Really formatting with a template, not just resolving the pattern.
        final ZonedDateTime when =
                ZonedDateTime.of(2026, 3, 9, 14, 5, 6, 0, ZoneOffset.ofHours(-3));
        // Formatting is done with templates of NUMERIC fields. This library's month and day names
        // exist in English only --a gap on the text side, not on the template side-- so an `MMM` in
        // German does not come out on both sides and cannot be compared. What is compared here is
        // that the template resolves to the right pattern and that that pattern is applied.
        for (int li = 0; li < locales.length; li++) {
            a.add("fmt|" + tags[li] + "|"
                    + new DateTimeFormatterBuilder().appendLocalized("yMd")
                        .toFormatter(locales[li]).format(when)
                    + "|"
                    + new DateTimeFormatterBuilder().appendLocalized("yMdHm")
                        .toFormatter(locales[li]).format(when));
        }
        // The same formatter changes language with `withLocale`: the pattern resolves on use.
        final DateTimeFormatter f = new DateTimeFormatterBuilder().appendLocalized("yMd")
                .toFormatter(Locale.US);
        a.add("relocalize|" + f.format(when) + "|"
                + f.withLocale(Locale.GERMANY).format(when));
        a.add("ofLocalizedPattern|"
                + DateTimeFormatter.ofLocalizedPattern("yMd").withLocale(Locale.US).format(when)
                + "|"
                + DateTimeFormatter.ofLocalizedPattern("Hm").withLocale(Locale.FRANCE)
                    .format(when));

        // The period of the day, hour by hour.
        final TextStyle[] styles = {TextStyle.FULL, TextStyle.NARROW};
        for (int li = 0; li < locales.length; li++) {
            for (int e = 0; e < styles.length; e++) {
                final DateTimeFormatter p = new DateTimeFormatterBuilder()
                        .appendDayPeriodText(styles[e]).toFormatter(locales[li]);
                final StringBuilder b = new StringBuilder();
                for (int h = 0; h < 24; h++) {
                    b.append(p.format(LocalTime.of(h, 0))).append(';');
                }
                a.add("day|" + tags[li] + "|" + styles[e] + "|" + b);
            }
        }
        // The minutes where the fine cuts are.
        final DateTimeFormatter pu = new DateTimeFormatterBuilder()
                .appendDayPeriodText(TextStyle.FULL).toFormatter(Locale.US);
        a.add("cuts|" + pu.format(LocalTime.of(0, 0)) + "|" + pu.format(LocalTime.of(0, 1))
                + "|" + pu.format(LocalTime.of(11, 59)) + "|" + pu.format(LocalTime.of(12, 0))
                + "|" + pu.format(LocalTime.of(12, 1)) + "|" + pu.format(LocalTime.of(23, 59)));
        a.add("day-null|" + attempt(new NullDayPeriod()));

        // The zone: the only thing constructible here is an offset.
        for (int li = 0; li < locales.length; li++) {
            final StringBuilder b = new StringBuilder();
            for (int e = 0; e < TextStyle.values().length; e++) {
                final TextStyle st = TextStyle.values()[e];
                b.append(new DateTimeFormatterBuilder().appendZoneText(st)
                        .toFormatter(locales[li]).format(when)).append(';');
                b.append(new DateTimeFormatterBuilder().appendGenericZoneText(st)
                        .toFormatter(locales[li]).format(when)).append(';');
            }
            a.add("zone|" + tags[li] + "|" + b);
        }
        final ZonedDateTime utc = ZonedDateTime.of(2026, 3, 9, 14, 0, 0, 0, ZoneOffset.UTC);
        final ZonedDateTime halfHour = ZonedDateTime.of(2026, 3, 9, 14, 0, 0, 0,
                ZoneOffset.ofHoursMinutes(5, 30));
        a.add("zone-utc|" + new DateTimeFormatterBuilder().appendZoneText(TextStyle.FULL)
                .toFormatter(Locale.US).format(utc)
                + "|" + new DateTimeFormatterBuilder().appendGenericZoneText(TextStyle.SHORT)
                .toFormatter(Locale.US).format(halfHour));
        a.add("zone-nulls|" + attempt(new NullZone()) + "|" + attempt(new NullZoneSet())
                + "|" + attempt(new NullGeneric()) + "|" + attempt(new NullGenericSet()));
        a.add("zone-without-zone|" + attempt(new ZoneWithoutZone()));
        a.add("zone-parse|" + new DateTimeFormatterBuilder().appendZoneText(TextStyle.FULL)
                .toFormatter(Locale.US).parse("+05:30")
                .query(java.time.temporal.TemporalQueries.zoneId()));

        return a.toArray(new String[a.size()]);
    }

    /** The pattern, or the simple name of whatever it threw. */
    static String pattern(String template, Locale l) {
        try {
            return DateTimeFormatterBuilder.getLocalizedDateTimePattern(template,
                    IsoChronology.INSTANCE, l);
        } catch (Throwable t) {
            final String n = t.getClass().getName();
            return n.substring(n.lastIndexOf('.') + 1);
        }
    }

    static String patternChrono(String template, java.time.chrono.Chronology c, Locale l) {
        try {
            return DateTimeFormatterBuilder.getLocalizedDateTimePattern(template, c, l);
        } catch (Throwable t) {
            final String n = t.getClass().getName();
            return n.substring(n.lastIndexOf('.') + 1);
        }
    }

    /** Something run to see what it fails with. */
    interface Attempt {
        void run() throws Exception;
    }

    /** Runs that and returns "ok" or the simple name of whatever it threw. */
    static String attempt(Attempt r) {
        try {
            r.run();
            return "ok";
        } catch (Throwable t) {
            final String n = t.getClass().getName();
            return n.substring(n.lastIndexOf('.') + 1);
        }
    }

    static class NullTemplate implements Attempt {
        public void run() throws Exception {
            DateTimeFormatterBuilder.getLocalizedDateTimePattern(null, IsoChronology.INSTANCE,
                    Locale.US);
        }
    }

    static class NullLocale implements Attempt {
        public void run() throws Exception {
            DateTimeFormatterBuilder.getLocalizedDateTimePattern("yMd", IsoChronology.INSTANCE,
                    null);
        }
    }

    static class NullAppend implements Attempt {
        public void run() throws Exception {
            new DateTimeFormatterBuilder().appendLocalized((String) null);
        }
    }

    static class NullOf implements Attempt {
        public void run() throws Exception {
            DateTimeFormatter.ofLocalizedPattern(null);
        }
    }

    static class NullDayPeriod implements Attempt {
        public void run() throws Exception {
            new DateTimeFormatterBuilder().appendDayPeriodText(null);
        }
    }

    static class NullZone implements Attempt {
        public void run() throws Exception {
            new DateTimeFormatterBuilder().appendZoneText(null);
        }
    }

    static class NullZoneSet implements Attempt {
        public void run() throws Exception {
            new DateTimeFormatterBuilder().appendZoneText(TextStyle.FULL, null);
        }
    }

    static class NullGeneric implements Attempt {
        public void run() throws Exception {
            new DateTimeFormatterBuilder().appendGenericZoneText(null);
        }
    }

    static class NullGenericSet implements Attempt {
        public void run() throws Exception {
            new DateTimeFormatterBuilder().appendGenericZoneText(TextStyle.FULL, null);
        }
    }

    static class ZoneWithoutZone implements Attempt {
        public void run() throws Exception {
            new DateTimeFormatterBuilder().appendZoneText(TextStyle.FULL).toFormatter(Locale.US)
                    .format(java.time.LocalDateTime.of(2026, 3, 9, 14, 0));
        }
    }

    /**
     * The index of the first answer that does not match the JDK's, or -1.
     *
     * @return the index, or -1
     */
    public static int where() {
        final String[] a;
        try {
            a = actual();
        } catch (Throwable e) {
            return 9000;
        }
        if (a.length != EXPECTED.length) {
            return 8000 + a.length;
        }
        for (int i = 0; i < a.length; i++) {
            if (!a[i].equals(EXPECTED[i])) {
                return i;
            }
        }
        return -1;
    }

    public static void main(String[] args) throws Exception {
        final String[] a = actual();
        if (args.length > 0) {
            for (int i = 0; i < a.length; i++) {
                System.out.println(a[i]);
            }
            return;
        }
        final int i = where();
        System.out.println(i < 0 ? "no differences"
                : i + ":\n  ours=" + a[i] + "\n  jdk    =" + EXPECTED[i]);
    }
}
