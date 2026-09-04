package java.net;

import java.io.IOException;
import java.util.List;
import java.util.Map;

// El punto donde se enchufa el manejo de cookies de toda la VM.
//
// Es abstracta y con un registro estatico, igual que `Authenticator`, y por la misma razon: quien
// hace pedidos HTTP no puede recibir la politica de cookies por parametro --esta diez capas mas
// abajo-- asi que la busca en un lugar acordado.
//
// La API habla en **headers crudos** (`Map<String, List<String>>`) y no en objetos `HttpCookie`, lo
// que parece un retroceso hasta que se ve el motivo: asi el handler puede manejar cookies que la
// plataforma no sabe modelar, y el cliente HTTP no necesita saber nada de cookies -- pasa los
// headers que recibio y pega los que le devuelven.
//
// Registrar y consultar un callback es computacion pura, y `CookieManager` implementa el trabajo de
// verdad sin tocar la red. Lo que falta en KajiJDK es un cliente HTTP que llame a esto, y eso no es
// parte de este contrato. Nada omitido.
public abstract class CookieHandler {

    private static CookieHandler cookieHandler;

    public CookieHandler() {
    }

    /** El handler instalado, o null si no hay ninguno. */
    public static synchronized CookieHandler getDefault() {
        return cookieHandler;
    }

    /** Instala el handler de toda la VM. */
    public static synchronized void setDefault(CookieHandler cHandler) {
        cookieHandler = cHandler;
    }

    /**
     * Los headers de cookies que hay que mandar en un pedido a {@code uri}.
     *
     * @param requestHeaders los headers que el cliente ya tiene armados, de solo lectura
     * @return un mapa de nombre de header a valores; tipicamente con la clave "Cookie"
     */
    public abstract Map<String, List<String>> get(URI uri, Map<String, List<String>> requestHeaders)
            throws IOException;

    /**
     * Guarda las cookies que trajo una respuesta de {@code uri}.
     *
     * @param responseHeaders los headers de la respuesta; interesan "Set-Cookie" y "Set-Cookie2"
     */
    public abstract void put(URI uri, Map<String, List<String>> responseHeaders) throws IOException;
}
