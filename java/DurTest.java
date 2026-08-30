// `java.time.Duration` completo, comprobado contra `java` real.
//
// Lo que mas se cuida aca son los **negativos**, que es donde esta casi todo el error posible.
// `truncatedTo` va hacia **cero** y no hacia abajo: `-1.5s` truncado a segundos es `-1s`, no `-2s`.
// El nombre sugiere lo contrario --"truncar" suena a `floor`-- y para los negativos es la direccion
// opuesta; yo lo escribi al reves y esta prueba, corrida contra `java` real, lo dijo. Una que solo
// mirara duraciones positivas no habria notado nada.
//
// Y la distincion entre las dos familias de accesores, que es facil de confundir: `toMinutes()` es
// la duracion **entera** en minutos y `toMinutesPart()` es el campo dentro de la hora. Para 3661
// segundos, 61 y 1.
//
// Con todo en verde devuelve -1; si no, el indice de la primera comprobacion que fallo.
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class DurTest {

    static int cuantas = 0;
    static int primerFallo = -1;

    static void ok(boolean b) {
        if (!b && primerFallo < 0) {
            primerFallo = cuantas;
        }
        cuantas = cuantas + 1;
    }

    static void fabricas() {
        ok(Duration.ZERO.isZero());
        ok(Duration.of(5, ChronoUnit.MINUTES).getSeconds() == 300L);
        ok(Duration.of(2, ChronoUnit.DAYS).toHours() == 48L);
        ok(Duration.of(1500, ChronoUnit.MILLIS).getSeconds() == 1L);
        ok(Duration.of(1500, ChronoUnit.MILLIS).getNano() == 500000000);

        // Una unidad **estimada** se rechaza: un mes no tiene una cantidad fija de segundos, asi que
        // no hay Duration que le corresponda. Eso es lo que modela Period.
        boolean tiro = false;
        try {
            Duration.of(1, ChronoUnit.MONTHS);
        } catch (java.time.DateTimeException e) {
            tiro = true;
        }
        ok(tiro);

        Duration entre = Duration.between(Instant.ofEpochSecond(100L), Instant.ofEpochSecond(160L));
        ok(entre.getSeconds() == 60L);
        // Negativa si el fin es anterior: es lo que la hace componible.
        ok(Duration.between(Instant.ofEpochSecond(160L), Instant.ofEpochSecond(100L))
                .getSeconds() == -60L);

        ok(Duration.from(Duration.ofSeconds(7L)).getSeconds() == 7L);
    }

    static void partes() {
        // 1h 1m 1s 500ms = 3661.5 s
        Duration d = Duration.ofSeconds(3661L, 500000000L);
        ok(d.toDays() == 0L);
        ok(d.toHours() == 1L);
        ok(d.toMinutes() == 61L);          // el TOTAL en minutos
        ok(d.toSeconds() == 3661L);
        ok(d.toMinutesPart() == 1);        // el campo dentro de la hora
        ok(d.toSecondsPart() == 1);
        ok(d.toMillisPart() == 500);
        ok(d.toNanosPart() == 500000000);
        ok(d.toHoursPart() == 1);
        ok(d.toDaysPart() == 0L);

        Duration larga = Duration.ofHours(50L);
        ok(larga.toDays() == 2L);
        ok(larga.toHoursPart() == 2);      // 50 horas = 2 dias y 2 horas

        ok(d.isPositive());
        ok(!Duration.ZERO.isPositive());
        ok(!Duration.ofSeconds(-1L).isPositive());
    }

    static void aritmetica() {
        Duration d = Duration.ofSeconds(60L);
        ok(d.plusMinutes(1L).getSeconds() == 120L);
        ok(d.plusHours(1L).getSeconds() == 3660L);
        ok(d.plusDays(1L).getSeconds() == 86460L);
        ok(d.plusMillis(500L).getNano() == 500000000);
        ok(d.plusNanos(1L).getNano() == 1);
        ok(d.minusMinutes(1L).isZero());
        ok(d.minusHours(1L).getSeconds() == -3540L);
        ok(d.minusDays(1L).getSeconds() == -86340L);
        ok(d.minusMillis(500L).getSeconds() == 59L);
        ok(d.minusMillis(500L).getNano() == 500000000);
        ok(d.minusNanos(1L).getNano() == 999999999);

        ok(d.plus(2L, ChronoUnit.MINUTES).getSeconds() == 180L);
        ok(d.minus(2L, ChronoUnit.MINUTES).getSeconds() == -60L);

        ok(d.multipliedBy(3L).getSeconds() == 180L);
        ok(d.multipliedBy(0L).isZero());
        ok(d.multipliedBy(-2L).getSeconds() == -120L);
        ok(d.dividedBy(4L).getSeconds() == 15L);
        ok(Duration.ofSeconds(100L).dividedBy(Duration.ofSeconds(30L)) == 3L);

        boolean tiro = false;
        try {
            d.dividedBy(0L);
        } catch (ArithmeticException e) {
            tiro = true;
        }
        ok(tiro);

        ok(Duration.ofSeconds(-5L).abs().getSeconds() == 5L);
        ok(Duration.ofSeconds(5L).abs().getSeconds() == 5L);

        ok(d.withSeconds(10L).getSeconds() == 10L);
        ok(Duration.ofSeconds(1L, 5L).withSeconds(9L).getNano() == 5);
        ok(d.withNanos(123).getNano() == 123);

        tiro = false;
        try {
            d.withNanos(-1);
        } catch (java.time.DateTimeException e) {
            tiro = true;
        }
        ok(tiro);
    }

    static void truncar() {
        Duration d = Duration.ofSeconds(3661L, 500000000L);
        ok(d.truncatedTo(ChronoUnit.SECONDS).getNano() == 0);
        ok(d.truncatedTo(ChronoUnit.SECONDS).getSeconds() == 3661L);
        ok(d.truncatedTo(ChronoUnit.MINUTES).getSeconds() == 3660L);
        ok(d.truncatedTo(ChronoUnit.HOURS).getSeconds() == 3600L);
        ok(d.truncatedTo(ChronoUnit.DAYS).isZero());
        ok(d.truncatedTo(ChronoUnit.MILLIS).getNano() == 500000000);

        // **El caso que importa, y el que me salio al reves.** Truncar va hacia **cero**, no hacia
        // abajo: -1.5s a segundos es -1s, y -90s a minutos es -60s. El nombre sugiere lo otro --
        // "truncar" suena a `floor`-- y para los negativos es la direccion opuesta. Lo dijo `java`
        // real antes de que se fuera al arbol.
        Duration neg = Duration.ofSeconds(-2L, 500000000L);   // -1.5 s
        ok(neg.getSeconds() == -2L && neg.getNano() == 500000000);
        ok(neg.truncatedTo(ChronoUnit.SECONDS).getSeconds() == -1L);
        ok(neg.truncatedTo(ChronoUnit.SECONDS).getNano() == 0);
        ok(Duration.ofSeconds(-90L).truncatedTo(ChronoUnit.MINUTES).getSeconds() == -60L);

        boolean tiro = false;
        try {
            d.truncatedTo(ChronoUnit.MONTHS);
        } catch (java.time.DateTimeException e) {
            tiro = true;
        }
        ok(tiro);
    }

    public static int run() {
        fabricas();
        partes();
        aritmetica();
        truncar();
        return primerFallo;
    }

    // Para el arnes de regresion, que corre la clase con el `java` real y compara la salida.
    public static void main(String[] args) {
        System.out.println(run());
    }
}
