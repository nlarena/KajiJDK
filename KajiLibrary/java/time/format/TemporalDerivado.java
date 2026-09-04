package java.time.format;

import java.time.ZoneId;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.Chronology;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQueries;
import java.time.temporal.TemporalQuery;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.time.temporal.ValueRange;

// La vista del objeto que el formateador ve mientras escribe: el original, mas los campos que se
// **deducen** de el, mas los reemplazos de `withZone` y `withChronology`.
//
// **POR QUE HACE FALTA DEDUCIR, Y POR QUE ES HONESTO.** El `LocalTime` de esta biblioteca responde
// cuatro campos: hora, minuto, segundo y nano. No responde `AMPM_OF_DAY` ni `CLOCK_HOUR_OF_AMPM`, que
// es lo que un patron `hh:mm a` necesita. Pero esos dos **no son informacion nueva**: son la hora
// dividida por doce y la hora modulo doce, dos cuentas que la especificacion fija y que no admiten
// otra respuesta. Deducirlas no inventa nada; negarse a hacerlo dejaria el patron de doce horas sin
// funcionar por una razon que no tiene nada que ver con el formateo.
//
// (El JDK deduce lo mismo, solo que adentro de `LocalTime`. Que aca este del lado del formateador es
// una diferencia de **donde**, no de que: `java.time` esta cerrado al 100 % de su API y no se toca en
// esta tanda, asi que la deduccion vive del lado que si se puede escribir. Queda anotado.)
//
// Lo que **no** se deduce es lo que no se sigue de lo que hay: un `LocalTime` no da una fecha, y por
// eso `isSupported(YEAR)` sigue siendo `false` y el formateo falla en vez de escribir un anio
// inventado.
final class TemporalDerivado implements TemporalAccessor {

    private final TemporalAccessor base;
    private final Chronology cronologia;
    private final ZoneId zona;
    // La fecha traducida al calendario de `withChronology`, cuando hay uno y no coincide con el del
    // original. Los campos de fecha salen de aca y los de hora del original: cambiar de calendario
    // mueve el anio y el mes, no la hora.
    private final ChronoLocalDate fechaConvertida;

    TemporalDerivado(TemporalAccessor base, Chronology cronologia, ZoneId zona) {
        this.base = base;
        this.cronologia = cronologia;
        this.zona = zona;
        ChronoLocalDate convertida = null;
        if (cronologia != null && base.isSupported(ChronoField.EPOCH_DAY)) {
            Chronology actual = base.query(TemporalQueries.chronology());
            // Solo se traduce cuando se sabe **de que** calendario se viene. Un objeto que no declara
            // ninguno no se toca: convertirlo supondria que era ISO, y esa suposicion es
            // precisamente la que no se puede hacer.
            if (actual != null && !cronologia.equals(actual)) {
                convertida = cronologia.dateEpochDay(base.getLong(ChronoField.EPOCH_DAY));
            }
        }
        this.fechaConvertida = convertida;
    }

    public boolean isSupported(TemporalField campo) {
        if (campo == null) {
            return false;
        }
        if (this.fechaConvertida != null && campo.isDateBased()
                && this.fechaConvertida.isSupported(campo)) {
            return true;
        }
        if (this.base.isSupported(campo)) {
            return true;
        }
        if (campo instanceof ChronoField) {
            return this.deducible((ChronoField) campo);
        }
        // Un campo que no es de `ChronoField` --los de `IsoFields`, los de `JulianFields`-- sabe
        // decir por si mismo si puede calcularse. Preguntarselo es lo que hace que `ISO_WEEK_DATE`
        // pueda escribir un `LocalDate`, que no conoce el anio-de-semanas pero tiene con que.
        return campo.isSupportedBy(this.base);
    }

    private boolean deducible(ChronoField campo) {
        if (campo == ChronoField.ERA || campo == ChronoField.YEAR_OF_ERA
                || campo == ChronoField.PROLEPTIC_MONTH) {
            return this.base.isSupported(ChronoField.YEAR)
                    && (campo != ChronoField.PROLEPTIC_MONTH
                        || this.base.isSupported(ChronoField.MONTH_OF_YEAR));
        }
        if (campo == ChronoField.MILLI_OF_SECOND || campo == ChronoField.MICRO_OF_SECOND) {
            return this.base.isSupported(ChronoField.NANO_OF_SECOND);
        }
        if (campo == ChronoField.INSTANT_SECONDS) {
            // Un `Instant` **es** un instante y sin embargo el `Instant` de esta biblioteca contesta
            // `false` a `INSTANT_SECONDS` --su `isSupported` solo conoce `NANO_OF_SECOND`--, asi que
            // `ISO_INSTANT.format(instante)` no funcionaria. El valor se toma de `getEpochSecond()`,
            // que es el mismo numero por la puerta de al lado. Queda anotado como bug de `java.time`:
            // el arreglo va en `Instant.isSupported`/`getLong`, no aca.
            return this.base instanceof java.time.Instant
                    || (this.base.isSupported(ChronoField.EPOCH_DAY)
                        && this.base.isSupported(ChronoField.HOUR_OF_DAY)
                        && this.base.isSupported(ChronoField.OFFSET_SECONDS));
        }
        if (campo == ChronoField.AMPM_OF_DAY || campo == ChronoField.HOUR_OF_AMPM
                || campo == ChronoField.CLOCK_HOUR_OF_AMPM
                || campo == ChronoField.CLOCK_HOUR_OF_DAY) {
            return this.base.isSupported(ChronoField.HOUR_OF_DAY);
        }
        if (campo == ChronoField.MINUTE_OF_DAY || campo == ChronoField.SECOND_OF_DAY
                || campo == ChronoField.NANO_OF_DAY || campo == ChronoField.MILLI_OF_DAY
                || campo == ChronoField.MICRO_OF_DAY) {
            return this.base.isSupported(ChronoField.HOUR_OF_DAY)
                    && this.base.isSupported(ChronoField.MINUTE_OF_HOUR);
        }
        return false;
    }

