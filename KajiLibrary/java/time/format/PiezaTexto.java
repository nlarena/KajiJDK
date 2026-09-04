package java.time.format;

import java.time.DateTimeException;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalField;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

// Un campo escrito con su nombre y no con digitos: `July`, `Monday`, `PM`.
//
// **LA PARED, Y DONDE SE DECIDIO PONERLA.** Esta biblioteca no trae los datos de texto del CLDR: no
// hay una tabla de nombres de mes por idioma, y no la va a haber inventandola. Lo unico que hay es el
// juego **ingles**, escrito a mano aca abajo, que es dato real y verificable, no relleno.
//
// De ahi sale la regla que gobierna esta pieza: si el locale efectivo es ingles --o `ROOT`, que en
// CLDR *es* el juego del que el ingles deriva-- escribe y lee esos nombres, que es lo correcto. Con
// cualquier otro locale **tira**, diciendo que falta el dato.
//
// Que no tira: un `DateTimeException` que dice "no tengo los nombres en frances" es informacion
// cierta. Escribir `July` bajo `Locale.FRENCH` seria una respuesta con la forma de la correcta y el
// contenido equivocado --exactamente lo que el proyecto no admite--, y encima silenciosa: el que
// llama se enteraria en produccion, no aca. La alternativa de dejar `appendText` afuera del todo
// costaria tambien los patrones `MMM`/`EEEE` en ingles, que **si** funcionan.
//
// Solo esta pieza depende del locale. Un patron sin nombres --`yyyy-MM-dd`, todos los `ISO_*` menos
// `RFC_1123_DATE_TIME`-- da el mismo texto en cualquier locale, y por eso `withLocale` sigue siendo
// una operacion honesta: guarda el locale, lo devuelve igual, y no cambia nada que no deba cambiar.
final class PiezaTexto extends Pieza {

    private final TemporalField campo;
    private final TextStyle estilo;

    PiezaTexto(TemporalField campo, TextStyle estilo) {
        this.campo = campo;
        this.estilo = estilo;
        if (TextoIngles.nombres(campo, estilo) == null) {
            throw new IllegalArgumentException("Field cannot be printed as text: " + campo);
        }
    }

    // ROOT y cualquier variante de ingles. `Locale.ROOT` entra porque es el locale "sin idioma", y
    // los datos raiz de CLDR son justamente estos nombres.
    static boolean hayTexto(Locale locale) {
        if (locale == null) {
            return true;
        }
        String idioma = locale.getLanguage();
        return idioma.length() == 0 || idioma.equals("en");
    }

    private void exigirDatos(Locale locale) {
        if (!PiezaTexto.hayTexto(locale)) {
            throw new DateTimeException("No text data available for locale " + locale
                    + ": this library ships the English/root names only, and has no CLDR text data."
                    + " Use a numeric pattern, or appendText(field, Map) with your own names.");
        }
    }

    boolean imprimir(CtxImprimir ctx, StringBuilder salida) {
        Long v = ctx.valor(this.campo);
        if (v == null) {
            return false;
        }
        this.exigirDatos(ctx.locale);
        String[] nombres = TextoIngles.nombres(this.campo, this.estilo);
        int i = TextoIngles.indice(this.campo, v.longValue());
        if (i < 0 || i >= nombres.length) {
            throw new DateTimeException("Value " + v + " is out of range for field " + this.campo);
        }
        salida.append(nombres[i]);
        return true;
    }

    int parsear(CtxParseo ctx, String texto, int pos) {
        this.exigirDatos(ctx.locale);
        String[] nombres = TextoIngles.nombres(this.campo, this.estilo);
        // Se prueba el **mas largo** primero. Sin eso, `EEEE` leyendo `Saturday` se quedaria con
        // `Sat` --que tambien encaja-- y dejaria `urday` sin consumir.
        int mejor = -1;
        int largoMejor = -1;
        int k = 0;
        while (k < nombres.length) {
            String n = nombres[k];
            if (n.length() > largoMejor
                    && texto.regionMatches(!ctx.sensible, pos, n, 0, n.length())) {
                mejor = k;
                largoMejor = n.length();
            }
            k = k + 1;
        }
        if (mejor < 0) {
            return ~pos;
        }
        ctx.poner(this.campo, TextoIngles.valor(this.campo, mejor));
        return pos + largoMejor;
    }
}

