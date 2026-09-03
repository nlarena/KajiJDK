package javax.xml.stream;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.XMLEventConsumer;

/**
 * KajiLibrary's javax.xml.stream.XMLEventWriter -- escribir XML entregando objetos de evento en vez
 * de llamando un metodo por pieza.
 *
 * <p>Es a {@link XMLStreamWriter} lo que {@link XMLEventReader} es a {@link XMLStreamReader}. La
 * ventaja practica esta en {@link #add(XMLEventReader)}: enchufar un lector de eventos a un escritor
 * copia un documento entero en una linea, y con un {@link EventFilter} en el medio se lo filtra sin
 * escribir el bucle. Eso con el modelo de cursor no se puede porque un evento de cursor no es un
 * objeto que se pueda pasar de mano en mano.
 *
 * <p>Extiende {@link XMLEventConsumer}, que es lo que permite que un
 * {@link javax.xml.stream.util.XMLEventAllocator} escriba directo aca sin saber que hay del otro
 * lado.
 *
 * <h2>Que hay escrito aca</h2>
 *
 * <p>Los nueve metodos. Sin implementacion, por lo mismo que el resto del paquete: no hay proveedor
 * de StAX en esta biblioteca. Ver {@link XMLOutputFactory}.
 */
public interface XMLEventWriter extends XMLEventConsumer {

    /**
     * Vuelca al destino lo que este en el buffer.
     *
     * @throws XMLStreamException si falla la escritura
     */
    void flush() throws XMLStreamException;

    /**
     * Libera lo que el escritor tenga tomado, sin cerrar el flujo de destino.
     *
     * @throws XMLStreamException si falla
     */
    void close() throws XMLStreamException;

    /**
     * Escribe un evento.
     *
     * @param event el evento
     * @throws XMLStreamException si falla la escritura
     */
    void add(XMLEvent event) throws XMLStreamException;

    /**
     * Escribe todo lo que quede en un lector de eventos, y lo deja vacio.
     *
     * <p>La copia de un documento en una linea; ver el encabezado.
     *
     * @param reader de donde sacar los eventos
     * @throws XMLStreamException si falla la lectura o la escritura
     */
    void add(XMLEventReader reader) throws XMLStreamException;

    /**
     * El prefijo ligado a un espacio de nombres, o null.
     *
     * @param uri el espacio de nombres
     * @return el prefijo
     * @throws XMLStreamException si falla
     */
    String getPrefix(String uri) throws XMLStreamException;

    /**
     * Liga un prefijo a un espacio de nombres para lo que se escriba de aca en mas.
     *
     * <p>Como en {@link XMLStreamWriter#setPrefix}, no escribe la declaracion.
     *
     * @param prefix el prefijo
     * @param uri el espacio de nombres
     * @throws XMLStreamException si falla
     */
    void setPrefix(String prefix, String uri) throws XMLStreamException;

    /**
     * Liga el espacio de nombres por omision, sin escribir la declaracion.
     *
     * @param uri el espacio de nombres
     * @throws XMLStreamException si falla
     */
    void setDefaultNamespace(String uri) throws XMLStreamException;

    /**
     * Reemplaza el contexto de espacios de nombres, solo antes del elemento raiz.
     *
     * @param context el contexto nuevo
     * @throws XMLStreamException si falla o si ya es tarde
     */
    void setNamespaceContext(NamespaceContext context) throws XMLStreamException;

    /**
     * Las ligaduras vigentes.
     *
     * @return el contexto
     */
    NamespaceContext getNamespaceContext();
}
