package java.net.http;

import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.InetAddress;
import java.net.ProxySelector;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

/**
 * El cliente HTTP del JDK: HTTP/1.1, HTTP/2 y WebSocket, con API sincronica y asincronica.
 *
 * <h2>Es un objeto pesado, y eso cambia como se usa</h2>
 *
 * <p>Un cliente lleva adentro el pool de conexiones, el pool de hilos, el contexto TLS y la cache de
 * sesiones. Crear uno por pedido —que es el reflejo natural viniendo de {@code URLConnection}— tira
 * todo eso a la basura cada vez y vuelve a hacer el handshake completo en cada llamada.
 *
 * <p>Lo correcto es <strong>uno por aplicacion</strong>, o uno por configuracion distinta, compartido
 * entre hilos: es inmutable y seguro para eso.
 *
 * <h2>Los dos modos</h2>
 *
 * <p>{@link #send} bloquea; {@link #sendAsync} devuelve un {@link CompletableFuture}. No son dos
 * implementaciones: la sincronica esta escrita sobre la otra. Por eso el cliente necesita un
 * {@link Executor} incluso para el modo bloqueante.
 *
 * <h2>El cierre, que llego tarde y por algo</h2>
 *
 * <p>Hasta Java 21 no habia forma de cerrarlo: un cliente quedaba con sus hilos vivos hasta que el
 * recolector lo alcanzara, y como los hilos lo referencian, eso podia no pasar nunca. Ahora es
 * {@link AutoCloseable}, con la distincion habitual entre {@link #shutdown} —no acepta pedidos
 * nuevos, termina los que hay— y {@link #shutdownNow}, que corta.
 *
 * <h2>En esta VM</h2>
 *
 * <p>Las dos fabricas declinan. Implementar esto seria escribir un cliente HTTP/2 entero —marcos,
 * multiplexado, HPACK— sobre un TLS que esta VM tampoco tiene proveedor para negociar. La API queda
 * completa para quien compile contra ella, y lo que falta es una implementacion, no una firma.
 *
 * @since 11
 */
public abstract class HttpClient implements AutoCloseable {

    /** Para las implementaciones. */
    protected HttpClient() {
    }

    /**
     * Un cliente con la configuracion por omision.
     *
     * @throws UnsupportedOperationException en esta VM — ver la nota de la clase
     */
    public static HttpClient newHttpClient() {
        return newBuilder().build();
    }

    /**
     * Un constructor de clientes.
     *
     * @throws UnsupportedOperationException en esta VM
     */
    public static Builder newBuilder() {
        throw new UnsupportedOperationException(
                "esta VM no trae implementacion del cliente HTTP");
    }

    /** El manejador de cookies, si se configuro uno. */
    public abstract Optional<CookieHandler> cookieHandler();

    /**
     * El plazo para <strong>conectar</strong>, si se configuro.
     *
     * <p>Distinto del plazo de {@link HttpRequest#timeout}, que es el del pedido entero. Vencerse
     * conectando da {@link HttpConnectTimeoutException}, y esa distincion decide si reintentar es
     * seguro.
     */
    public abstract Optional<Duration> connectTimeout();

    /** Que hace con las redirecciones. */
    public abstract Redirect followRedirects();

    /** El selector de proxy, si se configuro. */
    public abstract Optional<ProxySelector> proxy();

    /** El contexto TLS. */
    public abstract SSLContext sslContext();

    /** Los parametros TLS. */
    public abstract SSLParameters sslParameters();

    /** El autenticador, si se configuro. */
    public abstract Optional<Authenticator> authenticator();

    /** La version preferida. Es una preferencia: HTTP/2 se negocia y puede caer a 1.1. */
    public abstract Version version();

    /** El ejecutor, si se configuro uno propio. */
    public abstract Optional<Executor> executor();

    /**
     * Manda el pedido y espera la respuesta.
     *
     * @throws IOException si falla la red
     * @throws InterruptedException si interrumpen el hilo mientras espera
     */
    public abstract <T> HttpResponse<T> send(HttpRequest request,
            HttpResponse.BodyHandler<T> responseBodyHandler)
            throws IOException, InterruptedException;

