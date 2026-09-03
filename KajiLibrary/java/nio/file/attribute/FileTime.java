package java.nio.file.attribute;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

// Una marca de tiempo de archivo: un instante mas la granularidad con la que se lo midio.
//
// **Es puro valor.** No toca el disco, no necesita ningun nativo, y por eso esta completa mientras
// el resto del paquete queda a medias: `FileTime` es el unico tipo de `java.nio.file.attribute` que
// se puede escribir entero y honesto con lo que hay. Lo que falta es **quien la produzca** -- no hay
// nativo que devuelva la fecha de modificacion de un archivo, asi que `Files.getLastModifiedTime` no
// existe. La clase igual vale por si sola: es el tipo con el que `java.util.zip` expresa las fechas
// de una entrada, y ahi los datos vienen del propio ZIP, no del sistema de archivos.
//
// **Por que se guarda `(valor, unidad)` y no nanosegundos.** Un `long` de nanos cubre solo ~292
// años alrededor de 1970, y `FileTime.from(Long.MAX_VALUE, DAYS)` tiene que seguir funcionando.
// Guardando la unidad, el rango representable es el de la unidad mas gruesa que se haya usado, y la
// conversion se satura --a `Long.MIN_VALUE`/`Long.MAX_VALUE`-- solo cuando de verdad no entra.
//
// **Un `FileTime` construido desde un `Instant` no tiene unidad.** Se guarda el `Instant` tal cual y
// `unidad` queda en `null`; el campo `valor` no se usa en ese caso. Es la razon de que casi todos
// los metodos tengan dos ramas.
public final class FileTime implements Comparable<FileTime> {

    private static final long HORAS_POR_DIA = 24L;
    private static final long MINUTOS_POR_HORA = 60L;
    private static final long SEGUNDOS_POR_MINUTO = 60L;
    private static final long SEGUNDOS_POR_HORA = SEGUNDOS_POR_MINUTO * MINUTOS_POR_HORA;
    private static final long SEGUNDOS_POR_DIA = SEGUNDOS_POR_HORA * HORAS_POR_DIA;
    private static final long MILIS_POR_SEGUNDO = 1000L;
    private static final long MICROS_POR_SEGUNDO = 1000000L;
    private static final long NANOS_POR_SEGUNDO = 1000000000L;
    private static final int NANOS_POR_MILI = 1000000;
    private static final int NANOS_POR_MICRO = 1000;

    // Los limites de `Instant`, en segundos. Fuera de ahi `toInstant()` satura en vez de tirar.
    private static final long SEGUNDO_MIN = -31557014167219200L;
    private static final long SEGUNDO_MAX = 31556889864403199L;

    // Un ciclo de 400 años tiene 146097 dias; uno de 10000 años, 25 de esos.
    private static final long SEGUNDOS_POR_10000_ANIOS = 146097L * 25L * 86400L;
    private static final long SEGUNDOS_0000_A_1970 = ((146097L * 5L) - (30L * 365L + 7L)) * 86400L;

    /** La granularidad; `null` si se construyo desde un `Instant`. */
    private final TimeUnit unidad;

    /** El valor desde la epoca, en `unidad`. Puede ser negativo. Sin sentido si `unidad` es null. */
    private final long valor;

    // Memorizados: `toInstant()` y `toString()` son puros, y ambos se llaman repetido al ordenar o
    // al formatear una lista de archivos. No hay carrera que importe -- el peor caso es que dos
    // hilos calculen lo mismo y uno pise al otro con un valor identico.
    private Instant instante;
    private String texto;

    private FileTime(long valor, TimeUnit unidad, Instant instante) {
        this.valor = valor;
        this.unidad = unidad;
        this.instante = instante;
    }

    /**
     * Un `FileTime` con `valor` unidades desde la epoca.
     *
     * @param valor el valor desde 1970-01-01T00:00:00Z; puede ser negativo
     * @param unidad como interpretar `valor`
     */
    public static FileTime from(long valor, TimeUnit unidad) {
        Objects.requireNonNull(unidad, "unit");
        return new FileTime(valor, unidad, null);
    }

    /** Un `FileTime` desde milisegundos de la epoca. */
    public static FileTime fromMillis(long valor) {
        return new FileTime(valor, TimeUnit.MILLISECONDS, null);
    }

    /** Un `FileTime` en el mismo punto de la linea de tiempo que `instante`. */
    public static FileTime from(Instant instante) {
        Objects.requireNonNull(instante, "instant");
        return new FileTime(0, null, instante);
    }

