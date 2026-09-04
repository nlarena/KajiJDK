import java.text.ListFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Las cuatro fabricas localizadas de `java.text` que faltaban: `ListFormat.getInstance` y
 * `NumberFormat.getCompactNumberInstance`, en sus dos formas cada una.
 *
 * <p>Las expectativas <b>no estan transcriptas a mano</b>: son la salida literal del JDK 25 para
 * los mismos pedidos, tomada de `TextLocaleDump` y pegada aca por un generador. Es la unica forma
 * honesta de fijar texto traducido --una coma de mas en el locale equivocado no la ve nadie hasta
 * que la ve un usuario--, y es lo que hace que esta prueba de -1 en las dos VMs con las mismas
 * cadenas.
 *
 * <p>Lo que no se compara es <b>cuantos</b> locales hay: el JDK tiene mas de mil y esta biblioteca
 * seis, los mismos que `DecimalFormatSymbols`. Eso es el subconjunto documentado, no una falla.
 *
 * <p>La expectativa que mas vale de todas es la del aleman: `1000` en estilo corto da `1.000`, no
 * `1`. Un patron compacto sin sufijo significa "esta magnitud no se compacta", y confundirlo con
 * una forma compacta da el numero equivocado por tres ordenes de magnitud.
 */
public class TextLocaleTest {

    static int failures = 0;

    static void ok(String what, boolean cond) {
        if (!cond) {
            System.out.println("FALLA " + what);
            failures = failures + 1;
        }
    }

    static Locale loc(String tag) {
        return "und".equals(tag) ? Locale.ROOT : Locale.forLanguageTag(tag);
    }

    static List<String> lista(int n) {
        List<String> l = new ArrayList<String>();
        for (int i = 0; i < n; i = i + 1) {
            l.add("x" + Integer.toString(i));
        }
        return l;
    }

