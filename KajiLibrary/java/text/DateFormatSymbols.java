package java.text;

import java.util.Locale;

/**
 * The names a date pattern is rendered with, for one locale: months, weekdays, am/pm and eras.
 *
 * <p>Same split as {@link DecimalFormatSymbols}, one level up. A pattern says <em>which fields</em>
 * appear and in what order; this class says what the words are. {@code MMMM} means "the month, in
 * full" — whether that prints {@code January}, {@code enero} or {@code 1月} is decided here.
 *
 * <p>The array SHAPES are part of the contract and look wrong until you know why. {@link #getMonths()}
 * returns THIRTEEN entries with the last one empty, because some calendars have a thirteenth month
 * and the array is indexed by the calendar's own month constant. {@link #getWeekdays()} returns
 * EIGHT with the FIRST one empty, because {@code Calendar.SUNDAY} is 1, not 0. Both arrays are
 * indexed by constant, not by position, and trimming the holes would break every caller that indexes
 * them the documented way.
 *
 * @implNote The table was extracted by running the JDK's own {@code DateFormatSymbols}, and every
 *           non-ASCII character is written as a {@code \\uXXXX} escape so the source stays ASCII.
 *           Some of those escapes matter more than they look: Spanish am/pm contains {@code U+00A0},
 *           a NO-BREAK SPACE, not an ordinary one.
 *
 * @implNote La superficie está completa. {@code getZoneStrings}/{@code setZoneStrings} estuvieron
 *           afuera mientras se pensó que hacía falta una tabla propia de nombres de zona; no hace
 *           falta: los nombres se piden a {@code java.util.TimeZone}, que es de donde salen también
 *           en el JDK. Lo que sigue siendo corto son los DATOS — la tabla de locales cubre seis
 *           filas y un locale desconocido cae en ROOT, y la base de zonas de esta biblioteca es
 *           mínima, así que {@code getZoneStrings} devuelve pocas filas y con nombres de
 *           desplazamiento. Es lo que la biblioteca sabe; decirlo así es el punto.
 */
public class DateFormatSymbols implements Cloneable {

    private static String[] tags() {
        return new String[] {"und", "en-US", "es-AR", "de-DE", "fr-FR", "ja-JP"};
    }

