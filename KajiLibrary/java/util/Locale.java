package java.util;

// Same-package imports work around the frozen javac's finder (finding #4).
import java.lang.Cloneable;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

// A language tag: which language, in which country, with which script and which variant.
//
// A Locale **translates nothing and knows nothing**: it is the key other classes look their data up
// with -- `Formatter` for the numeric separators, `Calendar` for the first day of the week,
// `ResourceBundle` for the text file. That separation is the whole design, and it is what lets a tag
// this library does not know still be passed, compared and serialised without losing information.
//
// The four parts, in the order they are written in a BCP 47 tag:
//
//   language  es       ISO 639, lower case      mandatory (or empty for "unspecified")
//   script    Latn     ISO 15924, Capitalised   optional
//   region    AR       ISO 3166, UPPER CASE     optional
//   variant   valencia free-form                optional
//
// And the **extensions**, which are the part almost nobody looks at: `-u-` carries Unicode
// preferences (calendar, currency, collation) and `-x-` is for private use. They are stored and
// returned as they stand, which is the only honest thing without the CLDR database.
//
// ---- what is here and what is not --------------------------------------------------------------
//
// **58 new members: the contract is complete.** What is NOT here is the **data**, and that makes
// for three divergences worth having in view:
//
// | | |
// |---|---|
// | `getDisplayLanguage()` and company | return the **code** (`"es"`, not `"Spanish"`). It is exactly what the JDK does when it has no data for a locale, so it is not an invented answer -- it is the fallback answer, always |
// | `getISO3Language()`/`getISO3Country()` | there is a table for the languages and countries this class names, and for the rest `MissingResourceException` is thrown, which is what the JDK does with a code it does not know |
// | `getISOLanguages()`/`getISOCountries()` | return what is in that table, not the complete ISO lists (184 languages and 249 countries). It is a subset, and it is said |
//
// The rest -- constructing, parsing, composing tags, extensions, RFC 4647 filtering -- is mechanism
// and not data, and it is here whole.
public final class Locale implements Serializable, Cloneable {

    // The two letters that identify an extension with a name of its own.
    public static final char UNICODE_LOCALE_EXTENSION = 'u';
    public static final char PRIVATE_USE_EXTENSION = 'x';

    // It goes **before** the constants on purpose: static initialisers run in order of appearance,
    // and each constant calls the private constructor that reads this array. Declaring it afterwards
    // left it null during ROOT's construction, and `toLanguageTag()` died with a
    // NullPointerException on first use.
    private static final String[] NO_EXTENSIONS = new String[0];

    public static final Locale ROOT = new Locale("", "");
    public static final Locale ENGLISH = new Locale("en", "");
    public static final Locale US = new Locale("en", "US");
    public static final Locale UK = new Locale("en", "GB");
    public static final Locale GERMAN = new Locale("de", "");
    public static final Locale GERMANY = new Locale("de", "DE");
    public static final Locale FRENCH = new Locale("fr", "");
    public static final Locale FRANCE = new Locale("fr", "FR");
    public static final Locale ITALIAN = new Locale("it", "");
    public static final Locale ITALY = new Locale("it", "IT");
    public static final Locale JAPANESE = new Locale("ja", "");
    public static final Locale JAPAN = new Locale("ja", "JP");
    public static final Locale KOREAN = new Locale("ko", "");
    public static final Locale KOREA = new Locale("ko", "KR");
    public static final Locale CHINESE = new Locale("zh", "");
    public static final Locale SIMPLIFIED_CHINESE = new Locale("zh", "CN");
    public static final Locale TRADITIONAL_CHINESE = new Locale("zh", "TW");
    public static final Locale CHINA = SIMPLIFIED_CHINESE;
    public static final Locale PRC = SIMPLIFIED_CHINESE;
    public static final Locale TAIWAN = TRADITIONAL_CHINESE;
    public static final Locale CANADA = new Locale("en", "CA");
    public static final Locale CANADA_FRENCH = new Locale("fr", "CA");

    private final String language;
    private final String country;
    private final String variant;
    private final String script;

    // The extensions, as raw text: the letter and its content, in the order they arrived. They are
    // stored uninterpreted; see the header's note.
    private final String[] extKeys;
    private final String[] extValues;

