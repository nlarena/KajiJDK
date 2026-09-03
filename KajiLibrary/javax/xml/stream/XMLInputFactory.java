package javax.xml.stream;

import java.io.InputStream;
import java.io.Reader;

import javax.xml.stream.util.XMLEventAllocator;
import javax.xml.transform.Source;

/**
 * KajiLibrary's javax.xml.stream.XMLInputFactory -- la puerta de entrada a la lectura con StAX.
 *
 * <h2>Los dos modelos salen de aca</h2>
 *
 * <p>{@code createXMLStreamReader} devuelve el lector de cursor y {@code createXMLEventReader} el
 * de eventos; son la misma lectura con dos formas de entregarla, y estan en la misma fabrica porque
 * la configuracion --las propiedades de abajo-- vale para las dos.
 *
 * <h2>Que parser hay detras</h2>
 *
 * <p>Esta biblioteca trae un analizador de XML 1.0 propio, no validador y con soporte de espacios
 * de nombres, y es el que devuelven estos metodos. Lee la declaracion XML, elementos, atributos,
 * texto, secciones CDATA, comentarios, instrucciones de procesamiento y las cinco entidades
 * predefinidas mas las referencias numericas.
 *
 * <p>Lo que <b>no</b> hace, y hay que saberlo antes de confiarle un documento:
 *
 * <ul>
 *   <li>no interpreta el DTD. La declaracion {@code <!DOCTYPE ...>} se entrega entera como evento
 *       {@link XMLStreamConstants#DTD} y no se mira: no hay entidades declaradas por el usuario, ni
 *       valores de atributo por omision, ni tipos de atributo, ni espacio ignorable;
 *   <li>no resuelve entidades externas. Por eso {@link #SUPPORT_DTD} y
 *       {@link #IS_SUPPORTING_EXTERNAL_ENTITIES} son de solo lectura y valen false: aceptar que se
 *       pongan en true seria prometer algo que no pasa;
 *   <li>no valida. {@link #IS_VALIDATING} tambien es de solo lectura en false, que es ademas el
 *       valor por omision que manda la especificacion.
 * </ul>
 *
 * <p>Esa lista es exactamente la razon por la que {@link #isPropertySupported} contesta true para
 * las propiedades que si tienen efecto y {@link #setProperty} rechaza las otras con
 * {@link IllegalArgumentException}. Un {@code setProperty} que acepta y no hace nada es la clase de
 * mentira que hace que un documento con entidades externas se lea distinto de como el llamador
 * pidio, sin que nadie se entere.
 *
 * <h2>Que si se puede configurar</h2>
 *
 * <p>{@link #IS_COALESCING}, {@link #IS_NAMESPACE_AWARE},
 * {@link #IS_REPLACING_ENTITY_REFERENCES}, {@link #REPORTER}, {@link #RESOLVER} y
 * {@link #ALLOCATOR}. Las tres primeras cambian de verdad lo que sale del parser.
 */
public abstract class XMLInputFactory {

    /**
     * {@code javax.xml.stream.isNamespaceAware}: si el parser separa prefijo y espacio de nombres.
     *
     * <p>Por omision true, y en esta biblioteca cambiarlo a false hace lo que dice: los nombres
     * quedan sin calificar y las declaraciones {@code xmlns} pasan a ser atributos comunes.
     */
    public static final String IS_NAMESPACE_AWARE = "javax.xml.stream.isNamespaceAware";

    /**
     * {@code javax.xml.stream.isValidating}: si el parser valida contra el DTD.
     *
     * <p>De solo lectura en false; ver el encabezado de la clase.
     */
    public static final String IS_VALIDATING = "javax.xml.stream.isValidating";

    /**
     * {@code javax.xml.stream.isCoalescing}: si el texto adyacente se junta en un solo evento.
     *
     * <p>Por omision false. En true, el texto y las secciones CDATA que se tocan llegan como un
     * unico {@link XMLStreamConstants#CHARACTERS}, que es casi siempre lo que uno quiere: sin esto
     * un {@code &amp;} en medio de una frase la parte en tres eventos.
     */
    public static final String IS_COALESCING = "javax.xml.stream.isCoalescing";

