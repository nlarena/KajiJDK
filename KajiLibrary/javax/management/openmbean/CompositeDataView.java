package javax.management.openmbean;

/**
 * Lo que implementa una clase que quiere elegir cómo se convierte a {@link CompositeData}.
 *
 * <p>Sin esto, la conversión de un objeto a datos abiertos la deduce el framework de MXBean a
 * partir de los getters. Una clase que implementa esta interfaz toma el control: se le pide el
 * {@link CompositeType} que el framework calculó y ella devuelve el valor.
 *
 * <p>El caso que lo justifica es una clase cuyo estado útil no coincide con sus getters --por
 * ejemplo, una que quiere exponer un campo derivado y ocultar tres internos--.
 */
public interface CompositeDataView {

    /** Este objeto como un valor compuesto de ese tipo. */
    CompositeData toCompositeData(CompositeType ct);
}