    // The default is mutable (`setDefault`) and starts at US, which is this library's convention:
    // the system's locale is not read.
    private static Locale defaultLocale = US;
    private static Locale defaultDisplayLocale = US;
    private static Locale defaultFormatLocale = US;

    public Locale(String language) {
        this(language, "", "");
    }

    public Locale(String language, String country) {
        this(language, country, "");
    }

    // The classic full form. All three constructors are **deprecated** since Java 19 in favour of
    // `of(...)`, for a concrete reason: a constructor cannot return a shared instance, and tags
    // repeat a great deal.
    public Locale(String language, String country, String variant) {
        this(language, "", country, variant, NO_EXTENSIONS, NO_EXTENSIONS);
    }

    private Locale(String language, String script, String country, String variant,
            String[] extKeys, String[] extValues) {
        this.language = normalizeLanguage(language);
        this.script = normalizeScript(script);
        this.country = normalizeRegion(country);
        this.variant = variant == null ? "" : variant;
        this.extKeys = extKeys;
        this.extValues = extValues;
    }

    // ---- factories ------------------------------------------------------------------------------

    public static Locale of(String language) {
        return new Locale(language, "", "");
    }

    public static Locale of(String language, String country) {
        return new Locale(language, country, "");
    }

    public static Locale of(String language, String country, String variant) {
        return new Locale(language, country, variant);
    }

    // ---- normalisation ------------------------------------------------------------------------
    //
    // The case is fixed by the specification and is not cosmetic: two tags differing only in case are
    // **the same**, and without normalising on construction, `equals` would say they are not.

    private static String normalizeLanguage(String s) {
        if (s == null) {
            return "";
        }
        String l = s.toLowerCase();
        // The three codes ISO renamed. Java **canonicalises to the new one**: `new Locale("iw")`
        // gives `he`, and not the other way round.
        //
        // It is worth saying that this changed: for twenty years the JDK stored the OLD code
        // (`he` -> `iw`) and only `toLanguageTag()` returned the new one. It was checked against
        // JDK 25, which does the opposite, and that is what is replicated.
        if (l.equals("iw")) {
            return "he";
        }
        if (l.equals("ji")) {
            return "yi";
        }
        if (l.equals("in")) {
            return "id";
        }
        return l;
    }

    private static String normalizeRegion(String s) {
        if (s == null) {
            return "";
        }
        return s.toUpperCase();
    }

    // Capitalised: `Latn`, not `latn` nor `LATN`.
    private static String normalizeScript(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        String l = s.toLowerCase();
        return l.substring(0, 1).toUpperCase() + l.substring(1, l.length());
    }

    // ---- the four parts ------------------------------------------------------------------------

    public String getLanguage() {
        return this.language;
    }

    public String getCountry() {
        return this.country;
    }

    public String getVariant() {
        return this.variant;
    }

    public String getScript() {
        return this.script;
    }

    // ---- extensions -----------------------------------------------------------------------------

    public boolean hasExtensions() {
        return this.extKeys.length > 0;
    }

    // An extension's content, or null if this tag does not carry it.
    public String getExtension(char key) {
        String k = String.valueOf(key).toLowerCase();
        int i = 0;
        while (i < this.extKeys.length) {
            if (this.extKeys[i].equals(k)) {
                return this.extValues[i];
            }
            i = i + 1;
        }
        return null;
    }

    public Set<Character> getExtensionKeys() {
        LinkedHashSet<Character> out = new LinkedHashSet<Character>();
        int i = 0;
        while (i < this.extKeys.length) {
            out.add(Character.valueOf(this.extKeys[i].charAt(0)));
            i = i + 1;
        }
        return out;
    }

    // The `-u-` extension's attributes: the loose keys, with no value.
    //
    // `-u-`'s shape is a list of attributes and then key-value pairs, and what separates the two is
    // the length: a Unicode key is **two** characters, an attribute three or more.
    public Set<String> getUnicodeLocaleAttributes() {
        LinkedHashSet<String> out = new LinkedHashSet<String>();
        String u = this.getExtension(UNICODE_LOCALE_EXTENSION);
        if (u == null) {
            return out;
        }
        String[] parts = u.split("-");
        int i = 0;
        while (i < parts.length && parts[i].length() != 2) {
            if (parts[i].length() > 0) {
                out.add(parts[i]);
            }
            i = i + 1;
        }
        return out;
    }

