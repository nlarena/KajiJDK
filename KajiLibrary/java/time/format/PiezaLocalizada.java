package java.time.format;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.chrono.Chronology;
import java.time.chrono.IsoChronology;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalQueries;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

// Las tres piezas que dependen del locale y **si** se pueden dar: el patron localizado, el offset
// con la palabra `GMT` adelante, y el nombre del calendario.
//
// **De donde sale el patron, y por que ahora se puede.** El encabezado de `DateTimeFormatterBuilder`
// decia que `appendLocalized` estaba afuera porque el patron de una fecha corta --`M/d/yy` en los
// Estados Unidos, `dd.MM.yy` en Alemania-- es dato del CLDR y no se deduce. Eso sigue siendo cierto,
// pero el dato ya no falta: `java.text.PatronesLocales` lo trae, extraido del JDK 25, y se llega a el
// por `DateFormat.getDateInstance(estilo, locale)`.
//
// Y no es una equivalencia supuesta: se midio contra el JDK real para los cuatro estilos, las tres
// combinaciones (fecha, hora, las dos) y siete locales, y **el patron que devuelve
// `DateTimeFormatterBuilder.getLocalizedDateTimePattern` es exactamente el que devuelve
// `((SimpleDateFormat) DateFormat.getXxxInstance(estilo, locale)).toPattern()`**. Tiene sentido: los
// dos leen la misma fila del CLDR, y las letras de patron que aparecen ahi --`y M d E H h m s a
// z`-- significan lo mismo en `java.text` y en `java.time`.
//
// **Lo que sigue afuera, y por que.** Los nombres de zona (`appendZoneText`), los periodos del dia
// (`appendDayPeriodText`) y las plantillas tipo `yMMMd` (`ofLocalizedPattern`) piden tablas de texto
// del CLDR que esta biblioteca no trae. La distincion es la de siempre: un patron ausente se puede
// buscar en otro lado, un nombre inventado dice "este es el nombre" y no lo es.

// `appendLocalized(FormatStyle, FormatStyle)`: el patron sale del locale **en el momento de usar el
// formateador**, no al armarlo.
//
// Es la razon por la que esto es una pieza y no un `appendPattern` resuelto en el constructor:
// `withLocale` puede cambiar el locale despues, y entonces el patron tiene que cambiar con el. Los
// formateadores compilados se guardan por locale y cronologia, que es lo mismo que hace el JDK.
final class PiezaLocalizada extends Pieza {

    private final FormatStyle estiloFecha;
    private final FormatStyle estiloHora;
    private final Map<String, PiezaCompuesta> compiladas;

    PiezaLocalizada(FormatStyle estiloFecha, FormatStyle estiloHora) {
        this.estiloFecha = estiloFecha;
        this.estiloHora = estiloHora;
        this.compiladas = new HashMap<String, PiezaCompuesta>();
    }

    // El formateador para ese locale y esa cronologia, compilando la primera vez.
    private PiezaCompuesta para(Locale locale, Chronology cronologia) {
        Chronology c = cronologia == null ? IsoChronology.INSTANCE : cronologia;
        String clave = locale.toString() + "|" + c.getId();
        PiezaCompuesta ya = this.compiladas.get(clave);
        if (ya != null) {
            return ya;
        }
        String patron = DateTimeFormatterBuilder.getLocalizedDateTimePattern(
                this.estiloFecha, this.estiloHora, c, locale);
        DateTimeFormatter f = new DateTimeFormatterBuilder().appendPattern(patron)
                .toFormatter(locale);
        PiezaCompuesta armada = f.piezas();
        this.compiladas.put(clave, armada);
        return armada;
    }

    boolean imprimir(CtxImprimir ctx, StringBuilder salida) {
        Chronology c = ctx.consultar(TemporalQueries.chronology());
        return this.para(ctx.locale, c).imprimir(ctx, salida);
    }

    int parsear(CtxParseo ctx, String texto, int pos) {
        return this.para(ctx.locale, ctx.cronologia).parsear(ctx, texto, pos);
    }

    public String toString() {
        return "Localized(" + this.estiloFecha + "," + this.estiloHora + ")";
    }
}

// `appendLocalized(String)`: lo mismo que `PiezaLocalizada` pero pidiendo los campos por plantilla
// en vez de por estilo.
//
// La diferencia con los cuatro estilos es de grano. Un estilo --`MEDIUM`-- dice "una fecha de las
// medianas" y el idioma decide que campos entran; una plantilla dice exactamente que campos se
// quieren y deja que el idioma decida el orden. Es lo que hace falta cuando el programa sabe que
// necesita el mes y el dia pero no quiere el ano, que ningun estilo ofrece.
//
// El patron se resuelve al usar el formateador, no al armarlo, por lo mismo que alla: un
// `withLocale` posterior tiene que cambiar el resultado.
final class PiezaPlantilla extends Pieza {

