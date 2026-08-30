package java.time.chrono;

import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQuery;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.time.temporal.ValueRange;

// KajiLibrary's java.time.chrono.Era -- una era de un calendario (las BCE/CE del ISO, las cinco
// imperiales del japones, y las dos de cada uno de los otros).
//
// Que una era sea un `TemporalAccessor` suena raro hasta que uno mira que campo tiene: exactamente
// uno, `ERA`, y ninguno mas. No es "una fecha con muy pocos datos" sino **un valor de un solo campo**,
// y eso es justo lo que la interfaz pide. Ser `TemporalAdjuster` sale de lo mismo: ajustar una fecha
// con una era es poner ese campo.
//
// Queda afuera `getDisplayName(TextStyle, Locale)`: los nombres traducidos de las eras son datos del
// CLDR, no codigo. Devolver "CE" para cualquier locale seria un miembro que miente sobre lo que le
// pidieron -- misma razon por la que tampoco esta `Chronology.getDisplayName`.
public interface Era extends TemporalAccessor, TemporalAdjuster {

    int getValue();

    /** Una era **solo** sabe de `ERA`. */
    default boolean isSupported(TemporalField field) {
        if (field instanceof ChronoField) {
            return field == ChronoField.ERA;
        }
        return field != null && field.isSupportedBy(this);
    }

    default ValueRange range(TemporalField field) {
        if (field == ChronoField.ERA) {
            // El rango real depende del calendario --el japones tiene cinco eras y arranca en -1--,
            // pero una `Era` suelta no sabe de cual es. El de `ChronoField` es el rango generico, que
            // es lo que el JDK devuelve aca; el ajustado lo da `Chronology.range(ERA)`.
            return field.range();
        }
        if (field instanceof ChronoField) {
            throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
        }
        return field.rangeRefinedBy(this);
    }

    default int get(TemporalField field) {
        if (field == ChronoField.ERA) {
            return this.getValue();
        }
        if (field instanceof ChronoField) {
            throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
        }
        // Ligado a una local: encadenar por un intermedio de tipo interfaz se pierde (#108).
        ValueRange rango = field.rangeRefinedBy(this);
        long valor = field.getFrom(this);
        return (int) rango.checkValidIntValue(valor, field);
    }

    default long getLong(TemporalField field) {
        if (field == ChronoField.ERA) {
            return (long) this.getValue();
        }
        if (field instanceof ChronoField) {
            throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
        }
        return field.getFrom(this);
    }

    default <R> R query(TemporalQuery<R> query) {
        if (query == java.time.temporal.TemporalQueries.precision()) {
            return (R) java.time.temporal.ChronoUnit.ERAS;
        }
        return query.queryFrom(this);
    }

    /**
     * Pone esta era en `temporal`, dejando el anio de la era como estaba.
     *
     * <p>Ojo con lo que eso significa: `fecha.with(IsoEra.BCE)` sobre el anio 2024 da el anio -2023,
     * no el -2024, porque lo que se conserva es el **anio de la era** y no el proleptico. Es lo que
     * hace el JDK y es lo unico coherente: la era y el anio de la era son un par.
     */
    default Temporal adjustInto(Temporal temporal) {
        return temporal.with(ChronoField.ERA, (long) this.getValue());
    }
}
