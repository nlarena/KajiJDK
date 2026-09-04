package java.net;

// La decision de aceptar o no una cookie.
//
// Es una interfaz de un solo metodo con tres implementaciones ya hechas, y las tres constantes son
// la parte util: `ACCEPT_ORIGINAL_SERVER` --la que usa `CookieManager` por defecto-- es la unica
// defensa que trae la plataforma contra que un servidor le ponga cookies a otro dominio. Sin ella,
// un aviso embebido en una pagina podria escribir cookies de la pagina que lo hospeda.
//
// La politica se separa del almacen a proposito: guardar y decidir son dos decisiones distintas, y
// una aplicacion suele querer cambiar solo una de las dos.
//
// Decidir es computacion pura. Nada omitido.
public interface CookiePolicy {

    /** Acepta todas. Util para pruebas; en produccion es una politica sin defensa. */
    CookiePolicy ACCEPT_ALL = (uri, cookie) -> true;

    /** No acepta ninguna. */
    CookiePolicy ACCEPT_NONE = (uri, cookie) -> false;

    /** Solo del servidor que la manda: el dominio de la cookie tiene que cubrir al host de la URI. */
    CookiePolicy ACCEPT_ORIGINAL_SERVER = (uri, cookie) -> {
        if (uri == null || cookie == null) {
            return false;
        }
        return HttpCookie.domainMatches(cookie.getDomain(), uri.getHost());
    };

    /** Si {@code cookie}, llegada desde {@code uri}, se guarda. */
    boolean shouldAccept(URI uri, HttpCookie cookie);
}