    /**
     * {@code javax.xml.stream.isReplacingEntityReferences}: si las entidades se expanden.
     *
     * <p>Por omision true. En false, una referencia a una entidad que no sea de las cinco
     * predefinidas llega como {@link XMLStreamConstants#ENTITY_REFERENCE} en vez de expandirse.
     */
    public static final String IS_REPLACING_ENTITY_REFERENCES =
            "javax.xml.stream.isReplacingEntityReferences";

    /**
     * {@code javax.xml.stream.isSupportingExternalEntities}: si se van a buscar las entidades
     * externas.
     *
     * <p>De solo lectura en false; ver el encabezado de la clase.
     */
    public static final String IS_SUPPORTING_EXTERNAL_ENTITIES =
            "javax.xml.stream.isSupportingExternalEntities";

    /**
     * {@code javax.xml.stream.supportDTD}: si se procesa la declaracion de tipo de documento.
     *
     * <p>De solo lectura en false; ver el encabezado de la clase.
     */
    public static final String SUPPORT_DTD = "javax.xml.stream.supportDTD";

    /** {@code javax.xml.stream.reporter}: el {@link XMLReporter} al que avisar de los avisos. */
    public static final String REPORTER = "javax.xml.stream.reporter";

    /** {@code javax.xml.stream.resolver}: el {@link XMLResolver} con que resolver entidades. */
    public static final String RESOLVER = "javax.xml.stream.resolver";

    /** {@code javax.xml.stream.allocator}: el {@link XMLEventAllocator} que arma los eventos. */
    public static final String ALLOCATOR = "javax.xml.stream.allocator";

    /** La propiedad de sistema con que se enchufa otra implementacion. */
    static final String PROPERTY = "javax.xml.stream.XMLInputFactory";

    /** Para las subclases. */
    protected XMLInputFactory() {
    }

    // ---- descubrimiento ---------------------------------------------------------------------

    /**
     * La implementacion de la plataforma, sin mirar la configuracion.
     *
     * @return la fabrica de lectura de esta biblioteca; nunca null
     */
    public static XMLInputFactory newDefaultFactory() {
        return new KajiInputFactory();
    }

    /**
     * La fabrica configurada, o la de la plataforma si no hay ninguna.
     *
     * @return la fabrica; nunca null
     * @throws FactoryConfigurationError si la configuracion nombra una clase que no se puede usar
     */
    public static XMLInputFactory newInstance() {
        return newFactory();
    }

    /**
     * Lo mismo que {@link #newInstance()}, con el nombre nuevo.
     *
     * @return la fabrica; nunca null
     * @throws FactoryConfigurationError si la configuracion nombra una clase que no se puede usar
     */
    public static XMLInputFactory newFactory() {
        Object f = Factories.fromSystemProperty(PROPERTY, XMLInputFactory.class);
        if (f != null) {
            return (XMLInputFactory) f;
        }
        return newDefaultFactory();
    }

    /**
     * La fabrica nombrada explicitamente.
     *
     * @param factoryId el nombre de la clase; null cae en {@link #newFactory()}
     * @param classLoader el cargador con que buscarla; null usa el del contexto
     * @return la fabrica; nunca null
     * @throws FactoryConfigurationError si la clase no se puede cargar o no es una fabrica
     */
    public static XMLInputFactory newInstance(String factoryId, ClassLoader classLoader) {
        return newFactory(factoryId, classLoader);
    }

    /**
     * Lo mismo que {@link #newInstance(String, ClassLoader)}, con el nombre nuevo.
     *
     * @param factoryId el nombre de la clase; null cae en {@link #newFactory()}
     * @param classLoader el cargador con que buscarla; null usa el del contexto
     * @return la fabrica; nunca null
     * @throws FactoryConfigurationError si la clase no se puede cargar o no es una fabrica
     */
    public static XMLInputFactory newFactory(String factoryId, ClassLoader classLoader) {
        if (factoryId == null) {
            return newFactory();
        }
        return (XMLInputFactory) Factories.instantiate(factoryId, classLoader, XMLInputFactory.class);
    }

