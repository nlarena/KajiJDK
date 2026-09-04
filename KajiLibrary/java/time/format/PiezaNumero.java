package java.time.format;

import java.time.DateTimeException;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.Chronology;
import java.time.temporal.TemporalField;
import java.time.temporal.ValueRange;

// Un campo escrito con digitos: el `2024` de un anio, el `02` de un mes.
//
// `min` es cuantos digitos se escriben como minimo --el relleno con ceros-- y `max` cuantos se
// admiten como maximo. Al leer, `max` es lo que corta: `appendValue(YEAR, 4, 10, EXCEEDS_PAD)`
// seguido de `'-'` lee `2024` y para en el guion porque el guion no es un digito, pero
// `appendValue(YEAR, 4)` pegado a `appendValue(MONTH_OF_YEAR, 2)` --el `BASIC_ISO_DATE`-- solo
// funciona porque el primero se planta a los cuatro.
//
// **Lo que no hay: el "adjacent value parsing" del JDK.** Cuando dos campos de ancho *variable* se
// pegan sin separador, el JDK entra en un modo especial en el que el primero cede digitos al segundo.
// Aca la lectura es golosa y se corta por `max`, asi que dos campos variables pegados leen mal. No se
// simula: `DateTimeFormatterBuilder` no tiene forma de expresar ese caso sin un separador, y todos
// los formateadores predefinidos usan anchos fijos o separadores. Queda anotado como divergencia.
class PiezaNumero extends Pieza {

    final TemporalField campo;
    final int min;
    final int max;
    final SignStyle signo;

    PiezaNumero(TemporalField campo, int min, int max, SignStyle signo) {
        this.campo = campo;
        this.min = min;
        this.max = max;
        this.signo = signo;
    }

    // El valor que realmente se escribe. `PiezaNumeroReducido` lo pisa para recortar el siglo.
    long valorAEscribir(CtxImprimir ctx, long valor) {
        return valor;
    }

    boolean imprimir(CtxImprimir ctx, StringBuilder salida) {
        Long v = ctx.valor(this.campo);
        if (v == null) {
            return false;
        }
        long valor = this.valorAEscribir(ctx, v.longValue());
        String digitos = Long.toString(valor);
        boolean negativo = digitos.startsWith("-");
        if (negativo) {
            digitos = digitos.substring(1);
        }
        if (digitos.length() > this.max) {
            throw new DateTimeException("Field " + this.campo + " cannot be printed as the value "
                    + valor + " exceeds the maximum print width of " + this.max);
        }
        DecimalStyle s = ctx.simbolos;
        if (negativo) {
            if (this.signo == SignStyle.NOT_NEGATIVE) {
                throw new DateTimeException("Field " + this.campo
                        + " cannot be printed as the value " + valor
                        + " cannot be negative according to the SignStyle");
            }
            if (this.signo != SignStyle.NEVER) {
                salida.append(s.getNegativeSign());
            }
        } else {
            if (this.signo == SignStyle.ALWAYS
                    || (this.signo == SignStyle.EXCEEDS_PAD && digitos.length() > this.min)) {
                salida.append(s.getPositiveSign());
            }
        }
        Pieza.escribirDigitos(ctx, salida, digitos, this.min);
        return true;
    }

