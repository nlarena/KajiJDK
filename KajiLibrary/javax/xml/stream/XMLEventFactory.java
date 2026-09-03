package javax.xml.stream;

import java.util.Iterator;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.Comment;
import javax.xml.stream.events.DTD;
import javax.xml.stream.events.EndDocument;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.EntityDeclaration;
import javax.xml.stream.events.EntityReference;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.ProcessingInstruction;
import javax.xml.stream.events.StartDocument;
import javax.xml.stream.events.StartElement;

/**
 * KajiLibrary's javax.xml.stream.XMLEventFactory -- de donde salen los
 * {@link javax.xml.stream.events.XMLEvent} que uno construye a mano.
 *
 * <h2>La fabrica que no necesita parser</h2>
 *
 * <p>De las tres fabricas de StAX esta es la unica que no lee nada: cada metodo arma un evento a
 * partir de los datos que le pasa el llamador. Es la que se usa para <b>generar</b> XML con el
 * modelo de eventos, o para inyectar eventos en un flujo que se esta transformando.
 *
 * <p>Por eso aca funciona entera, con implementaciones de verdad detras. Ver
 * {@link XMLInputFactory}, que si depende de que haya un parser.
 *
 * <h2>El {@link Location} pegajoso</h2>
 *
 * <p>{@link #setLocation} es la unica pieza con estado y hay que mirarla con cuidado: fija una
 * ubicacion que se le va a poner a <b>todos</b> los eventos que la fabrica cree de ahi en adelante,
 * hasta que se la cambie. No es un parametro de un evento, es un modo de la fabrica.
 *
 * <p>El diseno viene de que un evento tiene ubicacion pero los metodos {@code createXxx} no la
 * reciben, y agregarle un parametro a los veintipico habria sido peor. La consecuencia practica es
 * que una instancia de esta fabrica <b>no</b> se puede compartir entre hilos si alguno llama a
 * {@code setLocation}: es estado mutable sin sincronizar. La recomendacion es una fabrica por
 * hilo, que ademas es barata.
 *
 * <h2>Los {@code Iterator} que reciben varios metodos</h2>
 *
 * <p>{@code createStartElement} y {@code createEndElement} toman iteradores de {@link Attribute} y
 * de {@link Namespace}. Se consumen <b>en el momento</b>: el evento que sale es inmutable y ya
 * tiene copiado lo que hacia falta, asi que el iterador se puede descartar despues. null vale y
 * significa "ninguno".
 */
public abstract class XMLEventFactory {

    /**
     * La propiedad de sistema con que se enchufa otra implementacion:
     * {@code javax.xml.stream.XMLEventFactory}.
     */
    static final String PROPERTY = "javax.xml.stream.XMLEventFactory";

    /** Para las subclases. */
    protected XMLEventFactory() {
    }

    // ---- descubrimiento ---------------------------------------------------------------------

    /**
     * La implementacion de la plataforma, sin mirar la configuracion.
     *
     * @return la fabrica de eventos de esta biblioteca; nunca null
     */
    public static XMLEventFactory newDefaultFactory() {
        return new KajiEventFactory();
    }

    /**
     * La fabrica configurada.
     *
     * <p>Mira la propiedad de sistema {@code javax.xml.stream.XMLEventFactory} y, si no esta, los
     * proveedores declarados como servicio; si tampoco hay, devuelve la de la plataforma.
     *
     * @return la fabrica encontrada; nunca null
     * @throws FactoryConfigurationError si la configuracion nombra una clase que no se puede usar
     */
    public static XMLEventFactory newInstance() {
        return newFactory();
    }

    /**
     * Lo mismo que {@link #newInstance()}, con el nombre nuevo.
     *
     * <p>Los dos existen porque {@code newInstance()} venia de la version 1.0 con una sobrecarga
     * que no distinguia bien los errores; {@code newFactory()} es la que la reemplaza. Aca hacen lo
     * mismo.
     *
     * @return la fabrica encontrada; nunca null
     * @throws FactoryConfigurationError si la configuracion nombra una clase que no se puede usar
     */
    public static XMLEventFactory newFactory() {
        Object f = Factories.fromSystemProperty(PROPERTY, XMLEventFactory.class);
        if (f != null) {
            return (XMLEventFactory) f;
        }
        return newDefaultFactory();
    }

