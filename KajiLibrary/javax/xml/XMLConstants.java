package javax.xml;

/**
 * KajiLibrary's javax.xml.XMLConstants -- el vocabulario compartido de todas las APIs de XML.
 *
 * <p>Dieciseis cadenas, constructor privado, cero estado: es un espacio de nombres, no un objeto.
 * Existe porque estas mismas URIs las tienen que escribir SAX, DOM, XSLT, XPath y la validacion, y
 * una constante mal tipeada en cualquiera de ellas produce un fallo mudo -- un espacio de nombres
 * que no coincide no da error, simplemente no matchea nada.
 *
 * <p>Las constantes se agrupan en tres familias que conviene distinguir porque se usan distinto:
 *
 * <ul>
 *   <li>los <b>espacios de nombres reservados</b> por la spec de XML Namespaces, que no se pueden
 *       redeclarar (`xml`, `xmlns`);
 *   <li>las <b>URIs de lenguajes de esquema</b>, que sirven para pedirle a una fabrica un validador
 *       de esa clase;
 *   <li>los <b>nombres de caracteristicas y propiedades de seguridad</b>, que son lo unico de aca
 *       que se le pasa a un `setFeature`/`setProperty` en vez de compararse.
 * </ul>
 */
public final class XMLConstants {

    /** Nadie instancia esto. */
    private XMLConstants() {
    }

    // ---- lo reservado por XML Namespaces -----------------------------------------------------

    /**
     * El espacio de nombres de lo que **no tiene** espacio de nombres: la cadena vacia.
     *
     * <p>Que sea la cadena vacia y no null es deliberado y ahorra un chequeo en cada comparacion:
     * un elemento sin calificar tiene URI de espacio de nombres, y es esta.
     */
    public static final String NULL_NS_URI = "";

    /** El prefijo por omision, que tambien es la cadena vacia: un nombre sin dos puntos. */
    public static final String DEFAULT_NS_PREFIX = "";

    /** El espacio de nombres del prefijo `xml`, fijado por la spec y no declarable. */
    public static final String XML_NS_URI = "http://www.w3.org/XML/1998/namespace";

    /** El prefijo `xml`, ligado por definicion a {@link #XML_NS_URI} y a ningun otro. */
    public static final String XML_NS_PREFIX = "xml";

    /** El espacio de nombres de los atributos `xmlns`, que son declaraciones y no atributos. */
    public static final String XMLNS_ATTRIBUTE_NS_URI = "http://www.w3.org/2000/xmlns/";

    /** El nombre local del atributo de declaracion: `xmlns`. */
    public static final String XMLNS_ATTRIBUTE = "xmlns";

    // ---- lenguajes de esquema ----------------------------------------------------------------

    /** W3C XML Schema 1.0. */
    public static final String W3C_XML_SCHEMA_NS_URI = "http://www.w3.org/2001/XMLSchema";

    /** Los atributos de instancia de W3C XML Schema (`xsi:type`, `xsi:nil`...). */
    public static final String W3C_XML_SCHEMA_INSTANCE_NS_URI = "http://www.w3.org/2001/XMLSchema-instance";

    /** Los tipos de datos de XPath 2.0. */
    public static final String W3C_XPATH_DATATYPE_NS_URI = "http://www.w3.org/2003/11/xpath-datatypes";

    /** La DTD de XML, para pedir validacion por DTD donde se espera una URI de esquema. */
    public static final String XML_DTD_NS_URI = "http://www.w3.org/TR/REC-xml";

    /** RELAX NG 1.0. */
    public static final String RELAXNG_NS_URI = "http://relaxng.org/ns/structure/1.0";

    // ---- seguridad ---------------------------------------------------------------------------

    /**
     * La caracteristica de **procesamiento seguro**, la unica que toda la plataforma XML soporta.
     *
     * <p>Prenderla le pone limites a lo que un documento hostil puede hacerle al procesador: acota
     * la expansion de entidades --la "bomba XML", que son diez lineas que se expanden a gigabytes--
     * y la profundidad de las estructuras. No es una opcion de rendimiento, es la diferencia entre
     * parsear entrada ajena y no poder hacerlo.
     *
     * <p>Y tiene una asimetria que conviene saber: una implementacion que la tenga prendida **no
     * esta obligada a dejar apagarla**. El modo seguro puede venir impuesto por el entorno.
     */
    public static final String FEATURE_SECURE_PROCESSING = "http://javax.xml.XMLConstants/feature/secure-processing";

    /**
     * Que protocolos se admiten para traer una DTD externa.
     *
     * <p>El valor es una lista de protocolos separados por comas; la cadena vacia prohibe todo, y
     * {@code "all"} permite todo. Los tres `ACCESS_EXTERNAL_*` son la defensa contra XXE: un
     * documento que declara una entidad apuntando a `file:///etc/passwd` --o a una URL interna--
     * usa al parser de proxy para leer lo que el atacante no alcanza.
     */
    public static final String ACCESS_EXTERNAL_DTD = "http://javax.xml.XMLConstants/property/accessExternalDTD";

    /** Idem para los esquemas externos (`xsi:schemaLocation`, `xsd:import`). */
    public static final String ACCESS_EXTERNAL_SCHEMA = "http://javax.xml.XMLConstants/property/accessExternalSchema";

    /** Idem para las hojas de estilo externas (`xsl:import`, `xsl:include`, `document()`). */
    public static final String ACCESS_EXTERNAL_STYLESHEET = "http://javax.xml.XMLConstants/property/accessExternalStylesheet";

    /**
     * Si se usa el catalogo de XML para resolver referencias externas.
     *
     * <p>Un catalogo mapea identificadores publicos a copias locales, asi que prenderlo es a la vez
     * mas rapido y mas seguro que salir a la red. Es la alternativa constructiva a los
     * `ACCESS_EXTERNAL_*`: en vez de prohibir, redirigir.
     */
    public static final String USE_CATALOG = "http://javax.xml.XMLConstants/feature/useCatalog";
}
