package java.net;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// La implementacion de `CookieHandler` que trae la plataforma: un almacen mas una politica.
//
// La separacion en tres piezas --handler, almacen, politica-- es lo que hace que se pueda cambiar
// una sin tocar las otras: persistir en disco es cambiar el `CookieStore`, endurecer el criterio es
// cambiar la `CookiePolicy`, y el codigo que hace pedidos no se entera de ninguna de las dos.
//
// El trabajo real de esta clase esta en `put`, y es rellenar lo que el servidor no dijo: una cookie
// sin `Path` hereda el **directorio** de la pagina que la mando (no la pagina), y una sin `Domain`
// hereda el host. Los dos defaults son del RFC 2965 y los dos son restrictivos a proposito -- una
// cookie sin atributos vuelve al lugar mas chico posible, no al mas grande.
//
// El otro detalle que se pasa por alto: el orden en que salen las cookies en `get` **es parte del
// protocolo**. El RFC 2965 pide las de ruta mas especifica primero, porque un servidor que reciba
// dos cookies del mismo nombre se queda con la primera. Esta ordenado en `sortByPathAndAge`.
//
// ===========================================================================================
// POR QUE ESTO ENTRA COMPLETO SIN RED
// ===========================================================================================
//
// `get` y `put` reciben y devuelven **mapas de headers**. No abren nada, no mandan nada: traducen
// entre texto de headers y el almacen. Todo el contrato se cumple entero aca. Lo que KajiJDK no
// tiene es un cliente HTTP que llame a estos metodos, pero eso es una ausencia del otro lado de la
// interfaz, no un agujero en esta clase.
//
// Nada omitido.
public class CookieManager extends CookieHandler {

    private CookiePolicy policyCallback;
    private CookieStore cookieJar;

    /** Un manager con almacen en memoria y la politica {@link CookiePolicy#ACCEPT_ORIGINAL_SERVER}. */
    public CookieManager() {
        this(null, null);
    }

    /**
     * Un manager con ese almacen y esa politica; null en cualquiera de los dos toma el
     * predeterminado.
     */
    public CookieManager(CookieStore store, CookiePolicy cookiePolicy) {
        if (cookiePolicy == null) {
            this.policyCallback = CookiePolicy.ACCEPT_ORIGINAL_SERVER;
        } else {
            this.policyCallback = cookiePolicy;
        }
        if (store == null) {
            this.cookieJar = new InMemoryCookieStore();
        } else {
            this.cookieJar = store;
        }
    }

    /** Cambia la politica. El almacen no se puede cambiar: las cookies ya guardadas son suyas. */
    public void setCookiePolicy(CookiePolicy cookiePolicy) {
        if (cookiePolicy != null) {
            this.policyCallback = cookiePolicy;
        }
    }

    public CookieStore getCookieStore() {
        return this.cookieJar;
    }

    public Map<String, List<String>> get(URI uri, Map<String, List<String>> requestHeaders)
            throws IOException {
        if (uri == null || requestHeaders == null) {
            throw new IllegalArgumentException("Argument is null");
        }
        Map<String, List<String>> cookieMap = new HashMap<String, List<String>>();
        if (this.cookieJar == null) {
            return Collections.unmodifiableMap(cookieMap);
        }
        boolean secureLink = "https".equalsIgnoreCase(uri.getScheme());
        List<HttpCookie> cookies = new ArrayList<HttpCookie>();
        String path = uri.getPath();
        if (path == null || path.length() == 0) {
            path = "/";
        }
        List<HttpCookie> candidates = this.cookieJar.get(uri);
        int i = 0;
        while (i < candidates.size()) {
            HttpCookie cookie = candidates.get(i);
            if (pathMatches(path, cookie.getPath()) && (secureLink || !cookie.getSecure())) {
                cookies.add(cookie);
            }
            i = i + 1;
        }
        cookieMap.put("Cookie", sortByPathAndAge(cookies));
        return Collections.unmodifiableMap(cookieMap);
    }

