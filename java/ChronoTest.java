// `java.time.chrono`: los cinco calendarios, sus eras, y los dos compuestos que esta tanda escribio
// (`ChronoLocalDateTimeImpl` y `ChronoZonedDateTimeImpl`, que son de paquete y se alcanzan por
// `atTime`/`atZone`).
//
// **Se comprueba contra `java` real corriendo lo mismo.** Es la primera vez que este paquete se
// ejercita: hasta ahora estaba medido por forma --que los miembros existan-- y nada mas, que es
// exactamente como `TemporalQueries` estuvo rota varias tandas devolviendo `null` en todo.
//
// Lo que mas se cuida es que **el calendario no se pierda por el camino**. Es el error que este
// paquete invita a cometer: todos los calendarios de aca guardan por dentro un dia epoch o un
// `LocalDate`, asi que una operacion que se olvide de reenvolver devuelve algo del calendario ISO
// que tiene el dia correcto y miente sobre todo lo demas. `atTime` hacia justo eso.
//
// **No se comprueba `getDisplayName`**: esta biblioteca no trae los datos de texto del CLDR y
// devuelve el valor numerico (eras) o el id (calendarios), que es la rama que el contrato define
// para cuando no hay nombre. Es una diferencia deliberada con el JDK y esta escrita en el javadoc de
// los dos metodos; compararla aca solo mediria eso.
//
// Con todo en verde devuelve -1; si no, el indice de la primera comprobacion que fallo.
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoLocalDateTime;
import java.time.chrono.ChronoPeriod;
import java.time.chrono.ChronoZonedDateTime;
import java.time.chrono.Chronology;
import java.time.chrono.Era;
import java.time.chrono.HijrahChronology;
import java.time.chrono.HijrahDate;
import java.time.chrono.IsoChronology;
import java.time.chrono.IsoEra;
import java.time.chrono.JapaneseChronology;
import java.time.chrono.JapaneseDate;
import java.time.chrono.MinguoChronology;
import java.time.chrono.MinguoDate;
import java.time.chrono.ThaiBuddhistChronology;
import java.time.chrono.ThaiBuddhistDate;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalQueries;

public class ChronoTest {

    static int cuantas = 0;
    static int primerFallo = -1;

    static void ok(boolean b) {
        if (!b && primerFallo < 0) {
            primerFallo = cuantas;
        }
        cuantas = cuantas + 1;
    }

    // Los cuatro calendarios no-ISO son corrimientos del ISO (salvo el hijri): la misma fecha con
    // otro numero de anio. Se comprueba el corrimiento **y** que el dia epoch coincida, que es la
    // unica magnitud que los cinco comparten.
    static void corrimientos() {
        LocalDate iso = LocalDate.of(2024, 2, 29);

        MinguoDate m = MinguoDate.of(2024 - 1911, 2, 29);
        ok(m.get(ChronoField.YEAR) == 113);
        ok(m.toEpochDay() == iso.toEpochDay());
        ok(m.lengthOfMonth() == 29);
        ok(m.isLeapYear());

        ThaiBuddhistDate t = ThaiBuddhistDate.of(2024 + 543, 2, 29);
        ok(t.get(ChronoField.YEAR) == 2567);
        ok(t.toEpochDay() == iso.toEpochDay());

        JapaneseDate j = JapaneseDate.of(2024, 2, 29);
        ok(j.toEpochDay() == iso.toEpochDay());

        // Las conversiones desde otro temporal, en las dos direcciones.
        ok(MinguoDate.from(iso).equals(m));
        ok(ThaiBuddhistDate.from(iso).equals(t));
        ok(JapaneseDate.from(iso).equals(j));
        ok(LocalDate.from(m).equals(iso));
        ok(LocalDate.from(t).equals(iso));
    }

