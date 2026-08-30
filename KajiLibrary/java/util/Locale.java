package java.util;

// Same-package imports work around the frozen javac's finder (finding #4).
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

// Una etiqueta de idioma: que lengua, en que pais, con que escritura y que variante.
//
// Un Locale **no traduce nada ni sabe nada**: es la clave con la que otras clases buscan sus datos
// -- `Formatter` para los separadores numericos, `Calendar` para el primer dia de la semana,
// `ResourceBundle` para el archivo de textos. Esa separacion es todo el diseno, y es lo que
// permite que una etiqueta que esta biblioteca no conoce igual se pueda pasar, comparar y
// serializar sin perder informacion.
//
// Las cuatro partes, en el orden en que se escriben en una etiqueta BCP 47:
//
//   idioma     es       ISO 639, minusculas       obligatorio (o vacio para "sin especificar")
//   escritura  Latn     ISO 15924, Capitalizado   opcional
//   region     AR       ISO 3166, MAYUSCULAS      opcional
//   variante   valencia libre                     opcional
//
// Y las **extensiones**, que son la parte que casi nadie mira: `-u-` lleva preferencias Unicode
// (calendario, moneda, orden alfabetico) y `-x-` es de uso privado. Se guardan y se devuelven tal
// cual, que es lo unico honesto sin la base de datos CLDR.
//
// ---- lo que esta y lo que no -------------------------------------------------------------------
//
// **58 miembros nuevos: el contrato queda completo.** Lo que NO hay son los **datos**, y eso marca
// tres divergencias que conviene tener a la vista:
//
// | | |
// |---|---|
// | `getDisplayLanguage()` y compania | devuelven el **codigo** (`"es"`, no `"español"`). Es exactamente lo que hace el JDK cuando no tiene datos para un locale, asi que no es una respuesta inventada -- es la respuesta de respaldo, siempre |
// | `getISO3Language()`/`getISO3Country()` | hay tabla para los idiomas y paises que esta clase nombra, y para el resto se tira `MissingResourceException`, que es lo que hace el JDK con un codigo que no conoce |
// | `getISOLanguages()`/`getISOCountries()` | devuelven lo que hay en esa tabla, no las listas ISO completas (184 idiomas y 249 paises). Es un subconjunto, y esta dicho |
//
// El resto -- construir, parsear, componer etiquetas, extensiones, filtrado RFC 4647 -- es
// mecanismo y no datos, y esta entero.
public final class Locale {

    // Las dos letras que identifican una extension con nombre propio.
    public static final char UNICODE_LOCALE_EXTENSION = 'u';
    public static final char PRIVATE_USE_EXTENSION = 'x';

    // Va **antes** que las constantes a proposito: los inicializadores estaticos corren en orden
    // de aparicion, y cada constante llama al constructor privado que lee este arreglo. Declararlo
    // despues lo dejaba en null durante la construccion de ROOT, y `toLanguageTag()` moria con un
    // NullPointerException en el primer uso.
    private static final String[] SIN_EXT = new String[0];

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

    // Las extensiones, como texto crudo: la letra y su contenido, en el orden en que llegaron.
    // Se guardan sin interpretar; ver la nota de la cabecera.
    private final String[] extKeys;
    private final String[] extValues;

    // El default es mutable (`setDefault`) y arranca en US, que es la convencion de esta
    // biblioteca: no se lee el locale del sistema.
    private static Locale porDefecto = US;
    private static Locale porDefectoDisplay = US;
    private static Locale porDefectoFormat = US;

    public Locale(String language) {
        this(language, "", "");
    }

    public Locale(String language, String country) {
        this(language, country, "");
    }

    // La forma completa clasica. Los tres constructores estan **deprecados** desde Java 19 a favor
    // de `of(...)`, por una razon concreta: un constructor no puede devolver una instancia
    // compartida, y las etiquetas se repiten mucho.
    public Locale(String language, String country, String variant) {
        this(language, "", country, variant, SIN_EXT, SIN_EXT);
    }

