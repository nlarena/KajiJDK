package javax.xml.stream;

import javax.xml.stream.events.XMLEvent;

/**
 * KajiLibrary's javax.xml.stream.EventFilter -- el mismo criterio que {@link StreamFilter}, pero
 * para el modelo de eventos.
 *
 * <p>La diferencia con {@link StreamFilter} no es cosmetica: aca el filtro recibe un
 * {@link XMLEvent}, que es un objeto completo e inmutable, y no un lector parado en una posicion.
 * Eso significa que **si** se lo puede guardar, comparar con otro y mirar despues, y que no existe
 * la regla de "no avanzar el lector" porque no hay lector que avanzar.
 *
 * <p>Ese es exactamente el intercambio entre los dos modelos de StAX: el de cursor no aloca un
 * objeto por evento y a cambio te da algo valido solo hasta el proximo {@code next()}; el de
 * eventos aloca y a cambio te da algo que dura.
 */
public interface EventFilter {

    /**
     * Decide si el evento se deja pasar.
     *
     * @param event el evento a juzgar
     * @return true para que llegue al llamador, false para saltearlo
     */
    boolean accept(XMLEvent event);
}