    // El calendario tiene que sobrevivir a cada operacion. Es lo que `atTime` no hacia.
    static void elCalendarioSobrevive() {
        MinguoDate m = MinguoDate.of(113, 2, 29);
        ok(m.getChronology() == MinguoChronology.INSTANCE);

        ChronoLocalDateTime cldt = m.atTime(LocalTime.of(14, 30));
        ok(cldt.getChronology() == MinguoChronology.INSTANCE);
        ok(cldt.toLocalTime().equals(LocalTime.of(14, 30)));
        ok(cldt.toLocalDate().equals(m));

        // Sumar horas cruza la medianoche y **arrastra el dia**, sin cambiar de calendario.
        ChronoLocalDateTime masDiez = cldt.plus(10L, ChronoUnit.HOURS);
        ok(masDiez.getChronology() == MinguoChronology.INSTANCE);
        ok(masDiez.toLocalTime().equals(LocalTime.of(0, 30)));
        ok(masDiez.toLocalDate().toEpochDay() == m.toEpochDay() + 1L);

        // Y restar cruza para atras: el piso se toma hacia abajo, no hacia el cero.
        ChronoLocalDateTime menosDieciseis = cldt.minus(16L, ChronoUnit.HOURS);
        ok(menosDieciseis.toLocalTime().equals(LocalTime.of(22, 30)));
        ok(menosDieciseis.toLocalDate().toEpochDay() == m.toEpochDay() - 1L);

        // Sumar meses es cosa del calendario y la hora no se toca.
        ChronoLocalDateTime masUnMes = cldt.plus(1L, ChronoUnit.MONTHS);
        ok(masUnMes.getChronology() == MinguoChronology.INSTANCE);
        ok(masUnMes.toLocalTime().equals(LocalTime.of(14, 30)));

        // La zona tampoco lo pierde.
        ChronoZonedDateTime czdt = cldt.atZone(ZoneOffset.ofHours(-3));
        ok(czdt.getChronology() == MinguoChronology.INSTANCE);
        ok(czdt.getOffset().equals(ZoneOffset.ofHours(-3)));
        ok(czdt.toLocalDateTime().equals(cldt));

        // El instante que designa es el mismo que el del ISO equivalente.
        LocalDateTime isoMismo = LocalDateTime.of(2024, 2, 29, 14, 30);
        ok(czdt.toEpochSecond() == isoMismo.toEpochSecond(ZoneOffset.ofHours(-3)));
        ok(czdt.toInstant().equals(isoMismo.toInstant(ZoneOffset.ofHours(-3))));

        // La misma zona no reconstruye; otra zona con el mismo instante corrige la hora local.
        ok(czdt.withZoneSameInstant(ZoneOffset.ofHours(-3)) == czdt);
        ChronoZonedDateTime enUtc = czdt.withZoneSameInstant(ZoneOffset.UTC);
        ok(enUtc.getChronology() == MinguoChronology.INSTANCE);
        ok(enUtc.toEpochSecond() == czdt.toEpochSecond());
        ok(enUtc.toLocalTime().equals(LocalTime.of(17, 30)));

        // Y el ISO **no** pasa por la implementacion generica: tiene la suya, que sabe mas.
        ok(LocalDate.of(2024, 2, 29).atTime(LocalTime.NOON) instanceof LocalDateTime);
    }

    // Mezclar calendarios tiene que fallar, no producir un hibrido.
    static void mezclarFalla() {
        MinguoDate m = MinguoDate.of(113, 2, 29);
        ChronoLocalDateTime cldt = m.atTime(LocalTime.of(14, 30));

        boolean tiro = false;
        try {
            cldt.with(LocalDate.of(2024, 3, 1));
        } catch (ClassCastException e) {
            tiro = true;
        }
        ok(tiro);

        // Un periodo de otro calendario tampoco se puede sumar.
        boolean tiro2 = false;
        try {
            m.plus(MinguoChronology.INSTANCE.period(0, 0, 1).plus(
                    ThaiBuddhistChronology.INSTANCE.period(0, 0, 1)));
        } catch (RuntimeException e) {
            tiro2 = true;
        }
        ok(tiro2);
    }

