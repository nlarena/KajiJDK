package org.xml.sax;

/**
 * KajiLibrary's org.xml.sax.ContentHandler -- donde caen los eventos del documento.
 *
 * <p>Es el manejador principal de {@link XMLReader}, el que recibe la estructura y el texto. Lo que
 * agrega sobre los otros tres es que sus llamadas estan **anidadas y ordenadas**: todo lo del
 * documento va entre `startDocument` y `endDocument`, y cada `startElement` tiene su `endElement`
 * aunque en el medio haya habido un error recuperable. Un manejador tipico es por eso una maquina de
 * estados con una pila.
 *
 * <p><strong>Las tres trampas de esta interfaz, que no se ven en las firmas:</strong>
 *
 * <p>Una: `characters` puede llamarse **varias veces para un mismo trozo de texto**. El parser puede
 * cortar donde le convenga --al fin de su buffer, al expandir una entidad-- y no esta obligado a
 * juntar. Quien haga `if (texto.equals("hola"))` adentro de `characters` tiene un bug que aparece con
 * documentos grandes y no con los del test. Lo correcto es acumular en un `StringBuilder` y mirarlo
 * en `endElement`.
 *
 * <p>Dos: el `char[]` que llega **no es del manejador**. El parser lo reusa en la llamada siguiente.
 * Guardarse la referencia en vez de copiar el rango `[start, start+length)` da datos corruptos mas
 * tarde, en otro lado, sin nada que apunte a la causa.
 *
 * <p>Tres: el objeto {@link Attributes} de `startElement` **solo vale durante esa llamada**. Para
 * conservarlo hay que copiarlo, que es justamente para lo que existe `AttributesImpl`.
 *
 * <p>Los metodos declaran `throws SAXException` porque es la unica manera que tiene un manejador de
 * frenar el parseo: la excepcion sube por `parse`.
 */
public interface ContentHandler {

    /**
     * Se llama antes que `startDocument`, si es que se llama.
     *
     * <p>El objeto que llega **no** hay que guardarlo para consultarlo despues: sus coordenadas
     * cambian solas a medida que el parser avanza, y fuera del manejador que las lee en el momento no
     * significan nada.
     */
    void setDocumentLocator(Locator locator);

    void startDocument() throws SAXException;

    /**
     * La declaracion `&lt;?xml version="1.0" encoding="UTF-8" standalone="yes"?&gt;`, si la habia.
     *
     * <p>Es `default` y no abstracta porque se agrego mucho despues que el resto de la interfaz:
     * hacerla abstracta hubiera roto todos los manejadores escritos hasta ese momento, que son
     * muchos. El cuerpo por omision no hace nada, que es exactamente lo que hacian antes.
     *
     * @param encoding `null` si la declaracion no lo traia.
     * @param standalone `null` si no estaba; si no, `"yes"` o `"no"`.
     */
    default void declaration(String version, String encoding, String standalone)
            throws SAXException {
    }

    void endDocument() throws SAXException;

    /**
     * Un prefijo empieza a estar atado a un URI.
     *
     * <p>Va **antes** del `startElement` que lo declara, no adentro, para que el manejador ya tenga
     * el mapa armado cuando le llegue el elemento. El prefijo por omision (`xmlns="..."`) llega como
     * cadena vacia, no como `null`.
     */
    void startPrefixMapping(String prefix, String uri) throws SAXException;

    /** Va **despues** del `endElement` correspondiente, en orden inverso al de apertura. */
    void endPrefixMapping(String prefix) throws SAXException;

    /**
     * @param uri vacio si el elemento no tiene espacio de nombres, o si el parser no los procesa.
     * @param localName vacio si el parser no procesa espacios de nombres.
     * @param qName el nombre tal cual estaba escrito; puede venir vacio si el parser no lo reporta.
     *        Que los tres puedan estar vacios segun la configuracion es lo que obliga a mirar
     *        `qName` en unos parsers y `localName` en otros.
     */
    void startElement(String uri, String localName, String qName, Attributes atts)
            throws SAXException;

    void endElement(String uri, String localName, String qName) throws SAXException;

    /** Ver arriba: el arreglo se reusa y el texto puede venir partido. */
    void characters(char[] ch, int start, int length) throws SAXException;

    /**
     * Blanco que la DTD dice que no es contenido, tipicamente sangria.
     *
     * <p>Sin validacion un parser no puede saber cual es y lo manda todo por `characters`. Que este
     * metodo no se llame nunca no significa que no hubiera sangria.
     */
    void ignorableWhitespace(char[] ch, int start, int length) throws SAXException;

    /** La declaracion `&lt;?xml ...?&gt;` no llega por aca: para eso esta `declaration`. */
    void processingInstruction(String target, String data) throws SAXException;

    /**
     * El parser se salteo una entidad en vez de expandirla.
     *
     * <p>Pasa cuando no valida y no leyo la DTD externa, o cuando se le apago la resolucion de
     * entidades. El nombre de una entidad parametro llega con `%` adelante.
     */
    void skippedEntity(String name) throws SAXException;
}