    int parsear(CtxParseo ctx, String texto, int pos) {
        int p = pos;
        boolean negativo = false;
        boolean haySigno = false;
        if (p < texto.length()) {
            char c = texto.charAt(p);
            if (c == ctx.simbolos.getPositiveSign()) {
                if (this.signo == SignStyle.NEVER || this.signo == SignStyle.NOT_NEGATIVE) {
                    return ~pos;
                }
                haySigno = true;
                p = p + 1;
            } else if (c == ctx.simbolos.getNegativeSign()) {
                if (this.signo == SignStyle.NEVER) {
                    return ~pos;
                }
                negativo = true;
                haySigno = true;
                p = p + 1;
            }
        }
        if (ctx.estricto && this.signo == SignStyle.ALWAYS && !haySigno) {
            return ~pos;
        }
        // Con signo escrito, el ancho minimo ya no cuenta el relleno: `+2024` trae cuatro digitos
        // igual, pero un `-1` con `min` 4 seria un texto que ningun `imprimir` genera, y en modo laxo
        // se admite igual. El maximo si se respeta siempre: es lo unico que corta la lectura golosa.
        int desde = p;
        long valor = 0L;
        while (p < texto.length() && p - desde < this.max) {
            int d = Pieza.digito(ctx, texto.charAt(p));
            if (d < 0) {
                break;
            }
            valor = valor * 10L + (long) d;
            p = p + 1;
        }
        int leidos = p - desde;
        if (leidos == 0) {
            return ~pos;
        }
        if (ctx.estricto && leidos < this.min) {
            return ~pos;
        }
        this.depositar(ctx, negativo ? -valor : valor, leidos);
        return p;
    }

    void depositar(CtxParseo ctx, long valor, int leidos) {
        ctx.poner(this.campo, valor);
    }
}

// `appendValueReduced`: el anio de dos digitos, y su generalizacion.
//
// Escribe **solo los ultimos `min` digitos** mientras el valor caiga en la ventana que arranca en
// `base` y dura `10^min`; fuera de la ventana escribe el numero entero. Al leer, un texto de
// exactamente `min` digitos se completa hacia la ventana, y uno mas largo se toma tal cual.
//
// La asimetria es a proposito y es del JDK: `withYear(2024)` con base 2000 escribe `24`, pero
// `withYear(1875)` escribe `1875` en vez de `75`, porque `75` se releeria como 2075. Un formateador
// que escribe algo que el mismo relee mal es peor que uno que escribe de mas.
final class PiezaNumeroReducido extends PiezaNumero {

    private final int base;
    private final ChronoLocalDate fechaBase;

    PiezaNumeroReducido(TemporalField campo, int min, int max, int base,
            ChronoLocalDate fechaBase) {
        super(campo, min, max, SignStyle.NOT_NEGATIVE);
        this.base = base;
        this.fechaBase = fechaBase;
    }

    private static long potencia10(int n) {
        long r = 1L;
        int i = 0;
        while (i < n) {
            r = r * 10L;
            i = i + 1;
        }
        return r;
    }

    // La base efectiva. Con una fecha base, el valor se relee **en la cronologia que este en juego**:
    // el "anio 2000" de una fecha ISO no es el mismo numero en el calendario japones, y tomar el
    // numero ISO daria una ventana corrida un par de milenios.
    private int base(Chronology cronologia) {
        if (this.fechaBase == null) {
            return this.base;
        }
        if (cronologia == null) {
            return (int) this.fechaBase.getLong(this.campo);
        }
        return (int) cronologia.date(this.fechaBase).getLong(this.campo);
    }

    long valorAEscribir(CtxImprimir ctx, long valor) {
        int b = this.base(ctx.consultar(java.time.temporal.TemporalQueries.chronology()));
        long ventana = potencia10(this.min);
        long abs = valor < 0L ? -valor : valor;
        if (valor >= (long) b && valor < (long) b + ventana) {
            return abs % ventana;
        }
        // Fuera de la ventana se escribe lo que **entre en `max`**, no el numero entero: con
        // `max == min` el anio se recorta igual (1875 sale `75`), y con un `max` mayor sale completo
        // (`1875`). El recorte no es una perdida silenciosa: `max` es justamente la promesa de
        // cuantos digitos como mucho va a haber.
        return abs % potencia10(this.max);
    }