    // {locale, tipo, estilo, cuantos, esperado}
    static String[][] listas() {
        return new String[][] {
            {"und", "STANDARD", "FULL", "2", "x0, x1"},
            {"und", "STANDARD", "FULL", "3", "x0, x1, x2"},
            {"und", "STANDARD", "FULL", "4", "x0, x1, x2, x3"},
            {"und", "STANDARD", "SHORT", "3", "x0, x1, x2"},
            {"und", "STANDARD", "NARROW", "3", "x0, x1, x2"},
            {"und", "OR", "FULL", "3", "x0, x1, or x2"},
            {"und", "OR", "SHORT", "3", "x0, x1, or x2"},
            {"und", "OR", "NARROW", "3", "x0, x1, or x2"},
            {"und", "UNIT", "FULL", "3", "x0, x1, x2"},
            {"und", "UNIT", "SHORT", "3", "x0, x1, x2"},
            {"und", "UNIT", "NARROW", "3", "x0, x1, x2"},
            {"en-US", "STANDARD", "FULL", "2", "x0 and x1"},
            {"en-US", "STANDARD", "FULL", "3", "x0, x1, and x2"},
            {"en-US", "STANDARD", "FULL", "4", "x0, x1, x2, and x3"},
            {"en-US", "STANDARD", "SHORT", "3", "x0, x1, & x2"},
            {"en-US", "STANDARD", "NARROW", "3", "x0, x1, x2"},
            {"en-US", "OR", "FULL", "3", "x0, x1, or x2"},
            {"en-US", "OR", "SHORT", "3", "x0, x1, or x2"},
            {"en-US", "OR", "NARROW", "3", "x0, x1, or x2"},
            {"en-US", "UNIT", "FULL", "3", "x0, x1, x2"},
            {"en-US", "UNIT", "SHORT", "3", "x0, x1, x2"},
            {"en-US", "UNIT", "NARROW", "3", "x0 x1 x2"},
            {"es-AR", "STANDARD", "FULL", "2", "x0 y x1"},
            {"es-AR", "STANDARD", "FULL", "3", "x0, x1 y x2"},
            {"es-AR", "STANDARD", "FULL", "4", "x0, x1, x2 y x3"},
            {"es-AR", "STANDARD", "SHORT", "3", "x0, x1 y x2"},
            {"es-AR", "STANDARD", "NARROW", "3", "x0, x1 y x2"},
            {"es-AR", "OR", "FULL", "3", "x0, x1 o x2"},
            {"es-AR", "OR", "SHORT", "3", "x0, x1 o x2"},
            {"es-AR", "OR", "NARROW", "3", "x0, x1 o x2"},
            {"es-AR", "UNIT", "FULL", "3", "x0, x1 y x2"},
            {"es-AR", "UNIT", "SHORT", "3", "x0, x1, x2"},
            {"es-AR", "UNIT", "NARROW", "3", "x0 x1 x2"},
            {"de-DE", "STANDARD", "FULL", "2", "x0 und x1"},
            {"de-DE", "STANDARD", "FULL", "3", "x0, x1 und x2"},
            {"de-DE", "STANDARD", "FULL", "4", "x0, x1, x2 und x3"},
            {"de-DE", "STANDARD", "SHORT", "3", "x0, x1 und x2"},
            {"de-DE", "STANDARD", "NARROW", "3", "x0, x1 und x2"},
            {"de-DE", "OR", "FULL", "3", "x0, x1 oder x2"},
            {"de-DE", "OR", "SHORT", "3", "x0, x1 oder x2"},
            {"de-DE", "OR", "NARROW", "3", "x0, x1 oder x2"},
            {"de-DE", "UNIT", "FULL", "3", "x0, x1 und x2"},
            {"de-DE", "UNIT", "SHORT", "3", "x0, x1 und x2"},
            {"de-DE", "UNIT", "NARROW", "3", "x0, x1 und x2"},
            {"fr-FR", "STANDARD", "FULL", "2", "x0 et x1"},
            {"fr-FR", "STANDARD", "FULL", "3", "x0, x1 et x2"},
            {"fr-FR", "STANDARD", "FULL", "4", "x0, x1, x2 et x3"},
            {"fr-FR", "STANDARD", "SHORT", "3", "x0, x1 et x2"},
            {"fr-FR", "STANDARD", "NARROW", "3", "x0, x1, x2"},
            {"fr-FR", "OR", "FULL", "3", "x0, x1 ou x2"},
            {"fr-FR", "OR", "SHORT", "3", "x0, x1 ou x2"},
            {"fr-FR", "OR", "NARROW", "3", "x0, x1 ou x2"},
            {"fr-FR", "UNIT", "FULL", "3", "x0, x1 et x2"},
            {"fr-FR", "UNIT", "SHORT", "3", "x0, x1 et x2"},
            {"fr-FR", "UNIT", "NARROW", "3", "x0 x1 x2"},
            {"ja-JP", "STANDARD", "FULL", "2", "x0\u3001x1"},
            {"ja-JP", "STANDARD", "FULL", "3", "x0\u3001x1\u3001x2"},
            {"ja-JP", "STANDARD", "FULL", "4", "x0\u3001x1\u3001x2\u3001x3"},
            {"ja-JP", "STANDARD", "SHORT", "3", "x0\u3001x1\u3001x2"},
            {"ja-JP", "STANDARD", "NARROW", "3", "x0\u3001x1\u3001x2"},
            {"ja-JP", "OR", "FULL", "3", "x0\u3001x1\u3001\u307e\u305f\u306fx2"},
            {"ja-JP", "OR", "SHORT", "3", "x0\u3001x1\u3001\u307e\u305f\u306fx2"},
            {"ja-JP", "OR", "NARROW", "3", "x0\u3001x1\u3001\u307e\u305f\u306fx2"},
            {"ja-JP", "UNIT", "FULL", "3", "x0\u3001x1\u3001x2"},
            {"ja-JP", "UNIT", "SHORT", "3", "x0 x1 x2"},
            {"ja-JP", "UNIT", "NARROW", "3", "x0x1x2"},
        };
    }