    /**
     * Manda el pedido sin esperar.
     *
     * <p>El futuro se completa cuando llegaron los encabezados <strong>y</strong> el cuerpo termino
     * de convertirse. Falla con la excepcion adentro, no la tira.
     */
    public abstract <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
            HttpResponse.BodyHandler<T> responseBodyHandler);

    /** Igual, atendiendo ademas las promesas que empuje el servidor. */
    public abstract <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
            HttpResponse.BodyHandler<T> responseBodyHandler,
            HttpResponse.PushPromiseHandler<T> pushPromiseHandler);

    /**
     * Un constructor de WebSocket.
     *
     * @throws UnsupportedOperationException si el cliente no lo soporta
     */
    public WebSocket.Builder newWebSocketBuilder() {
        throw new UnsupportedOperationException("este cliente no soporta WebSocket");
    }

    /**
     * Deja de aceptar pedidos nuevos; los que estan en curso terminan.
     *
     * @since 21
     */
    public void shutdown() {
        throw new UnsupportedOperationException("este cliente no soporta cierre ordenado");
    }

    /**
     * Espera hasta {@code duration} a que termine de cerrarse.
     *
     * @return {@code true} si termino, {@code false} si se vencio el plazo
     * @since 21
     */
    public boolean awaitTermination(Duration duration) throws InterruptedException {
        throw new UnsupportedOperationException("este cliente no soporta cierre ordenado");
    }

    /**
     * Si ya termino de cerrarse.
     *
     * @since 21
     */
    public boolean isTerminated() {
        throw new UnsupportedOperationException("este cliente no soporta cierre ordenado");
    }

    /**
     * Corta: los pedidos en curso fallan.
     *
     * @since 21
     */
    public void shutdownNow() {
        throw new UnsupportedOperationException("este cliente no soporta cierre ordenado");
    }

    /**
     * Cierra ordenadamente y espera.
     *
     * <p>No declara excepcion, a diferencia del {@code close} de {@link AutoCloseable}: cerrar tiene
     * que poder ir en un {@code try} con recursos sin obligar a atrapar nada.
     *
     * @since 21
     */
    public void close() {
        throw new UnsupportedOperationException("este cliente no soporta cierre ordenado");
    }

    /**
     * Que hacer con una redireccion.
     *
     * <p>{@link #NORMAL} es el unico que no es obvio, y es el que hay que usar: sigue las
     * redirecciones <strong>salvo</strong> las que bajan de HTTPS a HTTP. Seguir esas convertiria
     * una conexion segura en una en claro sin que nadie lo pida, que es un ataque conocido.
     */
    public enum Redirect {

        /** No seguir ninguna. */
        NEVER,
        /** Seguir todas, incluida la que degrada a HTTP. */
        ALWAYS,
        /** Seguir todas menos la que degrada de HTTPS a HTTP. */
        NORMAL
    }

    /** La version del protocolo. */
    public enum Version {

        /** HTTP/1.1. */
        HTTP_1_1,
        /**
         * HTTP/2, cayendo a 1.1 si el servidor no lo soporta.
         *
         * <p>Es una preferencia y no una exigencia: la version se negocia por ALPN durante el
         * handshake TLS, asi que pedirlo no garantiza obtenerlo.
         */
        HTTP_2
    }

    /**
     * Arma un {@link HttpClient}.
     *
     * <p>Todo tiene valor por omision, asi que {@code newBuilder().build()} es valido. Lo que se
     * configura son las excepciones a eso.
     */
    public interface Builder {

        /**
         * Que no use ningun proxy, ni siquiera el del sistema.
         *
         * <p>Existe porque no pasar nada <em>no</em> significa eso: sin configurar, el cliente usa
         * el selector por omision, que puede leer las variables de entorno del sistema.
         */
        public static final ProxySelector NO_PROXY = ProxySelector.of(null);

        /** El manejador de cookies. Sin uno, el cliente no guarda ninguna. */
        Builder cookieHandler(CookieHandler cookieHandler);

        /** El plazo para conectar. */
        Builder connectTimeout(Duration duration);

        /** El contexto TLS; sin uno, el por omision del sistema. */
        Builder sslContext(SSLContext sslContext);

        /** Los parametros TLS. */
        Builder sslParameters(SSLParameters sslParameters);

        /** Donde correr las tareas asincronicas; sin uno, el cliente arma su propio pool. */
        Builder executor(Executor executor);

        /** Que hacer con las redirecciones; por omision {@link Redirect#NEVER}. */
        Builder followRedirects(Redirect policy);

        /** La version preferida. */
        Builder version(Version version);

        /**
         * La prioridad de los flujos HTTP/2, entre 1 y 256.
         *
         * @throws IllegalArgumentException si esta fuera de rango
         */
        Builder priority(int priority);

        /** El selector de proxy; ver {@link #NO_PROXY}. */
        Builder proxy(ProxySelector proxySelector);

        /** El autenticador para los desafios {@code 401} y {@code 407}. */
        Builder authenticator(Authenticator authenticator);

        /**
         * Desde que direccion local salir.
         *
         * <p>Llego con cuerpo por compatibilidad. Sirve en una maquina con varias interfaces, donde
         * cual se usa cambia la ruta y a veces el permiso.
         *
         * @since 19
         */
        default Builder localAddress(InetAddress localAddr) {
            throw new UnsupportedOperationException(
                    "este constructor no soporta fijar la direccion local");
        }

        /** El cliente armado. */
        HttpClient build();
    }
}