    static void cronologias() {
        // Por id y por tipo CLDR.
        ok(Chronology.of("ISO") == IsoChronology.INSTANCE);
        ok(Chronology.of("iso8601") == IsoChronology.INSTANCE);
        ok(Chronology.of("Minguo") == MinguoChronology.INSTANCE);
        ok(Chronology.of("roc") == MinguoChronology.INSTANCE);
        ok(Chronology.of("Japanese") == JapaneseChronology.INSTANCE);
        ok(Chronology.of("ThaiBuddhist") == ThaiBuddhistChronology.INSTANCE);
        ok(Chronology.of("Hijrah-umalqura") != null);

        boolean tiro = false;
        try {
            Chronology.of("NoExiste");
        } catch (java.time.DateTimeException e) {
            tiro = true;
        }
        ok(tiro);

        // Las cinco estan disponibles.
        ok(Chronology.getAvailableChronologies().size() >= 5);
        ok(Chronology.getAvailableChronologies().contains(MinguoChronology.INSTANCE));

        // El calendario que un temporal declara; el ISO si no declara ninguno.
        ok(Chronology.from(LocalDate.of(2024, 1, 1)) == IsoChronology.INSTANCE);
        ok(Chronology.from(MinguoDate.of(113, 1, 1)) == MinguoChronology.INSTANCE);

        // Y por consulta, que es el camino que `TemporalQueries` habilita.
        ok(MinguoDate.of(113, 1, 1).query(TemporalQueries.chronology())
                == MinguoChronology.INSTANCE);
        ok(LocalDate.of(2024, 1, 1).query(TemporalQueries.chronology())
                == IsoChronology.INSTANCE);

        // Construccion por dia del anio y por era.
        ok(MinguoChronology.INSTANCE.dateYearDay(113, 60).toEpochDay()
                == LocalDate.of(2024, 2, 29).toEpochDay());
        ok(IsoChronology.INSTANCE.dateYearDay(2024, 60).equals(LocalDate.of(2024, 2, 29)));
        ok(IsoChronology.INSTANCE.date(IsoEra.CE, 2024, 2, 29).equals(LocalDate.of(2024, 2, 29)));

        // El anio proleptico: no hay anio cero, el 1 a.C. es el proleptico 0.
        ok(IsoChronology.INSTANCE.prolepticYear(IsoEra.BCE, 1) == 0);
        ok(IsoChronology.INSTANCE.prolepticYear(IsoEra.CE, 2024) == 2024);

        // Bisiestos, cada uno con su regla.
        ok(IsoChronology.INSTANCE.isLeapYear(2024L));
        ok(!IsoChronology.INSTANCE.isLeapYear(1900L));
        ok(MinguoChronology.INSTANCE.isLeapYear(113L));

        // `isIsoBased`: los corrimientos si, el hijri no.
        ok(IsoChronology.INSTANCE.isIsoBased());
        ok(MinguoChronology.INSTANCE.isIsoBased());
        ok(!HijrahChronology.INSTANCE.isIsoBased());

        // El segundo epoch sin construir la fecha.
        ok(IsoChronology.INSTANCE.epochSecond(2024, 2, 29, 14, 30, 0, ZoneOffset.UTC)
                == LocalDateTime.of(2024, 2, 29, 14, 30).toEpochSecond(ZoneOffset.UTC));
        ok(MinguoChronology.INSTANCE.epochSecond(113, 2, 29, 14, 30, 0, ZoneOffset.UTC)
                == LocalDateTime.of(2024, 2, 29, 14, 30).toEpochSecond(ZoneOffset.UTC));

        // Los rangos son propios de cada calendario.
        ok(IsoChronology.INSTANCE.range(ChronoField.MONTH_OF_YEAR).getMaximum() == 12L);
        ok(MinguoChronology.INSTANCE.range(ChronoField.MONTH_OF_YEAR).getMaximum() == 12L);
        ok(HijrahChronology.INSTANCE.range(ChronoField.DAY_OF_MONTH).getMaximum() == 30L);
    }

    static void eras() {
        ok(IsoEra.CE.getValue() == 1);
        ok(IsoEra.BCE.getValue() == 0);
        ok(IsoChronology.INSTANCE.eraOf(1) == IsoEra.CE);
        ok(IsoChronology.INSTANCE.eras().size() == 2);
        ok(MinguoChronology.INSTANCE.eras().size() == 2);

        // Una era es un `TemporalAccessor` de **un solo campo**.
        Era e = IsoEra.CE;
        ok(e.isSupported(ChronoField.ERA));
        ok(!e.isSupported(ChronoField.YEAR));
        ok(e.getLong(ChronoField.ERA) == 1L);
        ok(e.get(ChronoField.ERA) == 1);

        boolean tiro = false;
        try {
            e.getLong(ChronoField.YEAR);
        } catch (java.time.temporal.UnsupportedTemporalTypeException ex) {
            tiro = true;
        }
        ok(tiro);

        // La era de una fecha.
        ok(LocalDate.of(2024, 1, 1).getEra() == IsoEra.CE);
        ok(LocalDate.of(-100, 1, 1).getEra() == IsoEra.BCE);
        ok(MinguoDate.of(113, 1, 1).getEra().getValue() == 1);
        ok(MinguoDate.of(-5, 1, 1).getEra().getValue() == 0);
    }

