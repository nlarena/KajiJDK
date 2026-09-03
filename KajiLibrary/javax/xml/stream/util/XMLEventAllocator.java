package javax.xml.stream.util;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;

/**
 * KajiLibrary's javax.xml.stream.util.XMLEventAllocator -- el que convierte la posicion de un
 * cursor en un evento.
 *
 * <h2>El punto de extension entre los dos modelos</h2>
 *
 * <p>Un {@link javax.xml.stream.XMLEventReader} de esta biblioteca --y de casi todas-- es un
 * {@link XMLStreamReader} mas esto: el cursor avanza y el asignador fotografia cada posicion en un
 * objeto independiente. Que sea reemplazable
 * ({@link javax.xml.stream.XMLInputFactory#ALLOCATOR}) permite devolver implementaciones de evento
 * propias --con campos extra, o mas baratas-- sin tocar el parser.
 *
 * <h2>{@link #newInstance()} es un metodo de instancia, y esta bien</h2>
 *
 * <p>Sorprende que la forma de conseguir un asignador sea pedirselo a otro asignador. La razon es
 * que la fabrica recibe <b>una</b> instancia por configuracion y cada lector necesita la suya,
 * porque un asignador puede tener estado --tablas de nombres, buffers reutilizados--. Un metodo
 * estatico no serviria: la fabrica no conoce la clase, solo tiene el objeto. O sea que la instancia
 * configurada funciona de prototipo.
 */
public interface XMLEventAllocator {

    /**
     * Otro asignador de la misma clase, para un lector nuevo.
     *
     * <p>El que recibe la llamada hace de prototipo; ver el encabezado.
     *
     * @return un asignador nuevo, sin compartir estado con este
     */
    XMLEventAllocator newInstance();

    /**
     * El evento que corresponde a la posicion actual del cursor.
     *
     * <p>No avanza el lector: lo lee donde esta.
     *
     * @param reader el cursor, parado en un evento
     * @return el evento como objeto propio
     * @throws XMLStreamException si el lector falla al ser consultado
     */
    XMLEvent allocate(XMLStreamReader reader) throws XMLStreamException;

    /**
     * Lo mismo, pero entregandoselo a un consumidor en vez de devolverlo.
     *
     * <p>La variante existe para los casos en que una posicion del cursor da <b>mas de un</b>
     * evento --un asignador que decida partir un texto largo, por ejemplo--, cosa que la que
     * devuelve uno solo no puede expresar.
     *
     * @param reader el cursor, parado en un evento
     * @param consumer a quien darle lo que salga
     * @throws XMLStreamException si el lector o el consumidor fallan
     */
    void allocate(XMLStreamReader reader, XMLEventConsumer consumer) throws XMLStreamException;
}
