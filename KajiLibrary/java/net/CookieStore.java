package java.net;

import java.util.List;

// Donde viven las cookies guardadas.
//
// El almacen se define como interfaz --y no como una clase concreta-- porque la persistencia es
// decision de la aplicacion: en memoria mientras dure el proceso, en un archivo, o compartida entre
// varios. `CookieManager` usa uno en memoria si no le dan otro.
//
// Las dos consultas no son la misma: `get(URI)` devuelve las que **le corresponden** a esa URI
// aplicando las reglas de dominio, y `getCookies()` devuelve todas. La primera es la que usa el
// cliente HTTP; la segunda, quien quiera inspeccionar o exportar el almacen.
//
// Guardar y buscar es computacion pura. Nada omitido.
public interface CookieStore {

    /**
     * Guarda {@code cookie} como venida de {@code uri}.
     *
     * <p>Si ya habia una con el mismo nombre, dominio y ruta, la reemplaza: esos tres campos son la
     * identidad de una cookie (ver {@link HttpCookie#equals}).
     */
    void add(URI uri, HttpCookie cookie);

    /** Las cookies que le corresponden a {@code uri}, ya descartadas las vencidas. */
    List<HttpCookie> get(URI uri);

    /** Todas las cookies vivas del almacen. */
    List<HttpCookie> getCookies();

    /** Las URIs que tienen alguna cookie asociada. */
    List<URI> getURIs();

    /** Saca esa cookie. Devuelve si estaba. */
    boolean remove(URI uri, HttpCookie cookie);

    /** Vacia el almacen. Devuelve si habia algo que sacar. */
    boolean removeAll();
}