    private long deOBase(ChronoField campo, long porDefecto) {
        if (this.base.isSupported(campo)) {
            return this.base.getLong(campo);
        }
        return porDefecto;
    }

    public long getLong(TemporalField campo) {
        if (this.fechaConvertida != null && campo != null && campo.isDateBased()
                && this.fechaConvertida.isSupported(campo)) {
            return this.fechaConvertida.getLong(campo);
        }
        if (this.base.isSupported(campo)) {
            try {
                return this.base.getLong(campo);
            } catch (java.time.DateTimeException e) {
                // `isSupported` dijo que si y `getLong` tiro. No es imposible: `OffsetDateTime` de
                // esta biblioteca contesta `true` para **todo** `ChronoField` --su condicion es
                // `field != INSTANT_SECONDS || true`, que es siempre cierta-- y despues no sabe
                // devolver `YEAR_OF_ERA`. Antes que propagar el error se intenta deducirlo, que da
                // el valor correcto. Queda anotado como bug de `java.time`.
                if (!(campo instanceof ChronoField) || !this.deducible((ChronoField) campo)) {
                    throw e;
                }
            }
        }
        if (campo instanceof ChronoField && this.deducible((ChronoField) campo)) {
            ChronoField c = (ChronoField) campo;
            if (c == ChronoField.ERA) {
                return this.base.getLong(ChronoField.YEAR) >= 1L ? 1L : 0L;
            }
            if (c == ChronoField.YEAR_OF_ERA) {
                long y = this.base.getLong(ChronoField.YEAR);
                return y >= 1L ? y : 1L - y;
            }
            if (c == ChronoField.PROLEPTIC_MONTH) {
                return this.base.getLong(ChronoField.YEAR) * 12L
                        + this.base.getLong(ChronoField.MONTH_OF_YEAR) - 1L;
            }
            if (c == ChronoField.MILLI_OF_SECOND) {
                return this.base.getLong(ChronoField.NANO_OF_SECOND) / 1000000L;
            }
            if (c == ChronoField.MICRO_OF_SECOND) {
                return this.base.getLong(ChronoField.NANO_OF_SECOND) / 1000L;
            }
            if (c == ChronoField.INSTANT_SECONDS) {
                if (this.base instanceof java.time.Instant) {
                    return ((java.time.Instant) this.base).getEpochSecond();
                }
                return this.base.getLong(ChronoField.EPOCH_DAY) * 86400L
                        + this.base.getLong(ChronoField.HOUR_OF_DAY) * 3600L
                        + this.deOBase(ChronoField.MINUTE_OF_HOUR, 0L) * 60L
                        + this.deOBase(ChronoField.SECOND_OF_MINUTE, 0L)
                        - this.base.getLong(ChronoField.OFFSET_SECONDS);
            }
            long hora = this.base.getLong(ChronoField.HOUR_OF_DAY);
            if (c == ChronoField.AMPM_OF_DAY) {
                return hora / 12L;
            }
            if (c == ChronoField.HOUR_OF_AMPM) {
                return hora % 12L;
            }
            if (c == ChronoField.CLOCK_HOUR_OF_AMPM) {
                long h = hora % 12L;
                return h == 0L ? 12L : h;
            }
            if (c == ChronoField.CLOCK_HOUR_OF_DAY) {
                return hora == 0L ? 24L : hora;
            }
            long nanoDelDia = hora * 3600000000000L
                    + this.base.getLong(ChronoField.MINUTE_OF_HOUR) * 60000000000L
                    + this.deOBase(ChronoField.SECOND_OF_MINUTE, 0L) * 1000000000L
                    + this.deOBase(ChronoField.NANO_OF_SECOND, 0L);
            if (c == ChronoField.NANO_OF_DAY) {
                return nanoDelDia;
            }
            if (c == ChronoField.MICRO_OF_DAY) {
                return nanoDelDia / 1000L;
            }
            if (c == ChronoField.MILLI_OF_DAY) {
                return nanoDelDia / 1000000L;
            }
            if (c == ChronoField.SECOND_OF_DAY) {
                return nanoDelDia / 1000000000L;
            }
            if (c == ChronoField.MINUTE_OF_DAY) {
                return nanoDelDia / 60000000000L;
            }
        }
        if (campo != null && !(campo instanceof ChronoField) && campo.isSupportedBy(this.base)) {
            return campo.getFrom(this.base);
        }
        throw new UnsupportedTemporalTypeException("Unsupported field: " + campo);
    }

    public int get(TemporalField campo) {
        ValueRange rango = campo.range();
        return rango.checkValidIntValue(this.getLong(campo), campo);
    }

    public <R> R query(TemporalQuery<R> consulta) {
        if (this.zona != null && (consulta == TemporalQueries.zoneId()
                || consulta == TemporalQueries.zone())) {
            return (R) this.zona;
        }
        if (this.cronologia != null && consulta == TemporalQueries.chronology()) {
            return (R) this.cronologia;
        }
        return this.base.query(consulta);
    }

    public String toString() {
        return this.base.toString();
    }
}
