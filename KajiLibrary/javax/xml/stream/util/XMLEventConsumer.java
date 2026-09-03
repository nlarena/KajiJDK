package javax.xml.stream.util;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/**
 * KajiLibrary's javax.xml.stream.util.XMLEventConsumer -- todo lo que sabe recibir un evento.
 *
 * <p>Una interfaz de un solo metodo, y esa es toda la idea: separar "algo a lo que se le pueden dar
 * eventos" de "algo que escribe XML". {@link javax.xml.stream.XMLEventWriter} la extiende y es la
 * implementacion obvia, pero un buffer que junta eventos en una lista, un filtro que reenvia
 * algunos, o un validador que los mira al pasar tambien son consumidores y no escriben nada.
 *
 * <p>Es lo que le permite a {@link XMLEventAllocator#allocate(javax.xml.stream.XMLStreamReader,
 * XMLEventConsumer)} entregar el evento sin saber a donde va.
 */
public interface XMLEventConsumer {

    /**
     * Recibe un evento.
     *
     * @param event el evento; que se acepte o no un null depende de la implementacion
     * @throws XMLStreamException si el consumidor no lo puede aceptar
     */
    void add(XMLEvent event) throws XMLStreamException;
}
