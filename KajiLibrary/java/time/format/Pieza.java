package java.time.format;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.chrono.Chronology;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQuery;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

// El motor de `java.time.format`: una pieza sabe **escribir** un pedazo de texto y **leerlo**.
//
// Por que existe esta clase, y no una cadena de patron. La version anterior de `DateTimeFormatter`
// guardaba el patron literal (`"yyyy-MM-dd"`) y lo interpretaba en cada llamada. Eso alcanzaba para
// `ofPattern`, pero no para el resto del paquete: una **seccion opcional** --el `.SSS` de
// `ISO_LOCAL_TIME`, que sale solo si hay nanos-- no se puede escribir en una cadena de patron, y sin
// secciones opcionales no hay ni un solo `ISO_*` de verdad. Tampoco entra el relleno de `padNext`, ni
// un `appendText` con un mapa de nombres que el llamador elige.
//
// Asi que el patron paso a ser **un compilador a piezas** (`DateTimeFormatterBuilder.appendPattern`)
// en vez de la representacion. `ofPattern("yyyy-MM-dd")` da exactamente la misma lista de piezas que
// escribir los tres `appendValue` a mano, y por eso las dos mitades del paquete no se pueden
// desincronizar.
//
// **El contrato de los dos metodos, que es donde esta toda la sutileza:**
//
//   - `imprimir` devuelve `false` cuando el campo que necesita no esta y **estamos dentro de una
//     seccion opcional**. La seccion entera se descarta. Fuera de una seccion opcional el campo que
//     falta es un error y `ctx.valor` tira: un formateador que se come un campo ausente escribiria
//     una fecha incompleta sin avisar.
//
//   - `parsear` devuelve el indice siguiente al texto consumido, o **el complemento a uno del indice
//     del error** (`~i`) si no encaja. El complemento y no `-1` porque hace falta saber *donde*
//     fallo: `DateTimeParseException.getErrorIndex()` lo publica, y una seccion opcional que
//     retrocede necesita distinguir "no habia nada" de "habia algo mal". Como `~i` es negativo para
//     todo `i >= 0`, el chequeo sigue siendo `if (p < 0)`.
abstract class Pieza {

    abstract boolean imprimir(CtxImprimir ctx, StringBuilder salida);

    abstract int parsear(CtxParseo ctx, String texto, int pos);

    // El valor del digito que hay en `c`, o -1 si no es un digito. Va por `DecimalStyle` y no por
    // `'0'` a secas porque el estilo puede correr el cero a otro punto de Unicode.
    static int digito(CtxParseo ctx, char c) {
        int d = c - ctx.simbolos.getZeroDigit();
        if (d < 0 || d > 9) {
            return -1;
        }
        return d;
    }

    // Los digitos de `valor` (que ya viene sin signo) con el cero que pida el estilo.
    static void escribirDigitos(CtxImprimir ctx, StringBuilder salida, String digitos, int minimo) {
        char cero = ctx.simbolos.getZeroDigit();
        int faltan = minimo - digitos.length();
        while (faltan > 0) {
            salida.append(cero);
            faltan = faltan - 1;
        }
        int i = 0;
        while (i < digitos.length()) {
            salida.append((char) (cero + (digitos.charAt(i) - '0')));
            i = i + 1;
        }
    }
}

// Lo que una pieza necesita saber para escribir: de donde saca los valores, con que simbolos, y si
// esta o no dentro de una seccion opcional.
final class CtxImprimir {

    private final TemporalAccessor temporal;
    final Locale locale;
    final DecimalStyle simbolos;
    private int opcional;

    CtxImprimir(TemporalAccessor temporal, Locale locale, DecimalStyle simbolos) {
        this.temporal = temporal;
        this.locale = locale;
        this.simbolos = simbolos;
        this.opcional = 0;
    }

    TemporalAccessor temporal() {
        return this.temporal;
    }

    void entrarOpcional() {
        this.opcional = this.opcional + 1;
    }

    void salirOpcional() {
        this.opcional = this.opcional - 1;
    }

    boolean enOpcional() {
        return this.opcional > 0;
    }

    // Lo que hace `valor`, para las piezas que no sacan su dato de un campo sino de una consulta --la
    // zona, el calendario--: adentro de una seccion opcional la ausencia se tolera; afuera es un
    // error, porque el texto que saldria seria una fecha a la que le falta un pedazo.
    boolean faltaOTira(String que) {
        if (this.opcional > 0) {
            return false;
        }
        throw new DateTimeException("Unable to extract " + que + " from temporal " + this.temporal);
    }

    // `null` significa "el campo no esta y estamos en una seccion opcional, descartala". Afuera de
    // una seccion opcional no hay `null` posible: es un error y se tira.
    Long valor(TemporalField campo) {
        if (this.temporal.isSupported(campo)) {
            return Long.valueOf(this.temporal.getLong(campo));
        }
        if (this.opcional > 0) {
            return null;
        }
        throw new DateTimeException("Unsupported field: " + campo);
    }

