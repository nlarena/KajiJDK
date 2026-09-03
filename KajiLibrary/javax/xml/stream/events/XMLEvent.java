package javax.xml.stream.events;

import java.io.Writer;

import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

/**
 * KajiLibrary's javax.xml.stream.events.XMLEvent -- una pieza del documento como objeto propio, que
 * se puede guardar.
 *
 * <h2>El otro modelo de StAX, y por que hay dos</h2>
 *
 * <p>{@link javax.xml.stream.XMLStreamReader} y este paquete son las dos caras de StAX y resuelven
 * la misma lectura con un compromiso opuesto. El lector de cursor <b>es</b> el evento: no crea
 * objetos, y a cambio nada de lo que devuelve sobrevive al proximo {@code next()}. Aca cada evento
 * es un objeto inmutable e independiente: se puede guardar en una lista, comparar con otro, o pasar
 * a otro hilo.
 *
 * <p>El precio es un objeto por evento, que en un documento grande es exactamente el costo que el
 * modelo de cursor existe para evitar. La regla practica: cursor cuando se procesa al vuelo y se
 * descarta, eventos cuando hace falta mirar hacia atras --un mapeo que necesita el elemento padre,
 * un buffer que se reordena, un filtro que decide despues--.
 *
 * <h2>Los diez {@code isXxx} y los tres {@code asXxx}</h2>
 *
 * <p>La jerarquia usa {@code instanceof} disfrazado: {@link #isStartElement()} y compania dicen de
 * que subtipo es, y {@link #asStartElement()} hace el downcast. Es de antes de que el lenguaje
 * tuviera {@code instanceof} con patron, y sigue siendo la forma canonica de recorrer un
 * {@link javax.xml.stream.XMLEventReader}.
 *
 * <p>{@link #getEventType()} devuelve la constante de {@link XMLStreamConstants} correspondiente, o
 * sea que los dos modelos comparten el vocabulario de tipos de evento; eso es lo que permite
 * convertir de uno al otro sin traducir nada.
 *
 * <h2>Que hay aca y que no</h2>
 *
 * <p>La interfaz completa. Las implementaciones de esta biblioteca son las que devuelve
 * {@link javax.xml.stream.XMLEventFactory}, que construye eventos <b>a partir de datos que le
 * pasa el llamador</b> y no de un documento: para eso no hace falta ningun parser, y por eso esa
 * fabrica si funciona. Lo que no hay es un evento que salga de leer XML, porque no hay parser; ver
 * {@link javax.xml.stream.XMLInputFactory}.
 */
public interface XMLEvent extends XMLStreamConstants {

    /**
     * Que clase de evento es, con el vocabulario de {@link XMLStreamConstants}.
     *
     * @return {@link XMLStreamConstants#START_ELEMENT}, {@link XMLStreamConstants#CHARACTERS}, etc.
     */
    int getEventType();

    /**
     * Donde estaba este evento en el documento.
     *
     * <p>A diferencia del modelo de cursor, aca la ubicacion se guarda con el evento, asi que sigue
     * siendo util despues de haber seguido leyendo.
     *
     * @return la ubicacion; puede ser una sin datos, no null
     */
    Location getLocation();

    /**
     * Si es la apertura de un elemento.
     *
     * @return true si {@link #asStartElement()} va a andar
     */
    boolean isStartElement();

    /**
     * Si es un atributo.
     *
     * <p>Un atributo es un evento pero <b>no</b> aparece en el flujo: viene colgado del
     * {@link StartElement}. El tipo existe para poder tratarlo como evento cuando hace falta.
     *
     * @return true si es un {@link Attribute}
     */
    boolean isAttribute();

    /**
     * Si es una declaracion de espacio de nombres.
     *
     * @return true si es un {@link Namespace}
     */
    boolean isNamespace();

    /**
     * Si es el cierre de un elemento.
     *
     * @return true si {@link #asEndElement()} va a andar
     */
    boolean isEndElement();

    /**
     * Si es una referencia a entidad.
     *
     * @return true si es un {@link EntityReference}
     */
    boolean isEntityReference();

    /**
     * Si es una instruccion de procesamiento.
     *
     * @return true si es un {@link ProcessingInstruction}
     */
    boolean isProcessingInstruction();

    /**
     * Si es texto.
     *
     * <p>Contesta true tambien para {@link XMLStreamConstants#CDATA} y para
     * {@link XMLStreamConstants#SPACE}: los tres son {@link Characters} y se distinguen con
     * {@link Characters#isCData()} y {@link Characters#isIgnorableWhiteSpace()}.
     *
     * @return true si {@link #asCharacters()} va a andar
     */
    boolean isCharacters();

    /**
     * Si es el comienzo del documento.
     *
     * @return true si es un {@link StartDocument}
     */
    boolean isStartDocument();

    /**
     * Si es el final del documento.
     *
     * @return true si es un {@link EndDocument}
     */
    boolean isEndDocument();

    /**
     * Este evento como apertura de elemento.
     *
     * @return el mismo objeto, con el tipo mas preciso
     * @throws ClassCastException si no es una apertura de elemento
     */
    StartElement asStartElement();

    /**
     * Este evento como cierre de elemento.
     *
     * @return el mismo objeto, con el tipo mas preciso
     * @throws ClassCastException si no es un cierre de elemento
     */
    EndElement asEndElement();

    /**
     * Este evento como texto.
     *
     * @return el mismo objeto, con el tipo mas preciso
     * @throws ClassCastException si no es texto
     */
    Characters asCharacters();

    /**
     * El tipo de esquema de este evento, si alguien se lo asigno.
     *
     * <p>Existe para las implementaciones que validan mientras leen y pueden anotar cada evento con
     * el tipo que le corresponde. Una que no valide devuelve null, que es lo normal.
     *
     * @return el nombre calificado del tipo, o null
     */
    QName getSchemaType();

    /**
     * Escribe este evento como XML.
     *
     * <p>Es la operacion que hace que un {@code List<XMLEvent>} sea un documento: un bucle que
     * llama a esto sobre cada evento escribe el documento entero. El texto se escapa como
     * corresponda al tipo de evento.
     *
     * @param writer a donde escribir; no puede ser null
     * @throws XMLStreamException si el escritor falla
     */
    void writeAsEncodedUnicode(Writer writer) throws XMLStreamException;
}