    private final String plantilla;
    private final Map<String, PiezaCompuesta> compiladas;

    PiezaPlantilla(String plantilla) {
        this.plantilla = plantilla;
        this.compiladas = new HashMap<String, PiezaCompuesta>();
    }

    private PiezaCompuesta para(Locale locale, Chronology cronologia) {
        Chronology c = cronologia == null ? IsoChronology.INSTANCE : cronologia;
        String clave = locale.toString() + "|" + c.getId();
        PiezaCompuesta ya = this.compiladas.get(clave);
        if (ya != null) {
            return ya;
        }
        String patron = DateTimeFormatterBuilder.getLocalizedDateTimePattern(
                this.plantilla, c, locale);
        DateTimeFormatter f = new DateTimeFormatterBuilder().appendPattern(patron)
                .toFormatter(locale);
        PiezaCompuesta armada = f.piezas();
        this.compiladas.put(clave, armada);
        return armada;
    }

    boolean imprimir(CtxImprimir ctx, StringBuilder salida) {
        Chronology c = ctx.consultar(TemporalQueries.chronology());
        return this.para(ctx.locale, c).imprimir(ctx, salida);
    }

    int parsear(CtxParseo ctx, String texto, int pos) {
        return this.para(ctx.locale, ctx.cronologia).parsear(ctx, texto, pos);
    }

    public String toString() {
        return "Localized(" + this.plantilla + ")";
    }
}

// `appendLocalizedOffset`: `GMT`, y detras el desplazamiento.
//
// **La palabra `GMT` esta fija a proposito, y no es una aproximacion de esta biblioteca**: el JDK
// tiene ahi mismo un `// TODO: get localized version of 'GMT'` y escribe la constante. Copiar el
// codigo y no la intencion es lo que hace que las dos salidas coincidan.
//
// La diferencia entre los dos estilos esta solo en las horas: `FULL` siempre escribe dos digitos y
// los minutos, `SHORT` escribe la hora sin relleno y se saltea los minutos cuando son cero. Un
// offset de cero es `GMT` a secas en los dos.
final class PiezaOffsetLocalizado extends Pieza {

    private static final String GMT = "GMT";

    private final TextStyle estilo;

    PiezaOffsetLocalizado(TextStyle estilo) {
        this.estilo = estilo;
    }

    private static void dosDigitos(StringBuilder salida, int valor) {
        salida.append((char) (valor / 10 + '0'));
        salida.append((char) (valor % 10 + '0'));
    }

    boolean imprimir(CtxImprimir ctx, StringBuilder salida) {
        Long segundos = ctx.valor(ChronoField.OFFSET_SECONDS);
        if (segundos == null) {
            return false;
        }
        salida.append(GMT);
        int total = (int) segundos.longValue();
        if (total == 0) {
            return true;
        }
        int horas = Math.abs((total / 3600) % 100);
        int minutos = Math.abs((total / 60) % 60);
        int segs = Math.abs(total % 60);
        salida.append(total < 0 ? '-' : '+');
        if (this.estilo == TextStyle.FULL) {
            dosDigitos(salida, horas);
            salida.append(':');
            dosDigitos(salida, minutos);
            if (segs != 0) {
                salida.append(':');
                dosDigitos(salida, segs);
            }
        } else {
            if (horas >= 10) {
                salida.append((char) (horas / 10 + '0'));
            }
            salida.append((char) (horas % 10 + '0'));
            if (minutos != 0 || segs != 0) {
                salida.append(':');
                dosDigitos(salida, minutos);
                if (segs != 0) {
                    salida.append(':');
                    dosDigitos(salida, segs);
                }
            }
        }
        return true;
    }