    <R> R consultar(TemporalQuery<R> consulta) {
        return this.temporal.query(consulta);
    }
}

// Lo que se junta mientras se lee. Es mutable a proposito: las piezas van depositando campos aca, y
// las secciones opcionales lo guardan y lo restauran cuando retroceden.
final class CtxParseo {

    Map<TemporalField, Long> campos;
    ZoneId zona;
    ZoneOffset offset;
    Chronology cronologia;
    boolean sensible;
    boolean estricto;
    final Locale locale;
    final DecimalStyle simbolos;
    // Los dos casos que el texto puede traer y **ninguna hora puede representar**: `24:00`, que es la
    // medianoche del dia siguiente, y `:60`, el segundo intercalar. Se marcan aca en vez de forzarlos
    // dentro de un `LocalTime` --que no los admite-- y se publican por `parsedExcessDays` y
    // `parsedLeapSecond`, que es justo para lo que el JDK los tiene.
    boolean excesoDia;
    boolean segundoBisiesto;

    CtxParseo(Locale locale, DecimalStyle simbolos, boolean estricto, Chronology cronologia,
            ZoneId zona) {
        this.campos = new HashMap<TemporalField, Long>();
        this.sensible = true;
        this.estricto = estricto;
        this.locale = locale;
        this.simbolos = simbolos;
        this.cronologia = cronologia;
        this.zona = zona;
        this.excesoDia = false;
        this.segundoBisiesto = false;
    }

    void poner(TemporalField campo, long valor) {
        this.campos.put(campo, Long.valueOf(valor));
    }

    EstadoParseo guardar() {
        EstadoParseo e = new EstadoParseo();
        e.campos = new HashMap<TemporalField, Long>(this.campos);
        e.zona = this.zona;
        e.offset = this.offset;
        e.cronologia = this.cronologia;
        e.sensible = this.sensible;
        e.estricto = this.estricto;
        e.excesoDia = this.excesoDia;
        e.segundoBisiesto = this.segundoBisiesto;
        return e;
    }

    void restaurar(EstadoParseo e) {
        this.campos = e.campos;
        this.zona = e.zona;
        this.offset = e.offset;
        this.cronologia = e.cronologia;
        this.sensible = e.sensible;
        this.estricto = e.estricto;
        this.excesoDia = e.excesoDia;
        this.segundoBisiesto = e.segundoBisiesto;
    }
}

// La foto de `CtxParseo` que una seccion opcional saca antes de intentar.
final class EstadoParseo {

    Map<TemporalField, Long> campos;
    ZoneId zona;
    ZoneOffset offset;
    Chronology cronologia;
    boolean sensible;
    boolean estricto;
    boolean excesoDia;
    boolean segundoBisiesto;
}

// Texto fijo: el `-` de `2024-02-29`, la `T` de `2024-02-29T10:15`.
final class PiezaLiteral extends Pieza {

    private final String texto;

    PiezaLiteral(String texto) {
        this.texto = texto;
    }

    boolean imprimir(CtxImprimir ctx, StringBuilder salida) {
        salida.append(this.texto);
        return true;
    }

    int parsear(CtxParseo ctx, String texto, int pos) {
        int largo = this.texto.length();
        if (pos + largo > texto.length()) {
            return ~pos;
        }
        if (!texto.regionMatches(!ctx.sensible, pos, this.texto, 0, largo)) {
            return ~pos;
        }
        return pos + largo;
    }

    public String toString() {
        return "'" + this.texto + "'";
    }
}

// Una lista de piezas, y --si es opcional-- la que se descarta entera cuando algo falta.
//
// **Todo o nada**, en los dos sentidos, y es lo que hace que `ISO_LOCAL_TIME` funcione: `:30.5` se
// escribe si hay segundos, y si no hay no se escribe ni el `:`; y al leer, un texto que trae `:30`
// pero se corta antes de la fraccion no deja el `.` a medio consumir, retrocede al punto donde
// empezo la seccion.
final class PiezaCompuesta extends Pieza {

    private final Pieza[] piezas;
    private final boolean opcional;

    PiezaCompuesta(Pieza[] piezas, boolean opcional) {
        this.piezas = piezas;
        this.opcional = opcional;
    }

    Pieza[] piezas() {
        return this.piezas;
    }

    boolean esOpcional() {
        return this.opcional;
    }

    boolean imprimir(CtxImprimir ctx, StringBuilder salida) {
        int marca = salida.length();
        if (this.opcional) {
            ctx.entrarOpcional();
        }
        boolean entero = true;
        int i = 0;
        while (i < this.piezas.length) {
            if (!this.piezas[i].imprimir(ctx, salida)) {
                entero = false;
                i = this.piezas.length;
            } else {
                i = i + 1;
            }
        }
        if (this.opcional) {
            ctx.salirOpcional();
        }
        if (!entero) {
            salida.setLength(marca);
            // Si esta seccion era opcional el hueco es legitimo y se sigue; si no lo era, el hueco
            // sube y lo maneja la seccion opcional de mas afuera --o, si no hay ninguna, `valor` ya
            // habria tirado antes de llegar aca--.
            return this.opcional;
        }
        return true;
    }