    /**
     * La fabrica nombrada explicitamente, cargada con el cargador que se indique.
     *
     * @param factoryId el nombre de la clase de la fabrica; null cae en {@link #newFactory()}
     * @param classLoader el cargador con que buscarla; null usa el del contexto
     * @return la fabrica; nunca null
     * @throws FactoryConfigurationError si la clase no se puede cargar o no es una fabrica
     */
    public static XMLEventFactory newInstance(String factoryId, ClassLoader classLoader) {
        return newFactory(factoryId, classLoader);
    }

    /**
     * Lo mismo que {@link #newInstance(String, ClassLoader)}, con el nombre nuevo.
     *
     * @param factoryId el nombre de la clase de la fabrica; null cae en {@link #newFactory()}
     * @param classLoader el cargador con que buscarla; null usa el del contexto
     * @return la fabrica; nunca null
     * @throws FactoryConfigurationError si la clase no se puede cargar o no es una fabrica
     */
    public static XMLEventFactory newFactory(String factoryId, ClassLoader classLoader) {
        if (factoryId == null) {
            return newFactory();
        }
        return (XMLEventFactory) Factories.instantiate(factoryId, classLoader, XMLEventFactory.class);
    }

    // ---- la ubicacion pegajosa --------------------------------------------------------------

    /**
     * Fija la ubicacion que van a llevar todos los eventos que se creen de aca en adelante.
     *
     * <p>Es estado de la fabrica, no un parametro; ver el encabezado de la clase.
     *
     * @param location la ubicacion; null vuelve a la de por omision, que no tiene datos
     */
    public abstract void setLocation(Location location);

    // ---- los eventos ------------------------------------------------------------------------

    /**
     * Un atributo sin espacio de nombres.
     *
     * @param localName el nombre local
     * @param value el valor
     * @return el atributo
     */
    public abstract Attribute createAttribute(String localName, String value);

    /**
     * Un atributo calificado.
     *
     * @param prefix el prefijo con que escribirlo
     * @param namespaceURI el espacio de nombres
     * @param localName el nombre local
     * @param value el valor
     * @return el atributo
     */
    public abstract Attribute createAttribute(
            String prefix, String namespaceURI, String localName, String value);

    /**
     * Un atributo a partir de un nombre ya armado.
     *
     * @param name el nombre calificado
     * @param value el valor
     * @return el atributo
     */
    public abstract Attribute createAttribute(QName name, String value);

    /**
     * La declaracion del espacio de nombres por omision, o sea {@code xmlns="..."}.
     *
     * @param namespaceURI el URI a declarar
     * @return la declaracion
     */
    public abstract Namespace createNamespace(String namespaceURI);

    /**
     * La declaracion de un prefijo, o sea {@code xmlns:p="..."}.
     *
     * @param prefix el prefijo a declarar
     * @param namespaceUri el URI a asociarle
     * @return la declaracion
     */
    public abstract Namespace createNamespace(String prefix, String namespaceUri);

    /**
     * La apertura de un elemento, con sus atributos y sus declaraciones.
     *
     * @param name el nombre del elemento
     * @param attributes los atributos, o null si no hay
     * @param namespaces las declaraciones {@code xmlns}, o null si no hay
     * @return el evento
     */
    public abstract StartElement createStartElement(
            QName name, Iterator<? extends Attribute> attributes,
            Iterator<? extends Namespace> namespaces);

    /**
     * La apertura de un elemento, sin atributos ni declaraciones.
     *
     * @param prefix el prefijo con que escribirlo
     * @param namespaceUri el espacio de nombres
     * @param localName el nombre local
     * @return el evento
     */
    public abstract StartElement createStartElement(
            String prefix, String namespaceUri, String localName);

    /**
     * La apertura de un elemento, con sus atributos y sus declaraciones.
     *
     * @param prefix el prefijo con que escribirlo
     * @param namespaceUri el espacio de nombres
     * @param localName el nombre local
     * @param attributes los atributos, o null si no hay
     * @param namespaces las declaraciones {@code xmlns}, o null si no hay
     * @return el evento
     */
    public abstract StartElement createStartElement(
            String prefix, String namespaceUri, String localName,
            Iterator<? extends Attribute> attributes, Iterator<? extends Namespace> namespaces);

