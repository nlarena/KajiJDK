package java.net;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

// Quien decide, para cada URI, por que proxy salir.
//
// La abstraccion tiene dos metodos y el segundo es el que la gente ignora: `connectFailed` es como
// el selector **aprende**. Si `select` propuso tres proxies y el primero no anduvo, el que intento
// conectarse tiene que avisarlo, y recien ahi el selector puede dejar de proponerlo. Sin ese
// camino de vuelta, un proxy caido se sigue eligiendo para siempre.
//
// ===========================================================================================
// EL SELECTOR POR DEFECTO EN KajiJDK
// ===========================================================================================
//
// En el JDK real, `getDefault()` devuelve un selector que lee la configuracion de proxies del
// sistema (`http.proxyHost` y companiia, o el registro de Windows). Aca devuelve uno que contesta
// siempre `DIRECT`.
//
// Eso no es una mentira, es la verdad de esta VM: no hay conexiones que enrutar, asi que no hay
// configuracion de proxy que leer, y "salis derecho" es la respuesta correcta y completa. El
// contrato de `select` es "decime por donde salir", y este selector lo cumple. Distinto seria un
// `select` que tirara `UnsupportedOperationException`: eso si dejaria colgado a quien lo llama.
//
// `setDefault` funciona de verdad, asi que quien quiera otra politica la instala y anda.
public abstract class ProxySelector {

    private static volatile ProxySelector theProxySelector = new StaticProxySelector(null);

    public ProxySelector() {
    }

    /** El selector en uso, o null si alguien instalo null. */
    public static ProxySelector getDefault() {
        return theProxySelector;
    }

    /** Instala el selector que va a usar toda la VM. */
    public static void setDefault(ProxySelector ps) {
        theProxySelector = ps;
    }

    /**
     * Los proxies por los que se puede llegar a {@code uri}, en orden de preferencia.
     *
     * <p>Nunca devuelve una lista vacia: si no hay proxy, devuelve una lista con
     * {@link Proxy#NO_PROXY}.
     */
    public abstract List<Proxy> select(URI uri);

    /**
     * Aviso de que no se pudo conectar a {@code sa}. Ver la cabecera: sin esto el selector no
     * aprende.
     */
    public abstract void connectFailed(URI uri, SocketAddress sa, IOException ioe);

    /**
     * Un selector que propone siempre el mismo proxy para http y https, y conexion directa para
     * cualquier otro esquema.
     *
     * @param proxyAddress la direccion del proxy, o null para "siempre directo"
     */
    public static ProxySelector of(InetSocketAddress proxyAddress) {
        return new StaticProxySelector(proxyAddress);
    }

    // Un selector que no aprende nada porque no tiene nada que aprender: su respuesta es constante.
    private static class StaticProxySelector extends ProxySelector {

        private static final List<Proxy> NO_PROXY_LIST =
                Collections.singletonList(Proxy.NO_PROXY);

        private final List<Proxy> list;

        StaticProxySelector(InetSocketAddress address) {
            Proxy p;
            if (address == null) {
                p = Proxy.NO_PROXY;
            } else {
                p = new Proxy(Proxy.Type.HTTP, address);
            }
            this.list = Collections.singletonList(p);
        }

        public void connectFailed(URI uri, SocketAddress sa, IOException e) {
            // No hay estado que actualizar.
        }

        public List<Proxy> select(URI uri) {
            if (uri == null) {
                throw new IllegalArgumentException("URI can't be null");
            }
            String scheme = uri.getScheme();
            if (scheme != null) {
                scheme = scheme.toLowerCase();
                if (scheme.equals("http") || scheme.equals("https")) {
                    return this.list;
                }
            }
            return NO_PROXY_LIST;
        }
    }
}