    int parsear(CtxParseo ctx, String texto, int pos) {
        if (!this.opcional) {
            int p = pos;
            int i = 0;
            while (i < this.piezas.length) {
                p = this.piezas[i].parsear(ctx, texto, p);
                if (p < 0) {
                    return p;
                }
                i = i + 1;
            }
            return p;
        }
        EstadoParseo guardado = ctx.guardar();
        int p = pos;
        int i = 0;
        while (i < this.piezas.length) {
            p = this.piezas[i].parsear(ctx, texto, p);
            if (p < 0) {
                // Retroceso completo: los campos que la seccion alcanzo a depositar tambien se van.
                // Sin esto un `ISO_DATE_TIME` que lee `2024-01-01T00:00+05:00[Bad/Zone]` se quedaria
                // con el offset de una seccion que no encajo.
                ctx.restaurar(guardado);
                return pos;
            }
            i = i + 1;
        }
        return p;
    }
}

// `padNext`: rellena a la izquierda lo que escriba la pieza de adentro hasta `ancho`.
//
// Es la unica pieza que **no puede ser una cadena de patron**: el relleno depende del largo de lo que
// salio, que no se sabe hasta que salio.
final class PiezaRelleno extends Pieza {

    private final Pieza pieza;
    private final int ancho;
    private final char relleno;

    PiezaRelleno(Pieza pieza, int ancho, char relleno) {
        this.pieza = pieza;
        this.ancho = ancho;
        this.relleno = relleno;
    }

    boolean imprimir(CtxImprimir ctx, StringBuilder salida) {
        int marca = salida.length();
        if (!this.pieza.imprimir(ctx, salida)) {
            return false;
        }
        int escrito = salida.length() - marca;
        if (escrito > this.ancho) {
            throw new DateTimeException("Cannot print as output of " + escrito
                    + " characters exceeds pad width of " + this.ancho);
        }
        int faltan = this.ancho - escrito;
        while (faltan > 0) {
            salida.insert(marca, this.relleno);
            faltan = faltan - 1;
        }
        return true;
    }

    int parsear(CtxParseo ctx, String texto, int pos) {
        // Al leer, el relleno se salta y la pieza de adentro ve **solo** su tajada de `ancho`
        // caracteres. Acotar el texto es lo que evita que un `appendValue` de ancho variable se coma
        // los digitos del campo siguiente.
        boolean estricto = ctx.estricto;
        int p = pos;
        int fin = pos + this.ancho;
        if (fin > texto.length()) {
            if (estricto) {
                return ~pos;
            }
            fin = texto.length();
        }
        while (p < fin && texto.charAt(p) == this.relleno) {
            p = p + 1;
        }
        String tajada = texto.substring(0, fin);
        int r = this.pieza.parsear(ctx, tajada, p);
        if (r < 0) {
            return r;
        }
        if (r != fin && estricto) {
            return ~r;
        }
        return r;
    }
}

// Los interruptores del parseo --`parseCaseInsensitive`, `parseLenient` y sus opuestos--. No escriben
// nada; su unico efecto es sobre el contexto de lectura, y por eso son una pieza mas y no un campo
// del formateador: valen **desde donde aparecen hasta el final**, que es lo que dice el JDK.
final class PiezaAjuste extends Pieza {

    static final int SENSIBLE = 0;
    static final int INSENSIBLE = 1;
    static final int ESTRICTO = 2;
    static final int LAXO = 3;

    private final int cual;

    PiezaAjuste(int cual) {
        this.cual = cual;
    }

    boolean imprimir(CtxImprimir ctx, StringBuilder salida) {
        return true;
    }

    int parsear(CtxParseo ctx, String texto, int pos) {
        if (this.cual == SENSIBLE) {
            ctx.sensible = true;
        } else if (this.cual == INSENSIBLE) {
            ctx.sensible = false;
        } else if (this.cual == ESTRICTO) {
            ctx.estricto = true;
        } else {
            ctx.estricto = false;
        }
        return pos;
    }
}

// `parseDefaulting`: el valor que se usa **si el texto no lo trajo**.
//
// No escribe nada, y al leer no consume nada: solo rellena un hueco. Es lo que hace que un patron de
// `"yyyy-MM"` pueda dar un `LocalDate` --con `parseDefaulting(DAY_OF_MONTH, 1)`-- sin que el patron
// mienta diciendo que leyo un dia.
final class PiezaPorDefecto extends Pieza {

    private final TemporalField campo;
    private final long valor;

    PiezaPorDefecto(TemporalField campo, long valor) {
        this.campo = campo;
        this.valor = valor;
    }

    boolean imprimir(CtxImprimir ctx, StringBuilder salida) {
        return true;
    }

    int parsear(CtxParseo ctx, String texto, int pos) {
        if (!ctx.campos.containsKey(this.campo)) {
            ctx.poner(this.campo, this.valor);
        }
        return pos;
    }
}
