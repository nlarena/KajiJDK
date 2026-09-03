package org.w3c.dom.css;

/**
 * Un `counter()` o `counters()` de la propiedad `content`.
 *
 * <p>`getSeparator` es lo que distingue las dos formas: `counter()` no tiene separador y devuelve
 * nulo; `counters()` si lo tiene, y es lo que se intercala entre los niveles de un contador
 * anidado.
 */
public interface Counter {

    /** El nombre del contador. */
    String getIdentifier();

    /** El estilo de numeracion --`decimal`, `lower-roman`--. */
    String getListStyle();

    /** El separador de `counters()`, o nulo si es un `counter()` simple. */
    String getSeparator();
}
