package java.time.chrono;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.time.temporal.ValueRange;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

// KajiLibrary's java.time.chrono.Chronology -- un sistema de calendario: el ISO-8601 y los cuatro que
// trae el JDK (japones, hijri, minguo, budista tailandes). Ordenado por id, asi que extiende
// Comparable<Chronology>.
//
// La division con `ChronoLocalDate` es la del JDK y conviene tenerla clara: la **fecha** sabe que dia
// es, el **calendario** sabe como se cuentan los dias. Una `MinguoDate` no sabe cuantos meses tiene
// un anio; le pregunta a su `MinguoChronology`.
//
// Queda afuera `getDisplayName(TextStyle, Locale)`: necesita los nombres traducidos de cada
// calendario, que son datos --el CLDR-- y no codigo. Escribirlo devolviendo el id o un nombre en
// ingles para cualquier locale seria un miembro que **miente sobre lo que le pidieron**, que es
// peor que uno que falta. Misma razon por la que `Era.getDisplayName` tampoco esta.
public interface Chronology extends Comparable<Chronology> {

    String getId();

    String getCalendarType();

    boolean isLeapYear(long prolepticYear);

    ChronoLocalDate date(int prolepticYear, int month, int dayOfMonth);

    ChronoLocalDate dateEpochDay(long epochDay);

    Era eraOf(int eraValue);

    int compareTo(Chronology other);

    // ---- lo que cada calendario tiene que saber contestar -----------------------------------------

    /**
     * El anio **proleptico** que corresponde a ese anio dentro de esa era.
     *
     * <p>Un calendario cuenta los anios por era y vuelve a empezar: el 1 de Showa y el 1 de Heisei
     * son dos anios distintos. El anio proleptico es la numeracion corrida que no se reinicia, y es
     * la unica con la que se puede hacer aritmetica.
     */
    int prolepticYear(Era era, int yearOfEra);

    /** La fecha que `temporal` tiene, leida en **este** calendario. */
    ChronoLocalDate date(TemporalAccessor temporal);

    /** La fecha por anio y **dia del anio**, sin pasar por el mes. */
    ChronoLocalDate dateYearDay(int prolepticYear, int dayOfYear);

    /** El rango de valores que ese campo admite **en este calendario**. */
    ValueRange range(ChronoField field);

    /** Las eras de este calendario, de la mas antigua a la mas reciente. */
    List<Era> eras();

    // ---- construccion, con la era explicita -------------------------------------------------------

    /** La fecha por era, anio de la era, mes y dia. */
    default ChronoLocalDate date(Era era, int yearOfEra, int month, int dayOfMonth) {
        return this.date(this.prolepticYear(era, yearOfEra), month, dayOfMonth);
    }

    /** La fecha por era, anio de la era y dia del anio. */
    default ChronoLocalDate dateYearDay(Era era, int yearOfEra, int dayOfYear) {
        return this.dateYearDay(this.prolepticYear(era, yearOfEra), dayOfYear);
    }

    /** Hoy, en la zona por defecto del sistema. */
    default ChronoLocalDate dateNow() {
        return this.dateNow(Clock.systemDefaultZone());
    }

    /** Hoy en esa zona. */
    default ChronoLocalDate dateNow(ZoneId zone) {
        return this.dateNow(Clock.system(zone));
    }

    /** Hoy **segun ese reloj**, que es la forma que se puede probar con un `Clock.fixed`. */
    default ChronoLocalDate dateNow(Clock clock) {
        if (clock == null) {
            throw new NullPointerException("clock");
        }
        LocalDate hoy = LocalDate.now(clock);
        return this.dateEpochDay(hoy.toEpochDay());
    }

    // ---- los compuestos ---------------------------------------------------------------------------

    /** La fecha y hora que `temporal` tiene, en este calendario. */
    default ChronoLocalDateTime localDateTime(TemporalAccessor temporal) {
        if (temporal == null) {
            throw new NullPointerException("temporal");
        }
        ChronoLocalDate fecha = this.date(temporal);
        LocalTime hora = LocalTime.from(temporal);
        return ChronoLocalDateTimeImpl.of(fecha, hora);
    }

    /** La fecha, hora y zona que `temporal` tiene, en este calendario. */
    default ChronoZonedDateTime zonedDateTime(TemporalAccessor temporal) {
        if (temporal == null) {
            throw new NullPointerException("temporal");
        }
        ZoneId zona = ZoneId.from(temporal);
        if (temporal.isSupported(ChronoField.INSTANT_SECONDS)) {
            Instant instante = Instant.ofEpochSecond(temporal.getLong(ChronoField.INSTANT_SECONDS),
                    temporal.getLong(ChronoField.NANO_OF_SECOND));
            return this.zonedDateTime(instante, zona);
        }
        ChronoLocalDateTime local = this.localDateTime(temporal);
        return ChronoZonedDateTimeImpl.of(local, zona);
    }

    /** Ese instante visto desde esa zona, en este calendario. */
    default ChronoZonedDateTime zonedDateTime(Instant instant, ZoneId zone) {
        if (instant == null) {
            throw new NullPointerException("instant");
        }
        return ChronoZonedDateTimeImpl.ofInstant(this, instant, zone);
    }

    /** Un periodo de este calendario. Anios, meses y dias **no** se normalizan entre si. */
    default ChronoPeriod period(int years, int months, int days) {
        return new ChronoPeriodImpl(this, years, months, days);
    }