    // {locale, estilo, numero, esperado}
    static String[][] compactos() {
        return new String[][] {
            {"und", "SHORT", "1", "1"},
            {"und", "SHORT", "999", "999"},
            {"und", "SHORT", "1000", "1K"},
            {"und", "SHORT", "1500", "2K"},
            {"und", "SHORT", "12000", "12K"},
            {"und", "SHORT", "999999", "1M"},
            {"und", "SHORT", "1000000", "1M"},
            {"und", "SHORT", "2500000", "2M"},
            {"und", "SHORT", "1000000000", "1G"},
            {"und", "SHORT", "1500000000000", "2T"},
            {"und", "LONG", "1", "1"},
            {"und", "LONG", "999", "999"},
            {"und", "LONG", "1000", "1K"},
            {"und", "LONG", "1500", "2K"},
            {"und", "LONG", "12000", "12K"},
            {"und", "LONG", "999999", "1M"},
            {"und", "LONG", "1000000", "1M"},
            {"und", "LONG", "2500000", "2M"},
            {"und", "LONG", "1000000000", "1G"},
            {"und", "LONG", "1500000000000", "2T"},
            {"en-US", "SHORT", "1", "1"},
            {"en-US", "SHORT", "999", "999"},
            {"en-US", "SHORT", "1000", "1K"},
            {"en-US", "SHORT", "1500", "2K"},
            {"en-US", "SHORT", "12000", "12K"},
            {"en-US", "SHORT", "999999", "1M"},
            {"en-US", "SHORT", "1000000", "1M"},
            {"en-US", "SHORT", "2500000", "2M"},
            {"en-US", "SHORT", "1000000000", "1B"},
            {"en-US", "SHORT", "1500000000000", "2T"},
            {"en-US", "LONG", "1", "1"},
            {"en-US", "LONG", "999", "999"},
            {"en-US", "LONG", "1000", "1 thousand"},
            {"en-US", "LONG", "1500", "2 thousand"},
            {"en-US", "LONG", "12000", "12 thousand"},
            {"en-US", "LONG", "999999", "1 million"},
            {"en-US", "LONG", "1000000", "1 million"},
            {"en-US", "LONG", "2500000", "2 million"},
            {"en-US", "LONG", "1000000000", "1 billion"},
            {"en-US", "LONG", "1500000000000", "2 trillion"},
            {"es-AR", "SHORT", "1", "1"},
            {"es-AR", "SHORT", "999", "999"},
            {"es-AR", "SHORT", "1000", "1\u00a0K"},
            {"es-AR", "SHORT", "1500", "2\u00a0K"},
            {"es-AR", "SHORT", "12000", "12\u00a0k"},
            {"es-AR", "SHORT", "999999", "1\u00a0M"},
            {"es-AR", "SHORT", "1000000", "1\u00a0M"},
            {"es-AR", "SHORT", "2500000", "2\u00a0M"},
            {"es-AR", "SHORT", "1000000000", "1000\u00a0M"},
            {"es-AR", "SHORT", "1500000000000", "2\u00a0B"},
            {"es-AR", "LONG", "1", "1"},
            {"es-AR", "LONG", "999", "999"},
            {"es-AR", "LONG", "1000", "1\u00a0K"},
            {"es-AR", "LONG", "1500", "2\u00a0K"},
            {"es-AR", "LONG", "12000", "12\u00a0k"},
            {"es-AR", "LONG", "999999", "1\u00a0M"},
            {"es-AR", "LONG", "1000000", "1\u00a0M"},
            {"es-AR", "LONG", "2500000", "2\u00a0M"},
            {"es-AR", "LONG", "1000000000", "1000\u00a0M"},
            {"es-AR", "LONG", "1500000000000", "2\u00a0B"},
            {"de-DE", "SHORT", "1", "1"},
            {"de-DE", "SHORT", "999", "999"},
            {"de-DE", "SHORT", "1000", "1.000"},
            {"de-DE", "SHORT", "1500", "1.500"},
            {"de-DE", "SHORT", "12000", "12.000"},
            {"de-DE", "SHORT", "999999", "999.999"},
            {"de-DE", "SHORT", "1000000", "1\u00a0Mio."},
            {"de-DE", "SHORT", "2500000", "2\u00a0Mio."},
            {"de-DE", "SHORT", "1000000000", "1\u00a0Mrd."},
            {"de-DE", "SHORT", "1500000000000", "2\u00a0Bio."},
            {"de-DE", "LONG", "1", "1"},
            {"de-DE", "LONG", "999", "999"},
            {"de-DE", "LONG", "1000", "1 Tausend"},
            {"de-DE", "LONG", "1500", "2 Tausend"},
            {"de-DE", "LONG", "12000", "12 Tausend"},
            {"de-DE", "LONG", "999999", "1 Million"},
            {"de-DE", "LONG", "1000000", "1 Million"},
            {"de-DE", "LONG", "2500000", "2 Millionen"},
            {"de-DE", "LONG", "1000000000", "1 Milliarde"},
            {"de-DE", "LONG", "1500000000000", "2 Billionen"},
            {"fr-FR", "SHORT", "1", "1"},
            {"fr-FR", "SHORT", "999", "999"},
            {"fr-FR", "SHORT", "1000", "1\u00a0k"},
            {"fr-FR", "SHORT", "1500", "2\u00a0k"},
            {"fr-FR", "SHORT", "12000", "12\u00a0k"},
            {"fr-FR", "SHORT", "999999", "1\u00a0M"},
            {"fr-FR", "SHORT", "1000000", "1\u00a0M"},
            {"fr-FR", "SHORT", "2500000", "2\u00a0M"},
            {"fr-FR", "SHORT", "1000000000", "1\u00a0Md"},
            {"fr-FR", "SHORT", "1500000000000", "2\u00a0Bn"},
            {"fr-FR", "LONG", "1", "1"},
            {"fr-FR", "LONG", "999", "999"},
            {"fr-FR", "LONG", "1000", "1 millier"},
            {"fr-FR", "LONG", "1500", "2 mille"},
            {"fr-FR", "LONG", "12000", "12 mille"},
            {"fr-FR", "LONG", "999999", "1 million"},
            {"fr-FR", "LONG", "1000000", "1 million"},
            {"fr-FR", "LONG", "2500000", "2 millions"},
            {"fr-FR", "LONG", "1000000000", "1 milliard"},
            {"fr-FR", "LONG", "1500000000000", "2 billions"},
            {"ja-JP", "SHORT", "1", "1"},
            {"ja-JP", "SHORT", "999", "999"},
            {"ja-JP", "SHORT", "1000", "1,000"},
            {"ja-JP", "SHORT", "1500", "1,500"},
            {"ja-JP", "SHORT", "12000", "1\u4e07"},
            {"ja-JP", "SHORT", "999999", "100\u4e07"},
            {"ja-JP", "SHORT", "1000000", "100\u4e07"},
            {"ja-JP", "SHORT", "2500000", "250\u4e07"},
            {"ja-JP", "SHORT", "1000000000", "10\u5104"},
            {"ja-JP", "SHORT", "1500000000000", "2\u5146"},
            {"ja-JP", "LONG", "1", "1"},
            {"ja-JP", "LONG", "999", "999"},
            {"ja-JP", "LONG", "1000", "1,000"},
            {"ja-JP", "LONG", "1500", "1,500"},
            {"ja-JP", "LONG", "12000", "1\u4e07"},
            {"ja-JP", "LONG", "999999", "100\u4e07"},
            {"ja-JP", "LONG", "1000000", "100\u4e07"},
            {"ja-JP", "LONG", "2500000", "250\u4e07"},
            {"ja-JP", "LONG", "1000000000", "10\u5104"},
            {"ja-JP", "LONG", "1500000000000", "2\u5146"},
        };
    }

