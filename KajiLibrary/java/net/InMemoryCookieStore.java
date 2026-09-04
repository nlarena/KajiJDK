package java.net;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// El almacen que usa `CookieManager` cuando no le dan otro: todo en memoria y nada en disco.
//
// No es publica y no forma parte de la API -- se llega a ella por `new CookieManager()` y se la usa
// a traves de `CookieStore`. El JDK hace exactamente lo mismo con una clase del mismo nombre.
//
// Guarda cada cookie junto con la **URI efectiva** de donde vino, que es la URI recortada a esquema
// y host ("http://ejemplo.org"). El recorte es lo que hace que dos paginas del mismo sitio compartan
// cookies sin que la ruta ni el puerto las separen; las reglas de ruta las aplica `CookieManager`
// mas arriba, que es donde corresponde.
final class InMemoryCookieStore implements CookieStore {

    // Una lista y no un mapa: el almacen es chico y toda operacion interesante --matcheo de
    // dominios, descarte de vencidas-- recorre igual. Un indice por dominio aca solo agregaria un
    // estado mas que mantener coherente.
    private final List<URI> uris = new ArrayList<URI>();
    private final List<HttpCookie> cookies = new ArrayList<HttpCookie>();

    InMemoryCookieStore() {
    }

    public void add(URI uri, HttpCookie cookie) {
        if (cookie == null) {
            throw new NullPointerException("cookie is null");
        }
        synchronized (this) {
            // Una cookie con el mismo nombre, dominio y ruta es **la misma** cookie con otro valor,
            // asi que pisa a la anterior en vez de sumarse.
            int i = this.cookies.indexOf(cookie);
            while (i >= 0) {
                this.cookies.remove(i);
                this.uris.remove(i);
                i = this.cookies.indexOf(cookie);
            }
            // maxAge cero significa "vencida al nacer": es como un servidor borra una cookie.
            // Guardarla seria guardar exactamente lo que pidio que se borre.
            if (cookie.getMaxAge() != 0) {
                this.cookies.add(cookie);
                this.uris.add(effectiveUri(uri));
            }
        }
    }

    public List<HttpCookie> get(URI uri) {
        if (uri == null) {
            throw new NullPointerException();
        }
        List<HttpCookie> out = new ArrayList<HttpCookie>();
        synchronized (this) {
            this.purge();
            boolean secureLink = "https".equalsIgnoreCase(uri.getScheme());
            String host = uri.getHost();
            URI eff = effectiveUri(uri);
            int i = 0;
            while (i < this.cookies.size()) {
                HttpCookie c = this.cookies.get(i);
                // Una cookie marcada `Secure` no sale por un enlace que no lo es: ese es todo su
                // proposito.
                if (secureLink || !c.getSecure()) {
                    boolean matches;
                    if (c.getDomain() != null) {
                        matches = HttpCookie.domainMatches(c.getDomain(), host);
                    } else {
                        // Sin dominio, solo vuelve al mismo sitio del que vino.
                        matches = eff != null && eff.equals(this.uris.get(i));
                    }
                    if (matches && !out.contains(c)) {
                        out.add(c);
                    }
                }
                i = i + 1;
            }
        }
        return out;
    }

    public List<HttpCookie> getCookies() {
        synchronized (this) {
            this.purge();
            return Collections.unmodifiableList(new ArrayList<HttpCookie>(this.cookies));
        }
    }

    public List<URI> getURIs() {
        List<URI> out = new ArrayList<URI>();
        synchronized (this) {
            int i = 0;
            while (i < this.uris.size()) {
                URI u = this.uris.get(i);
                if (u != null && !out.contains(u)) {
                    out.add(u);
                }
                i = i + 1;
            }
        }
        return Collections.unmodifiableList(out);
    }

    public boolean remove(URI uri, HttpCookie ck) {
        if (ck == null) {
            throw new NullPointerException("cookie is null");
        }
        synchronized (this) {
            int i = this.cookies.indexOf(ck);
            if (i < 0) {
                return false;
            }
            this.cookies.remove(i);
            this.uris.remove(i);
            return true;
        }
    }

    public boolean removeAll() {
        synchronized (this) {
            if (this.cookies.isEmpty()) {
                return false;
            }
            this.cookies.clear();
            this.uris.clear();
            return true;
        }
    }

    // Las vencidas se sacan al consultar y no con un temporizador: un almacen que no se consulta no
    // le importa a nadie, y un hilo de limpieza seria mas maquinaria que beneficio.
    private void purge() {
        int i = 0;
        while (i < this.cookies.size()) {
            if (this.cookies.get(i).hasExpired()) {
                this.cookies.remove(i);
                this.uris.remove(i);
            } else {
                i = i + 1;
            }
        }
    }

    // La URI recortada a esquema y host. Si no se puede armar, se guarda la original: es preferible
    // una clave demasiado especifica --que solo hace que la cookie vuelva a menos lugares-- a
    // perderla.
    private static URI effectiveUri(URI uri) {
        if (uri == null) {
            return null;
        }
        try {
            return new URI("http", uri.getHost(), null, null, null);
        } catch (URISyntaxException e) {
            return uri;
        }
    }
}
