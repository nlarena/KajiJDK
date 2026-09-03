package javax.xml.namespace;

import java.io.Serializable;

import javax.xml.XMLConstants;

/**
 * KajiLibrary's javax.xml.namespace.QName -- un nombre calificado de XML: espacio de nombres mas
 * nombre local, y un prefijo que viene de acompaniante.
 *
 * <p>Es la clase mas chica de toda la pila de XML y la que aparece en todas: la usan DOM, SAX, StAX,
 * XPath, la validacion y JAXB. Lo unico que hace es juntar dos cadenas, pero **cuales dos** es
 * exactamente la pregunta que XML Namespaces vino a contestar, y de ahi sale toda la sutileza.
 *
 * <h2>El prefijo no es parte de la identidad</h2>
 *
 * <p>Esta es la regla que hay que tener presente y la que mas sorprende: {@link #equals} y
 * {@link #hashCode} miran el espacio de nombres y el nombre local, y **no** el prefijo. Los dos
 * documentos
 *
 * <pre>{@code
 * <a:precio xmlns:a="http://tienda"/>
 * <b:precio xmlns:b="http://tienda"/>
 * }</pre>
 *
 * <p>dicen lo mismo: el prefijo es una abreviatura local del documento, elegida por quien lo
 * escribio, y dos documentos que eligieron distinto no por eso hablan de cosas distintas. Si el
 * prefijo contara, un {@code Map<QName, ?>} fallaria segun quien haya serializado la entrada, que es
 * la clase de bug que no se encuentra nunca.
 *
 * <p>El prefijo igual se guarda --y por eso {@link #getPrefix} existe-- porque quien vuelve a
 * escribir el documento lo necesita para no inventar prefijos nuevos en cada elemento. O sea: el
 * prefijo es informacion, no identidad. Los dos metodos son {@code final} justamente para que
 * ninguna subclase pueda cambiar esa regla por abajo.
 *
 * <h2>El formato {@code {uri}local}</h2>
 *
 * <p>{@link #toString} y {@link #valueOf} son inversas y usan la notacion de James Clark: un nombre
 * con espacio de nombres se escribe <code>{http://tienda}precio</code>, y uno sin espacio de nombres
 * se escribe pelado, {@code precio}. Es un formato de ida y vuelta para configuraciones y mensajes
 * de error, no una sintaxis de XML: no aparece en ningun documento.
 *
 * <p>La ida y vuelta pierde el prefijo a proposito --{@code valueOf} siempre devuelve
 * {@link XMLConstants#DEFAULT_NS_PREFIX}-- y es coherente con lo de arriba: el prefijo no es parte
 * del nombre, asi que no se transporta.
 *
 * <h2>Que hay aca</h2>
 *
 * <p>La clase esta completa: los tres constructores, los tres accesores, {@code equals},
 * {@code hashCode}, {@code toString} y {@code valueOf}, con las mismas validaciones y los mismos
 * mensajes de error que el JDK --hay codigo que compara esos textos--. Es {@link Serializable} y
 * declara el mismo {@code serialVersionUID} que la clase original, para que una instancia escrita
 * por una biblioteca se pueda leer con la otra.
 */
public class QName implements Serializable {

    /** El mismo de la clase original: dos instancias equivalentes tienen que ser intercambiables. */
    private static final long serialVersionUID = -9120448754896609940L;

    /** El espacio de nombres; nunca null, la cadena vacia cuando no hay. */
    private final String namespaceURI;

    /** El nombre local; nunca null, y lo unico que un nombre no puede no tener. */
    private final String localPart;

    /** El prefijo con que se escribio; nunca null, y ajeno a {@link #equals}. */
    private final String prefix;

    /**
     * Un nombre sin espacio de nombres.
     *
     * <p>Atajo de {@code QName(NULL_NS_URI, localPart, DEFAULT_NS_PREFIX)}: el espacio de nombres
     * queda en la cadena vacia, que es como se representa "no tiene", y no en null.
     *
     * @param localPart el nombre local
     * @throws IllegalArgumentException si {@code localPart} es null
     */
    public QName(String localPart) {
        this(XMLConstants.NULL_NS_URI, localPart, XMLConstants.DEFAULT_NS_PREFIX);
    }

    /**
     * Un nombre calificado sin prefijo asociado.
     *
     * <p>El caso normal cuando el nombre se construye a mano: se sabe de que vocabulario es, no de
     * como se abreviaba en un documento que quiza no existe.
     *
     * @param namespaceURI el espacio de nombres; null se toma como la cadena vacia
     * @param localPart el nombre local
     * @throws IllegalArgumentException si {@code localPart} es null
     */
    public QName(String namespaceURI, String localPart) {
        this(namespaceURI, localPart, XMLConstants.DEFAULT_NS_PREFIX);
    }

    /**
     * Un nombre calificado con el prefijo con que aparecio.
     *
     * <p>Las tres validaciones son asimetricas y vale entender por que: el espacio de nombres
     * ausente **es** un caso valido --un nombre sin calificar-- asi que null se normaliza a la
     * cadena vacia; el nombre local y el prefijo ausentes son errores del llamador, porque no hay
     * nada sensato que significar con ellos, asi que revientan.
     *
     * @param namespaceURI el espacio de nombres; null se toma como la cadena vacia
     * @param localPart el nombre local
     * @param prefix el prefijo; la cadena vacia si no hay
     * @throws IllegalArgumentException si {@code localPart} o {@code prefix} son null
     */
    public QName(String namespaceURI, String localPart, String prefix) {
        if (namespaceURI == null) {
            this.namespaceURI = XMLConstants.NULL_NS_URI;
        } else {
            this.namespaceURI = namespaceURI;
        }
        if (localPart == null) {
            throw new IllegalArgumentException("local part cannot be \"null\" when creating a QName");
        }
        this.localPart = localPart;
        if (prefix == null) {
            throw new IllegalArgumentException("prefix cannot be \"null\" when creating a QName");
        }
        this.prefix = prefix;
    }

