package org.w3c.dom.css;

/**
 * Una regla `@` que esta implementacion no reconoce.
 *
 * <p>No agrega ningun miembro, y eso es lo que dice: de una regla desconocida se conserva el texto
 * --que esta en `getCssText`, heredado-- y nada mas. Existir como tipo propio es lo que permite
 * conservarla en la hoja en vez de descartarla.
 */
public interface CSSUnknownRule extends CSSRule {
}