    // ---- lectores de cursor -----------------------------------------------------------------

    /**
     * Un lector de cursor sobre un {@link Reader}.
     *
     * @param reader de donde leer
     * @return el lector, parado antes del primer evento
     * @throws XMLStreamException si no se puede empezar a leer
     */
    public abstract XMLStreamReader createXMLStreamReader(Reader reader) throws XMLStreamException;

    /**
     * Un lector de cursor sobre un {@link Source}.
     *
     * @param source de donde leer
     * @return el lector
     * @throws XMLStreamException si el tipo de {@code Source} no se soporta o no se puede leer
     */
    public abstract XMLStreamReader createXMLStreamReader(Source source) throws XMLStreamException;

    /**
     * Un lector de cursor sobre un flujo de bytes.
     *
     * @param stream de donde leer
     * @return el lector
     * @throws XMLStreamException si no se puede empezar a leer
     */
    public abstract XMLStreamReader createXMLStreamReader(InputStream stream)
            throws XMLStreamException;

    /**
     * Un lector de cursor sobre un flujo de bytes con la codificacion dada.
     *
     * @param stream de donde leer
     * @param encoding la codificacion, que gana sobre la que declare el documento
     * @return el lector
     * @throws XMLStreamException si no se puede empezar a leer
     */
    public abstract XMLStreamReader createXMLStreamReader(InputStream stream, String encoding)
            throws XMLStreamException;

    /**
     * Un lector de cursor sobre un flujo de bytes, recordando de donde vino.
     *
     * @param systemId el identificador de sistema, para los mensajes y las ubicaciones
     * @param stream de donde leer
     * @return el lector
     * @throws XMLStreamException si no se puede empezar a leer
     */
    public abstract XMLStreamReader createXMLStreamReader(String systemId, InputStream stream)
            throws XMLStreamException;

    /**
     * Un lector de cursor sobre un {@link Reader}, recordando de donde vino.
     *
     * @param systemId el identificador de sistema, para los mensajes y las ubicaciones
     * @param reader de donde leer
     * @return el lector
     * @throws XMLStreamException si no se puede empezar a leer
     */
    public abstract XMLStreamReader createXMLStreamReader(String systemId, Reader reader)
            throws XMLStreamException;

    // ---- lectores de eventos ----------------------------------------------------------------

    /**
     * Un lector de eventos sobre un {@link Reader}.
     *
     * @param reader de donde leer
     * @return el lector
     * @throws XMLStreamException si no se puede empezar a leer
     */
    public abstract XMLEventReader createXMLEventReader(Reader reader) throws XMLStreamException;

    /**
     * Un lector de eventos sobre un {@link Reader}, recordando de donde vino.
     *
     * @param systemId el identificador de sistema
     * @param reader de donde leer
     * @return el lector
     * @throws XMLStreamException si no se puede empezar a leer
     */
    public abstract XMLEventReader createXMLEventReader(String systemId, Reader reader)
            throws XMLStreamException;

    /**
     * Un lector de eventos montado sobre un lector de cursor que ya existe.
     *
     * <p>Es el puente entre los dos modelos: el cursor sigue haciendo el trabajo y este envuelve
     * cada posicion en un evento propio.
     *
     * @param reader el lector de cursor
     * @return el lector de eventos
     * @throws XMLStreamException si no se puede envolver
     */
    public abstract XMLEventReader createXMLEventReader(XMLStreamReader reader)
            throws XMLStreamException;

    /**
     * Un lector de eventos sobre un {@link Source}.
     *
     * @param source de donde leer
     * @return el lector
     * @throws XMLStreamException si el tipo de {@code Source} no se soporta o no se puede leer
     */
    public abstract XMLEventReader createXMLEventReader(Source source) throws XMLStreamException;