    /**
     * El valor en la granularidad pedida.
     *
     * <p>Si no entra en un `long` **satura** en vez de dar vuelta el signo: bajar de dias a
     * nanosegundos multiplica por 86400000000000, y un desborde silencioso convertiria una fecha
     * lejana en el futuro en una del pasado.
     */
    public long to(TimeUnit unidad) {
        Objects.requireNonNull(unidad, "unit");
        if (this.unidad != null) {
            return unidad.convert(this.valor, this.unidad);
        }
        long segs = unidad.convert(this.instante.getEpochSecond(), TimeUnit.SECONDS);
        if (segs == Long.MIN_VALUE || segs == Long.MAX_VALUE) {
            return segs;
        }
        long nanos = unidad.convert(this.instante.getNano(), TimeUnit.NANOSECONDS);
        long r = segs + nanos;
        // La suma desbordo si los dos sumandos tienen el signo contrario al resultado.
        if (((segs ^ r) & (nanos ^ r)) < 0) {
            return (segs < 0) ? Long.MIN_VALUE : Long.MAX_VALUE;
        }
        return r;
    }

    /** El valor en milisegundos, saturando igual que `to`. */
    public long toMillis() {
        if (this.unidad != null) {
            return this.unidad.toMillis(this.valor);
        }
        long segs = this.instante.getEpochSecond();
        int nanos = this.instante.getNano();
        long r = segs * 1000;
        long ax = Math.abs(segs);
        if (((ax | 1000) >>> 31) != 0) {
            if ((r / 1000) != segs) {
                return (segs < 0) ? Long.MIN_VALUE : Long.MAX_VALUE;
            }
        }
        return r + nanos / 1000000;
    }

    // Multiplica `d` por `m` saturando: por encima de `tope` el producto no entra en un long.
    private static long escalar(long d, long m, long tope) {
        if (d > tope) {
            return Long.MAX_VALUE;
        }
        if (d < -tope) {
            return Long.MIN_VALUE;
        }
        return d * m;
    }

    /**
     * El mismo punto de la linea de tiempo, como `Instant`.
     *
     * <p>`FileTime` llega mas lejos que `Instant` en las dos direcciones, asi que lo que quede
     * afuera se satura en `Instant.MIN` o `Instant.MAX`.
     */
    public Instant toInstant() {
        Instant i = this.instante;
        if (i != null) {
            return i;
        }
        long segs = 0L;
        int nanos = 0;
        TimeUnit u = this.unidad;
        if (u == TimeUnit.DAYS) {
            segs = escalar(this.valor, SEGUNDOS_POR_DIA, Long.MAX_VALUE / SEGUNDOS_POR_DIA);
        } else if (u == TimeUnit.HOURS) {
            segs = escalar(this.valor, SEGUNDOS_POR_HORA, Long.MAX_VALUE / SEGUNDOS_POR_HORA);
        } else if (u == TimeUnit.MINUTES) {
            segs = escalar(this.valor, SEGUNDOS_POR_MINUTO, Long.MAX_VALUE / SEGUNDOS_POR_MINUTO);
        } else if (u == TimeUnit.SECONDS) {
            segs = this.valor;
        } else if (u == TimeUnit.MILLISECONDS) {
            segs = Math.floorDiv(this.valor, MILIS_POR_SEGUNDO);
            nanos = ((int) Math.floorMod(this.valor, MILIS_POR_SEGUNDO)) * NANOS_POR_MILI;
        } else if (u == TimeUnit.MICROSECONDS) {
            segs = Math.floorDiv(this.valor, MICROS_POR_SEGUNDO);
            nanos = ((int) Math.floorMod(this.valor, MICROS_POR_SEGUNDO)) * NANOS_POR_MICRO;
        } else {
            segs = Math.floorDiv(this.valor, NANOS_POR_SEGUNDO);
            nanos = (int) Math.floorMod(this.valor, NANOS_POR_SEGUNDO);
        }
        if (segs <= SEGUNDO_MIN) {
            i = Instant.MIN;
        } else if (segs >= SEGUNDO_MAX) {
            i = Instant.MAX;
        } else {
            i = Instant.ofEpochSecond(segs, nanos);
        }
        this.instante = i;
        return i;
    }

    /** Igual si representan el mismo momento, aunque las unidades difieran. */
    public boolean equals(Object obj) {
        return (obj instanceof FileTime) && this.compareTo((FileTime) obj) == 0;
    }

    /**
     * El hash del `Instant` equivalente.
     *
     * <p>Tiene que salir de ahi y no de `(valor, unidad)`: `from(1, SECONDS)` y `fromMillis(1000)`
     * son iguales por `equals`, asi que deben coincidir en el hash.
     */
    public int hashCode() {
        return this.toInstant().hashCode();
    }

    private long aDias() {
        if (this.unidad != null) {
            return this.unidad.toDays(this.valor);
        }
        return TimeUnit.SECONDS.toDays(this.toInstant().getEpochSecond());
    }

    private long nanosSobrantes(long dias) {
        if (this.unidad != null) {
            return this.unidad.toNanos(this.valor - this.unidad.convert(dias, TimeUnit.DAYS));
        }
        return TimeUnit.SECONDS.toNanos(
                this.toInstant().getEpochSecond() - TimeUnit.DAYS.toSeconds(dias));
    }