// `appendText(field, Map)`: los nombres los pone el llamador.
//
// Esta pieza **no depende de ningun dato de locale** --el mapa es el dato-- y por eso es la salida
// honesta para quien necesite nombres en otro idioma: `RFC_1123_DATE_TIME` la usa, porque el RFC fija
// nombres en ingles que no son "el ingles del locale" sino parte del formato.
final class PiezaTextoMapa extends Pieza {

    private final TemporalField campo;
    private final Map<Long, String> nombres;

    PiezaTextoMapa(TemporalField campo, Map<Long, String> nombres) {
        this.campo = campo;
        this.nombres = nombres;
    }

    boolean imprimir(CtxImprimir ctx, StringBuilder salida) {
        Long v = ctx.valor(this.campo);
        if (v == null) {
            return false;
        }
        String n = this.nombres.get(v);
        if (n == null) {
            throw new DateTimeException("Value " + v + " has no text in the supplied map for field "
                    + this.campo);
        }
        salida.append(n);
        return true;
    }

    int parsear(CtxParseo ctx, String texto, int pos) {
        Long mejor = null;
        int largoMejor = -1;
        Iterator<Map.Entry<Long, String>> it = this.nombres.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, String> e = it.next();
            String n = e.getValue();
            if (n.length() > largoMejor
                    && texto.regionMatches(!ctx.sensible, pos, n, 0, n.length())) {
                mejor = e.getKey();
                largoMejor = n.length();
            }
        }
        if (mejor == null) {
            return ~pos;
        }
        ctx.poner(this.campo, mejor.longValue());
        return pos + largoMejor;
    }
}

// Los nombres en ingles, que son todo el dato de texto que esta biblioteca tiene.
//
// Estan escritos y no derivados: las formas `NARROW` son las del CLDR ingles --la inicial-- y no una
// invencion. Que sean ambiguas al leer (`J` es enero, junio y julio) es una propiedad del dato, no un
// defecto de esta implementacion; se resuelve por primer encaje, igual que el JDK.
final class TextoIngles {

    static final String[] MESES = {"January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"};
    static final String[] MESES_CORTO = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
    static final String[] MESES_INICIAL = {"J", "F", "M", "A", "M", "J",
        "J", "A", "S", "O", "N", "D"};
    static final String[] DIAS = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday",
        "Saturday", "Sunday"};
    static final String[] DIAS_CORTO = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
    static final String[] DIAS_INICIAL = {"M", "T", "W", "T", "F", "S", "S"};
    static final String[] MERIDIANO = {"AM", "PM"};
    static final String[] MERIDIANO_INICIAL = {"a", "p"};
    static final String[] ERAS = {"Before Christ", "Anno Domini"};
    static final String[] ERAS_CORTO = {"BC", "AD"};
    static final String[] ERAS_INICIAL = {"B", "A"};

    private TextoIngles() {
    }

    // `null` --y no una excepcion-- porque el constructor de `PiezaTexto` lo usa como test: si un
    // campo no tiene nombres, el `append` falla en el sitio donde se escribio, no al formatear.
    static String[] nombres(TemporalField campo, TextStyle estilo) {
        boolean corto = estilo == TextStyle.SHORT || estilo == TextStyle.SHORT_STANDALONE;
        boolean inicial = estilo == TextStyle.NARROW || estilo == TextStyle.NARROW_STANDALONE;
        if (campo == ChronoField.MONTH_OF_YEAR) {
            return inicial ? MESES_INICIAL : (corto ? MESES_CORTO : MESES);
        }
        if (campo == ChronoField.DAY_OF_WEEK) {
            return inicial ? DIAS_INICIAL : (corto ? DIAS_CORTO : DIAS);
        }
        if (campo == ChronoField.AMPM_OF_DAY) {
            return inicial ? MERIDIANO_INICIAL : MERIDIANO;
        }
        if (campo == ChronoField.ERA) {
            return inicial ? ERAS_INICIAL : (corto ? ERAS_CORTO : ERAS);
        }
        return null;
    }

    // De valor de campo a indice del arreglo. Mes y dia de semana empiezan en 1; AM/PM y era en 0.
    static int indice(TemporalField campo, long valor) {
        if (campo == ChronoField.MONTH_OF_YEAR || campo == ChronoField.DAY_OF_WEEK) {
            return (int) valor - 1;
        }
        return (int) valor;
    }

    static long valor(TemporalField campo, int indice) {
        if (campo == ChronoField.MONTH_OF_YEAR || campo == ChronoField.DAY_OF_WEEK) {
            return (long) (indice + 1);
        }
        return (long) indice;
    }
}