    // Lee `GMT` y, si lo que sigue es un signo, el desplazamiento. `GMT` a secas vale cero, que es
    // lo que corresponde: sin signo no hay desplazamiento escrito.
    int parsear(CtxParseo ctx, String texto, int pos) {
        int p = pos;
        if (!texto.regionMatches(!ctx.sensible, p, GMT, 0, GMT.length())) {
            return ~p;
        }
        p = p + GMT.length();
        char signo = p < texto.length() ? texto.charAt(p) : ' ';
        if (signo != '+' && signo != '-') {
            ctx.poner(ChronoField.OFFSET_SECONDS, 0L);
            return p;
        }
        int negativo = signo == '-' ? -1 : 1;
        p = p + 1;
        // Las horas van sin dos puntos delante y pueden ser uno o dos digitos; los minutos y los
        // segundos vienen cada uno detras de sus dos puntos, y son opcionales de afuera para
        // adentro: no hay segundos sin minutos.
        int d1 = digitoDe(texto, p);
        if (d1 < 0) {
            return ~p;
        }
        p = p + 1;
        int horas = d1;
        int d2 = digitoDe(texto, p);
        if (d2 >= 0) {
            horas = horas * 10 + d2;
            p = p + 1;
        }
        int minutos = 0;
        int segs = 0;
        if (p + 2 < texto.length() && texto.charAt(p) == ':') {
            int m1 = digitoDe(texto, p + 1);
            int m2 = digitoDe(texto, p + 2);
            if (m1 >= 0 && m2 >= 0) {
                minutos = m1 * 10 + m2;
                p = p + 3;
                if (p + 2 < texto.length() && texto.charAt(p) == ':') {
                    int s1 = digitoDe(texto, p + 1);
                    int s2 = digitoDe(texto, p + 2);
                    if (s1 >= 0 && s2 >= 0) {
                        segs = s1 * 10 + s2;
                        p = p + 3;
                    }
                }
            }
        }
        long total = negativo * (horas * 3600L + minutos * 60L + segs);
        ctx.poner(ChronoField.OFFSET_SECONDS, total);
        return p;
    }

    private static int digitoDe(String texto, int p) {
        if (p >= texto.length()) {
            return -1;
        }
        char c = texto.charAt(p);
        if (c < '0' || c > '9') {
            return -1;
        }
        return c - '0';
    }

    public String toString() {
        return "LocalizedOffset(" + this.estilo + ")";
    }
}

// `appendChronologyText`: el nombre del calendario.
//
// Delega en `Chronology.getDisplayName(TextStyle, Locale)`, que es lo que hace el JDK. Esa
// biblioteca no trae los nombres traducidos y ese metodo cae siempre en su rama de reserva --el id
// del calendario--; para el ISO coincide con el JDK, para los demas se queda corto de una palabra.
// Esta dicho alla y no se repite aca como si fuera nuevo.
final class PiezaCronologiaTexto extends Pieza {

    private final TextStyle estilo;

    PiezaCronologiaTexto(TextStyle estilo) {
        this.estilo = estilo;
    }

    boolean imprimir(CtxImprimir ctx, StringBuilder salida) {
        Chronology c = ctx.consultar(TemporalQueries.chronology());
        if (c == null) {
            return ctx.faltaOTira("Chronology");
        }
        salida.append(c.getDisplayName(this.estilo, ctx.locale));
        return true;
    }

    // Gana el nombre mas largo que encaje, como en `appendChronologyId`: sin eso, un calendario cuyo
    // nombre es prefijo de otro se llevaria la lectura.
    int parsear(CtxParseo ctx, String texto, int pos) {
        Chronology mejor = null;
        int largoMejor = 0;
        Iterator<Chronology> it = Chronology.getAvailableChronologies().iterator();
        while (it.hasNext()) {
            Chronology c = it.next();
            String nombre = c.getDisplayName(this.estilo, ctx.locale);
            if (nombre.length() > largoMejor
                    && texto.regionMatches(!ctx.sensible, pos, nombre, 0, nombre.length())) {
                mejor = c;
                largoMejor = nombre.length();
            }
        }
        if (mejor == null) {
            return ~pos;
        }
        ctx.cronologia = mejor;
        return pos + largoMejor;
    }

    public String toString() {
        return "ChronologyText(" + this.estilo + ")";
    }
}

// La consulta de patron localizado, aparte de la clase publica para que su javadoc no tenga que
// contar de donde sale.
//
// Los cuatro estilos de `FormatStyle` estan en el mismo orden que las cuatro constantes de
// `DateFormat` --FULL, LONG, MEDIUM, SHORT-- asi que el ordinal alcanza. Se escribe la tabla igual,
// porque depender de que dos enumeraciones ajenas queden alineadas es la clase de supuesto que se
// rompe callado.
final class PatronLocalizado {

    private static final int[] ESTILOS = {
        DateFormat.FULL,
        DateFormat.LONG,
        DateFormat.MEDIUM,
        DateFormat.SHORT,
    };

    private PatronLocalizado() {
    }

    static String de(FormatStyle estiloFecha, FormatStyle estiloHora, Locale locale) {
        DateFormat df;
        if (estiloFecha != null && estiloHora != null) {
            df = DateFormat.getDateTimeInstance(ESTILOS[estiloFecha.ordinal()],
                    ESTILOS[estiloHora.ordinal()], locale);
        } else if (estiloFecha != null) {
            df = DateFormat.getDateInstance(ESTILOS[estiloFecha.ordinal()], locale);
        } else {
            df = DateFormat.getTimeInstance(ESTILOS[estiloHora.ordinal()], locale);
        }
        return ((SimpleDateFormat) df).toPattern();
    }
}
