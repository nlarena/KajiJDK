package java.time.temporal;

// KajiLibrary's java.time.temporal.TemporalAccessor -- acceso de solo lectura a una fecha u hora,
// por campo. Es la base de todos los tipos valor (LocalDate, Instant, ...).
public interface TemporalAccessor {

    boolean isSupported(TemporalField field);

    long getLong(TemporalField field);

    // The field's value as an int (throws if it overflows, in the JDK; we just narrow).
    default int get(TemporalField field) {
        return (int) this.getLong(field);
    }

    /**
     * El rango de valores validos de `field` **en este** temporal.
     *
     * <p>Afinado por el propio objeto: `DAY_OF_MONTH` sobre un febrero bisiesto da 1..29. El default
     * delega en el campo, que es lo que sabe; un tipo que pueda afinar mas lo sobreescribe.
     */
    default ValueRange range(TemporalField field) {
        if (field instanceof ChronoField) {
            if (this.isSupported(field)) {
                return field.range();
            }
            throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
        }
        return field.rangeRefinedBy(this);
    }

    /**
     * Consulta este temporal con una estrategia.
     *
     * <p>Es el punto de extension del paquete: en vez de un metodo por cada cosa que se pueda querer
     * saber, se pasa la pregunta. `TemporalQueries` trae las estandar.
     */
    default <R> R query(TemporalQuery<R> query) {
        return query.queryFrom(this);
    }
}
