package java.time.format;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.Chronology;
import java.time.chrono.IsoChronology;
import java.time.temporal.ChronoField;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQueries;
import java.time.temporal.TemporalQuery;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.time.temporal.ValueRange;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

// Lo que quedo de un parseo: los campos que el texto traia, ya resueltos.
//
// No es una fecha ni una hora: es la bolsa intermedia, y existe porque el que parsea no sabe --ni
// tiene por que saber-- en que clase van a entrar los campos. `LocalDate.from(esto)`, `Year.from(esto)`
// y `OffsetTime.from(esto)` sacan cada uno lo suyo de la misma bolsa.
//
// Los campos **crudos** se conservan ademas de los resueltos, y eso importa: `Year.from` lee `YEAR`
// directo, asi que si la resolucion los consumiera, un patron de `"yyyy"` no daria un `Year`.
//
// **Resolver es donde se decide que significa el texto**, y por eso `ResolverStyle` esta aca y no en
// el lector. El lector solo sabe que leyo `02` donde iba el mes; que `2023-02-30` sea un error
// (`STRICT`), el 28 de febrero (`SMART`) o el 2 de marzo (`LENIENT`) es una pregunta posterior, y la
// respuesta la da la cronologia, no el patron.
final class Parsed implements TemporalAccessor {

    private final Map<TemporalField, Long> campos;
    private final ZoneOffset offset;
    private final ZoneId zona;
    private final Chronology cronologia;
    private final LocalDate fecha;
    private final LocalTime hora;
    private final Period exceso;
    private final Boolean bisiesto;

    Parsed(CtxParseo ctx, ResolverStyle estilo, Set<TemporalField> soloEstos, ZoneId zonaPorDefecto,
            Chronology cronologiaPorDefecto) {
        Map<TemporalField, Long> m = ctx.campos;
        if (soloEstos != null) {
            // `withResolverFields`: los campos que no estan en el juego se sacan **antes** de
            // resolver. Es la unica forma de desempatar un texto que trae informacion redundante y
            // contradictoria --anio+dia-del-anio contra anio+mes+dia-- diciendo cual de las dos vale.
            Map<TemporalField, Long> filtrado = new HashMap<TemporalField, Long>();
            Iterator<Map.Entry<TemporalField, Long>> it = m.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<TemporalField, Long> e = it.next();
                if (soloEstos.contains(e.getKey())) {
                    filtrado.put(e.getKey(), e.getValue());
                }
            }
            m = filtrado;
        }
        this.campos = m;
        this.offset = ctx.offset;
        // Sin zona escrita pero con desplazamiento, la zona **es** el desplazamiento: es lo unico
        // que se sabe del lugar, y es cierto. Y si el formateador traia una zona por `withZone`, esa
        // es la que vale cuando el texto no dijo nada.
        ZoneId z = ctx.zona;
        if (z == null) {
            z = ctx.offset;
        }
        if (z == null) {
            z = zonaPorDefecto;
        }
        this.zona = z;
        Chronology c = ctx.cronologia;
        if (c == null) {
            c = cronologiaPorDefecto;
        }
        if (c == null) {
            c = IsoChronology.INSTANCE;
        }
        this.cronologia = c;