    /**
     * Orden cronologico.
     *
     * <p>Con la misma unidad alcanza con comparar los valores. Con unidades distintas hay que pasar
     * por `Instant`, y ahi aparece el caso raro que justifica la ultima rama: dos momentos **muy**
     * lejanos saturan los dos al mismo `Instant.MAX` y pareceria que son iguales. Cuando los
     * segundos dan justo en el limite se vuelve a comparar en dias y nanos del dia, que no saturan.
     */
    public int compareTo(FileTime otro) {
        if (this.unidad != null && this.unidad == otro.unidad) {
            return Long.compare(this.valor, otro.valor);
        }
        long segs = this.toInstant().getEpochSecond();
        long segsOtro = otro.toInstant().getEpochSecond();
        int cmp = Long.compare(segs, segsOtro);
        if (cmp != 0) {
            return cmp;
        }
        cmp = Long.compare(this.toInstant().getNano(), otro.toInstant().getNano());
        if (cmp != 0) {
            return cmp;
        }
        if (segs != SEGUNDO_MAX && segs != SEGUNDO_MIN) {
            return 0;
        }
        long dias = this.aDias();
        long diasOtro = otro.aDias();
        if (dias == diasOtro) {
            return Long.compare(this.nanosSobrantes(dias), otro.nanosSobrantes(diasOtro));
        }
        return Long.compare(dias, diasOtro);
    }

    // Escribe `d` con `ancho` digitos y ceros a la izquierda; `ancho` viene como potencia de diez.
    private static StringBuilder rellenar(StringBuilder sb, int ancho, int d) {
        int w = ancho;
        int v = d;
        while (w > 0) {
            sb.append((char) (v / w + '0'));
            v = v % w;
            w = w / 10;
        }
        return sb;
    }

    /**
     * La fecha en ISO 8601: `YYYY-MM-DDThh:mm:ss[.s+]Z`, siempre en UTC.
     *
     * <p>La fraccion de segundo aparece solo si no es cero, y sin ceros al final: `fromMillis(
     * 1234567890000L)` da `"2009-02-13T23:31:30Z"` y no `"...30.000Z"`.
     *
     * <p>Para años fuera de `0001..9999` --que `FileTime` puede representar y ISO 8601 no-- se
     * sigue la desviacion de XML Schema: mas de cuatro digitos, sin ceros a la izquierda, y signo
     * menos para las fechas anteriores. La cuenta de `hi`/`lo` esta para eso: parte los segundos en
     * ciclos de 10000 años **antes** de pasarselos a `LocalDateTime`, que solo cubre el rango
     * chico.
     */
    public String toString() {
        String s = this.texto;
        if (s != null) {
            return s;
        }
        long segs;
        int nanos = 0;
        if (this.instante == null && this.unidad.compareTo(TimeUnit.SECONDS) >= 0) {
            segs = this.unidad.toSeconds(this.valor);
        } else {
            segs = this.toInstant().getEpochSecond();
            nanos = this.toInstant().getNano();
        }
        LocalDateTime ldt;
        int anio;
        if (segs >= -SEGUNDOS_0000_A_1970) {
            long ceroSegs = segs - SEGUNDOS_POR_10000_ANIOS + SEGUNDOS_0000_A_1970;
            long hi = Math.floorDiv(ceroSegs, SEGUNDOS_POR_10000_ANIOS) + 1;
            long lo = Math.floorMod(ceroSegs, SEGUNDOS_POR_10000_ANIOS);
            ldt = LocalDateTime.ofEpochSecond(lo - SEGUNDOS_0000_A_1970, nanos, ZoneOffset.UTC);
            anio = ldt.getYear() + ((int) hi) * 10000;
        } else {
            long ceroSegs = segs + SEGUNDOS_0000_A_1970;
            long hi = ceroSegs / SEGUNDOS_POR_10000_ANIOS;
            long lo = ceroSegs % SEGUNDOS_POR_10000_ANIOS;
            ldt = LocalDateTime.ofEpochSecond(lo - SEGUNDOS_0000_A_1970, nanos, ZoneOffset.UTC);
            anio = ldt.getYear() + ((int) hi) * 10000;
        }
        // No hay año cero: el anterior a 0001 es -0001.
        if (anio <= 0) {
            anio = anio - 1;
        }
        int fraccion = ldt.getNano();
        StringBuilder sb = new StringBuilder(64);
        sb.append(anio < 0 ? "-" : "");
        anio = (int) Math.abs((long) anio);
        if (anio < 10000) {
            rellenar(sb, 1000, anio);
        } else {
            sb.append(String.valueOf(anio));
        }
        sb.append('-');
        rellenar(sb, 10, ldt.getMonthValue());
        sb.append('-');
        rellenar(sb, 10, ldt.getDayOfMonth());
        sb.append('T');
        rellenar(sb, 10, ldt.getHour());
        sb.append(':');
        rellenar(sb, 10, ldt.getMinute());
        sb.append(':');
        rellenar(sb, 10, ldt.getSecond());
        if (fraccion != 0) {
            sb.append('.');
            int w = 100000000;
            while (fraccion % 10 == 0) {
                fraccion = fraccion / 10;
                w = w / 10;
            }
            rellenar(sb, w, fraccion);
        }
        sb.append('Z');
        s = sb.toString();
        this.texto = s;
        return s;
    }
}