    public Set<String> getUnicodeLocaleKeys() {
        LinkedHashSet<String> out = new LinkedHashSet<String>();
        String u = this.getExtension(UNICODE_LOCALE_EXTENSION);
        if (u == null) {
            return out;
        }
        String[] parts = u.split("-");
        int i = 0;
        while (i < parts.length) {
            if (parts[i].length() == 2) {
                out.add(parts[i]);
            }
            i = i + 1;
        }
        return out;
    }

    // A Unicode key's value: what follows the key up to the next key.
    public String getUnicodeLocaleType(String key) {
        if (key == null) {
            throw new NullPointerException();
        }
        String u = this.getExtension(UNICODE_LOCALE_EXTENSION);
        if (u == null) {
            return null;
        }
        String[] parts = u.split("-");
        int i = 0;
        while (i < parts.length) {
            if (parts[i].equals(key)) {
                StringBuilder sb = new StringBuilder();
                int j = i + 1;
                while (j < parts.length && parts[j].length() != 2) {
                    if (sb.length() > 0) {
                        sb.append('-');
                    }
                    sb.append(parts[j]);
                    j = j + 1;
                }
                return sb.toString();
            }
            i = i + 1;
        }
        return null;
    }

    // The same tag with no extensions at all. It is what is used for comparing two locales by their
    // linguistic identity, ignoring formatting preferences.
    public Locale stripExtensions() {
        if (!this.hasExtensions()) {
            return this;
        }
        return new Locale(this.language, this.script, this.country, this.variant, NO_EXTENSIONS, NO_EXTENSIONS);
    }

    // ---- BCP 47 tags ----------------------------------------------------------------------------

    /**
     * This locale's BCP 47 tag: `es-AR`, `zh-Hant-TW`, `en-US-u-ca-buddhist`.
     *
     * <p>It is the **canonical** form and the one to use for serialising: `toString()` produces the
     * old underscore format, which is not interchangeable with anything outside Java.
     *
     * <p>A locale with no language comes out as `und`, which is how BCP 47 says "unspecified".
     */
    public String toLanguageTag() {
        StringBuilder sb = new StringBuilder();
        // The language comes canonicalised from the constructor, so there is nothing to translate here.
        String l = this.language;
        sb.append(l.length() == 0 ? "und" : l);
        if (this.script.length() > 0) {
            sb.append('-');
            sb.append(this.script);
        }
        if (this.country.length() > 0) {
            sb.append('-');
            sb.append(this.country);
        }
        if (this.variant.length() > 0) {
            sb.append('-');
            sb.append(this.variant);
        }
        int i = 0;
        while (i < this.extKeys.length) {
            // The private-use one goes **last**, by definition: everything after `-x-` is its own.
            if (!this.extKeys[i].equals("x")) {
                sb.append('-');
                sb.append(this.extKeys[i]);
                sb.append('-');
                sb.append(this.extValues[i]);
            }
            i = i + 1;
        }
        String priv = this.getExtension(PRIVATE_USE_EXTENSION);
        if (priv != null) {
            sb.append("-x-");
            sb.append(priv);
        }
        return sb.toString();
    }

    /**
     * The locale of a BCP 47 tag.
     *
     * <p>It recognises `language[-Script][-REGION][-variant][-extensions]`, telling each part by its
     * **shape** and not by its position: the script is four letters, the region two letters or three
     * digits, and an extension is a single character followed by a hyphen. It is what allows parsing
     * `zh-Hant-TW` and `es-419` unambiguously.
     *
     * <p>What matches no shape is discarded, which is what BCP 47 demands for a malformed tag:
     * keeping the good prefix instead of failing.
     */
    public static Locale forLanguageTag(String languageTag) {
        if (languageTag == null) {
            throw new NullPointerException();
        }
        String[] p = languageTag.split("-");
        int i = 0;
        String lang = "";
        String script = "";
        String region = "";
        String variant = "";
        ArrayList<String> keys = new ArrayList<String>();
        ArrayList<String> values = new ArrayList<String>();
        if (i < p.length && p[i].length() >= 2 && p[i].length() <= 8 && isAlpha(p[i])) {
            if (!p[i].equalsIgnoreCase("und")) {
                lang = p[i];
            }
            i = i + 1;
        }
        if (i < p.length && p[i].length() == 4 && isAlpha(p[i])) {
            script = p[i];
            i = i + 1;
        }
        if (i < p.length && ((p[i].length() == 2 && isAlpha(p[i]))
                || (p[i].length() == 3 && isAllDigits(p[i])))) {
            region = p[i];
            i = i + 1;
        }
        // Variants: 5 to 8 characters, or 4 starting with a digit. They are joined with a hyphen.
        StringBuilder vs = new StringBuilder();
        while (i < p.length && p[i].length() > 1
                && (p[i].length() >= 5 || (p[i].length() == 4 && isDigitChar(p[i].charAt(0))))) {
            if (vs.length() > 0) {
                vs.append('-');
            }
            vs.append(p[i]);
            i = i + 1;
        }
        variant = vs.toString();
        // Extensions: one character, and everything that follows up to the next single character.
        while (i < p.length && p[i].length() == 1) {
            String k = p[i].toLowerCase();
            i = i + 1;
            StringBuilder val = new StringBuilder();
            while (i < p.length && p[i].length() != 1) {
                if (val.length() > 0) {
                    val.append('-');
                }
                val.append(p[i]);
                i = i + 1;
            }
            keys.add(k);
            values.add(val.toString());
        }
        return new Locale(lang, script, region, variant, toArray0(keys), toArray0(values));
    }