    public static int run() throws Exception {
        failures = 0;

        for (String[] f : listas()) {
            ListFormat lf = ListFormat.getInstance(loc(f[0]), ListFormat.Type.valueOf(f[1]),
                                                   ListFormat.Style.valueOf(f[2]));
            String got = lf.format(lista(Integer.parseInt(f[3])));
            ok("lista " + f[0] + " " + f[1] + " " + f[2] + " " + f[3] + ": esperaba [" + f[4]
               + "] y dio [" + got + "]", f[4].equals(got));
        }

        for (String[] f : compactos()) {
            NumberFormat nf = NumberFormat.getCompactNumberInstance(loc(f[0]),
                                                                    NumberFormat.Style.valueOf(f[1]));
            String got = nf.format(Long.parseLong(f[2]));
            ok("compacto " + f[0] + " " + f[1] + " " + f[2] + ": esperaba [" + f[3] + "] y dio ["
               + got + "]", f[3].equals(got));
        }

        // Las formas sin argumentos son las del locale por omision, que aca no se compara con el
        // JDK porque depende de la maquina; lo que se fija es que sean coherentes con la explicita.
        // La categoria es FORMAT: en esta misma maquina el locale de presentacion y el de formato
        // son distintos, y la primera version de esta prueba lo descubrio fallando contra el JDK.
        Locale formato = Locale.getDefault(Locale.Category.FORMAT);
        ok("ListFormat.getInstance() es la del locale por omision",
                ListFormat.getInstance().format(lista(3)).equals(
                        ListFormat.getInstance(formato, ListFormat.Type.STANDARD,
                                               ListFormat.Style.FULL).format(lista(3))));
        ok("getCompactNumberInstance() es la corta del locale por omision",
                NumberFormat.getCompactNumberInstance().format(1500000L).equals(
                        NumberFormat.getCompactNumberInstance(formato, NumberFormat.Style.SHORT)
                                .format(1500000L)));

        ok("hay locales con datos", ListFormat.getAvailableLocales().length > 0);
        ok("un locale desconocido cae en ROOT",
                ListFormat.getInstance(Locale.forLanguageTag("xx-YY"), ListFormat.Type.STANDARD,
                                       ListFormat.Style.FULL).format(lista(2))
                        .equals(ListFormat.getInstance(Locale.ROOT, ListFormat.Type.STANDARD,
                                                       ListFormat.Style.FULL).format(lista(2))));

        boolean tiroNulo = false;
        try {
            ListFormat.getInstance(null, ListFormat.Type.STANDARD, ListFormat.Style.FULL);
        } catch (NullPointerException e) {
            tiroNulo = true;
        }
        ok("un locale null tira", tiroNulo);

        if (failures == 0) {
            return -1;
        }
        return failures;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("TextLocaleTest " + TextLocaleTest.run());
    }
}