    public void put(URI uri, Map<String, List<String>> responseHeaders) throws IOException {
        if (uri == null || responseHeaders == null) {
            throw new IllegalArgumentException("Argument is null");
        }
        if (this.cookieJar == null) {
            return;
        }
        for (String headerKey : responseHeaders.keySet()) {
            if (headerKey == null) {
                continue;
            }
            if (!headerKey.equalsIgnoreCase("Set-Cookie2")
                    && !headerKey.equalsIgnoreCase("Set-Cookie")) {
                continue;
            }
            List<String> values = responseHeaders.get(headerKey);
            if (values == null) {
                continue;
            }
            int v = 0;
            while (v < values.size()) {
                String headerValue = values.get(v);
                v = v + 1;
                List<HttpCookie> cookies;
                try {
                    cookies = HttpCookie.parse(headerValue);
                } catch (IllegalArgumentException e) {
                    // Un header roto no invalida los otros del mismo mensaje.
                    continue;
                }
                int c = 0;
                while (c < cookies.size()) {
                    HttpCookie cookie = cookies.get(c);
                    c = c + 1;
                    if (cookie.getPath() == null) {
                        // El **directorio** de la pagina, no la pagina: /dir/pag da "/dir/".
                        String p = uri.getPath();
                        if (p == null) {
                            p = "/";
                        }
                        if (!p.endsWith("/")) {
                            int slash = p.lastIndexOf('/');
                            if (slash > 0) {
                                p = p.substring(0, slash + 1);
                            } else {
                                p = "/";
                            }
                        }
                        cookie.setPath(p);
                    }
                    if (cookie.getDomain() == null) {
                        String host = uri.getHost();
                        // Un host sin puntos ("intranet") no puede matchear ningun dominio con
                        // punto interno; el sufijo ".local" es lo que le da uno.
                        if (host != null && host.indexOf('.') == -1) {
                            host = host + ".local";
                        }
                        cookie.setDomain(host);
                    }
                    String ports = cookie.getPortlist();
                    if (ports != null) {
                        int port = uri.getPort();
                        if (port == -1) {
                            port = "https".equals(uri.getScheme()) ? 443 : 80;
                        }
                        if (ports.length() == 0) {
                            // Port sin valor significa "solo el puerto por el que llegue".
                            cookie.setPortlist("" + port);
                            if (this.shouldAcceptInternal(uri, cookie)) {
                                this.cookieJar.add(uri, cookie);
                            }
                        } else if (isInPortList(ports, port)) {
                            if (this.shouldAcceptInternal(uri, cookie)) {
                                this.cookieJar.add(uri, cookie);
                            }
                        }
                    } else if (this.shouldAcceptInternal(uri, cookie)) {
                        this.cookieJar.add(uri, cookie);
                    }
                }
            }
        }
    }

    // Una politica que explota no puede decidir, y "no se pudo decidir" es "no". Dejar que la
    // excepcion suba haria que una politica mal escrita rompiera el pedido entero.
    private boolean shouldAcceptInternal(URI uri, HttpCookie cookie) {
        try {
            return this.policyCallback.shouldAccept(uri, cookie);
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean isInPortList(String lst, int port) {
        int i = lst.indexOf(",");
        int val = -1;
        while (i > 0) {
            try {
                val = Integer.parseInt(lst.substring(0, i));
                if (val == port) {
                    return true;
                }
            } catch (NumberFormatException ignored) {
                // Un puerto ilegible se saltea; los otros de la lista siguen valiendo.
            }
            lst = lst.substring(i + 1);
            i = lst.indexOf(",");
        }
        if (lst.length() > 0) {
            try {
                val = Integer.parseInt(lst);
                if (val == port) {
                    return true;
                }
            } catch (NumberFormatException ignored) {
                // idem
            }
        }
        return false;
    }

    // La ruta de la cookie tiene que ser un prefijo de la del pedido. Es prefijo textual y no de
    // segmentos, igual que en el JDK: "/ab" cubre a "/abc".
    private static boolean pathMatches(String path, String pathToMatchWith) {
        if (path == pathToMatchWith) {
            return true;
        }
        if (path == null || pathToMatchWith == null) {
            return false;
        }
        return path.startsWith(pathToMatchWith);
    }

    // Ruta mas larga primero, y a igual ruta la mas vieja primero. Ver la cabecera: el orden es
    // parte del protocolo, no una comodidad.
    static List<String> sortByPathAndAge(List<HttpCookie> cookies) {
        List<HttpCookie> copy = new ArrayList<HttpCookie>(cookies);
        int i = 1;
        while (i < copy.size()) {
            HttpCookie c = copy.get(i);
            int j = i - 1;
            while (j >= 0 && precedes(c, copy.get(j))) {
                copy.set(j + 1, copy.get(j));
                j = j - 1;
            }
            copy.set(j + 1, c);
            i = i + 1;
        }
        List<String> out = new ArrayList<String>();
        i = 0;
        while (i < copy.size()) {
            out.add(copy.get(i).toString());
            i = i + 1;
        }
        return out;
    }

    private static boolean precedes(HttpCookie a, HttpCookie b) {
        int la = a.getPath() == null ? 0 : a.getPath().length();
        int lb = b.getPath() == null ? 0 : b.getPath().length();
        if (la != lb) {
            return la > lb;
        }
        return a.getCreationTime() < b.getCreationTime();
    }
}
