package org.xml.sax.ext;

import org.xml.sax.Locator;

/**
 * KajiLibrary's org.xml.sax.ext.Locator2 -- el `Locator` mas la version y la codificacion de la
 * entidad en la que el parser esta parado ahora.
 *
 * <p>No es lo mismo que "la version y la codificacion del documento", y ahi esta el detalle: un
 * documento XML 1.0 puede incluir entidades externas con su propia declaracion, y mientras se lee
 * una de ellas estos dos metodos contestan por **esa** entidad. Como el `Locator` es vivo --sigue
 * contestando por la posicion actual a medida que el analisis avanza--, las respuestas cambian
 * durante el recorrido. Quien quiera la version del documento la tiene que leer temprano, o
 * quedarsela via `ContentHandler.declaration`.
 *
 * <p>Se descubre como {@link Attributes2}: el objeto que llega a `setDocumentLocator` puede ser uno
 * de estos, y el codigo lo averigua con `instanceof`. No hay feature que lo prenda.
 *
 * <p>Las dos respuestas pueden ser `null`, y significan cosas distintas segun el metodo: en
 * `getEncoding` es "no se sabe" --pasa cuando la codificacion vino de afuera, por ejemplo de una
 * cabecera HTTP, y el parser no la propago--; en `getXMLVersion` no deberia pasar durante el
 * analisis, porque una entidad sin declaracion es 1.0 por definicion.
 */
public interface Locator2 extends Locator {

    /** `"1.0"` o `"1.1"`. Es la de la entidad actual, no necesariamente la del documento. */
    String getXMLVersion();

    /**
     * El nombre de la codificacion en uso. Si el documento lo declaro, es lo declarado; si se
     * dedujo del BOM o de los primeros bytes, es lo deducido; si vino de un `InputSource` con
     * `Reader` ya armado, el parser no la sabe y devuelve `null`.
     */
    String getEncoding();
}