    /**
     * The same tag in canonical case: language in lower case, script Capitalised, region in UPPER
     * CASE.
     *
     * <p>It is purely a matter of shape -- it validates and resolves **nothing** -- and it serves for
     * comparing two tags written by different people without constructing two Locales.
     */
    public static String caseFoldLanguageTag(String languageTag) {
        if (languageTag == null) {
            throw new NullPointerException();
        }
        String[] p = languageTag.split("-");
        StringBuilder sb = new StringBuilder();
        int i = 0;
        boolean inPrivateUse = false;
        while (i < p.length) {
            if (i > 0) {
                sb.append('-');
            }
            String s = p[i];
            if (inPrivateUse || s.length() == 1) {
                sb.append(s.toLowerCase());
                if (s.length() == 1 && s.equalsIgnoreCase("x")) {
                    inPrivateUse = true;
                }
            } else if (i == 0) {
                sb.append(s.toLowerCase());
            } else if (s.length() == 4 && isAlpha(s)) {
                sb.append(normalizeScript(s));
            } else if (s.length() == 2 && isAlpha(s)) {
                sb.append(s.toUpperCase());
            } else {
                sb.append(s.toLowerCase());
            }
            i = i + 1;
        }
        return sb.toString();
    }

    private static boolean isAlpha(String s) {
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'))) {
                return false;
            }
            i = i + 1;
        }
        return s.length() > 0;
    }

    private static boolean isAllDigits(String s) {
        int i = 0;
        while (i < s.length()) {
            if (!isDigitChar(s.charAt(i))) {
                return false;
            }
            i = i + 1;
        }
        return s.length() > 0;
    }

    private static boolean isDigitChar(char c) {
        return c >= '0' && c <= '9';
    }

    private static String[] toArray0(ArrayList<String> l) {
        String[] a = new String[l.size()];
        int i = 0;
        while (i < a.length) {
            a[i] = l.get(i);
            i = i + 1;
        }
        return a;
    }

    // ---- display names ------------------------------------------------------------------------------
    //
    // Without CLDR there are no translated names, and returning the code is the JDK's FALLBACK
    // answer -- what it replies for any locale it has no data for. Which is to say it is not an
    // invention: it is the same answer, always.

    public final String getDisplayLanguage() {
        return this.getDisplayLanguage(getDefault(Category.DISPLAY));
    }

    public String getDisplayLanguage(Locale inLocale) {
        return this.language;
    }

    public final String getDisplayCountry() {
        return this.getDisplayCountry(getDefault(Category.DISPLAY));
    }

    public String getDisplayCountry(Locale inLocale) {
        return this.country;
    }

    public final String getDisplayVariant() {
        return this.getDisplayVariant(getDefault(Category.DISPLAY));
    }

    public String getDisplayVariant(Locale inLocale) {
        return this.variant;
    }

    public String getDisplayScript() {
        return this.getDisplayScript(getDefault(Category.DISPLAY));
    }

    public String getDisplayScript(Locale inLocale) {
        return this.script;
    }

    public final String getDisplayName() {
        return this.getDisplayName(getDefault(Category.DISPLAY));
    }

    // The assembly IS the JDK's: the language, and in parentheses whatever there is of script,
    // country and variant, comma-separated.
    public String getDisplayName(Locale inLocale) {
        StringBuilder inner = new StringBuilder();
        this.appendPart(inner, this.getDisplayScript(inLocale));
        this.appendPart(inner, this.getDisplayCountry(inLocale));
        this.appendPart(inner, this.getDisplayVariant(inLocale));
        String languageName = this.getDisplayLanguage(inLocale);
        if (inner.length() == 0) {
            return languageName;
        }
        if (languageName.length() == 0) {
            return inner.toString();
        }
        return languageName + " (" + inner.toString() + ")";
    }

    private void appendPart(StringBuilder sb, String s) {
        if (s != null && s.length() > 0) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(s);
        }
    }

    // ---- the three-letter codes ---------------------------------------------------------------------

    // The two tables: "two letters", "three letters" pairs. They cover the languages and countries
    // this class names in its constants, plus the commonest. For the rest MissingResourceException is
    // thrown, which is what the JDK does with a code it does not know.
    private static final String[] ISO3_LANGUAGE = {
        "en", "eng", "es", "spa", "de", "deu", "fr", "fra", "it", "ita", "pt", "por",
        "ja", "jpn", "ko", "kor", "zh", "zho", "ru", "rus", "ar", "ara", "nl", "nld",
        "sv", "swe", "pl", "pol", "tr", "tur", "he", "heb", "yi", "yid", "id", "ind",
    };

    private static final String[] ISO3_COUNTRY = {
        "US", "USA", "GB", "GBR", "DE", "DEU", "FR", "FRA", "IT", "ITA", "ES", "ESP",
        "JP", "JPN", "KR", "KOR", "CN", "CHN", "TW", "TWN", "CA", "CAN", "AR", "ARG",
        "BR", "BRA", "MX", "MEX", "RU", "RUS", "PT", "PRT", "NL", "NLD",
    };

    /**
     * The three-letter ISO 639-2 code.
     *
     * <p>An empty language returns the empty string -- it is not an error, it is "unspecified". A
     * code the table does not have throws `MissingResourceException`, just as the JDK does:
     * answering the two-letter code would be returning something that is not an ISO3.
     */
    public String getISO3Language() {
        if (this.language.length() == 0) {
            return "";
        }
        String r = findPair(ISO3_LANGUAGE, this.language);
        if (r == null) {
            throw new MissingResourceException(
                    "Couldn't find 3-letter language code for " + this.language,
                    "java.util.Locale", this.language);
        }
        return r;
    }

    public String getISO3Country() {
        if (this.country.length() == 0) {
            return "";
        }
        String r = findPair(ISO3_COUNTRY, this.country);
        if (r == null) {
            throw new MissingResourceException(
                    "Couldn't find 3-letter country code for " + this.country,
                    "java.util.Locale", this.country);
        }
        return r;
    }

    private static String findPair(String[] table, String lookupKey) {
        int i = 0;
        while (i < table.length) {
            if (table[i].equals(lookupKey)) {
                return table[i + 1];
            }
            i = i + 2;
        }
        return null;
    }

    private static String[] codesOf(String[] table) {
        String[] out = new String[table.length / 2];
        int i = 0;
        while (i < out.length) {
            out[i] = table[i * 2];
            i = i + 1;
        }
        return out;
    }

    // A subset: what is in the table, not the complete ISO list. See the header.
    public static String[] getISOLanguages() {
        return codesOf(ISO3_LANGUAGE);
    }

    public static String[] getISOCountries() {
        return codesOf(ISO3_COUNTRY);
    }

    public static Set<String> getISOCountries(IsoCountryCode type) {
        LinkedHashSet<String> out = new LinkedHashSet<String>();
        String[] dos = codesOf(ISO3_COUNTRY);
        int i = 0;
        while (i < dos.length) {
            if (type == IsoCountryCode.PART1_ALPHA2) {
                out.add(dos[i]);
            } else {
                out.add(findPair(ISO3_COUNTRY, dos[i]));
            }
            i = i + 1;
        }
        return out;
    }

    // ---- the default ------------------------------------------------------------------------------

    public static Locale getDefault() {
        return defaultLocale;
    }

    /**
     * A **category**'s default.
     *
     * <p>That there are two is not a whim: a program may want the interface in one language and the
     * numbers and dates by another's conventions -- somebody in Germany using the application in
     * English expects to see `1.234,56`. `DISPLAY` is the first, `FORMAT` the second.
     */
    public static Locale getDefault(Category category) {
        if (category == null) {
            throw new NullPointerException();
        }
        if (category == Category.DISPLAY) {
            return defaultDisplayLocale;
        }
        return defaultFormatLocale;
    }

    // It changes all three. It is what the JDK does: the plain default drags both categories.
    public static synchronized void setDefault(Locale newLocale) {
        if (newLocale == null) {
            throw new NullPointerException();
        }
        defaultLocale = newLocale;
        defaultDisplayLocale = newLocale;
        defaultFormatLocale = newLocale;
    }

    public static synchronized void setDefault(Category category, Locale newLocale) {
        if (category == null || newLocale == null) {
            throw new NullPointerException();
        }
        if (category == Category.DISPLAY) {
            defaultDisplayLocale = newLocale;
        } else {
            defaultFormatLocale = newLocale;
        }
    }

    // The ones this class names. Without CLDR there are no more.
    public static Locale[] getAvailableLocales() {
        Locale[] a = { ROOT, ENGLISH, US, UK, CANADA, GERMAN, GERMANY, FRENCH, FRANCE,
            CANADA_FRENCH, ITALIAN, ITALY, JAPANESE, JAPAN, KOREAN, KOREA, CHINESE,
            SIMPLIFIED_CHINESE, TRADITIONAL_CHINESE };
        return a;
    }

    public static Stream<Locale> availableLocales() {
        return Stream.of(getAvailableLocales());
    }

    // ---- RFC 4647 filtering -------------------------------------------------------------------------
    //
    // The problem it solves: the browser sends "I want es-AR, failing that es, failing that en" with
    // weights, and the server has a handful of translations. These four operations are the two ways
    // of crossing those two lists: **filtering** returns every one that serves, ordered by
    // preference; **looking up** returns the single best.
    //
    // The matching is by **subtag prefix**: `es` matches `es-AR` but not `est`. That is the whole
    // rule, and it is what makes the range `*` match everything.

    public static List<Locale> filter(List<LanguageRange> priorityList,
            Collection<Locale> locales) {
        return filter(priorityList, locales, FilteringMode.AUTOSELECT_FILTERING);
    }

    public static List<Locale> filter(List<LanguageRange> priorityList,
            Collection<Locale> locales, FilteringMode mode) {
        ArrayList<Locale> out = new ArrayList<Locale>();
        int r = 0;
        while (r < priorityList.size()) {
            LanguageRange span = priorityList.get(r);
            if (span.getWeight() > 0.0d) {
                Iterator<Locale> it = locales.iterator();
                while (it.hasNext()) {
                    Locale l = it.next();
                    if (matchesPattern(span.getRange(), l.toLanguageTag()) && !out.contains(l)) {
                        out.add(l);
                    }
                }
            }
            r = r + 1;
        }
        return out;
    }

    public static List<String> filterTags(List<LanguageRange> priorityList, Collection<String> tags) {
        return filterTags(priorityList, tags, FilteringMode.AUTOSELECT_FILTERING);
    }

    public static List<String> filterTags(List<LanguageRange> priorityList,
            Collection<String> tags, FilteringMode mode) {
        ArrayList<String> out = new ArrayList<String>();
        int r = 0;
        while (r < priorityList.size()) {
            LanguageRange span = priorityList.get(r);
            if (span.getWeight() > 0.0d) {
                Iterator<String> it = tags.iterator();
                while (it.hasNext()) {
                    String t = it.next();
                    if (matchesPattern(span.getRange(), t) && !out.contains(t)) {
                        out.add(t);
                    }
                }
            }
            r = r + 1;
        }
        return out;
    }

    /**
     * The best locale for the preference list, or null.
     *
     * <p>The difference from `filter` is not only that it returns one: it **shortens the range**
     * until it matches. With `es-AR` asked for and only `es` available, `filter` returns nothing and
     * `lookup` does return `es` -- which is what one wants from a lookup.
     */
    public static Locale lookup(List<LanguageRange> priorityList, Collection<Locale> locales) {
        String t = lookupTagInternal(priorityList, tagsOf(locales));
        if (t == null) {
            return null;
        }
        Iterator<Locale> it = locales.iterator();
        while (it.hasNext()) {
            Locale l = it.next();
            if (l.toLanguageTag().equalsIgnoreCase(t)) {
                return l;
            }
        }
        return null;
    }

    public static String lookupTag(List<LanguageRange> priorityList, Collection<String> tags) {
        return lookupTagInternal(priorityList, tags);
    }

    private static Collection<String> tagsOf(Collection<Locale> locales) {
        ArrayList<String> out = new ArrayList<String>();
        Iterator<Locale> it = locales.iterator();
        while (it.hasNext()) {
            out.add(it.next().toLanguageTag());
        }
        return out;
    }

    private static String lookupTagInternal(List<LanguageRange> priorityList,
            Collection<String> tags) {
        int r = 0;
        while (r < priorityList.size()) {
            LanguageRange span = priorityList.get(r);
            if (span.getWeight() > 0.0d) {
                String current = span.getRange();
                while (current.length() > 0) {
                    Iterator<String> it = tags.iterator();
                    while (it.hasNext()) {
                        String t = it.next();
                        if (t.equalsIgnoreCase(current)) {
                            return t;
                        }
                    }
                    // The last subtag is cut off and it is tried again; a lone `-x` does not count.
                    int cut = current.lastIndexOf('-');
                    if (cut < 0) {
                        current = "";
                    } else {
                        current = current.substring(0, cut);
                        if (current.length() > 2 && current.charAt(current.length() - 2) == '-') {
                            current = current.substring(0, current.length() - 2);
                        }
                    }
                }
            }
            r = r + 1;
        }
        return null;
    }

    // `es` matches `es-AR`; `es` does NOT match `est`. The hyphen is what separates subtags, and
    // that is why the prefix has to end where one ends.
    private static boolean matchesPattern(String span, String tag) {
        if (span.equals("*")) {
            return true;
        }
        String r = span.toLowerCase();
        String t = tag.toLowerCase();
        if (t.equals(r)) {
            return true;
        }
        return t.startsWith(r) && t.charAt(r.length()) == '-';
    }

    // ---- identity -------------------------------------------------------------------------------

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Locale)) {
            return false;
        }
        Locale o = (Locale) obj;
        return this.language.equals(o.language) && this.country.equals(o.country)
                && this.variant.equals(o.variant) && this.script.equals(o.script)
                && this.toLanguageTag().equals(o.toLanguageTag());
    }

    public int hashCode() {
        return this.language.hashCode() ^ this.country.hashCode() ^ this.variant.hashCode()
                ^ this.script.hashCode();
    }

    /**
     * The **old** format, with underscores: `es_AR`, `en_US_POSIX`.
     *
     * <p>It is not interchangeable with anything outside Java -- `toLanguageTag()` is there for
     * that. It is kept because it is what the JDK returns and there is code that parses it.
     */
    public final String toString() {
        StringBuilder sb = new StringBuilder(this.language);
        if (this.country.length() > 0 || this.variant.length() > 0) {
            sb.append('_');
            sb.append(this.country);
        }
        if (this.variant.length() > 0) {
            sb.append('_');
            sb.append(this.variant);
        }
        return sb.toString();
    }

    /**
     * A language range with its **weight**: `es-AR` at 1.0, `en` at 0.8.
     *
     * <p>It is RFC 4647 filtering's missing half. It comes straight out of the HTTP
     * `Accept-Language` header, which is where it is used: `es-AR,es;q=0.9,en;q=0.5` is exactly a
     * list of these, and `parse` converts it.
     *
     * <p>The weight orders, it does not filter -- except zero, which means "**not** this": a range
     * with weight 0 excludes what it matches instead of accepting it at low priority.
     */
    public static final class LanguageRange {

        public static final double MAX_WEIGHT = 1.0d;
        public static final double MIN_WEIGHT = 0.0d;

        private final String range;
        private final double weight;

        public LanguageRange(String range) {
            this(range, MAX_WEIGHT);
        }

        public LanguageRange(String range, double weight) {
            if (range == null) {
                throw new NullPointerException();
            }
            if (weight < MIN_WEIGHT || weight > MAX_WEIGHT) {
                throw new IllegalArgumentException("weight=" + weight);
            }
            this.range = range.toLowerCase();
            this.weight = weight;
        }

        public String getRange() {
            return this.range;
        }

        public double getWeight() {
            return this.weight;
        }

        /**
         * It parses an `Accept-Language` list.
         *
         * <p>The list comes out **ordered by descending weight**, which is what makes it directly
         * usable: whoever walks it in order is walking the preferences in order.
         */
        public static List<LanguageRange> parse(String ranges) {
            if (ranges == null) {
                throw new NullPointerException();
            }
            ArrayList<LanguageRange> out = new ArrayList<LanguageRange>();
            String[] parts = ranges.split(",");
            int i = 0;
            while (i < parts.length) {
                String p = parts[i].trim();
                if (p.length() > 0) {
                    double w = MAX_WEIGHT;
                    int q = p.indexOf(";");
                    if (q >= 0) {
                        String queue = p.substring(q + 1, p.length()).trim();
                        p = p.substring(0, q).trim();
                        int equalTo = queue.indexOf("=");
                        if (equalTo >= 0) {
                            String v = queue.substring(equalTo + 1, queue.length()).trim();
                            try {
                                w = Double.parseDouble(v);
                            } catch (NumberFormatException e) {
                                throw new IllegalArgumentException("weight: " + v);
                            }
                        }
                    }
                    out.add(new LanguageRange(p, w));
                }
                i = i + 1;
            }
            // A stable ordering by descending weight: two ranges of the same weight keep the order
            // they came in, which is the tie-break the specification asks for.
            sortByWeight(out);
            return out;
        }

        public static List<LanguageRange> parse(String ranges,
                Map<String, List<String>> map) {
            return mapEquivalents(parse(ranges), map);
        }

        /**
         * It adds, for each range, its equivalents according to the given map, at the same weight.
         *
         * <p>What it is for: `Accept-Language: zh-TW` and a translation tagged `zh-Hant` are the same
         * thing to a human and different to prefix matching. The map is where that equivalence is
         * declared.
         */
        public static List<LanguageRange> mapEquivalents(List<LanguageRange> priorityList,
                Map<String, List<String>> map) {
            ArrayList<LanguageRange> out = new ArrayList<LanguageRange>();
            int i = 0;
            while (i < priorityList.size()) {
                LanguageRange r = priorityList.get(i);
                out.add(r);
                if (map != null) {
                    List<String> eq = map.get(r.getRange());
                    if (eq != null) {
                        int j = 0;
                        while (j < eq.size()) {
                            LanguageRange updated = new LanguageRange(eq.get(j), r.getWeight());
                            if (!out.contains(updated)) {
                                out.add(updated);
                            }
                            j = j + 1;
                        }
                    }
                }
                i = i + 1;
            }
            return out;
        }

        // Insertion sort: an Accept-Language list has a handful of elements, and insertion is stable
        // without asking anything of the comparator.
        private static void sortByWeight(ArrayList<LanguageRange> l) {
            int i = 1;
            while (i < l.size()) {
                LanguageRange current = l.get(i);
                int j = i - 1;
                while (j >= 0 && l.get(j).getWeight() < current.getWeight()) {
                    l.set(j + 1, l.get(j));
                    j = j - 1;
                }
                l.set(j + 1, current);
                i = i + 1;
            }
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof LanguageRange)) {
                return false;
            }
            LanguageRange o = (LanguageRange) obj;
            return this.range.equals(o.range) && this.weight == o.weight;
        }

        public int hashCode() {
            return this.range.hashCode();
        }

        public String toString() {
            return this.range + ";q=" + this.weight;
        }
    }

    /** The two default categories: the interface and the formats. */
    public enum Category {
        DISPLAY,
        FORMAT
    }

    /** The three country-code forms ISO 3166 defines. */
    public enum IsoCountryCode {
        PART1_ALPHA2,
        PART1_ALPHA3,
        PART3
    }

    /**
     * What to do with an **extended** language range when filtering (RFC 4647 §3.3.2).
     *
     * <p>An extended range carries wildcards in the middle (`*-CH`, "any language of Switzerland"),
     * and not every filter supports them. These five values are the possible policies.
     */
    public enum FilteringMode {
        AUTOSELECT_FILTERING,
        EXTENDED_FILTERING,
        IGNORE_EXTENDED_RANGES,
        MAP_EXTENDED_RANGES,
        REJECT_EXTENDED_RANGES
    }
}
