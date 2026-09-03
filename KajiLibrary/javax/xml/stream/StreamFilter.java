package javax.xml.stream;

/**
 * KajiLibrary's javax.xml.stream.StreamFilter -- el criterio con que
 * {@link XMLInputFactory#createFilteredReader(XMLStreamReader, StreamFilter)} decide que eventos
 * dejar pasar.
 *
 * <p>Filtrar del lado del que tira es la ventaja concreta de StAX sobre SAX: el lector filtrado
 * **saltea** los eventos rechazados sin devolverlos, asi que el codigo de la aplicacion no ve nunca
 * lo que no pidio y tampoco paga por descartarlo. En SAX el filtro tiene que estar en el handler,
 * que igual se llama para todo.
 *
 * <p>El filtro se llama con el lector **parado** en el evento a juzgar, no con una copia: puede
 * mirar {@code getLocalName()}, los atributos, la profundidad. Eso es lo que lo hace util y lo que
 * obliga a la unica regla que tiene: <b>no puede avanzar el lector</b>. Un {@code accept} que llame
 * a {@code next()} le come eventos al recorrido de afuera y el resultado depende de cuando se
 * evalue el filtro, que es la definicion de un bug que no se reproduce.
 */
public interface StreamFilter {

    /**
     * Decide si el evento en que esta parado el lector se deja pasar.
     *
     * @param reader el lector, posicionado en el evento a juzgar; no se debe avanzar
     * @return true para que el evento llegue al llamador, false para saltearlo
     */
    boolean accept(XMLStreamReader reader);
}