    /**
     * El espacio de nombres, o la cadena vacia si el nombre no esta calificado.
     *
     * @return nunca null
     */
    public String getNamespaceURI() {
        return namespaceURI;
    }

    /**
     * El nombre local, que es la unica parte obligatoria.
     *
     * @return nunca null
     */
    public String getLocalPart() {
        return localPart;
    }

    /**
     * El prefijo con que se escribio este nombre, o la cadena vacia.
     *
     * <p>No participa de {@link #equals} ni de {@link #hashCode}; ver el encabezado de la clase.
     *
     * @return nunca null
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Dos nombres son el mismo si coinciden espacio de nombres y nombre local.
     *
     * <p>Es {@code final} para que la regla no se pueda relajar en una subclase: si un
     * {@code QName} derivado hiciera entrar el prefijo en la comparacion, romperia la simetria con
     * los {@code QName} de la biblioteca --{@code a.equals(b)} y {@code b.equals(a)} darian
     * distinto-- y con ella cualquier tabla que los use de clave.
     *
     * @param objectToTest el otro objeto
     * @return true si es un {@code QName} con el mismo espacio de nombres y nombre local
     */
    public final boolean equals(Object objectToTest) {
        if (objectToTest == this) {
            return true;
        }
        if (!(objectToTest instanceof QName)) {
            return false;
        }
        QName other = (QName) objectToTest;
        return localPart.equals(other.localPart) && namespaceURI.equals(other.namespaceURI);
    }

    /**
     * El o exclusivo de los dos hashes que importan.
     *
     * <p>Tampoco mira el prefijo, que es lo que hace falta para que sea coherente con
     * {@link #equals}. Tambien {@code final}, y por el mismo motivo.
     *
     * @return el hash
     */
    public final int hashCode() {
        return namespaceURI.hashCode() ^ localPart.hashCode();
    }

    /**
     * El nombre en notacion {@code {uri}local}, o pelado si no tiene espacio de nombres.
     *
     * <p>Sin cache: se calcula cada vez. Guardarlo ahorraria concatenaciones en un camino que casi
     * siempre es un mensaje de error o una traza, y a cambio agregaria un campo que no participa de
     * la identidad y que habria que excluir a mano de la serializacion.
     *
     * @return la representacion textual, que {@link #valueOf} sabe deshacer
     */
    public String toString() {
        if (namespaceURI.equals(XMLConstants.NULL_NS_URI)) {
            return localPart;
        }
        return "{" + namespaceURI + "}" + localPart;
    }

    /**
     * Deshace {@link #toString}: de {@code {uri}local} sale el nombre calificado.
     *
     * <p>El prefijo del resultado es siempre {@link XMLConstants#DEFAULT_NS_PREFIX}, porque el
     * formato no lo transporta.
     *
     * <p>Los casos de borde no son arbitrarios y conviene leerlos juntos:
     *
     * <ul>
     *   <li>la cadena vacia da un nombre con nombre local vacio, que es legal aunque no sea un
     *       nombre de XML valido: se acepta por compatibilidad con la version 1.0 de esta clase;
     *   <li><code>{}local</code> **falla**, y es el unico caso que sorprende: pedir explicitamente
     *       el espacio de nombres vacio es un error, porque la forma de decir eso es escribir
     *       {@code local} a secas, y quien escribio las llaves vacias casi seguro creia estar
     *       diciendo otra cosa;
     *   <li>una llave que abre y no cierra falla;
     *   <li>una llave que cierra sin abrir es nombre local, no error: {@code }x} es un nombre local
     *       raro pero es un nombre local.
     * </ul>
     *
     * @param qNameAsString la cadena a interpretar
     * @return el nombre calificado
     * @throws IllegalArgumentException si es null o si el formato esta mal
     */
    public static QName valueOf(String qNameAsString) {
        if (qNameAsString == null) {
            throw new IllegalArgumentException("cannot create QName from \"null\" or \"\" String");
        }
        if (qNameAsString.length() == 0) {
            return new QName(
                    XMLConstants.NULL_NS_URI, qNameAsString, XMLConstants.DEFAULT_NS_PREFIX);
        }
        if (qNameAsString.charAt(0) != '{') {
            return new QName(
                    XMLConstants.NULL_NS_URI, qNameAsString, XMLConstants.DEFAULT_NS_PREFIX);
        }
        if (qNameAsString.startsWith("{" + XMLConstants.NULL_NS_URI + "}")) {
            throw new IllegalArgumentException(
                    "Namespace URI .equals(XMLConstants.NULL_NS_URI), "
                            + ".equals(\"" + XMLConstants.NULL_NS_URI + "\"), "
                            + "only the local part, "
                            + "\"" + qNameAsString.substring(2) + "\", "
                            + "should be provided.");
        }
        int endOfUri = qNameAsString.indexOf('}');
        if (endOfUri == -1) {
            throw new IllegalArgumentException(
                    "cannot create QName from \"" + qNameAsString + "\", missing closing \"}\"");
        }
        return new QName(
                qNameAsString.substring(1, endOfUri),
                qNameAsString.substring(endOfUri + 1),
                XMLConstants.DEFAULT_NS_PREFIX);
    }
}
