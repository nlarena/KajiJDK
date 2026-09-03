package org.w3c.dom.ranges;

/**
 * KajiLibrary's org.w3c.dom.ranges.DocumentRange -- la fabrica de rangos.
 *
 * <p>La implementa el {@code Document}, por lo mismo que {@code DocumentTraversal}: un rango queda
 * atado a su documento --se ajusta cuando el documento cambia-- y para eso el documento tiene que
 * saber que existe.
 *
 * <p>Un {@code Document} que no soporte rangos no implementa esta interfaz; se pregunta con
 * {@code hasFeature("Range", "2.0")}.
 */
public interface DocumentRange {

    /**
     * Un rango nuevo, con los dos extremos puestos al principio del documento -- o sea, colapsado.
     */
    Range createRange();
}