    void depositar(CtxParseo ctx, long valor, int leidos) {
        long v = valor;
        if (leidos == this.min && v >= 0L) {
            int b = this.base(ctx.cronologia);
            long ventana = potencia10(this.min);
            long resto = (long) b % ventana;
            long entero = (long) b - resto;
            if (b > 0) {
                v = entero + v;
            } else {
                v = entero - v;
            }
            if (v < (long) b) {
                v = v + ventana;
            }
        }
        ctx.poner(this.campo, v);
    }
}

// `appendFraction`: la parte fraccionaria de un campo, tipicamente los nanos.
//
// **No escribe el numero, escribe la fraccion.** `NANO_OF_SECOND` vale 400000000 y el texto es
// `.4`, no `.400000000`: el valor se divide por el tamanio de su rango y se emite en base diez,
// recortando los ceros de la derecha hasta `min` digitos. Es la unica forma de que `.5` y `.500`
// signifiquen lo mismo, que es lo que ISO-8601 pide.
final class PiezaFraccion extends Pieza {

    private final TemporalField campo;
    private final int min;
    private final int max;
    private final boolean punto;

    PiezaFraccion(TemporalField campo, int min, int max, boolean punto) {
        this.campo = campo;
        this.min = min;
        this.max = max;
        this.punto = punto;
    }

    private static long potencia10(int n) {
        long r = 1L;
        int i = 0;
        while (i < n) {
            r = r * 10L;
            i = i + 1;
        }
        return r;
    }

    boolean imprimir(CtxImprimir ctx, StringBuilder salida) {
        Long v = ctx.valor(this.campo);
        if (v == null) {
            return false;
        }
        ValueRange rango = this.campo.range();
        long minimo = rango.getMinimum();
        long tamanio = rango.getMaximum() - minimo + 1L;
        long resto = v.longValue() - minimo;
        StringBuilder digitos = new StringBuilder();
        int i = 0;
        while (i < this.max) {
            resto = resto * 10L;
            digitos.append((char) ('0' + (int) (resto / tamanio)));
            resto = resto % tamanio;
            i = i + 1;
        }
        String d = digitos.toString();
        int fin = d.length();
        while (fin > this.min && fin > 0 && d.charAt(fin - 1) == '0') {
            fin = fin - 1;
        }
        d = d.substring(0, fin);
        if (d.length() == 0) {
            // Con `min` cero y un valor cero no hay fraccion que escribir, y **tampoco el punto**:
            // `10:15:30` y no `10:15:30.`.
            return true;
        }
        if (this.punto) {
            salida.append(ctx.simbolos.getDecimalSeparator());
        }
        Pieza.escribirDigitos(ctx, salida, d, 0);
        return true;
    }

    int parsear(CtxParseo ctx, String texto, int pos) {
        int p = pos;
        boolean hayPunto = false;
        if (this.punto) {
            if (p < texto.length() && texto.charAt(p) == ctx.simbolos.getDecimalSeparator()) {
                hayPunto = true;
                p = p + 1;
            } else if (this.min > 0) {
                return ~pos;
            } else {
                return pos;
            }
        }
        int desde = p;
        long acumulado = 0L;
        while (p < texto.length() && p - desde < this.max) {
            int d = Pieza.digito(ctx, texto.charAt(p));
            if (d < 0) {
                break;
            }
            acumulado = acumulado * 10L + (long) d;
            p = p + 1;
        }
        int leidos = p - desde;
        if (leidos < this.min || (hayPunto && leidos == 0)) {
            return ~pos;
        }
        if (leidos == 0) {
            return pos;
        }
        ValueRange rango = this.campo.range();
        long minimo = rango.getMinimum();
        long tamanio = rango.getMaximum() - minimo + 1L;
        // `acumulado / 10^leidos` es la fraccion; multiplicarla por el tamanio del rango la devuelve
        // a la escala del campo. Se multiplica **antes** de dividir para no perder los digitos bajos:
        // 4/10 * 1000000000 en enteros da 0 si se divide primero.
        long valor = minimo + acumulado * tamanio / potencia10(leidos);
        ctx.poner(this.campo, valor);
        return p;
    }
}