    // ---- el segundo epoch, sin construir la fecha -------------------------------------------------

    /**
     * Los segundos desde el epoch de esa fecha y hora **de este calendario** con ese desplazamiento.
     *
     * <p>Existe para no tener que construir el objeto intermedio cuando lo unico que se quiere es el
     * numero: es el camino que usan las bases de datos y los formatos binarios.
     */
    default long epochSecond(int prolepticYear, int month, int dayOfMonth, int hour, int minute,
            int second, ZoneOffset zoneOffset) {
        if (zoneOffset == null) {
            throw new NullPointerException("zoneOffset");
        }
        ChronoField.HOUR_OF_DAY.checkValidValue((long) hour);
        ChronoField.MINUTE_OF_HOUR.checkValidValue((long) minute);
        ChronoField.SECOND_OF_MINUTE.checkValidValue((long) second);
        ChronoLocalDate fecha = this.date(prolepticYear, month, dayOfMonth);
        long dia = fecha.toEpochDay();
        long segundos = dia * 86400L + (long) (hour * 3600 + minute * 60 + second);
        return segundos - (long) zoneOffset.getTotalSeconds();
    }

    /** El mismo, con la era y el anio de la era en vez del anio proleptico. */
    default long epochSecond(Era era, int yearOfEra, int month, int dayOfMonth, int hour, int minute,
            int second, ZoneOffset zoneOffset) {
        return this.epochSecond(this.prolepticYear(era, yearOfEra), month, dayOfMonth, hour, minute,
                second, zoneOffset);
    }

    /**
     * Si este calendario cuenta los dias igual que el ISO.
     *
     * <p>Los cuatro no-ISO de esta biblioteca son todos corrimientos del ISO --el mismo dia con otro
     * numero de anio-- salvo el hijri, que tiene su propia tabla de meses. Lo que responde no es "es
     * el ISO" sino "puedo tratar sus anios y meses como los del ISO".
     */
    default boolean isIsoBased() {
        return false;
    }

    /**
     * Reconstruye una fecha a partir de campos sueltos, resolviendo lo que se contradiga segun
     * `resolverStyle`.
     *
     * <p>Es lo que usa el parseo: un formateador junta `ERA`, `YEAR_OF_ERA`, `MONTH_OF_YEAR`... y
     * alguien tiene que decidir que combinacion gana y que hacer con un 31 de febrero.
     */
    ChronoLocalDate resolveDate(Map<TemporalField, Long> fieldValues, java.time.format.ResolverStyle resolverStyle);

    // ---- busqueda ---------------------------------------------------------------------------------

    /** El calendario que `temporal` declara; el ISO si no declara ninguno. */
    static Chronology from(TemporalAccessor temporal) {
        if (temporal == null) {
            throw new NullPointerException("temporal");
        }
        Chronology chrono = temporal.query(java.time.temporal.TemporalQueries.chronology());
        if (chrono != null) {
            return chrono;
        }
        return IsoChronology.INSTANCE;
    }

    /**
     * El calendario de ese id (`"ISO"`, `"Minguo"`...) o de ese tipo CLDR (`"iso8601"`, `"roc"`...).
     *
     * @throws java.time.DateTimeException si no hay ninguno con ese nombre
     */
    static Chronology of(String id) {
        if (id == null) {
            throw new NullPointerException("id");
        }
        for (Chronology c : ChronologyTable.TODAS) {
            if (id.equals(c.getId()) || id.equals(c.getCalendarType())) {
                return c;
            }
        }
        throw new java.time.DateTimeException("Unknown chronology: " + id);
    }

    /**
     * El calendario que pide ese locale por su extension Unicode `ca`, o el ISO si no pide ninguno.
     *
     * <p>Solo se mira la extension explicita --`th-TH-u-ca-buddhist`--. El JDK ademas tiene un mapa
     * de "que calendario usa por defecto cada region", que otra vez son datos del CLDR y no codigo;
     * sin ese mapa, `Locale.forLanguageTag("th-TH")` da ISO aca y budista en el JDK. Se prefiere esa
     * diferencia, que es visible y esta escrita, a inventar un mapa parcial que acierte a veces.
     */
    static Chronology ofLocale(java.util.Locale locale) {
        if (locale == null) {
            throw new NullPointerException("locale");
        }
        String ca = locale.getUnicodeLocaleType("ca");
        if (ca == null || "iso8601".equals(ca)) {
            return IsoChronology.INSTANCE;
        }
        return Chronology.of(ca);
    }

    /** Todos los calendarios disponibles. */
    static Set<Chronology> getAvailableChronologies() {
        return new java.util.HashSet<Chronology>(ChronologyTable.TODAS);
    }
}

// La lista de calendarios, aparte porque una interfaz no puede tener campos privados y uno `public`
// seria un miembro que el JDK no tiene.
final class ChronologyTable {

    // `List` y no un array: `getAvailableChronologies` devuelve una copia y `of` solo la recorre, asi
    // que nadie puede modificarla desde afuera.
    static final List<Chronology> TODAS = crear();

    private ChronologyTable() {
    }

    private static List<Chronology> crear() {
        List<Chronology> todas = new ArrayList<Chronology>();
        todas.add(IsoChronology.INSTANCE);
        todas.add(HijrahChronology.INSTANCE);
        todas.add(JapaneseChronology.INSTANCE);
        todas.add(MinguoChronology.INSTANCE);
        todas.add(ThaiBuddhistChronology.INSTANCE);
        return todas;
    }
}
