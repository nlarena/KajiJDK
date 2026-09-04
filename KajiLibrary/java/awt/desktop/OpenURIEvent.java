package java.awt.desktop;

import java.net.URI;

/**
 * KajiLibrary's java.awt.desktop.OpenURIEvent -- el sistema pide abrir una direccion.
 *
 * <p>Lo entrega {@link OpenURIHandler}. Llega cuando alguien abre un enlace de un esquema que el
 * programa declaro manejar -- {@code mailto:}, o uno propio como {@code miapp:}.
 *
 * <p>Es la puerta de entrada de datos que vienen de <b>afuera del programa</b>, tipicamente de una
 * pagina web. Lo que llegue por aca hay que validarlo como cualquier entrada no confiable: el esquema
 * no garantiza nada sobre el resto de la direccion.
 */
public final class OpenURIEvent extends AppEvent {

    private static final long serialVersionUID = 221209100935933476L;

    /** La direccion. */
    final URI uri;

    /** @param uri la direccion a abrir */
    public OpenURIEvent(final URI uri) {
        this.uri = uri;
    }

    /** La direccion. Ver la nota de la clase: no es confiable. */
    public URI getURI() {
        return this.uri;
    }
}
