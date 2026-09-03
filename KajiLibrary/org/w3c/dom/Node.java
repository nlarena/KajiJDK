package org.w3c.dom;

/**
 * KajiLibrary's org.w3c.dom.Node -- el tipo base de todo lo que vive en un arbol DOM.
 *
 * <p><strong>El modelo, una vez.</strong> El DOM representa un documento XML como un arbol de
 * **nodos**. Un elemento es un nodo, un atributo es un nodo, el texto suelto es un nodo, un
 * comentario es un nodo, y el documento entero tambien. Por eso `Node` esta arriba de casi todo el
 * paquete: es el minimo comun denominador que permite escribir un recorrido generico --bajar por
 * `getFirstChild()`, avanzar por `getNextSibling()`-- sin saber que hay en cada escalon. El precio de
 * ese minimo comun es que la interfaz **declara mas de lo que cualquier nodo concreto cumple**, y esa
 * es la clave para leer todo el resto de este paquete.
 *
 * <p><strong>Por que el DOM esta lleno de metodos que devuelven `null`.</strong> `getAttributes()`
 * solo tiene sentido en un `Element`; en un `Text` devuelve `null`. `getOwnerDocument()` devuelve
 * `null` justo en el `Document`. `getNamespaceURI()`, `getPrefix()` y `getLocalName()` devuelven
 * `null` en todo nodo creado con la API del DOM nivel 1, que no conocia espacios de nombres. No son
 * huecos de la especificacion: son la consecuencia de haber elegido **una** interfaz para doce tipos
 * de nodo en vez de doce interfaces sin ancestro comun. Quien recorre un arbol DOM comprueba
 * `getNodeType()` antes de creerle a un getter.
 *
 * <p><strong>Los doce tipos.</strong> Las constantes `*_NODE` son el discriminador: `getNodeType()`
 * devuelve una de ellas y de ahi sale a que interfaz hija se puede castear. La tabla de la norma
 * tambien fija que devuelven `getNodeName()` y `getNodeValue()` para cada uno --`#text` y el
 * contenido para un `Text`, el nombre de la etiqueta y `null` para un `Element`-- y esa tabla es
 * parte del contrato aunque no se pueda escribir en Java.
 *
 * <p><strong>Las constantes `DOCUMENT_POSITION_*` son una mascara de bits</strong>, no un enum:
 * `compareDocumentPosition` devuelve la **or** de todas las que apliquen. Por eso valen 1, 2, 4, 8,
 * 16 y 32 y no 1..6. Un nodo que contiene a otro devuelve `CONTAINS | PRECEDING`.
 *
 * <p><strong>Esto es una interfaz y no hay implementacion en KajiLibrary.</strong> Declarar el
 * contrato es honesto justamente porque es un contrato: no promete que alguien lo cumpla. Lo que no
 * se podria hacer es dar un `Document` de mentira que finja parsear XML. Los metodos que fabrican o
 * modifican arboles --`appendChild`, `cloneNode`, `normalize`-- estan declarados porque son parte de
 * la interfaz que un implementador tiene que cumplir, no porque aca haya un arbol que modificar.
 */
public interface Node {

    // ---- los doce tipos de nodo ----------------------------------------------------------------
    //
    // El orden y los valores son los de la norma y son API observable: hay codigo que los guarda en
    // tablas indexadas por el numero. `getNodeType()` devuelve uno de estos.

    /** Un `Element`: una etiqueta con atributos e hijos. */
    short ELEMENT_NODE = 1;

    /** Un `Attr`. Cuelga de su elemento, pero **no** es hijo suyo: `getParentNode()` da `null`. */
    short ATTRIBUTE_NODE = 2;

    /** Un `Text`: caracteres sueltos entre etiquetas. */
    short TEXT_NODE = 3;

    /** Una `CDATASection`: texto que el parser no interpreta. */
    short CDATA_SECTION_NODE = 4;

    /** Una `EntityReference`: un `&amp;nombre;` sin expandir. */
    short ENTITY_REFERENCE_NODE = 5;

    /** Una `Entity` declarada en la DTD. */
    short ENTITY_NODE = 6;

    /** Una `ProcessingInstruction`: `&lt;?destino datos?&gt;`. */
    short PROCESSING_INSTRUCTION_NODE = 7;

    /** Un `Comment`. */
    short COMMENT_NODE = 8;

    /** El `Document`: la raiz del arbol, que no es el elemento raiz. */
    short DOCUMENT_NODE = 9;

    /** El `DocumentType`: el `&lt;!DOCTYPE ...&gt;`. */
    short DOCUMENT_TYPE_NODE = 10;

    /** Un `DocumentFragment`: un contenedor liviano para mover varios nodos de una. */
    short DOCUMENT_FRAGMENT_NODE = 11;

    /** Una `Notation` declarada en la DTD. */
    short NOTATION_NODE = 12;

    // ---- posicion relativa: mascara de bits, no valores exclusivos -------------------------------

    /** Los dos nodos no estan en el mismo arbol. */
    short DOCUMENT_POSITION_DISCONNECTED = 0x01;

    /** El otro nodo va **antes** que este. */
    short DOCUMENT_POSITION_PRECEDING = 0x02;

    /** El otro nodo va **despues** que este. */
    short DOCUMENT_POSITION_FOLLOWING = 0x04;

    /** El otro nodo es antepasado de este. */
    short DOCUMENT_POSITION_CONTAINS = 0x08;

    /** El otro nodo es descendiente de este. */
    short DOCUMENT_POSITION_CONTAINED_BY = 0x10;

    /** El orden entre los dos lo elige la implementacion y puede cambiar entre corridas. */
    short DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC = 0x20;

