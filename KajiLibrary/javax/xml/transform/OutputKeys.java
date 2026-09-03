package javax.xml.transform;

/**
 * KajiLibrary's javax.xml.transform.OutputKeys -- los nombres de las propiedades de serializacion.
 *
 * <p>Diez constantes y nada mas: la clase es un espacio de nombres, no un objeto. El constructor es
 * privado a proposito y no hay ningun miembro de instancia.
 *
 * <p>Por que existe una clase entera para diez cadenas: {@link Transformer#setOutputProperty} recibe
 * el nombre de la propiedad como `String`, porque XSLT permite tambien propiedades de extension con
 * nombre calificado (`{uri}local`), y con un `enum` no entrarian. El precio de esa apertura es que
 * un nombre mal escrito no lo ve el compilador -- y `setOutputProperty("ident", "yes")` no indenta
 * nada y tampoco falla. Estas constantes son la parte del vocabulario que si se puede chequear.
 *
 * <p>Los valores son **exactamente** los atributos de `&lt;xsl:output&gt;` de la spec de XSLT 1.0,
 * §16: la API no reinventa el vocabulario, lo expone. Por eso son `method` y no `METHOD`, y por eso
 * los booleanos se escriben `"yes"`/`"no"` y no `"true"`/`"false"`.
 */
public class OutputKeys {

    /** Nadie instancia esto. */
    private OutputKeys() {
    }

    /**
     * Que clase de documento emitir: {@code "xml"}, {@code "html"}, {@code "text"}, o un nombre
     * calificado para un metodo de extension.
     *
     * <p>Es la propiedad que mas cambia el resultado, y la unica que el procesador elige solo si no
     * se la fijan: mira el documento de salida y usa `html` si el elemento raiz es `&lt;html&gt;`.
     */
    public static final String METHOD = "method";

    /** La version del metodo de salida; para {@code xml}, la version de XML. */
    public static final String VERSION = "version";

    /** El nombre del juego de caracteres preferido para la salida. */
    public static final String ENCODING = "encoding";

    /** {@code "yes"} para no escribir la declaracion XML. */
    public static final String OMIT_XML_DECLARATION = "omit-xml-declaration";

    /** {@code "yes"} o {@code "no"} para el atributo `standalone` de la declaracion. */
    public static final String STANDALONE = "standalone";

    /** El identificador publico del DOCTYPE que se emite. */
    public static final String DOCTYPE_PUBLIC = "doctype-public";

    /** El identificador de sistema del DOCTYPE que se emite. */
    public static final String DOCTYPE_SYSTEM = "doctype-system";

    /**
     * La lista, separada por espacios, de los elementos cuyo contenido de texto va dentro de una
     * seccion CDATA en vez de escapado.
     */
    public static final String CDATA_SECTION_ELEMENTS = "cdata-section-elements";

    /**
     * {@code "yes"} para que el serializador pueda agregar espacio en blanco.
     *
     * <p>Vale la aclaracion porque se malinterpreta: indentar **cambia el documento**. El espacio
     * agregado es texto y aparece en el arbol de quien lo lea. Sirve para leerlo con los ojos, no
     * para comparar dos salidas.
     */
    public static final String INDENT = "indent";

    /** El tipo de medio (MIME) del documento de salida. */
    public static final String MEDIA_TYPE = "media-type";
}