    /**
     * Un lector de eventos sobre un flujo de bytes.
     *
     * @param stream de donde leer
     * @return el lector
     * @throws XMLStreamException si no se puede empezar a leer
     */
    public abstract XMLEventReader createXMLEventReader(InputStream stream)
            throws XMLStreamException;

    /**
     * Un lector de eventos sobre un flujo de bytes con la codificacion dada.
     *
     * @param stream de donde leer
     * @param encoding la codificacion, que gana sobre la que declare el documento
     * @return el lector
     * @throws XMLStreamException si no se puede empezar a leer
     */
    public abstract XMLEventReader createXMLEventReader(InputStream stream, String encoding)
            throws XMLStreamException;

    /**
     * Un lector de eventos sobre un flujo de bytes, recordando de donde vino.
     *
     * @param systemId el identificador de sistema
     * @param stream de donde leer
     * @return el lector
     * @throws XMLStreamException si no se puede empezar a leer
     */
    public abstract XMLEventReader createXMLEventReader(String systemId, InputStream stream)
            throws XMLStreamException;

    // ---- filtros ----------------------------------------------------------------------------

    /**
     * Un lector de cursor que solo se detiene en los eventos que el filtro acepta.
     *
     * @param reader el lector de abajo
     * @param filter que eventos dejar pasar
     * @return el lector filtrado
     * @throws XMLStreamException si no se puede construir
     */
    public abstract XMLStreamReader createFilteredReader(XMLStreamReader reader, StreamFilter filter)
            throws XMLStreamException;

    /**
     * Un lector de eventos que solo entrega los eventos que el filtro acepta.
     *
     * @param reader el lector de abajo
     * @param filter que eventos dejar pasar
     * @return el lector filtrado
     * @throws XMLStreamException si no se puede construir
     */
    public abstract XMLEventReader createFilteredReader(XMLEventReader reader, EventFilter filter)
            throws XMLStreamException;

    // ---- configuracion ----------------------------------------------------------------------

    /**
     * El resolutor de entidades configurado.
     *
     * @return el resolutor, o null si no hay
     */
    public abstract XMLResolver getXMLResolver();

    /**
     * Pone el resolutor de entidades.
     *
     * @param resolver el resolutor
     */
    public abstract void setXMLResolver(XMLResolver resolver);

    /**
     * El informador de avisos configurado.
     *
     * @return el informador, o null si no hay
     */
    public abstract XMLReporter getXMLReporter();

    /**
     * Pone el informador de avisos.
     *
     * @param reporter el informador
     */
    public abstract void setXMLReporter(XMLReporter reporter);

    /**
     * Cambia una propiedad de la fabrica.
     *
     * @param name el nombre de la propiedad
     * @param value el valor
     * @throws IllegalArgumentException si la propiedad no se conoce, o se conoce pero es de solo
     *     lectura en esta implementacion y el valor pedido no es el que tiene
     */
    public abstract void setProperty(String name, Object value) throws IllegalArgumentException;

    /**
     * El valor de una propiedad.
     *
     * @param name el nombre de la propiedad
     * @return el valor
     * @throws IllegalArgumentException si la propiedad no se conoce
     */
    public abstract Object getProperty(String name) throws IllegalArgumentException;

    /**
     * Si la fabrica conoce una propiedad.
     *
     * <p>Conocerla no es lo mismo que dejar cambiarla: ver {@link #setProperty}.
     *
     * @param name el nombre de la propiedad
     * @return true si la conoce
     */
    public abstract boolean isPropertySupported(String name);

    /**
     * Pone el constructor de eventos que va a usar el lector de eventos.
     *
     * @param allocator el constructor
     */
    public abstract void setEventAllocator(XMLEventAllocator allocator);

    /**
     * El constructor de eventos configurado.
     *
     * @return el constructor; nunca null
     */
    public abstract XMLEventAllocator getEventAllocator();
}
