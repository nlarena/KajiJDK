import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

// Comportamiento de java.util.Locale y java.util.Properties.
//
// Lo que NO se compara contra `java` real, y por que: los nombres para mostrar
// (`getDisplayLanguage` devuelve el codigo aca y "English" alla, porque no hay CLDR), el largo de
// `getISOLanguages()` (aca es la tabla propia), y la salida cruda de `store` (lleva una linea con
// la fecha). De `store` se prueba el round-trip, que es lo que importa: lo que se escribe se puede
// volver a leer.
public class LocPropTest {

    public static int run() {
        int r = 0;

        // ---- Locale: las partes -------------------------------------------------------------------
        Locale ar = Locale.of("es", "AR");
        r = r + (ar.getLanguage().equals("es") ? 1 : 0);
        r = r + (ar.getCountry().equals("AR") ? 2 : 0);
        r = r + (ar.getVariant().equals("") ? 4 : 0);
        r = r + (ar.getScript().equals("") ? 8 : 0);
        r = r + (ar.toString().equals("es_AR") ? 16 : 0);
        r = r + (ar.toLanguageTag().equals("es-AR") ? 32 : 0);

        // el caso se normaliza al construir: idioma abajo, region arriba
        Locale raro = Locale.of("ES", "ar");
        r = r + (raro.getLanguage().equals("es") && raro.getCountry().equals("AR") ? 64 : 0);
        r = r + (raro.equals(ar) ? 128 : 0);
        r = r + (raro.hashCode() == ar.hashCode() ? 256 : 0);

        // con variante
        Locale conVar = Locale.of("en", "US", "POSIX");
        r = r + (conVar.getVariant().equals("POSIX") ? 512 : 0);
        r = r + (conVar.toString().equals("en_US_POSIX") ? 1024 : 0);

        // las constantes
        r = r + (Locale.JAPAN.getCountry().equals("JP") ? 2048 : 0);
        r = r + (Locale.CANADA_FRENCH.getLanguage().equals("fr") ? 4096 : 0);
        r = r + (Locale.CHINA == Locale.SIMPLIFIED_CHINESE ? 8192 : 0);
        r = r + (Locale.ROOT.toString().equals("") ? 16384 : 0);

        // los tres codigos que ISO renombro: Java canonicaliza al NUEVO, o sea que `iw` da `he`.
        // (Durante veinte anios fue al reves; el JDK 25 hace esto.)
        Locale hebreo = Locale.of("iw");
        r = r + (hebreo.getLanguage().equals("he") ? 32768 : 0);
        r = r + (hebreo.toLanguageTag().equals("he") ? 65536 : 0);

        // ---- etiquetas BCP 47 ----------------------------------------------------------------------
        Locale zh = Locale.forLanguageTag("zh-Hant-TW");
        r = r + (zh.getLanguage().equals("zh") ? 1 : 0);
        r = r + (zh.getScript().equals("Hant") ? 2 : 0);
        r = r + (zh.getCountry().equals("TW") ? 4 : 0);
        r = r + (zh.toLanguageTag().equals("zh-Hant-TW") ? 8 : 0);

        // una region numerica (una macro-region, no un pais)
        Locale latam = Locale.forLanguageTag("es-419");
        r = r + (latam.getCountry().equals("419") ? 16 : 0);

        // sin idioma
        r = r + (Locale.forLanguageTag("und-US").getLanguage().equals("") ? 32 : 0);
        r = r + (Locale.ROOT.toLanguageTag().equals("und") ? 64 : 0);

        // el caso canonico, sin construir ningun Locale
        r = r + (Locale.caseFoldLanguageTag("ZH-hant-tw").equals("zh-Hant-TW") ? 128 : 0);
        r = r + (Locale.caseFoldLanguageTag("EN-us").equals("en-US") ? 256 : 0);

        // ---- extensiones ----------------------------------------------------------------------------
        Locale conExt = Locale.forLanguageTag("en-US-u-ca-buddhist-nu-thai");
        r = r + (conExt.hasExtensions() ? 512 : 0);
        r = r + (conExt.getExtension('u').equals("ca-buddhist-nu-thai") ? 1024 : 0);
        r = r + (conExt.getUnicodeLocaleType("ca").equals("buddhist") ? 2048 : 0);
        r = r + (conExt.getUnicodeLocaleType("nu").equals("thai") ? 4096 : 0);
        r = r + (conExt.getUnicodeLocaleType("zz") == null ? 8192 : 0);
        r = r + (conExt.getUnicodeLocaleKeys().size() == 2 ? 16384 : 0);
        r = r + (conExt.getExtensionKeys().size() == 1 ? 32768 : 0);
        // sacarlas deja la etiqueta linguistica sola
        r = r + (conExt.stripExtensions().toLanguageTag().equals("en-US") ? 65536 : 0);
        r = r + (ar.hasExtensions() ? 0 : 131072);
        // la de uso privado. Ojo con `x-lvariant-`: el JDK le da un trato especial --lo convierte
        // en la VARIANTE, no en una extension-- asi que para probar `-x-` hay que usar otra cosa.
        Locale priv = Locale.forLanguageTag("en-x-privada");
        r = r + (priv.getExtension('x').equals("privada") ? 262144 : 0);

        // ---- codigos de tres letras -------------------------------------------------------------------
        r = r + (Locale.US.getISO3Language().equals("eng") ? 1 : 0);
        r = r + (Locale.US.getISO3Country().equals("USA") ? 2 : 0);
        r = r + (Locale.JAPAN.getISO3Language().equals("jpn") ? 4 : 0);
        r = r + (Locale.ROOT.getISO3Language().equals("") ? 8 : 0);

        // ---- filtrado RFC 4647 --------------------------------------------------------------------------
        List<Locale.LanguageRange> rangos = Locale.LanguageRange.parse("es-AR,es;q=0.9,en;q=0.5");
        r = r + (rangos.size() == 3 ? 16 : 0);
        r = r + (rangos.get(0).getRange().equals("es-AR".toLowerCase()) ? 32 : 0);
        r = r + (rangos.get(0).getWeight() == 1.0d ? 64 : 0);
        // ordenados por peso descendente
        r = r + (rangos.get(1).getWeight() == 0.9d && rangos.get(2).getWeight() == 0.5d ? 128 : 0);

        List<Locale> disponibles = new ArrayList<Locale>();
        disponibles.add(Locale.of("es", "AR"));
        disponibles.add(Locale.of("es", "ES"));
        disponibles.add(Locale.US);
        disponibles.add(Locale.JAPAN);
        List<Locale> filtrados = Locale.filter(rangos, disponibles);
        // `es-ar` matchea es-AR; `es` matchea las dos de espanol; `en` matchea en-US
        r = r + (filtrados.size() == 3 ? 256 : 0);
        r = r + (filtrados.get(0).getCountry().equals("AR") ? 512 : 0);
        r = r + (filtrados.contains(Locale.JAPAN) ? 0 : 1024);

        // lookup ACORTA el rango hasta que matchee: pide es-AR, solo hay es-ES, devuelve... nada,
        // porque `es-AR` acortado es `es` y `es` no esta como etiqueta exacta.
        List<Locale> soloEs = new ArrayList<Locale>();
        soloEs.add(Locale.of("es"));
        List<Locale.LanguageRange> pideAr = Locale.LanguageRange.parse("es-AR");
        r = r + (Locale.lookup(pideAr, soloEs).getLanguage().equals("es") ? 2048 : 0);

        List<String> tags = new ArrayList<String>();
        tags.add("es-AR");
        tags.add("en-US");
        r = r + (Locale.filterTags(rangos, tags).size() == 2 ? 4096 : 0);
        r = r + (Locale.lookupTag(pideAr, tags).equals("es-AR") ? 8192 : 0);

        // el comodin matchea todo
        List<Locale.LanguageRange> todo = Locale.LanguageRange.parse("*");
        r = r + (Locale.filter(todo, disponibles).size() == 4 ? 16384 : 0);
        // y un peso cero excluye
        List<Locale.LanguageRange> cero = Locale.LanguageRange.parse("es;q=0");
        r = r + (Locale.filter(cero, disponibles).size() == 0 ? 32768 : 0);

        // el default es mutable
        Locale antes = Locale.getDefault();
        Locale.setDefault(Locale.GERMANY);
        r = r + (Locale.getDefault().equals(Locale.GERMANY) ? 65536 : 0);
        r = r + (Locale.getDefault(Locale.Category.FORMAT).equals(Locale.GERMANY) ? 131072 : 0);
        Locale.setDefault(antes);
        r = r + (Locale.getDefault().equals(antes) ? 262144 : 0);

        // ---- Properties: lo de siempre ------------------------------------------------------------------
        Properties p = new Properties();
        p.setProperty("uno", "1");
        p.setProperty("dos", "2");
        r = r + (p.getProperty("uno").equals("1") ? 1 : 0);
        r = r + (p.size() == 2 ? 2 : 0);
        r = r + (p.getProperty("tres") == null ? 4 : 0);
        r = r + (p.getProperty("tres", "x").equals("x") ? 8 : 0);

        // las operaciones de Map sobre Object
        r = r + (p.get("uno").equals("1") ? 16 : 0);
        r = r + (p.getOrDefault("nada", "d").equals("d") ? 32 : 0);
        p.putIfAbsent("uno", "9");
        r = r + (p.getProperty("uno").equals("1") ? 64 : 0);          // ya estaba
        p.putIfAbsent("tres", "3");
        r = r + (p.getProperty("tres").equals("3") ? 128 : 0);
        r = r + (p.replace("tres", "33").equals("3") ? 256 : 0);
        r = r + (p.replace("tres", "33", "333") ? 512 : 0);
        r = r + (p.getProperty("tres").equals("333") ? 1024 : 0);
        r = r + (p.remove("tres").equals("333") ? 2048 : 0);
        r = r + (p.size() == 2 ? 4096 : 0);

        // ---- round-trip del formato de texto ---------------------------------------------------------------
        Properties conRaros = new Properties();
        conRaros.setProperty("clave con espacios", "valor con = y :");
        conRaros.setProperty("normal", "simple");
        conRaros.setProperty("conTab", "a\tb");
        int leidos = 0;
        try {
            StringWriter w = new StringWriter();
            conRaros.store(w, "una prueba");
            Properties vuelta = new Properties();
            vuelta.load(new java.io.StringReader(w.toString()));
            leidos = vuelta.size();
            r = r + (leidos == 3 ? 1 : 0);
            r = r + (vuelta.getProperty("clave con espacios").equals("valor con = y :") ? 2 : 0);
            r = r + (vuelta.getProperty("conTab").equals("a\tb") ? 4 : 0);
            r = r + (vuelta.getProperty("normal").equals("simple") ? 8 : 0);

            // el mismo round-trip por bytes
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            conRaros.store(bo, null);
            Properties vuelta2 = new Properties();
            vuelta2.load(new ByteArrayInputStream(bo.toByteArray()));
            r = r + (vuelta2.size() == 3 ? 16 : 0);
            r = r + (vuelta2.getProperty("clave con espacios").equals("valor con = y :") ? 32 : 0);

            // y el de XML
            ByteArrayOutputStream bx = new ByteArrayOutputStream();
            conRaros.storeToXML(bx, "un comentario");
            Properties vuelta3 = new Properties();
            vuelta3.loadFromXML(new ByteArrayInputStream(bx.toByteArray()));
            r = r + (vuelta3.size() == 3 ? 64 : 0);
            r = r + (vuelta3.getProperty("clave con espacios").equals("valor con = y :") ? 128 : 0);
            r = r + (vuelta3.getProperty("conTab").equals("a\tb") ? 256 : 0);

            // XML con caracteres que hay que escapar
            Properties conXml = new Properties();
            conXml.setProperty("a<b", "x&y \"z\"");
            ByteArrayOutputStream bx2 = new ByteArrayOutputStream();
            conXml.storeToXML(bx2, null);
            Properties vuelta4 = new Properties();
            vuelta4.loadFromXML(new ByteArrayInputStream(bx2.toByteArray()));
            r = r + (vuelta4.getProperty("a<b").equals("x&y \"z\"") ? 512 : 0);
        } catch (java.io.IOException e) {
            r = r + 100000;
        }

        // ---- defaults encadenados --------------------------------------------------------------------------
        Properties base = new Properties();
        base.setProperty("heredada", "de la base");
        Properties hija = new Properties(base);
        hija.setProperty("propia", "de la hija");
        r = r + (hija.getProperty("heredada").equals("de la base") ? 1024 : 0);
        r = r + (hija.size() == 1 ? 2048 : 0);                        // size NO cuenta las heredadas
        r = r + (hija.stringPropertyNames().size() == 2 ? 4096 : 0);  // pero stringPropertyNames si

        // guardar una tabla guarda LO SUYO, no lo heredado
        try {
            StringWriter w2 = new StringWriter();
            hija.store(w2, null);
            Properties vuelta5 = new Properties();
            vuelta5.load(new java.io.StringReader(w2.toString()));
            r = r + (vuelta5.size() == 1 ? 8192 : 0);
        } catch (java.io.IOException e) {
            r = r + 200000;
        }

        return r;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