    // ---- identidad del nodo ----------------------------------------------------------------------

    String getNodeName();

    /**
     * El valor, para los nodos que tienen uno; `null` para `Element`, `Document` y compania.
     *
     * @throws DOMException con `DOMSTRING_SIZE_ERR` si el valor no entra en un `String`. Es el resto
     *         de una epoca en que `DOMString` podia ser mas grande que lo direccionable.
     */
    String getNodeValue() throws DOMException;

    /**
     * @throws DOMException con `NO_MODIFICATION_ALLOWED_ERR` si el nodo es de solo lectura, cosa que
     *         pasa con todo lo que cuelga de una `Entity` o de un `EntityReference`.
     */
    void setNodeValue(String nodeValue) throws DOMException;

    short getNodeType();

    // ---- navegacion ------------------------------------------------------------------------------
    //
    // Los cinco getters de abajo devuelven `null` cuando no hay a donde ir. Es el criterio del DOM
    // entero: nunca una excepcion para "no hay", siempre `null`.

    Node getParentNode();

    /** Nunca `null`: un nodo sin hijos devuelve una lista vacia, no `null`. */
    NodeList getChildNodes();

    Node getFirstChild();

    Node getLastChild();

    Node getPreviousSibling();

    Node getNextSibling();

    /** Solo un `Element` devuelve algo; el resto, `null`. */
    NamedNodeMap getAttributes();

    /** El documento que **creo** este nodo. `null` en el propio `Document`. */
    Document getOwnerDocument();

    // ---- modificar el arbol ----------------------------------------------------------------------
    //
    // Los cuatro **mueven**, no copian: insertar un nodo que ya tiene padre lo saca de donde estaba.
    // Y si lo que se inserta es un `DocumentFragment`, lo que entra son sus hijos y no el fragmento.

    /**
     * @param refChild si es `null`, equivale a `appendChild`.
     * @throws DOMException con `HIERARCHY_REQUEST_ERR` si el tipo de hijo no va ahi o si crearia un
     *         ciclo; `WRONG_DOCUMENT_ERR` si viene de otro documento; `NOT_FOUND_ERR` si `refChild`
     *         no es hijo de este nodo.
     */
    Node insertBefore(Node newChild, Node refChild) throws DOMException;

    /** Devuelve el nodo **sacado**, no el puesto. */
    Node replaceChild(Node newChild, Node oldChild) throws DOMException;

    Node removeChild(Node oldChild) throws DOMException;

    Node appendChild(Node newChild) throws DOMException;

    boolean hasChildNodes();

    /**
     * @param deep si es `false`, el clon no tiene hijos. Un `Element` clona igual sus atributos:
     *        `deep` habla de los hijos, no de los atributos.
     */
    Node cloneNode(boolean deep);

    /**
     * Junta los `Text` adyacentes en uno y tira los vacios.
     *
     * <p>Importa porque un arbol recien parseado puede tener el mismo parrafo partido en varios
     * `Text` --por ejemplo si en el medio hubo una referencia a entidad-- y eso rompe cualquier
     * comparacion ingenua. Despues de `normalize()` la forma del arbol es la que se obtendria de
     * serializar y volver a parsear.
     */
    void normalize();

    /** Lo reemplazo `getFeature` en el nivel 3, que ademas de decir si esta devuelve el objeto. */
    boolean isSupported(String feature, String version);

    // ---- espacios de nombres ---------------------------------------------------------------------
    //
    // Los tres devuelven `null` en todo nodo creado con la API nivel 1 (`createElement` en vez de
    // `createElementNS`). No es que el nodo este sin espacio de nombres: es que **no participa** del
    // modelo de espacios de nombres, que es distinto.

    String getNamespaceURI();

    String getPrefix();

    /**
     * @throws DOMException con `NAMESPACE_ERR` si el prefijo es malformado, o si se intenta atar
     *         `xml` o `xmlns` a un URI que no es el suyo.
     */
    void setPrefix(String prefix) throws DOMException;

    String getLocalName();

    boolean hasAttributes();

    // ---- agregados del nivel 3 -------------------------------------------------------------------

    /** El URI base para resolver referencias relativas, siguiendo `xml:base`. */
    String getBaseURI();

    /** Una **or** de las constantes `DOCUMENT_POSITION_*`. */
    short compareDocumentPosition(Node other) throws DOMException;

    /** Todo el texto de abajo concatenado, sin marcado. */
    String getTextContent() throws DOMException;

    /** Reemplaza todos los hijos por un unico `Text`; con `null` o `""` los borra a todos. */
    void setTextContent(String textContent) throws DOMException;

    /**
     * Identidad, no igualdad. Existe porque una implementacion puede entregar mas de un objeto Java
     * para el mismo nodo del documento, y entonces `==` no alcanza.
     */
    boolean isSameNode(Node other);

    String lookupPrefix(String namespaceURI);

    boolean isDefaultNamespace(String namespaceURI);

    String lookupNamespaceURI(String prefix);

    /** Igualdad estructural: mismo tipo, mismo nombre, mismos atributos, mismos hijos en orden. */
    boolean isEqualNode(Node arg);

    /**
     * El objeto que implementa `feature` para este nodo, o `null`.
     *
     * <p>Devuelve `Object` y no algo mas preciso porque el que sale de aca suele ser de **otro**
     * paquete --`org.w3c.dom.events.EventTarget`, `org.w3c.dom.ls.LSSerializer`-- y el nucleo del
     * DOM no depende de sus modulos opcionales.
     */
    Object getFeature(String feature, String version);

    /** Devuelve lo que hubiera antes con esa clave. */
    Object setUserData(String key, Object data, UserDataHandler handler);

    Object getUserData(String key);
}
