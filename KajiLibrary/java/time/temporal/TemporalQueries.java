package java.time.temporal;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.chrono.Chronology;

// KajiLibrary's java.time.temporal.TemporalQueries -- las consultas estandar.
//
// **Son singletons, y eso no es una optimizacion: es el contrato.** Un temporal reconoce que le
// preguntaron comparando `query == TemporalQueries.zone()` por identidad --asi esta escrito en el
// JDK y asi esta escrito en toda esta biblioteca-- porque una `TemporalQuery` no tiene ningun otro
// dato con el que identificarse. Si cada llamada devolviera un objeto nuevo, ninguna de esas
// comparaciones acertaria nunca: `query()` caeria siempre al `queryFrom` generico, que para las
// consultas marcadoras devuelve `null`.
//
// Eso es exactamente lo que pasaba, y es un buen ejemplo de un error que **no rompe nada
// visiblemente**: todo compilaba, `zonedDateTime.query(zone())` devolvia `null` en vez de la zona, y
// el sintoma aparecia tres capas mas arriba como un "Unable to obtain ZonedDateTime" al parsear. Lo
// encontro `FmtTest`.
//
// `zoneId()` y `zone()` devuelven **la misma** instancia: en el JDK son dos consultas distintas --la
// segunda acepta un desplazamiento cuando no hay zona de region-- pero aca las zonas son siempre de
// desplazamiento fijo, con lo cual las dos respuestas coinciden siempre. Se comparte la instancia en
// vez de tener dos para que un temporal que reconozca una reconozca las dos.
//
// `localDate`/`localTime` son las unicas que hacen trabajo de verdad: sacan la fecha o la hora de
// cualquier temporal que lleve los campos. Las demas son marcadoras: sin un `query()` que las
// reconozca, la respuesta correcta es `null`.
public final class TemporalQueries {

    // Instancias unicas. Un campo `static final` de tipo referencia se lee bien en tiempo de
    // ejecucion; lo que no se puede es un `static final` primitivo (finding #112).
    private static final ZoneIdQuery ZONE = new ZoneIdQuery();
    private static final ChronologyQuery CHRONOLOGY = new ChronologyQuery();
    private static final PrecisionQuery PRECISION = new PrecisionQuery();
    private static final OffsetQuery OFFSET = new OffsetQuery();
    private static final LocalDateQuery LOCAL_DATE = new LocalDateQuery();
    private static final LocalTimeQuery LOCAL_TIME = new LocalTimeQuery();

    private TemporalQueries() {
    }

    public static TemporalQuery<ZoneId> zoneId() {
        return ZONE;
    }

    public static TemporalQuery<Chronology> chronology() {
        return CHRONOLOGY;
    }

    public static TemporalQuery<TemporalUnit> precision() {
        return PRECISION;
    }

    public static TemporalQuery<ZoneId> zone() {
        return ZONE;
    }

    public static TemporalQuery<ZoneOffset> offset() {
        return OFFSET;
    }

    public static TemporalQuery<LocalDate> localDate() {
        return LOCAL_DATE;
    }

    public static TemporalQuery<LocalTime> localTime() {
        return LOCAL_TIME;
    }
}

final class LocalDateQuery implements TemporalQuery<LocalDate> {
    public LocalDate queryFrom(TemporalAccessor temporal) {
        if (temporal.isSupported(ChronoField.EPOCH_DAY)) {
            return LocalDate.ofEpochDay(temporal.getLong(ChronoField.EPOCH_DAY));
        }
        return null;
    }
}

final class LocalTimeQuery implements TemporalQuery<LocalTime> {
    public LocalTime queryFrom(TemporalAccessor temporal) {
        // Por `NANO_OF_DAY` y no por los cuatro campos sueltos: es el campo que **cualquier**
        // portador de una hora tiene, y el que `LocalTime.from` usa. Preguntar por
        // `NANO_OF_SECOND` dejaba afuera a los que llevan la hora entera en un solo numero.
        if (temporal.isSupported(ChronoField.NANO_OF_DAY)) {
            return LocalTime.ofNanoOfDay(temporal.getLong(ChronoField.NANO_OF_DAY));
        }
        if (temporal.isSupported(ChronoField.NANO_OF_SECOND)) {
            return LocalTime.of(temporal.get(ChronoField.HOUR_OF_DAY),
                temporal.get(ChronoField.MINUTE_OF_HOUR),
                temporal.get(ChronoField.SECOND_OF_MINUTE),
                temporal.get(ChronoField.NANO_OF_SECOND));
        }
        return null;
    }
}

final class ZoneIdQuery implements TemporalQuery<ZoneId> {
    public ZoneId queryFrom(TemporalAccessor temporal) {
        return null;
    }
}

final class ChronologyQuery implements TemporalQuery<Chronology> {
    public Chronology queryFrom(TemporalAccessor temporal) {
        return null;
    }
}

final class PrecisionQuery implements TemporalQuery<TemporalUnit> {
    public TemporalUnit queryFrom(TemporalAccessor temporal) {
        return null;
    }
}

final class OffsetQuery implements TemporalQuery<ZoneOffset> {
    public ZoneOffset queryFrom(TemporalAccessor temporal) {
        return null;
    }
}
