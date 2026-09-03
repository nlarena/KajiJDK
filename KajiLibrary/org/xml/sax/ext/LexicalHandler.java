package org.xml.sax.ext;

import org.xml.sax.SAXException;

/**
 * KajiLibrary's org.xml.sax.ext.LexicalHandler -- los eventos que SAX2 tira a la basura porque no
 * cambian *que* dice el documento, solo *como* estaba escrito.
 *
 * <p>Un `ContentHandler` no se entera de los comentarios, ni de donde empezaba y terminaba una
 * seccion `CDATA`, ni de que un texto vino de expandir una entidad, ni de que habia una DTD. Para
 * leer el documento eso sobra: `&lt;a&gt;x&lt;/a&gt;` y `&lt;a&gt;&lt;![CDATA[x]]&gt;&lt;/a&gt;`
 * son el mismo documento. Para **reescribirlo** no: un editor o un serializador que pierda esos
 * limites devuelve un archivo distinto del que le dieron. Esta interfaz existe para esa segunda
 * clase de consumidor.
 *
 * <p>No se instala con un `setXxxHandler` como los cuatro manejadores basicos, sino con la
 * propiedad `http://xml.org/sax/properties/lexical-handler` del `XMLReader`. Es una extension: un
 * parser conforme puede no reconocerla, y entonces tira `SAXNotRecognizedException`. Esa es la
 * diferencia practica entre el nucleo y `ext`.
 *
 * <p>Los eventos anidan de verdad, y esa es la unica forma de interpretarlos: entre `startCDATA` y
 * `endCDATA` el texto sigue llegando por `ContentHandler.characters` --aca no llega ningun texto--,
 * y entre `startEntity` y `endEntity` llegan los eventos del contenido de la entidad. El manejador
 * no recibe el contenido dos veces; recibe marcas que le dicen de donde salio.
 *
 * <p>Sobre el orden respecto de `startDocument`: `startDTD`/`endDTD` van **despues** de
 * `startDocument` y **antes** del primer evento del elemento raiz, y todo lo que el `DTDHandler` y
 * el {@link DeclHandler} reporten cae adentro de ese par. Un comentario que este afuera del
 * elemento raiz llega igual, antes o despues.
 *
 * <p><strong>En KajiLibrary nadie produce estos eventos todavia</strong>, porque el arbol no trae
 * un parser XML (ver `org.xml.sax.helpers.XMLReaderFactory`). La interfaz esta completa y un driver
 * externo que se instale por la propiedad `org.xml.sax.driver` la va a poder usar; lo que no hay es
 * un emisor propio.
 */
public interface LexicalHandler {

    /**
     * El `&lt;!DOCTYPE&gt;`. `publicId` y `systemId` pueden ser `null` cuando la DTD es solo
     * interna. Todo lo que llegue hasta `endDTD` describe la declaracion, no el documento.
     */
    void startDTD(String name, String publicId, String systemId) throws SAXException;

    void endDTD() throws SAXException;

    /**
     * Empieza el contenido de una entidad. El nombre es `[dtd]` para el subconjunto externo, y
     * lleva `%` adelante cuando es una entidad de parametro; los dos casos son nombres que XML no
     * le deja usar a nadie mas, asi que no hay ambiguedad con una entidad general.
     */
    void startEntity(String name) throws SAXException;

    void endEntity(String name) throws SAXException;

    /** El texto de adentro sigue llegando por `characters`; esto solo marca el limite. */
    void startCDATA() throws SAXException;

    void endCDATA() throws SAXException;

    /**
     * Un comentario, con el arreglo prestado igual que en `characters`: vale dentro de la llamada
     * y el parser lo reusa despues. Quien quiera conservarlo copia.
     */
    void comment(char ch[], int start, int length) throws SAXException;
}