    /**
     * La apertura de un elemento, con el contexto de espacios de nombres que la rodea.
     *
     * @param prefix el prefijo con que escribirlo
     * @param namespaceUri el espacio de nombres
     * @param localName el nombre local
     * @param attributes los atributos, o null si no hay
     * @param namespaces las declaraciones {@code xmlns}, o null si no hay
     * @param context el alcance vigente, o null
     * @return el evento
     */
    public abstract StartElement createStartElement(
            String prefix, String namespaceUri, String localName,
            Iterator<? extends Attribute> attributes, Iterator<? extends Namespace> namespaces,
            NamespaceContext context);

    /**
     * El cierre de un elemento.
     *
     * @param name el nombre del elemento
     * @param namespaces los espacios de nombres que salen de alcance, o null
     * @return el evento
     */
    public abstract EndElement createEndElement(
            QName name, Iterator<? extends Namespace> namespaces);

    /**
     * El cierre de un elemento.
     *
     * @param prefix el prefijo con que escribirlo
     * @param namespaceUri el espacio de nombres
     * @param localName el nombre local
     * @return el evento
     */
    public abstract EndElement createEndElement(
            String prefix, String namespaceUri, String localName);

    /**
     * El cierre de un elemento.
     *
     * @param prefix el prefijo con que escribirlo
     * @param namespaceUri el espacio de nombres
     * @param localName el nombre local
     * @param namespaces los espacios de nombres que salen de alcance, o null
     * @return el evento
     */
    public abstract EndElement createEndElement(
            String prefix, String namespaceUri, String localName,
            Iterator<? extends Namespace> namespaces);

    /**
     * Texto comun.
     *
     * @param content el texto
     * @return el evento, con {@link XMLStreamConstants#CHARACTERS}
     */
    public abstract Characters createCharacters(String content);

    /**
     * Texto en una seccion {@code <![CDATA[...]]>}.
     *
     * @param content el texto
     * @return el evento, con {@link XMLStreamConstants#CDATA}
     */
    public abstract Characters createCData(String content);

    /**
     * Espacio en blanco.
     *
     * @param content el espacio
     * @return el evento, con {@link XMLStreamConstants#CHARACTERS}
     */
    public abstract Characters createSpace(String content);

    /**
     * Espacio en blanco marcado como ignorable.
     *
     * @param content el espacio
     * @return el evento, con {@link XMLStreamConstants#SPACE}
     */
    public abstract Characters createIgnorableSpace(String content);

    /**
     * El comienzo del documento, con los valores por omision: {@code 1.0} y {@code UTF-8}, ninguno
     * de los dos declarado.
     *
     * @return el evento
     */
    public abstract StartDocument createStartDocument();

    /**
     * El comienzo del documento con una codificacion declarada.
     *
     * @param encoding la codificacion
     * @return el evento
     */
    public abstract StartDocument createStartDocument(String encoding);

    /**
     * El comienzo del documento con codificacion y version declaradas.
     *
     * @param encoding la codificacion
     * @param version la version
     * @return el evento
     */
    public abstract StartDocument createStartDocument(String encoding, String version);

    /**
     * El comienzo del documento con las tres partes de la declaracion.
     *
     * @param encoding la codificacion
     * @param version la version
     * @param standalone el valor de {@code standalone}
     * @return el evento
     */
    public abstract StartDocument createStartDocument(
            String encoding, String version, boolean standalone);

    /**
     * El final del documento.
     *
     * @return el evento
     */
    public abstract EndDocument createEndDocument();

    /**
     * Una referencia a entidad sin expandir.
     *
     * @param name el nombre de la entidad
     * @param declaration su declaracion
     * @return el evento
     */
    public abstract EntityReference createEntityReference(
            String name, EntityDeclaration declaration);

    /**
     * Un comentario.
     *
     * @param text el texto, sin los delimitadores
     * @return el evento
     */
    public abstract Comment createComment(String text);

    /**
     * Una instruccion de procesamiento.
     *
     * @param target a quien va dirigida
     * @param data el resto, crudo
     * @return el evento
     */
    public abstract ProcessingInstruction createProcessingInstruction(String target, String data);

    /**
     * Una declaracion de tipo de documento, a partir de su texto crudo.
     *
     * @param dtd el texto de la declaracion
     * @return el evento
     */
    public abstract DTD createDTD(String dtd);
}
