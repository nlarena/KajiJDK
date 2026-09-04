package java.net;

import java.io.IOException;
import java.util.List;
import java.util.Map;

// El cache de respuestas de toda la VM.
//
// Mismo patron que `CookieHandler` y `Authenticator`: abstracta con un registro estatico, porque
// quien hace el pedido esta demasiado abajo para recibir el cache por parametro.
//
// Los dos metodos son las dos mitades del ciclo. `get` se llama **antes** de conectar: si devuelve
// una `CacheResponse`, no hay conexion. `put` se llama **despues** de recibir, y devuelve el canal
// donde escribir --o null, que significa "esta no la guardes"--. Decidir que se guarda es del cache,
// no del cliente, y por eso `put` puede negarse.
//
// El registro y la consulta son computacion pura y estan enteros. Lo que falta en KajiJDK es un
// cliente HTTP que llame a esto; eso no es parte de este contrato. Nada omitido.
public abstract class ResponseCache {

    private static ResponseCache theResponseCache;

    public ResponseCache() {
    }

    /** El cache instalado, o null si no hay ninguno. */
    public static synchronized ResponseCache getDefault() {
        return theResponseCache;
    }

    /** Instala el cache de toda la VM; null lo desinstala. */
    public static synchronized void setDefault(ResponseCache responseCache) {
        theResponseCache = responseCache;
    }

    /**
     * La respuesta guardada para ese pedido, o null si no hay ninguna utilizable.
     *
     * @param uri            el recurso que se esta pidiendo
     * @param rqstMethod     el metodo del pedido ("GET")
     * @param rqstHeaders    los headers del pedido, que pueden cambiar que respuesta aplica
     * @throws IOException si falla la lectura del cache
     */
    public abstract CacheResponse get(URI uri, String rqstMethod, Map<String, List<String>> rqstHeaders)
            throws IOException;

    /**
     * Ofrece al cache guardar la respuesta de {@code conn}.
     *
     * @return donde escribir el cuerpo, o null si el cache decide no guardarla
     * @throws IOException si falla la escritura
     */
    public abstract CacheRequest put(URI uri, URLConnection conn) throws IOException;
}
