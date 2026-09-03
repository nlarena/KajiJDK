package javax.xml.stream;

import java.util.Iterator;

import javax.xml.stream.events.XMLEvent;

/**
 * KajiLibrary's javax.xml.stream.XMLEventReader -- el otro modelo de StAX: un iterador de eventos
 * que **si** se pueden guardar.
 *
 * <p>Hace lo mismo que {@link XMLStreamReader} --tira del documento en vez de recibirlo empujado--
 * pero devuelve un {@link XMLEvent} por vez: un objeto completo, inmutable y con toda su informacion
 * adentro. La diferencia practica es una sola, y decide cual usar:
 *
 * <ul>
 *   <li>con el cursor, {@code getLocalName()} vale hasta el proximo {@code next()};
 *   <li>con los eventos, el objeto se puede guardar en una lista, comparar con otro de mas adelante
 *       y devolver desde un metodo.
 * </ul>
 *
 * <p>El precio es un objeto por evento. Vale la pena cuando hay que mirar hacia atras --emparejar
 * una apertura con su cierre, juntar los hijos de un elemento antes de decidir-- y no vale cuando se
 * procesa cada evento y se lo olvida.
 *
 * <h2>Un iterador con dos caras</h2>
 *
 * <p>Extiende {@link Iterator} de {@code Object} y no de {@code XMLEvent}, que es una herencia de
 * cuando la interfaz se escribio sin genericos. La consecuencia esta a la vista: {@link #next()}
 * devuelve {@code Object} y hay que castear, mientras que {@link #nextEvent()} devuelve el tipo
 * correcto. Son el mismo avance, con dos diferencias:
 *
 * <ul>
 *   <li>{@code nextEvent()} declara {@link XMLStreamException}, {@code next()} no --tiene que
 *       envolverla en una no chequeada--;
 *   <li>{@code next()} existe para que un {@code for} mejorado funcione, no porque sea mejor.
 * </ul>
 *
 * <p>Preferir siempre {@code nextEvent()}: el error de lectura llega como lo que es.
 *
 * <h2>Que hay escrito aca</h2>
 *
 * <p>Los siete metodos. Implementacion no hay --esta biblioteca no trae parser de XML-- pero si esta
 * {@link javax.xml.stream.util.EventReaderDelegate}, que es la clase base para envolver a uno ajeno
 * y filtrarlo o transformarlo.
 */
public interface XMLEventReader extends Iterator<Object> {

    /**
     * El proximo evento.
     *
     * @return el evento
     * @throws XMLStreamException si el documento esta mal formado o falla la lectura
     * @throws java.util.NoSuchElementException si ya no hay mas
     */
    XMLEvent nextEvent() throws XMLStreamException;

    /**
     * Si queda al menos un evento.
     *
     * <p>Redeclarado sin {@code throws} porque viene de {@link Iterator}: un error de lectura tiene
     * que salir por {@link #nextEvent()}, no por aca.
     *
     * @return true si hay mas
     */
    boolean hasNext();

    /**
     * Mira el proximo evento **sin** consumirlo.
     *
     * <p>Es lo que el modelo de cursor no puede dar, y la razon mas comun para elegir este modelo:
     * decidir que hacer segun lo que viene, sin haber avanzado todavia.
     *
     * @return el proximo evento, o null si no hay
     * @throws XMLStreamException si falla la lectura
     */
    XMLEvent peek() throws XMLStreamException;

    /**
     * El texto de un elemento de solo texto, dejando el lector despues de su cierre.
     *
     * @return el texto
     * @throws XMLStreamException si el evento actual no es una apertura o el elemento tiene hijos
     */
    String getElementText() throws XMLStreamException;

    /**
     * Saltea espacio, comentarios e instrucciones de proceso hasta la proxima etiqueta.
     *
     * @return el evento de apertura o de cierre
     * @throws XMLStreamException si encuentra algo que no sea salteable ni una etiqueta
     */
    XMLEvent nextTag() throws XMLStreamException;

    /**
     * El valor de una propiedad de la implementacion.
     *
     * @param name el nombre de la propiedad
     * @return el valor
     * @throws IllegalArgumentException si la propiedad no existe
     */
    Object getProperty(String name) throws IllegalArgumentException;

    /**
     * Libera lo que el lector tenga tomado, sin cerrar el flujo de origen.
     *
     * @throws XMLStreamException si falla
     */
    void close() throws XMLStreamException;
}