        // `y` deja `YEAR_OF_ERA`, y casi todo lo que viene despues quiere `YEAR`: `Year.from`,
        // `YearMonth.from` y la resolucion de la fecha. La conversion se hace **antes** y se deja
        // anotada en el mapa, no adentro de la copia que consume la cronologia, porque un patron de
        // `"yyyy-MM"` no llega a formar una fecha y aun asi tiene que dar un `YearMonth`.
        //
        // **Menos en modo estricto y sin era escrita**, donde el anio de la era no alcanza: `2021`
        // sin decir si es de esta era o de la anterior no designa un anio, y suponer la corriente es
        // exactamente la clase de suposicion que `STRICT` existe para no hacer. Es la razon por la
        // que `uuuu` --anio proleptico-- es el que hay que usar con `STRICT`, y no `yyyy`.
        boolean anioSinEra = estilo == ResolverStyle.STRICT
                && this.campos.containsKey(ChronoField.YEAR_OF_ERA)
                && !this.campos.containsKey(ChronoField.ERA)
                && !this.campos.containsKey(ChronoField.YEAR);
        if (!anioSinEra && !this.campos.containsKey(ChronoField.YEAR)
                && this.campos.containsKey(ChronoField.YEAR_OF_ERA)) {
            long yoe = this.campos.get(ChronoField.YEAR_OF_ERA).longValue();
            Long era = this.campos.get(ChronoField.ERA);
            java.time.chrono.Era e;
            if (era != null) {
                e = this.cronologia.eraOf((int) era.longValue());
            } else {
                // Sin era escrita, la corriente: es lo que quiere decir el que escribe un anio solo.
                java.util.List<java.time.chrono.Era> eras = this.cronologia.eras();
                e = eras.get(eras.size() - 1);
            }
            this.campos.put(ChronoField.YEAR,
                    Long.valueOf((long) this.cronologia.prolepticYear(e, (int) yoe)));
        }

        int[] excesoDias = new int[1];
        this.hora = resolverHora(this.campos, estilo, ctx.excesoDia, excesoDias);
        this.exceso = excesoDias[0] == 0 ? Period.ZERO : Period.ofDays(excesoDias[0]);
        this.bisiesto = Boolean.valueOf(ctx.segundoBisiesto);
        ChronoLocalDate cruda = resolverFecha(this.campos, estilo, this.cronologia, anioSinEra);
        // Se **normaliza por el dia epoch** antes de mirarla. El `LocalDate.of` de esta biblioteca
        // no valida el dia contra el largo del mes: `LocalDate.of(2023, 2, 30)` devuelve un objeto
        // que se imprime `2023-02-30` y cuyo `toEpochDay()` es el del 2 de marzo. Pasar por el dia
        // epoch convierte ese objeto en la fecha que de verdad designa, que es la unica sobre la que
        // tiene sentido comprobar nada.
        ChronoLocalDate resuelta = cruda == null ? null
                : this.cronologia.dateEpochDay(cruda.toEpochDay());
        this.fecha = resuelta == null ? null : LocalDate.ofEpochDay(resuelta.toEpochDay());