    // Thirteen entries, the last empty: the array is indexed by Calendar's month constant, and a
    // lunisolar calendar can have a thirteenth month.
    private static String[] monthRow(int i) {
        if (i == 0) {
            return new String[] {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec", ""};
        }
        if (i == 1) {
            return new String[] {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December", ""};
        }
        if (i == 2) {
            return new String[] {"enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre", ""};
        }
        if (i == 3) {
            return new String[] {"Januar", "Februar", "M\u00e4rz", "April", "Mai", "Juni", "Juli", "August", "September", "Oktober", "November", "Dezember", ""};
        }
        if (i == 4) {
            return new String[] {"janvier", "f\u00e9vrier", "mars", "avril", "mai", "juin", "juillet", "ao\u00fbt", "septembre", "octobre", "novembre", "d\u00e9cembre", ""};
        }
        return new String[] {"1\u6708", "2\u6708", "3\u6708", "4\u6708", "5\u6708", "6\u6708", "7\u6708", "8\u6708", "9\u6708", "10\u6708", "11\u6708", "12\u6708", ""};
    }

    private static String[] shortMonthRow(int i) {
        if (i == 0) {
            return new String[] {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec", ""};
        }
        if (i == 1) {
            return new String[] {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec", ""};
        }
        if (i == 2) {
            return new String[] {"ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sept", "oct", "nov", "dic", ""};
        }
        if (i == 3) {
            return new String[] {"Jan.", "Feb.", "M\u00e4rz", "Apr.", "Mai", "Juni", "Juli", "Aug.", "Sept.", "Okt.", "Nov.", "Dez.", ""};
        }
        if (i == 4) {
            return new String[] {"janv.", "f\u00e9vr.", "mars", "avr.", "mai", "juin", "juil.", "ao\u00fbt", "sept.", "oct.", "nov.", "d\u00e9c.", ""};
        }
        return new String[] {"1\u6708", "2\u6708", "3\u6708", "4\u6708", "5\u6708", "6\u6708", "7\u6708", "8\u6708", "9\u6708", "10\u6708", "11\u6708", "12\u6708", ""};
    }

    // Eight entries, the FIRST empty: Calendar.SUNDAY is 1.
    private static String[] weekdayRow(int i) {
        if (i == 0) {
            return new String[] {"", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        }
        if (i == 1) {
            return new String[] {"", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        }
        if (i == 2) {
            return new String[] {"", "domingo", "lunes", "martes", "mi\u00e9rcoles", "jueves", "viernes", "s\u00e1bado"};
        }
        if (i == 3) {
            return new String[] {"", "Sonntag", "Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag", "Samstag"};
        }
        if (i == 4) {
            return new String[] {"", "dimanche", "lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi"};
        }
        return new String[] {"", "\u65e5\u66dc\u65e5", "\u6708\u66dc\u65e5", "\u706b\u66dc\u65e5", "\u6c34\u66dc\u65e5", "\u6728\u66dc\u65e5", "\u91d1\u66dc\u65e5", "\u571f\u66dc\u65e5"};
    }

    private static String[] shortWeekdayRow(int i) {
        if (i == 0) {
            return new String[] {"", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        }
        if (i == 1) {
            return new String[] {"", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        }
        if (i == 2) {
            return new String[] {"", "dom", "lun", "mar", "mi\u00e9", "jue", "vie", "s\u00e1b"};
        }
        if (i == 3) {
            return new String[] {"", "So.", "Mo.", "Di.", "Mi.", "Do.", "Fr.", "Sa."};
        }
        if (i == 4) {
            return new String[] {"", "dim.", "lun.", "mar.", "mer.", "jeu.", "ven.", "sam."};
        }
        return new String[] {"", "\u65e5", "\u6708", "\u706b", "\u6c34", "\u6728", "\u91d1", "\u571f"};
    }

    private static String[] amPmRow(int i) {
        if (i == 0) {
            return new String[] {"AM", "PM"};
        }
        if (i == 1) {
            return new String[] {"AM", "PM"};
        }
        if (i == 2) {
            return new String[] {"a.\u00a0m.", "p.\u00a0m."};
        }
        if (i == 3) {
            return new String[] {"AM", "PM"};
        }
        if (i == 4) {
            return new String[] {"AM", "PM"};
        }
        return new String[] {"\u5348\u524d", "\u5348\u5f8c"};
    }

    private static String[] eraRow(int i) {
        if (i == 0) {
            return new String[] {"BCE", "CE"};
        }
        if (i == 1) {
            return new String[] {"BC", "AD"};
        }
        if (i == 2) {
            return new String[] {"a.C.", "d.C."};
        }
        if (i == 3) {
            return new String[] {"v. Chr.", "n. Chr."};
        }
        if (i == 4) {
            return new String[] {"av. J.-C.", "ap. J.-C."};
        }
        return new String[] {"\u7d00\u5143\u524d", "\u897f\u66a6"};
    }

    private final Locale locale;
    private String[] eras;
    private String[] months;
    private String[] shortMonths;
    private String[] weekdays;
    private String[] shortWeekdays;
    private String[] amPmStrings;
    private String localPatternChars;
    // null mientras nadie las haya fijado a mano: en ese caso se derivan de java.util.TimeZone al
    // pedirlas. Guardar la tabla derivada sería guardar una copia de datos que viven en otro lado.
    private String[][] zoneStrings;

    /**
     * Creates symbols for the default locale.
     */
    public DateFormatSymbols() {
        this(Locale.getDefault());
    }

    /**
     * Creates symbols for the given locale.
     *
     * @param locale the locale whose names to use
     */
    public DateFormatSymbols(Locale locale) {
        this.locale = locale;
        int i = DateFormatSymbols.indexOf(locale);
        this.months = DateFormatSymbols.copy(DateFormatSymbols.monthRow(i));
        this.shortMonths = DateFormatSymbols.copy(DateFormatSymbols.shortMonthRow(i));
        this.weekdays = DateFormatSymbols.copy(DateFormatSymbols.weekdayRow(i));
        this.shortWeekdays = DateFormatSymbols.copy(DateFormatSymbols.shortWeekdayRow(i));
        this.amPmStrings = DateFormatSymbols.copy(DateFormatSymbols.amPmRow(i));
        this.eras = DateFormatSymbols.copy(DateFormatSymbols.eraRow(i));
        // Identical across every locale the JDK ships; it is the legacy pattern-letter alphabet.
        this.localPatternChars = "GyMdkHmsSEDFwWahKzZ";
    }

    private static String[] copy(String[] a) {
        String[] out = new String[a.length];
        int i = 0;
        while (i < a.length) {
            out[i] = a[i];
            i = i + 1;
        }
        return out;
    }

    // Exact "lang-COUNTRY" first, then the language alone, then ROOT — so es-MX gets Spanish names
    // rather than the root ones.
    private static int indexOf(Locale locale) {
        String lang = locale.getLanguage();
        String country = locale.getCountry();
        String full = lang;
        if (country.length() > 0) {
            full = lang + "-" + country;
        }
        String[] all = DateFormatSymbols.tags();
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
     * Returns the locales for which names are available.
     *
     * @return the supported locales
     */
    public static Locale[] getAvailableLocales() {
        String[] all = DateFormatSymbols.tags();
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
            } else {
                out[i] = new Locale(tag);
            }
            i = i + 1;
        }
        return out;
    }

    /**
     * Returns names for the default locale.
     *
     * @return the symbols
     */
    public static final DateFormatSymbols getInstance() {
        return new DateFormatSymbols();
    }

    /**
     * Returns names for the given locale.
     *
     * @param locale the locale
     * @return the symbols
     */
    public static final DateFormatSymbols getInstance(Locale locale) {
        return new DateFormatSymbols(locale);
    }

    /**
     * Returns the era names, BCE first.
     *
     * @return a copy of the era strings
     * @implSpec Every getter returns a COPY. The arrays are mutable, and handing out the internal
     *           one would let any caller rewrite the month names for everyone.
     */
    public String[] getEras() {
        return DateFormatSymbols.copy(this.eras);
    }

    /**
     * Sets the era names.
     *
     * @param newEras the era strings
     */
    public void setEras(String[] newEras) {
        this.eras = DateFormatSymbols.copy(newEras);
    }

    /**
     * Returns the full month names, indexed by the calendar's month constant.
     *
     * @return a copy of the month strings, thirteen entries with the last empty
     */
    public String[] getMonths() {
        return DateFormatSymbols.copy(this.months);
    }

    /**
     * Sets the full month names.
     *
     * @param newMonths the month strings
     */
    public void setMonths(String[] newMonths) {
        this.months = DateFormatSymbols.copy(newMonths);
    }

    /**
     * Returns the abbreviated month names.
     *
     * @return a copy of the short month strings
     */
    public String[] getShortMonths() {
        return DateFormatSymbols.copy(this.shortMonths);
    }

    /**
     * Sets the abbreviated month names.
     *
     * @param newShortMonths the short month strings
     */
    public void setShortMonths(String[] newShortMonths) {
        this.shortMonths = DateFormatSymbols.copy(newShortMonths);
    }

    /**
     * Returns the full weekday names, indexed by the calendar's day constant.
     *
     * @return a copy of the weekday strings, eight entries with the first empty
     */
    public String[] getWeekdays() {
        return DateFormatSymbols.copy(this.weekdays);
    }

    /**
     * Sets the full weekday names.
     *
     * @param newWeekdays the weekday strings
     */
    public void setWeekdays(String[] newWeekdays) {
        this.weekdays = DateFormatSymbols.copy(newWeekdays);
    }

    /**
     * Returns the abbreviated weekday names.
     *
     * @return a copy of the short weekday strings
     */
    public String[] getShortWeekdays() {
        return DateFormatSymbols.copy(this.shortWeekdays);
    }

    /**
     * Sets the abbreviated weekday names.
     *
     * @param newShortWeekdays the short weekday strings
     */
    public void setShortWeekdays(String[] newShortWeekdays) {
        this.shortWeekdays = DateFormatSymbols.copy(newShortWeekdays);
    }

    /**
     * Returns the am/pm strings.
     *
     * @return a copy of the am/pm strings
     */
    public String[] getAmPmStrings() {
        return DateFormatSymbols.copy(this.amPmStrings);
    }

    /**
     * Sets the am/pm strings.
     *
     * @param newAmpms the am/pm strings
     */
    public void setAmPmStrings(String[] newAmpms) {
        this.amPmStrings = DateFormatSymbols.copy(newAmpms);
    }

    /**
     * Los nombres de las zonas horarias, una fila por zona.
     *
     * <p>Cada fila es {@code {id, largo estándar, corto estándar, largo de verano, corto de
     * verano}}. El javadoc pide al menos cinco columnas y define esas cinco; acá se devuelven
     * exactamente cinco, sin las de nombre "genérico" que el JDK agrega, porque
     * {@code java.util.TimeZone} no tiene de dónde sacarlas y rellenarlas con el nombre estándar
     * sería presentar un dato como otro.
     *
     * <p><b>De dónde salen los nombres.</b> Si nadie llamó a {@link #setZoneStrings}, de
     * {@code TimeZone.getDisplayName()} para cada ID que {@code TimeZone.getAvailableIDs()}
     * declare. No hay una tabla propia: sería una segunda copia de los mismos datos, y las dos se
     * separarían. Con la base de zonas reducida que trae esta biblioteca la tabla sale corta y con
     * nombres de desplazamiento en vez de nombres traducidos — que es lo que la biblioteca sabe, y
     * decirlo así es lo correcto; inventar "Hora Estándar del Este" cuando no hay tzdb detrás sería
     * lo contrario.
     *
     * @return los nombres de zona
     */
    public String[][] getZoneStrings() {
        if (this.zoneStrings != null) {
            return DateFormatSymbols.copiar(this.zoneStrings);
        }
        String[] ids = java.util.TimeZone.getAvailableIDs();
        String[][] out = new String[ids.length][];
        for (int i = 0; i < ids.length; i = i + 1) {
            java.util.TimeZone z = java.util.TimeZone.getTimeZone(ids[i]);
            String[] fila = new String[5];
            fila[0] = ids[i];
            fila[1] = z.getDisplayName(false, java.util.TimeZone.LONG, this.locale);
            fila[2] = z.getDisplayName(false, java.util.TimeZone.SHORT, this.locale);
            fila[3] = z.getDisplayName(true, java.util.TimeZone.LONG, this.locale);
            fila[4] = z.getDisplayName(true, java.util.TimeZone.SHORT, this.locale);
            out[i] = fila;
        }
        return out;
    }

    /**
     * Reemplaza los nombres de zona.
     *
     * <p>Lo que se fije acá lo usa de verdad {@link SimpleDateFormat} para las letras {@code z} y
     * {@code zzzz}: un setter que no cambiara la salida sería peor que no tenerlo.
     *
     * @param newZoneStrings las filas, cada una con al menos cinco entradas
     * @throws IllegalArgumentException si alguna fila tiene menos de cinco
     */
    public void setZoneStrings(String[][] newZoneStrings) {
        if (newZoneStrings == null) {
            throw new NullPointerException();
        }
        for (int i = 0; i < newZoneStrings.length; i = i + 1) {
            if (newZoneStrings[i] == null || newZoneStrings[i].length < 5) {
                throw new IllegalArgumentException("Row " + Integer.toString(i)
                        + " of the input array does not have a length of at least 5");
            }
        }
        this.zoneStrings = DateFormatSymbols.copiar(newZoneStrings);
    }

    // La fila de una zona, o null si no está. La usa SimpleDateFormat; devuelve null en vez de una
    // fila vacía para que el llamador pueda distinguir "no hay" de "se llama así".
    String[] filaDeZona(String id) {
        if (this.zoneStrings == null) {
            return null;
        }
        for (int i = 0; i < this.zoneStrings.length; i = i + 1) {
            if (this.zoneStrings[i][0].equals(id)) {
                return this.zoneStrings[i];
            }
        }
        return null;
    }

    private static String[][] copiar(String[][] in) {
        String[][] out = new String[in.length][];
        for (int i = 0; i < in.length; i = i + 1) {
            String[] fila = new String[in[i].length];
            for (int k = 0; k < in[i].length; k = k + 1) {
                fila[k] = in[i][k];
            }
            out[i] = fila;
        }
        return out;
    }

    /**
     * Returns the localized pattern characters.
     *
     * @return the pattern character alphabet
     */
    public String getLocalPatternChars() {
        return this.localPatternChars;
    }

    /**
     * Sets the localized pattern characters.
     *
     * @param newLocalPatternChars the pattern character alphabet
     */
    public void setLocalPatternChars(String newLocalPatternChars) {
        this.localPatternChars = newLocalPatternChars;
    }

    /**
     * Returns a copy of these symbols.
     *
     * @return a copy
     */
    public Object clone() {
        DateFormatSymbols out = new DateFormatSymbols(this.locale);
        out.eras = DateFormatSymbols.copy(this.eras);
        out.months = DateFormatSymbols.copy(this.months);
        out.shortMonths = DateFormatSymbols.copy(this.shortMonths);
        out.weekdays = DateFormatSymbols.copy(this.weekdays);
        out.shortWeekdays = DateFormatSymbols.copy(this.shortWeekdays);
        out.amPmStrings = DateFormatSymbols.copy(this.amPmStrings);
        out.localPatternChars = this.localPatternChars;
        if (this.zoneStrings != null) {
            out.zoneStrings = DateFormatSymbols.copiar(this.zoneStrings);
        }
        return out;
    }

    /**
     * Compares these symbols with another set.
     *
     * @param obj the object to compare with
     * @return {@code true} if every name matches
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DateFormatSymbols) {
            DateFormatSymbols other = (DateFormatSymbols) obj;
            return DateFormatSymbols.same(this.eras, other.eras)
                    && DateFormatSymbols.same(this.months, other.months)
                    && DateFormatSymbols.same(this.shortMonths, other.shortMonths)
                    && DateFormatSymbols.same(this.weekdays, other.weekdays)
                    && DateFormatSymbols.same(this.shortWeekdays, other.shortWeekdays)
                    && DateFormatSymbols.same(this.amPmStrings, other.amPmStrings)
                    && this.localPatternChars.equals(other.localPatternChars);
        }
        return false;
    }

    private static boolean same(String[] a, String[] b) {
        if (a.length != b.length) {
            return false;
        }
        boolean equal = true;
        int i = 0;
        while (i < a.length) {
            if (!a[i].equals(b[i])) {
                equal = false;
                i = a.length;
            } else {
                i = i + 1;
            }
        }
        return equal;
    }

    /**
     * Returns a hash code for these symbols.
     *
     * @return the hash code
     */
    public int hashCode() {
        int result = this.months.length;
        result = result * 31 + this.weekdays.length;
        result = result * 31 + this.localPatternChars.hashCode();
        return result;
    }
}