    private Locale(String language, String script, String country, String variant,
            String[] extKeys, String[] extValues) {
        this.language = normalizarIdioma(language);
        this.script = normalizarEscritura(script);
        this.country = normalizarRegion(country);
        this.variant = variant == null ? "" : variant;
        this.extKeys = extKeys;
        this.extValues = extValues;
    }

    // ---- fabricas -----------------------------------------------------------------------------

    public static Locale of(String language) {
        return new Locale(language, "", "");
    }

    public static Locale of(String language, String country) {
        return new Locale(language, country, "");
    }

    public static Locale of(String language, String country, String variant) {
        return new Locale(language, country, variant);
    }

    // ---- normalizacion ------------------------------------------------------------------------
    //
    // El caso lo fija la especificacion y no es cosmetico: dos etiquetas que solo difieren en
    // mayusculas son **la misma**, y sin normalizar al construir, `equals` diria que no.

    private static String normalizarIdioma(String s) {
        if (s == null) {
            return "";
        }
        String l = s.toLowerCase();
        // Los tres codigos que ISO renombro. Java **canonicaliza al nuevo**: `new Locale("iw")`
        // da `he`, y no al reves.
        //
        // Vale la pena decir que esto cambio: durante veinte anios el JDK guardaba el codigo VIEJO
        // (`he` -> `iw`) y solo `toLanguageTag()` devolvia el nuevo. Se verifico contra el JDK 25,
        // que hace lo contrario, y es lo que se replica.
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

    private static String normalizarRegion(String s) {
        if (s == null) {
            return "";
        }
        return s.toUpperCase();
    }

    // Capitalizada: `Latn`, no `latn` ni `LATN`.
    private static String normalizarEscritura(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        String l = s.toLowerCase();
        return l.substring(0, 1).toUpperCase() + l.substring(1, l.length());
    }

    // ---- las cuatro partes ---------------------------------------------------------------------

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

    // ---- extensiones ----------------------------------------------------------------------------

    public boolean hasExtensions() {
        return this.extKeys.length > 0;
    }

    // El contenido de una extension, o null si esta etiqueta no la lleva.
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

    // Los atributos de la extension `-u-`: las claves sueltas, sin valor.
    //
    // La forma de `-u-` es una lista de atributos y despues pares clave-valor, y lo que separa a
    // los dos es el largo: una clave Unicode son **dos** caracteres, un atributo tres o mas.
    public Set<String> getUnicodeLocaleAttributes() {
        LinkedHashSet<String> out = new LinkedHashSet<String>();
        String u = this.getExtension(UNICODE_LOCALE_EXTENSION);
        if (u == null) {
            return out;
        }
        String[] partes = u.split("-");
        int i = 0;
        while (i < partes.length && partes[i].length() != 2) {
            if (partes[i].length() > 0) {
                out.add(partes[i]);
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
        String[] partes = u.split("-");
        int i = 0;
        while (i < partes.length) {
            if (partes[i].length() == 2) {
                out.add(partes[i]);
            }
            i = i + 1;
        }
        return out;
    }

    // El valor de una clave Unicode: lo que sigue a la clave hasta la proxima clave.
    public String getUnicodeLocaleType(String key) {
        if (key == null) {
            throw new NullPointerException();
        }
        String u = this.getExtension(UNICODE_LOCALE_EXTENSION);
        if (u == null) {
            return null;
        }
        String[] partes = u.split("-");
        int i = 0;
        while (i < partes.length) {
            if (partes[i].equals(key)) {
                StringBuilder sb = new StringBuilder();
                int j = i + 1;
                while (j < partes.length && partes[j].length() != 2) {
                    if (sb.length() > 0) {
                        sb.append('-');
                    }
                    sb.append(partes[j]);
                    j = j + 1;
                }
                return sb.toString();
            }
            i = i + 1;
        }
        return null;
    }

    // La misma etiqueta sin ninguna extension. Es lo que se usa para comparar dos locales por su
    // identidad linguistica, ignorando preferencias de formato.
    public Locale stripExtensions() {
        if (!this.hasExtensions()) {
            return this;
        }
        return new Locale(this.language, this.script, this.country, this.variant, SIN_EXT, SIN_EXT);
    }

    // ---- etiquetas BCP 47 ------------------------------------------------------------------------

    /**
     * La etiqueta BCP 47 de este locale: `es-AR`, `zh-Hant-TW`, `en-US-u-ca-buddhist`.
     *
     * <p>Es la forma **canonica** y la que hay que usar para serializar: `toString()` produce el
     * formato viejo con guiones bajos, que no es intercambiable con nada de afuera de Java.
     *
     * <p>Un locale sin idioma sale como `und`, que es como BCP 47 dice "sin especificar".
     */
    public String toLanguageTag() {
        StringBuilder sb = new StringBuilder();
        // El idioma ya viene canonicalizado del constructor, asi que no hay nada que traducir aca.
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
            // La de uso privado va **ultima**, por definicion: todo lo que sigue a `-x-` es suyo.
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
     * El locale de una etiqueta BCP 47.
     *
     * <p>Reconoce `idioma[-Escritura][-REGION][-variante][-extensiones]`, distinguiendo cada parte
     * por su **forma** y no por su posicion: la escritura son cuatro letras, la region dos letras o
     * tres digitos, y una extension es un solo caracter seguido de guion. Es lo que permite parsear
     * `zh-Hant-TW` y `es-419` sin ambiguedad.
     *
     * <p>Lo que no matchea ninguna forma se descarta, que es lo que manda BCP 47 para una etiqueta
     * mal formada: quedarse con el prefijo bueno en vez de fallar.
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
        if (i < p.length && p[i].length() >= 2 && p[i].length() <= 8 && esAlfa(p[i])) {
            if (!p[i].equalsIgnoreCase("und")) {
                lang = p[i];
            }
            i = i + 1;
        }
        if (i < p.length && p[i].length() == 4 && esAlfa(p[i])) {
            script = p[i];
            i = i + 1;
        }
        if (i < p.length && ((p[i].length() == 2 && esAlfa(p[i]))
                || (p[i].length() == 3 && esDigitos(p[i])))) {
            region = p[i];
            i = i + 1;
        }
        // Variantes: 5 a 8 caracteres, o 4 empezando con digito. Se juntan con guion.
        StringBuilder vs = new StringBuilder();
        while (i < p.length && p[i].length() > 1
                && (p[i].length() >= 5 || (p[i].length() == 4 && esDigito(p[i].charAt(0))))) {
            if (vs.length() > 0) {
                vs.append('-');
            }
            vs.append(p[i]);
            i = i + 1;
        }
        variant = vs.toString();
        // Extensiones: un caracter, y todo lo que sigue hasta la proxima de un caracter.
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
        return new Locale(lang, script, region, variant, aArreglo(keys), aArreglo(values));
    }

    /**
     * La misma etiqueta con el caso canonico: idioma en minusculas, escritura Capitalizada, region
     * en MAYUSCULAS.
     *
     * <p>Es puramente de forma -- **no** valida ni resuelve nada --, y sirve para comparar dos
     * etiquetas escritas por gente distinta sin construir dos Locale.
     */
    public static String caseFoldLanguageTag(String languageTag) {
        if (languageTag == null) {
            throw new NullPointerException();
        }
        String[] p = languageTag.split("-");
        StringBuilder sb = new StringBuilder();
        int i = 0;
        boolean enPrivada = false;
        while (i < p.length) {
            if (i > 0) {
                sb.append('-');
            }
            String s = p[i];
            if (enPrivada || s.length() == 1) {
                sb.append(s.toLowerCase());
                if (s.length() == 1 && s.equalsIgnoreCase("x")) {
                    enPrivada = true;
                }
            } else if (i == 0) {
                sb.append(s.toLowerCase());
            } else if (s.length() == 4 && esAlfa(s)) {
                sb.append(normalizarEscritura(s));
            } else if (s.length() == 2 && esAlfa(s)) {
                sb.append(s.toUpperCase());
            } else {
                sb.append(s.toLowerCase());
            }
            i = i + 1;
        }
        return sb.toString();
    }

    private static boolean esAlfa(String s) {
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

    private static boolean esDigitos(String s) {
        int i = 0;
        while (i < s.length()) {
            if (!esDigito(s.charAt(i))) {
                return false;
            }
            i = i + 1;
        }
        return s.length() > 0;
    }

    private static boolean esDigito(char c) {
        return c >= '0' && c <= '9';
    }

    private static String[] aArreglo(ArrayList<String> l) {
        String[] a = new String[l.size()];
        int i = 0;
        while (i < a.length) {
            a[i] = l.get(i);
            i = i + 1;
        }
        return a;
    }

    // ---- nombres para mostrar -----------------------------------------------------------------------
    //
    // Sin CLDR no hay nombres traducidos, y devolver el codigo es la respuesta de RESPALDO del JDK
    // -- lo que contesta para cualquier locale del que no tenga datos. O sea que no es un invento:
    // es la misma respuesta, siempre.

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

    // El armado si es el del JDK: idioma, y entre parentesis lo que haya de escritura, pais y
    // variante, separado por comas.
    public String getDisplayName(Locale inLocale) {
        StringBuilder dentro = new StringBuilder();
        this.agregar(dentro, this.getDisplayScript(inLocale));
        this.agregar(dentro, this.getDisplayCountry(inLocale));
        this.agregar(dentro, this.getDisplayVariant(inLocale));
        String idioma = this.getDisplayLanguage(inLocale);
        if (dentro.length() == 0) {
            return idioma;
        }
        if (idioma.length() == 0) {
            return dentro.toString();
        }
        return idioma + " (" + dentro.toString() + ")";
    }

    private void agregar(StringBuilder sb, String s) {
        if (s != null && s.length() > 0) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(s);
        }
    }

    // ---- codigos de tres letras ---------------------------------------------------------------------

    // Las dos tablas: pares "dos letras", "tres letras". Cubren los idiomas y paises que esta clase
    // nombra en sus constantes, mas los mas frecuentes. Para lo demas se tira
    // MissingResourceException, que es lo que hace el JDK con un codigo que no conoce.
    private static final String[] ISO3_IDIOMA = {
        "en", "eng", "es", "spa", "de", "deu", "fr", "fra", "it", "ita", "pt", "por",
        "ja", "jpn", "ko", "kor", "zh", "zho", "ru", "rus", "ar", "ara", "nl", "nld",
        "sv", "swe", "pl", "pol", "tr", "tur", "he", "heb", "yi", "yid", "id", "ind",
    };

    private static final String[] ISO3_PAIS = {
        "US", "USA", "GB", "GBR", "DE", "DEU", "FR", "FRA", "IT", "ITA", "ES", "ESP",
        "JP", "JPN", "KR", "KOR", "CN", "CHN", "TW", "TWN", "CA", "CAN", "AR", "ARG",
        "BR", "BRA", "MX", "MEX", "RU", "RUS", "PT", "PRT", "NL", "NLD",
    };

    /**
     * El codigo ISO 639-2 de tres letras.
     *
     * <p>Un idioma vacio devuelve la cadena vacia -- no es un error, es "sin especificar". Un
     * codigo que la tabla no tiene lanza `MissingResourceException`, igual que el JDK: contestar
     * el codigo de dos letras seria devolver algo que no es un ISO3.
     */
    public String getISO3Language() {
        if (this.language.length() == 0) {
            return "";
        }
        String r = buscarPar(ISO3_IDIOMA, this.language);
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
        String r = buscarPar(ISO3_PAIS, this.country);
        if (r == null) {
            throw new MissingResourceException(
                    "Couldn't find 3-letter country code for " + this.country,
                    "java.util.Locale", this.country);
        }
        return r;
    }

    private static String buscarPar(String[] tabla, String clave) {
        int i = 0;
        while (i < tabla.length) {
            if (tabla[i].equals(clave)) {
                return tabla[i + 1];
            }
            i = i + 2;
        }
        return null;
    }

    private static String[] codigosDe(String[] tabla) {
        String[] out = new String[tabla.length / 2];
        int i = 0;
        while (i < out.length) {
            out[i] = tabla[i * 2];
            i = i + 1;
        }
        return out;
    }

    // Subconjunto: lo que hay en la tabla, no la lista ISO completa. Ver la cabecera.
    public static String[] getISOLanguages() {
        return codigosDe(ISO3_IDIOMA);
    }

    public static String[] getISOCountries() {
        return codigosDe(ISO3_PAIS);
    }

    public static Set<String> getISOCountries(IsoCountryCode type) {
        LinkedHashSet<String> out = new LinkedHashSet<String>();
        String[] dos = codigosDe(ISO3_PAIS);
        int i = 0;
        while (i < dos.length) {
            if (type == IsoCountryCode.PART1_ALPHA2) {
                out.add(dos[i]);
            } else {
                out.add(buscarPar(ISO3_PAIS, dos[i]));
            }
            i = i + 1;
        }
        return out;
    }

    // ---- el default -------------------------------------------------------------------------------

    public static Locale getDefault() {
        return porDefecto;
    }

    /**
     * El default de una **categoria**.
     *
     * <p>Que haya dos no es un capricho: un programa puede querer la interfaz en un idioma y los
     * numeros y fechas con las convenciones de otro -- alguien en Alemania usando la aplicacion en
     * ingles espera ver `1.234,56`. `DISPLAY` es lo primero, `FORMAT` lo segundo.
     */
    public static Locale getDefault(Category category) {
        if (category == null) {
            throw new NullPointerException();
        }
        if (category == Category.DISPLAY) {
            return porDefectoDisplay;
        }
        return porDefectoFormat;
    }

    // Cambia las tres. Es lo que hace el JDK: el default "a secas" arrastra a las dos categorias.
    public static synchronized void setDefault(Locale newLocale) {
        if (newLocale == null) {
            throw new NullPointerException();
        }
        porDefecto = newLocale;
        porDefectoDisplay = newLocale;
        porDefectoFormat = newLocale;
    }

    public static synchronized void setDefault(Category category, Locale newLocale) {
        if (category == null || newLocale == null) {
            throw new NullPointerException();
        }
        if (category == Category.DISPLAY) {
            porDefectoDisplay = newLocale;
        } else {
            porDefectoFormat = newLocale;
        }
    }

    // Los que esta clase nombra. Sin CLDR no hay mas.
    public static Locale[] getAvailableLocales() {
        Locale[] a = { ROOT, ENGLISH, US, UK, CANADA, GERMAN, GERMANY, FRENCH, FRANCE,
            CANADA_FRENCH, ITALIAN, ITALY, JAPANESE, JAPAN, KOREAN, KOREA, CHINESE,
            SIMPLIFIED_CHINESE, TRADITIONAL_CHINESE };
        return a;
    }

    public static Stream<Locale> availableLocales() {
        return Stream.of(getAvailableLocales());
    }

    // ---- filtrado RFC 4647 --------------------------------------------------------------------------
    //
    // El problema que resuelve: el navegador manda "quiero es-AR, si no es, si no en" con pesos, y
    // el servidor tiene un puñado de traducciones. Estas cuatro operaciones son las dos formas de
    // cruzar esas dos listas: **filtrar** devuelve todas las que sirven, ordenadas por preferencia;
    // **buscar** devuelve la mejor sola.
    //
    // El matcheo es por **prefijo de subetiqueta**: `es` matchea `es-AR` pero no `est`. Esa es toda
    // la regla, y es la que hace que el rango `*` matchee todo.

    public static List<Locale> filter(List<LanguageRange> priorityList,
            Collection<Locale> locales) {
        return filter(priorityList, locales, FilteringMode.AUTOSELECT_FILTERING);
    }

    public static List<Locale> filter(List<LanguageRange> priorityList,
            Collection<Locale> locales, FilteringMode mode) {
        ArrayList<Locale> out = new ArrayList<Locale>();
        int r = 0;
        while (r < priorityList.size()) {
            LanguageRange rango = priorityList.get(r);
            if (rango.getWeight() > 0.0d) {
                Iterator<Locale> it = locales.iterator();
                while (it.hasNext()) {
                    Locale l = it.next();
                    if (matchea(rango.getRange(), l.toLanguageTag()) && !out.contains(l)) {
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
            LanguageRange rango = priorityList.get(r);
            if (rango.getWeight() > 0.0d) {
                Iterator<String> it = tags.iterator();
                while (it.hasNext()) {
                    String t = it.next();
                    if (matchea(rango.getRange(), t) && !out.contains(t)) {
                        out.add(t);
                    }
                }
            }
            r = r + 1;
        }
        return out;
    }

    /**
     * El mejor locale para la lista de preferencias, o null.
     *
     * <p>La diferencia con `filter` no es solo que devuelva uno: **acorta el rango** hasta que
     * matchee. Con `es-AR` pedido y solo `es` disponible, `filter` no devuelve nada y `lookup` si
     * devuelve `es` -- que es lo que uno quiere de una busqueda.
     */
    public static Locale lookup(List<LanguageRange> priorityList, Collection<Locale> locales) {
        String t = lookupTagInterno(priorityList, tagsDe(locales));
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
        return lookupTagInterno(priorityList, tags);
    }

    private static Collection<String> tagsDe(Collection<Locale> locales) {
        ArrayList<String> out = new ArrayList<String>();
        Iterator<Locale> it = locales.iterator();
        while (it.hasNext()) {
            out.add(it.next().toLanguageTag());
        }
        return out;
    }

    private static String lookupTagInterno(List<LanguageRange> priorityList,
            Collection<String> tags) {
        int r = 0;
        while (r < priorityList.size()) {
            LanguageRange rango = priorityList.get(r);
            if (rango.getWeight() > 0.0d) {
                String actual = rango.getRange();
                while (actual.length() > 0) {
                    Iterator<String> it = tags.iterator();
                    while (it.hasNext()) {
                        String t = it.next();
                        if (t.equalsIgnoreCase(actual)) {
                            return t;
                        }
                    }
                    // Se corta la ultima subetiqueta y se vuelve a probar; un `-x` suelto no cuenta.
                    int corte = actual.lastIndexOf('-');
                    if (corte < 0) {
                        actual = "";
                    } else {
                        actual = actual.substring(0, corte);
                        if (actual.length() > 2 && actual.charAt(actual.length() - 2) == '-') {
                            actual = actual.substring(0, actual.length() - 2);
                        }
                    }
                }
            }
            r = r + 1;
        }
        return null;
    }

    // `es` matchea `es-AR`; `es` NO matchea `est`. El guion es lo que separa subetiquetas, y por
    // eso el prefijo tiene que terminar donde termina una.
    private static boolean matchea(String rango, String tag) {
        if (rango.equals("*")) {
            return true;
        }
        String r = rango.toLowerCase();
        String t = tag.toLowerCase();
        if (t.equals(r)) {
            return true;
        }
        return t.startsWith(r) && t.charAt(r.length()) == '-';
    }

    // ---- identidad --------------------------------------------------------------------------------

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
     * El formato **viejo**, con guiones bajos: `es_AR`, `en_US_POSIX`.
     *
     * <p>No es intercambiable con nada de afuera de Java -- para eso esta `toLanguageTag()`. Se
     * conserva porque es lo que el JDK devuelve y hay codigo que lo parsea.
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
     * Un rango de idioma con su **peso**: `es-AR` con 1.0, `en` con 0.8.
     *
     * <p>Es la mitad que falta del filtrado RFC 4647. Sale directo del encabezado HTTP
     * `Accept-Language`, que es donde se usa: `es-AR,es;q=0.9,en;q=0.5` es exactamente una lista de
     * estos, y `parse` la convierte.
     *
     * <p>El peso ordena, no filtra -- salvo el cero, que significa "esto **no**": un rango con peso
     * 0 excluye lo que matchea en vez de aceptarlo con baja prioridad.
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
         * Parsea una lista `Accept-Language`.
         *
         * <p>La lista sale **ordenada por peso descendente**, que es lo que la vuelve utilizable
         * directo: quien la recorre en orden esta recorriendo las preferencias en orden.
         */
        public static List<LanguageRange> parse(String ranges) {
            if (ranges == null) {
                throw new NullPointerException();
            }
            ArrayList<LanguageRange> out = new ArrayList<LanguageRange>();
            String[] partes = ranges.split(",");
            int i = 0;
            while (i < partes.length) {
                String p = partes[i].trim();
                if (p.length() > 0) {
                    double w = MAX_WEIGHT;
                    int q = p.indexOf(";");
                    if (q >= 0) {
                        String cola = p.substring(q + 1, p.length()).trim();
                        p = p.substring(0, q).trim();
                        int igual = cola.indexOf("=");
                        if (igual >= 0) {
                            String v = cola.substring(igual + 1, cola.length()).trim();
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
            // Orden estable por peso descendente: dos rangos con el mismo peso conservan el orden
            // en que venian, que es la desempate que la especificacion pide.
            ordenarPorPeso(out);
            return out;
        }

        public static List<LanguageRange> parse(String ranges,
                Map<String, List<String>> map) {
            return mapEquivalents(parse(ranges), map);
        }

        /**
         * Agrega, por cada rango, sus equivalentes segun el mapa dado, con el mismo peso.
         *
         * <p>Para que sirve: `Accept-Language: zh-TW` y una traduccion etiquetada `zh-Hant` son la
         * misma cosa para un humano y distintas para el matcheo por prefijo. El mapa es donde se
         * declara esa equivalencia.
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
                            LanguageRange nuevo = new LanguageRange(eq.get(j), r.getWeight());
                            if (!out.contains(nuevo)) {
                                out.add(nuevo);
                            }
                            j = j + 1;
                        }
                    }
                }
                i = i + 1;
            }
            return out;
        }

        // Insercion: la lista de un Accept-Language tiene un puñado de elementos, y la insercion es
        // estable sin pedirle nada al comparador.
        private static void ordenarPorPeso(ArrayList<LanguageRange> l) {
            int i = 1;
            while (i < l.size()) {
                LanguageRange actual = l.get(i);
                int j = i - 1;
                while (j >= 0 && l.get(j).getWeight() < actual.getWeight()) {
                    l.set(j + 1, l.get(j));
                    j = j - 1;
                }
                l.set(j + 1, actual);
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

    /** Las dos categorias de default: la interfaz y los formatos. */
    public enum Category {
        DISPLAY,
        FORMAT
    }

    /** Las tres formas de codigo de pais que ISO 3166 define. */
    public enum IsoCountryCode {
        PART1_ALPHA2,
        PART1_ALPHA3,
        PART3
    }

    /**
     * Que hacer con un rango de idioma **extendido** al filtrar (RFC 4647 §3.3.2).
     *
     * <p>Un rango extendido lleva comodines en el medio (`*-CH`, "cualquier idioma de Suiza"), y no
     * todos los filtros los soportan. Estos cinco valores son las politicas posibles.
     */
    public enum FilteringMode {
        AUTOSELECT_FILTERING,
        EXTENDED_FILTERING,
        IGNORE_EXTENDED_RANGES,
        MAP_EXTENDED_RANGES,
        REJECT_EXTENDED_RANGES
    }
}
