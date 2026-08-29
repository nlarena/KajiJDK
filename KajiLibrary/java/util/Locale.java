package java.util;

// KajiLibrary's java.util.Locale — a language/country tag. A KajiLibrary subset: the common
// constants, the (language[, country]) constructors and the getters. It carries no locale
// data itself; Formatter reads the language to pick number separators (only German diverges
// from the US/ROOT convention in this subset — full CLDR data is future work).
public final class Locale {

    public static final Locale ROOT = new Locale("", "");
    public static final Locale ENGLISH = new Locale("en", "");
    public static final Locale US = new Locale("en", "US");
    public static final Locale UK = new Locale("en", "GB");
    public static final Locale GERMAN = new Locale("de", "");
    public static final Locale GERMANY = new Locale("de", "DE");
    public static final Locale FRENCH = new Locale("fr", "");
    public static final Locale FRANCE = new Locale("fr", "FR");

    private final String language;
    private final String country;

    public Locale(String language) {
        this.language = language;
        this.country = "";
    }

    public Locale(String language, String country) {
        this.language = language;
        this.country = country;
    }

    public String getLanguage() {
        return this.language;
    }

    public String getCountry() {
        return this.country;
    }

    // A US-convention default (the subset doesn't read the host locale).
    public static Locale getDefault() {
        return US;
    }

    public final String toString() {
        if (this.country.length() == 0) {
            return this.language;
        }
        if (this.language.length() == 0) {
            return "_" + this.country;
        }
        return this.language + "_" + this.country;
    }
}
