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
// Sobre `getDisplayName(TextStyle, Locale)`: esta, y devuelve el **valor numerico**. Ver su javadoc,
// que explica por que eso no es una mentira sino la rama que el contrato define para cuando no hay
// datos de texto.
public interface Era extends TemporalAccessor, TemporalAdjuster {

    int getValue();

    /**
     * El nombre de esta era para mostrarle a alguien.
     *
     * <p>Devuelve **el valor numerico**, que es lo que el contrato manda cuando no hay un nombre
     * para el estilo y el locale pedidos: <i>"If no textual mapping is found then the numeric value
     * is returned"</i>.
     *
     * <p>**Esta biblioteca no trae los datos de texto del CLDR**, asi que esa rama se toma
     * **siempre**, para cualquier locale. La diferencia con el JDK es concreta:
     * `IsoEra.CE.getDisplayName(FULL, ENGLISH)` da `"1"` aca y `"AD"` en el JDK.
     *
     * <p>Que esto se pueda escribir --y que antes se hubiera dejado afuera-- es por un detalle que
     * vale la pena anotar: el contrato **define** que hacer cuando no hay nombre. Lo que seria
     * mentir es inventar uno. Y el numero **se anuncia solo**: nadie confunde `"1"` con un nombre
     * traducido, mientras que un `"CE"` devuelto para un locale frances pasaria por bueno.
     *
     * @throws NullPointerException si `style` o `locale` son `null`
     */
    default String getDisplayName(java.time.format.TextStyle style, java.util.Locale locale) {
        if (style == null) {
            throw new NullPointerException("style");
        }
        if (locale == null) {
            throw new NullPointerException("locale");
        }
        return Integer.toString(this.getValue());
    }

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
