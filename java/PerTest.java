// `java.time.Period` completo, comprobado contra `java` real.
//
// **Lo que esta prueba existe para fijar** es que los tres campos son independientes. Un `Period` de
// "1 mes y 1 dia" no son 31 dias ni 32: son un mes y un dia, y cuantos dias resulten depende de a
// que fecha se le sumen. Por eso `plus` suma campo a campo y no convierte nada, y por eso `between`
// toma primero los meses completos y despues el resto -- si lo hiciera al reves,
// `start.plus(between(start, end))` dejaria de dar `end` en los meses de distinta longitud.
//
// Con todo en verde devuelve -1; si no, el indice de la primera comprobacion que fallo.
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;

public class PerTest {

    static int cuantas = 0;
    static int primerFallo = -1;

    static void ok(boolean b) {
        if (!b && primerFallo < 0) {
            primerFallo = cuantas;
        }
        cuantas = cuantas + 1;
    }

    static void basicos() {
        ok(Period.ZERO.isZero());
        ok(Period.ZERO.getYears() == 0);

        Period p = Period.of(1, 2, 3);
        ok(p.withYears(9).getYears() == 9);
        ok(p.withYears(9).getMonths() == 2);      // los otros no se tocan
        ok(p.withMonths(9).getMonths() == 9);
        ok(p.withDays(9).getDays() == 9);
        ok(p.withYears(1) == p);                  // mismo valor: se devuelve el mismo objeto

        ok(p.multipliedBy(2).getYears() == 2);
        ok(p.multipliedBy(2).getMonths() == 4);
        ok(p.multipliedBy(2).getDays() == 6);
        ok(p.multipliedBy(0).isZero());
        ok(p.negated().getYears() == -1);
        ok(p.negated().getDays() == -3);

        ok(p.getChronology() == java.time.chrono.IsoChronology.INSTANCE);
    }

    static void sumas() {
        Period a = Period.of(1, 2, 3);
        Period b = Period.of(4, 5, 6);
        ok(a.plus(b).getYears() == 5);
        ok(a.plus(b).getMonths() == 7);
        ok(a.plus(b).getDays() == 9);
        ok(b.minus(a).getYears() == 3);
        ok(b.minus(a).getMonths() == 3);
        ok(b.minus(a).getDays() == 3);

        // **Campo a campo, sin conversion.** Un mes mas treinta dias es "1 mes y 30 dias".
        Period mes = Period.ofMonths(1);
        Period dias = Period.ofDays(30);
        ok(mes.plus(dias).getMonths() == 1);
        ok(mes.plus(dias).getDays() == 30);

        ok(Period.from(Period.of(1, 1, 1)).getYears() == 1);

        // Una unidad que no es año/mes/dia se rechaza.
        boolean tiro = false;
        try {
            Period.from(java.time.Duration.ofHours(1L));
        } catch (java.time.DateTimeException e) {
            tiro = true;
        }
        ok(tiro);
    }

    static void entre() {
        // Un mes justo.
        ok(Period.between(LocalDate.of(2024, 1, 15), LocalDate.of(2024, 2, 15)).getMonths() == 1);
        ok(Period.between(LocalDate.of(2024, 1, 15), LocalDate.of(2024, 2, 15)).getDays() == 0);

        // **El caso que separa las dos formas de contar.** Del 31 de enero al 1 de marzo: el dia
        // destino (1) es anterior al de origen (31), asi que el ultimo mes no se completo. Son "1 mes
        // y 1 dia", contando desde el 29 de febrero (2024 es bisiesto).
        Period p = Period.between(LocalDate.of(2024, 1, 31), LocalDate.of(2024, 3, 1));
        ok(p.getMonths() == 1);
        ok(p.getDays() == 1);

        // Y la propiedad que lo justifica: sumar el periodo a la fecha de origen da la de destino.
        ok(LocalDate.of(2024, 1, 31).plus(p).equals(LocalDate.of(2024, 3, 1)));

        // Un año y algo.
        Period q = Period.between(LocalDate.of(2020, 3, 10), LocalDate.of(2023, 7, 20));
        ok(q.getYears() == 3);
        ok(q.getMonths() == 4);
        ok(q.getDays() == 10);
        ok(LocalDate.of(2020, 3, 10).plus(q).equals(LocalDate.of(2023, 7, 20)));

        // Al reves: negativo, y tambien componible.
        Period r = Period.between(LocalDate.of(2023, 7, 20), LocalDate.of(2020, 3, 10));
        ok(r.getYears() == -3);
        ok(LocalDate.of(2023, 7, 20).plus(r).equals(LocalDate.of(2020, 3, 10)));

        // Mismas fechas: cero.
        ok(Period.between(LocalDate.of(2024, 5, 5), LocalDate.of(2024, 5, 5)).isZero());
    }

    static void duracion() {
        // Duration, de contraste: ahi si todo es una cantidad exacta de segundos.
        java.time.Duration d = java.time.Duration.of(90, ChronoUnit.MINUTES);
        ok(d.toHours() == 1L);
        ok(d.toMinutesPart() == 30);
    }

    public static int run() {
        basicos();
        sumas();
        entre();
        duracion();
        return primerFallo;
    }

    // Para el arnes de regresion, que corre la clase con el `java` real y compara la salida.
    public static void main(String[] args) {
        System.out.println(run());
    }
}