        if (resuelta != null && estilo == ResolverStyle.STRICT) {
            // **La comprobacion cruzada.** En modo estricto, la fecha resuelta tiene que decir lo
            // mismo que decia el texto. Existe porque el camino estricto de la cronologia termina en
            // `LocalDate.of(anio, mes, dia)`, y el `LocalDate.of` de esta biblioteca **no valida el
            // dia contra el largo del mes**: `LocalDate.of(2023, 2, 30)` devuelve el 2 de marzo en
            // vez de tirar. Sin esta vuelta, `ISO_LOCAL_DATE.parse("2023-02-30")` daria una fecha
            // --equivocada y silenciosa-- donde el JDK da un error. Queda anotado: el arreglo de
            // fondo va en `LocalDate.of`, y el dia que este se puede sacar.
            this.cruzar(resuelta, ChronoField.YEAR);
            this.cruzar(resuelta, ChronoField.MONTH_OF_YEAR);
            this.cruzar(resuelta, ChronoField.DAY_OF_MONTH);
            this.cruzar(resuelta, ChronoField.DAY_OF_YEAR);
            this.cruzar(resuelta, ChronoField.DAY_OF_WEEK);
        }
        if (resuelta != null) {
            // Los campos que la fecha resuelta implica se depositan de vuelta. Sin esto, un patron de
            // `"yyyy-DDD"` --anio y dia del anio-- daria un `LocalDate` pero no un `YearMonth`, y el
            // texto traia lo que hacia falta para los dos.
            this.campos.put(ChronoField.EPOCH_DAY, Long.valueOf(resuelta.toEpochDay()));
            this.anotar(resuelta, ChronoField.YEAR);
            this.anotar(resuelta, ChronoField.MONTH_OF_YEAR);
            this.anotar(resuelta, ChronoField.DAY_OF_MONTH);
            this.anotar(resuelta, ChronoField.DAY_OF_YEAR);
            this.anotar(resuelta, ChronoField.DAY_OF_WEEK);
        }
        if (this.hora != null) {
            this.campos.put(ChronoField.NANO_OF_DAY, Long.valueOf(this.hora.toNanoOfDay()));
            this.campos.put(ChronoField.HOUR_OF_DAY, Long.valueOf((long) this.hora.getHour()));
            this.campos.put(ChronoField.MINUTE_OF_HOUR, Long.valueOf((long) this.hora.getMinute()));
            this.campos.put(ChronoField.SECOND_OF_MINUTE, Long.valueOf((long) this.hora.getSecond()));
            this.campos.put(ChronoField.NANO_OF_SECOND, Long.valueOf((long) this.hora.getNano()));
        }
        if (this.offset != null) {
            this.campos.put(ChronoField.OFFSET_SECONDS,
                    Long.valueOf((long) this.offset.getTotalSeconds()));
        }
        // Un instante leido --`ISO_INSTANT`-- trae solo `INSTANT_SECONDS`. Desplegarlo a fecha y hora
        // locales es lo que permite que el mismo texto de tambien un `OffsetDateTime`, y es una
        // deduccion exacta: con el desplazamiento a mano no hay ninguna eleccion que hacer.
        Long instante = this.campos.get(ChronoField.INSTANT_SECONDS);
        if (instante != null && resuelta == null && this.offset != null) {
            LocalDateTime ldt = LocalDateTime.ofEpochSecond(instante.longValue(), 0, this.offset);
            Long nano = this.campos.get(ChronoField.NANO_OF_SECOND);
            this.campos.put(ChronoField.EPOCH_DAY, Long.valueOf(ldt.toLocalDate().toEpochDay()));
            this.campos.put(ChronoField.NANO_OF_DAY, Long.valueOf(ldt.toLocalTime().toNanoOfDay()
                    + (nano == null ? 0L : nano.longValue())));
        }
    }

    private void cruzar(ChronoLocalDate d, ChronoField campo) {
        Long leido = this.campos.get(campo);
        if (leido != null && d.isSupported(campo) && d.getLong(campo) != leido.longValue()) {
            throw new DateTimeException("Conflict found: " + campo + " " + leido
                    + " differs from " + campo + " " + d.getLong(campo) + " derived from " + d);
        }
    }

    private void anotar(ChronoLocalDate d, ChronoField campo) {
        if (d.isSupported(campo)) {
            this.campos.put(campo, Long.valueOf(d.getLong(campo)));
        }
    }

    LocalDate fecha() {
        return this.fecha;
    }

    LocalTime hora() {
        return this.hora;
    }

    Period exceso() {
        return this.exceso;
    }

    Boolean bisiesto() {
        return this.bisiesto;
    }

    private static Long sacar(Map<TemporalField, Long> campos, TemporalField campo) {
        return campos.remove(campo);
    }

    // Los campos de hora se derrumban hacia `HOUR_OF_DAY`/`MINUTE_OF_HOUR`/`SECOND_OF_MINUTE`/
    // `NANO_OF_SECOND`, que son los cuatro que un `LocalTime` entiende, y recien despues se arma la
    // hora. El orden importa: `NANO_OF_DAY` gana sobre los sueltos porque es mas especifico.
    private static LocalTime resolverHora(Map<TemporalField, Long> campos, ResolverStyle estilo,
            boolean excesoDelLector, int[] excesoDias) {
        Map<TemporalField, Long> t = new HashMap<TemporalField, Long>(campos);
        boolean laxo = estilo == ResolverStyle.LENIENT;

        Long nanoDelDia = sacar(t, ChronoField.NANO_OF_DAY);
        if (nanoDelDia != null) {
            long v = nanoDelDia.longValue();
            t.put(ChronoField.HOUR_OF_DAY, Long.valueOf(v / 3600000000000L));
            t.put(ChronoField.MINUTE_OF_HOUR, Long.valueOf(v / 60000000000L % 60L));
            t.put(ChronoField.SECOND_OF_MINUTE, Long.valueOf(v / 1000000000L % 60L));
            t.put(ChronoField.NANO_OF_SECOND, Long.valueOf(v % 1000000000L));
        }
        Long microDelDia = sacar(t, ChronoField.MICRO_OF_DAY);
        if (microDelDia != null && !t.containsKey(ChronoField.HOUR_OF_DAY)) {
            long v = microDelDia.longValue();
            t.put(ChronoField.HOUR_OF_DAY, Long.valueOf(v / 3600000000L));
            t.put(ChronoField.MINUTE_OF_HOUR, Long.valueOf(v / 60000000L % 60L));
            t.put(ChronoField.SECOND_OF_MINUTE, Long.valueOf(v / 1000000L % 60L));
            t.put(ChronoField.NANO_OF_SECOND, Long.valueOf(v % 1000000L * 1000L));
        }
        Long miliDelDia = sacar(t, ChronoField.MILLI_OF_DAY);
        if (miliDelDia != null && !t.containsKey(ChronoField.HOUR_OF_DAY)) {
            long v = miliDelDia.longValue();
            t.put(ChronoField.HOUR_OF_DAY, Long.valueOf(v / 3600000L));
            t.put(ChronoField.MINUTE_OF_HOUR, Long.valueOf(v / 60000L % 60L));
            t.put(ChronoField.SECOND_OF_MINUTE, Long.valueOf(v / 1000L % 60L));
            t.put(ChronoField.NANO_OF_SECOND, Long.valueOf(v % 1000L * 1000000L));
        }
        Long segDelDia = sacar(t, ChronoField.SECOND_OF_DAY);
        if (segDelDia != null && !t.containsKey(ChronoField.HOUR_OF_DAY)) {
            long v = segDelDia.longValue();
            t.put(ChronoField.HOUR_OF_DAY, Long.valueOf(v / 3600L));
            t.put(ChronoField.MINUTE_OF_HOUR, Long.valueOf(v / 60L % 60L));
            t.put(ChronoField.SECOND_OF_MINUTE, Long.valueOf(v % 60L));
        }
        Long minDelDia = sacar(t, ChronoField.MINUTE_OF_DAY);
        if (minDelDia != null && !t.containsKey(ChronoField.HOUR_OF_DAY)) {
            long v = minDelDia.longValue();
            t.put(ChronoField.HOUR_OF_DAY, Long.valueOf(v / 60L));
            t.put(ChronoField.MINUTE_OF_HOUR, Long.valueOf(v % 60L));
        }
        Long micro = sacar(t, ChronoField.MICRO_OF_SECOND);
        if (micro != null && !t.containsKey(ChronoField.NANO_OF_SECOND)) {
            t.put(ChronoField.NANO_OF_SECOND, Long.valueOf(micro.longValue() * 1000L));
        }
        Long mili = sacar(t, ChronoField.MILLI_OF_SECOND);
        if (mili != null && !t.containsKey(ChronoField.NANO_OF_SECOND)) {
            t.put(ChronoField.NANO_OF_SECOND, Long.valueOf(mili.longValue() * 1000000L));
        }

        // El reloj de doce horas. `12 AM` es la hora 0 y `12 PM` la hora 12: la conversion pasa por
        // `HOUR_OF_AMPM` --que va 0..11-- justamente para no tener que tratar el 12 aparte dos veces.
        Long relojDia = sacar(t, ChronoField.CLOCK_HOUR_OF_DAY);
        if (relojDia != null) {
            long v = relojDia.longValue();
            if (estilo == ResolverStyle.STRICT && (v < 1L || v > 24L)) {
                throw new DateTimeException("Invalid value for CLOCK_HOUR_OF_DAY: " + v);
            }
            t.put(ChronoField.HOUR_OF_DAY, Long.valueOf(v == 24L ? 0L : v));
        }
        Long relojAmpm = sacar(t, ChronoField.CLOCK_HOUR_OF_AMPM);
        if (relojAmpm != null) {
            long v = relojAmpm.longValue();
            if (estilo == ResolverStyle.STRICT && (v < 1L || v > 12L)) {
                throw new DateTimeException("Invalid value for CLOCK_HOUR_OF_AMPM: " + v);
            }
            t.put(ChronoField.HOUR_OF_AMPM, Long.valueOf(v == 12L ? 0L : v));
        }
        Long ampm = sacar(t, ChronoField.AMPM_OF_DAY);
        Long horaAmpm = sacar(t, ChronoField.HOUR_OF_AMPM);
        if (ampm != null && horaAmpm != null) {
            t.put(ChronoField.HOUR_OF_DAY,
                    Long.valueOf(ampm.longValue() * 12L + horaAmpm.longValue()));
        } else if (horaAmpm != null && !t.containsKey(ChronoField.HOUR_OF_DAY)) {
            t.put(ChronoField.HOUR_OF_DAY, horaAmpm);
        }

        Long h = t.get(ChronoField.HOUR_OF_DAY);
        if (h == null) {
            return null;
        }
        long hora = h.longValue();
        long minuto = valor(t, ChronoField.MINUTE_OF_HOUR);
        long segundo = valor(t, ChronoField.SECOND_OF_MINUTE);
        long nano = valor(t, ChronoField.NANO_OF_SECOND);

        // `24:00:00` no es una hora: es la medianoche del dia siguiente. Se guarda como `00:00` mas
        // un dia de exceso --que `parsedExcessDays` publica-- en vez de rechazarse, porque ISO-8601
        // la escribe y rechazarla haria ilegible un texto valido.
        int dias = excesoDelLector ? 1 : 0;
        if (hora == 24L && minuto == 0L && segundo == 0L && nano == 0L
                && estilo != ResolverStyle.STRICT) {
            hora = 0L;
            dias = dias + 1;
        }
        if (laxo) {
            // En modo laxo los desbordes se acumulan hacia arriba en vez de ser un error.
            long nanoTotal = hora * 3600000000000L + minuto * 60000000000L
                    + segundo * 1000000000L + nano;
            long diasEnteros = Math.floorDiv(nanoTotal, 86400000000000L);
            long resto = Math.floorMod(nanoTotal, 86400000000000L);
            dias = dias + (int) diasEnteros;
            excesoDias[0] = dias;
            return LocalTime.ofNanoOfDay(resto);
        }
        // El segundo intercalar existe en el texto y no en el reloj: se lee como `:59` y se anota.
        if (segundo == 60L && estilo != ResolverStyle.STRICT) {
            segundo = 59L;
        }
        excesoDias[0] = dias;
        return LocalTime.of((int) hora, (int) minuto, (int) segundo, (int) nano);
    }

    private static long valor(Map<TemporalField, Long> t, TemporalField campo) {
        Long v = t.get(campo);
        return v == null ? 0L : v.longValue();
    }

    // Se resuelve sobre una **copia**: el resolvedor consume el mapa que le dan, y los campos crudos
    // tienen que seguir estando para el que los lea directo.
    private static ChronoLocalDate resolverFecha(Map<TemporalField, Long> campos,
            ResolverStyle estilo, Chronology cronologia, boolean anioSinEra) {
        Map<TemporalField, Long> copia = new HashMap<TemporalField, Long>(campos);
        if (anioSinEra) {
            // La cronologia supondria la era corriente igual que arriba; se le saca el campo para
            // que no lo haga.
            copia.remove(ChronoField.YEAR_OF_ERA);
        }
        ChronoLocalDate d = cronologia.resolveDate(copia, estilo);
        if (d == null) {
            d = resolverSemanaIso(campos);
        }
        return d;
    }

    // La fecha de `ISO_WEEK_DATE`: `2024-W07-3`.
    //
    // Vive aca y no en `IsoFields` porque los `resolve` de `TemporalField` no estan implementados en
    // `java.time.temporal` --el paquete esta cerrado al 100 % de su API y no se toca en esta tanda--
    // y sin esto `ISO_WEEK_DATE` escribiria un texto que despues no puede releer. Queda anotado como
    // lo que hay que mover cuando `IsoField.resolve` exista.
    private static LocalDate resolverSemanaIso(Map<TemporalField, Long> campos) {
        Long anio = campos.get(IsoFields.WEEK_BASED_YEAR);
        Long semana = campos.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        Long dia = campos.get(ChronoField.DAY_OF_WEEK);
        if (anio == null || semana == null || dia == null) {
            return null;
        }
        // El 4 de enero cae siempre en la semana 1 --esa es la definicion ISO-- asi que el lunes de
        // la semana 1 es el 4 de enero corrido hacia atras hasta el lunes.
        LocalDate cuatroDeEnero = LocalDate.of((int) anio.longValue(), 1, 4);
        LocalDate lunesUno = cuatroDeEnero.minusDays(
                (long) (cuatroDeEnero.getDayOfWeek().getValue() - 1));
        return lunesUno.plusDays((semana.longValue() - 1L) * 7L + dia.longValue() - 1L);
    }

    public boolean isSupported(TemporalField field) {
        return field != null && this.campos.containsKey(field);
    }

    public long getLong(TemporalField field) {
        Long v = this.campos.get(field);
        if (v == null) {
            throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
        }
        return v.longValue();
    }

    public int get(TemporalField field) {
        long v = this.getLong(field);
        ValueRange rango = field.range();
        return (int) rango.checkValidIntValue(v, field);
    }

    public <R> R query(TemporalQuery<R> query) {
        if (query == TemporalQueries.localDate()) {
            return (R) this.fecha;
        }
        if (query == TemporalQueries.localTime()) {
            return (R) this.hora;
        }
        if (query == TemporalQueries.offset()) {
            return (R) this.offset;
        }
        if (query == TemporalQueries.zone() || query == TemporalQueries.zoneId()) {
            return (R) this.zona;
        }
        if (query == TemporalQueries.chronology()) {
            return (R) this.cronologia;
        }
        return query.queryFrom(this);
    }

    public String toString() {
        return this.campos.toString();
    }
}

// `DateTimeFormatter.parsedExcessDays()`: el dia que sobro de un `24:00`.
//
// Es una clase y no una lambda porque la consulta tiene que ser **el mismo objeto siempre**: los
// `query` de esta biblioteca se comparan por identidad, y una lambda nueva en cada llamada nunca
// coincidiria con la que el `Parsed` reconoce.
final class ConsultaExceso implements TemporalQuery<Period> {

    public Period queryFrom(TemporalAccessor temporal) {
        if (temporal instanceof Parsed) {
            return ((Parsed) temporal).exceso();
        }
        return Period.ZERO;
    }
}

// `DateTimeFormatter.parsedLeapSecond()`: si el texto decia `:60`.
final class ConsultaBisiesto implements TemporalQuery<Boolean> {

    public Boolean queryFrom(TemporalAccessor temporal) {
        if (temporal instanceof Parsed) {
            return ((Parsed) temporal).bisiesto();
        }
        return Boolean.FALSE;
    }
}