    static void periodos() {
        MinguoDate a = MinguoDate.of(113, 1, 1);
        MinguoDate b = MinguoDate.of(114, 3, 15);
        ChronoPeriod p = a.until(b);
        ok(p.getChronology() == MinguoChronology.INSTANCE);
        ok(p.get(ChronoUnit.YEARS) == 1L);
        ok(p.get(ChronoUnit.MONTHS) == 2L);
        ok(p.get(ChronoUnit.DAYS) == 14L);
        ok(!p.isZero());
        ok(!p.isNegative());

        // El mismo periodo, por la entrada estatica.
        ChronoPeriod q = ChronoPeriod.between(a, b);
        ok(q.equals(p));

        // Y sumado de vuelta da la fecha de llegada.
        ok(a.plus(p).equals(b));

        ChronoPeriod cero = MinguoChronology.INSTANCE.period(0, 0, 0);
        ok(cero.isZero());
        ok(MinguoChronology.INSTANCE.period(0, 0, -1).isNegative());
        ok(MinguoChronology.INSTANCE.period(1, 2, 3).negated().get(ChronoUnit.YEARS) == -1L);
        ok(MinguoChronology.INSTANCE.period(1, 2, 3).multipliedBy(2).get(ChronoUnit.MONTHS) == 4L);
    }

    // El hijri no es el ISO corrido: sus meses salen de una tabla y su anio dura 354 o 355 dias.
    static void hijri() {
        HijrahDate h = HijrahDate.of(1445, 8, 20);
        ok(h.getChronology() == HijrahChronology.INSTANCE);
        ok(h.get(ChronoField.YEAR) == 1445);
        ok(h.get(ChronoField.MONTH_OF_YEAR) == 8);
        ok(h.get(ChronoField.DAY_OF_MONTH) == 20);

        // Un mes hijri tiene 29 o 30 dias, nunca 31.
        int largo = h.lengthOfMonth();
        ok(largo == 29 || largo == 30);
        int largoAnio = h.lengthOfYear();
        ok(largoAnio == 354 || largoAnio == 355);

        // Ida y vuelta por el dia epoch.
        ok(HijrahDate.from(LocalDate.ofEpochDay(h.toEpochDay())).equals(h));

        // Sumar un dia avanza un dia, aunque el mes no se sepa de antemano.
        ok(h.plus(1L, ChronoUnit.DAYS).toEpochDay() == h.toEpochDay() + 1L);

        // La unica variante que existe es la que se trae.
        ok(h.withVariant(HijrahChronology.INSTANCE) == h);
    }

    // El orden natural desempata por calendario; `timeLineOrder` no.
    static void orden() {
        MinguoDate m = MinguoDate.of(113, 2, 29);
        LocalDate iso = LocalDate.of(2024, 2, 29);
        ok(m.toEpochDay() == iso.toEpochDay());

        ChronoLocalDate a = m;
        ChronoLocalDate b = iso;
        ok(a.compareTo(b) != 0);                       // el mismo dia, distinto calendario
        ok(ChronoLocalDate.timeLineOrder().compare(a, b) == 0);
        ok(a.isEqual(b));                              // por linea de tiempo
        ok(!a.equals(b));                              // pero no son iguales

        ok(MinguoDate.of(113, 1, 1).isBefore(m));
        ok(m.isAfter(MinguoDate.of(113, 1, 1)));
    }

    public static int run() {
        corrimientos();
        elCalendarioSobrevive();
        mezclarFalla();
        cronologias();
        eras();
        periodos();
        hijri();
        orden();
        return primerFallo;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
