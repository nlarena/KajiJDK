package java.net.http;

import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Flow;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.net.ssl.SSLSession;

/**
 * Una respuesta HTTP, con el cuerpo ya convertido al tipo que se pidio.
 *
 * <h2>El parametro de tipo, que es lo primero que sorprende</h2>
 *
 * <p>{@code HttpResponse<T>} donde {@code T} lo elige <strong>quien hace el pedido</strong>, con el
 * {@link BodyHandler} que le pasa al cliente. Pedir {@code BodyHandlers.ofString()} da un
 * {@code HttpResponse<String>}; {@code ofFile(ruta)} da un {@code HttpResponse<Path>}.
 *
 * <p>Eso no es azucar. La conversion pasa <strong>mientras el cuerpo llega</strong>, no despues: un
 * cuerpo que se escribe a un archivo nunca esta entero en memoria, y uno que se descarta no se
 * guarda en ningun lado. Un {@code HttpResponse} con un {@code byte[]} adentro no podria hacer nada
 * de eso.
 *
 * <h2>La cadena de tres piezas</h2>
 *
 * <ul>
 * <li>{@link BodyHandler} — mira el {@link ResponseInfo} (codigo, encabezados, version) y
 *     <strong>decide</strong> como leer el cuerpo. Es lo que permite descartar el cuerpo de un 404 y
 *     guardar el de un 200 sin dos pedidos;</li>
 * <li>{@link BodySubscriber} — el que efectivamente lo lee, con contrapresion;</li>
 * <li>{@link BodyHandlers} y {@link BodySubscribers} — los que trae el JDK, para no escribirlos.</li>
 * </ul>
 *
 * @param <T> el tipo del cuerpo ya convertido
 * @since 11
 */
public interface HttpResponse<T> {

    /** El codigo HTTP. */
    int statusCode();

    /**
     * Una etiqueta de la conexion por la que vino, para diagnostico.
     *
     * <p>Llego con cuerpo por compatibilidad, y vacia por omision: no toda implementacion tiene un
     * identificador de conexion que ofrecer.
     *
     * @since 25
     */
    default Optional<String> connectionLabel() {
        return Optional.empty();
    }

    /** El pedido que la genero; puede no ser el original si hubo redirecciones. */
    HttpRequest request();

    /**
     * La respuesta anterior, si esta vino despues de una redireccion.
     *
     * <p>Es una cadena: cada eslabon apunta al anterior. Sirve para ver por donde paso el pedido,
     * que de otro modo seria invisible.
     */
    Optional<HttpResponse<T>> previousResponse();

    /** Los encabezados. */
    HttpHeaders headers();

    /** El cuerpo, ya convertido. */
    T body();

    /** La sesion TLS, si vino por HTTPS. */
    Optional<SSLSession> sslSession();

    /** La URI de la que finalmente se obtuvo, siguiendo las redirecciones. */
    URI uri();

    /** La version con la que se hablo. */
    HttpClient.Version version();

    /**
     * Decide como leer el cuerpo, mirando lo que ya se sabe de la respuesta.
     *
     * <p>Se lo consulta <strong>una vez</strong>, cuando llegaron los encabezados y antes que el
     * cuerpo. Ese momento es todo el punto: es lo que permite elegir en funcion del codigo o del
     * tipo de contenido sin haber bajado nada todavia.
     */
    @FunctionalInterface
    public interface BodyHandler<T> {

        /** El lector para esta respuesta. */
        BodySubscriber<T> apply(ResponseInfo responseInfo);
    }

    /** Lo que un {@link BodyHandler} sabe de la respuesta antes de que llegue el cuerpo. */
    public interface ResponseInfo {

        /** El codigo HTTP. */
        int statusCode();

        /** Los encabezados. */
        HttpHeaders headers();

        /** La version. */
        HttpClient.Version version();
    }

    /**
     * Lee el cuerpo y produce el resultado.
     *
     * <p>Es un {@link Flow.Subscriber} de <strong>listas</strong> de {@link ByteBuffer} y no de
     * buffers sueltos: la red entrega en bloques y agruparlos evita una notificacion por cada uno.
     *
     * <p>{@link #getBody} devuelve un {@link CompletionStage} que se completa cuando el cuerpo
     * termino. Puede completarse <em>antes</em> de haber leido todo —un lector que solo quiere los
     * encabezados no necesita el resto— y esa es la diferencia con esperar al {@code onComplete}.
     */
    public interface BodySubscriber<T> extends Flow.Subscriber<List<ByteBuffer>> {

        /** El resultado, cuando este. */
        CompletionStage<T> getBody();
    }

    /**
     * Atiende las respuestas que el servidor manda sin que se las pidan.
     *
     * <p>Es de HTTP/2: el servidor que sirve una pagina puede empujar de una las hojas de estilo que
     * sabe que van a pedirse. Sin este manejador el cliente las rechaza, que es lo correcto por
     * omision — aceptar contenido no pedido tiene que ser una decision explicita.
     */
    public interface PushPromiseHandler<T> {

        /** Llega una promesa; aceptarla es llamar al {@code acceptor} con un manejador de cuerpo. */
        void applyPushPromise(HttpRequest initiatingRequest, HttpRequest pushPromiseRequest,
                Function<BodyHandler<T>, CompletableFuture<HttpResponse<T>>> acceptor);

        /** El manejador que acepta todas y las junta en ese mapa. */
        static <T> PushPromiseHandler<T> of(
                Function<HttpRequest, BodyHandler<T>> pushPromiseHandler,
                ConcurrentMap<HttpRequest, CompletableFuture<HttpResponse<T>>> pushPromisesMap) {
            throw new UnsupportedOperationException(
                    "esta VM no trae implementacion del cliente HTTP; ver HttpClient");
        }
    }

    /**
     * Los {@link BodyHandler} que trae el JDK.
     *
     * <p>En esta VM declinan: no hay implementacion del cliente HTTP. Ver {@link HttpClient}.
     */
    public static class BodyHandlers {

        private BodyHandlers() {
        }

        private static <T> BodyHandler<T> declinar() {
            throw new UnsupportedOperationException(
                    "esta VM no trae implementacion del cliente HTTP; ver HttpClient");
        }

        /** Le pasa los bloques a ese suscriptor; el cuerpo queda en {@code null}. */
        public static BodyHandler<Void> fromSubscriber(
                Flow.Subscriber<? super List<ByteBuffer>> subscriber) {
            return declinar();
        }

        /** Igual, extrayendo el resultado del suscriptor con esa funcion. */
        public static <S extends Flow.Subscriber<? super List<ByteBuffer>>, T> BodyHandler<T>
                fromSubscriber(S subscriber, Function<? super S, ? extends T> finisher) {
            return declinar();
        }

        /** Le pasa el cuerpo linea por linea a ese suscriptor. */
        public static BodyHandler<Void> fromLineSubscriber(
                Flow.Subscriber<? super String> subscriber) {
            return declinar();
        }

        /** Igual, con extractor y separador de linea propio. */
        public static <S extends Flow.Subscriber<? super String>, T> BodyHandler<T>
                fromLineSubscriber(S subscriber, Function<? super S, ? extends T> finisher,
                        String lineSeparator) {
            return declinar();
        }

        /** Descarta el cuerpo. No lo ignora: lo lee y lo tira, que es lo que libera la conexion. */
        public static BodyHandler<Void> discarding() {
            return declinar();
        }

        /** Descarta el cuerpo y devuelve ese valor fijo. */
        public static <U> BodyHandler<U> replacing(U value) {
            return declinar();
        }

        /** El cuerpo como cadena, con ese juego de caracteres. */
        public static BodyHandler<String> ofString(Charset charset) {
            return declinar();
        }

        /** El cuerpo a ese archivo, con esas opciones de apertura. */
        public static BodyHandler<Path> ofFile(Path file, OpenOption... openOptions) {
            return declinar();
        }

        /** El cuerpo a ese archivo. */
        public static BodyHandler<Path> ofFile(Path file) {
            return declinar();
        }

        /**
         * El cuerpo a un archivo dentro de ese directorio, con el nombre que diga el servidor.
         *
         * <p>El nombre sale del encabezado {@code Content-Disposition}, o sea <strong>del otro
         * lado</strong>. El cliente lo valida para que no se escape del directorio, y aun asi
         * conviene saber que quien elige el nombre es el servidor.
         */
        public static BodyHandler<Path> ofFileDownload(Path directory, OpenOption... openOptions) {
            return declinar();
        }

        /**
         * El cuerpo como flujo que se lee despues.
         *
         * <p>Hay que cerrarlo o leerlo entero: mientras no se haga, la conexion queda tomada.
         */
        public static BodyHandler<InputStream> ofInputStream() {
            return declinar();
        }

        /** El cuerpo como flujo de lineas. */
        public static BodyHandler<Stream<String>> ofLines() {
            return declinar();
        }

        /** Le pasa los bloques a ese consumidor; el vacio marca el final. */
        public static BodyHandler<Void> ofByteArrayConsumer(
                Consumer<Optional<byte[]>> consumer) {
            return declinar();
        }

        /** El cuerpo como arreglo de bytes, entero en memoria. */
        public static BodyHandler<byte[]> ofByteArray() {
            return declinar();
        }

        /** El cuerpo como cadena, en UTF-8 o lo que diga el {@code Content-Type}. */
        public static BodyHandler<String> ofString() {
            return declinar();
        }

        /** El cuerpo como publicador, para consumirlo con contrapresion propia. */
        public static BodyHandler<Flow.Publisher<List<ByteBuffer>>> ofPublisher() {
            return declinar();
        }

        /** Agrupa los bloques hasta ese tamano antes de entregarlos. */
        public static <T> BodyHandler<T> buffering(BodyHandler<T> downstream, int bufferSize) {
            return declinar();
        }

        /**
         * Corta si el cuerpo pasa de ese tamano.
         *
         * <p>Es la defensa contra un servidor que manda mas de lo que uno puede guardar, sea por
         * error o a proposito.
         */
        public static <T> BodyHandler<T> limiting(BodyHandler<T> downstream, long capacity) {
            return declinar();
        }
    }

    /**
     * Los {@link BodySubscriber} que trae el JDK.
     *
     * <p>Son la contraparte de {@link BodyHandlers}: aquellos <em>eligen</em> mirando la respuesta,
     * estos <em>leen</em>. Se usan directamente al escribir un manejador propio.
     *
     * <p>En esta VM declinan; ver {@link HttpClient}.
     */
    public static class BodySubscribers {

        private BodySubscribers() {
        }

        private static <T> BodySubscriber<T> declinar() {
            throw new UnsupportedOperationException(
                    "esta VM no trae implementacion del cliente HTTP; ver HttpClient");
        }

        /** Reenvia los bloques a ese suscriptor. */
        public static BodySubscriber<Void> fromSubscriber(
                Flow.Subscriber<? super List<ByteBuffer>> subscriber) {
            return declinar();
        }

        /** Igual, extrayendo el resultado. */
        public static <S extends Flow.Subscriber<? super List<ByteBuffer>>, T> BodySubscriber<T>
                fromSubscriber(S subscriber, Function<? super S, ? extends T> finisher) {
            return declinar();
        }

        /** Reenvia linea por linea. */
        public static BodySubscriber<Void> fromLineSubscriber(
                Flow.Subscriber<? super String> subscriber) {
            return declinar();
        }

        /** Igual, con extractor, juego de caracteres y separador propios. */
        public static <S extends Flow.Subscriber<? super String>, T> BodySubscriber<T>
                fromLineSubscriber(S subscriber, Function<? super S, ? extends T> finisher,
                        Charset charset, String lineSeparator) {
            return declinar();
        }

        /** Junta el cuerpo en una cadena. */
        public static BodySubscriber<String> ofString(Charset charset) {
            return declinar();
        }

        /** Junta el cuerpo en un arreglo. */
        public static BodySubscriber<byte[]> ofByteArray() {
            return declinar();
        }

        /** Escribe el cuerpo a ese archivo. */
        public static BodySubscriber<Path> ofFile(Path file, OpenOption... openOptions) {
            return declinar();
        }

        /** Escribe el cuerpo a ese archivo. */
        public static BodySubscriber<Path> ofFile(Path file) {
            return declinar();
        }

        /** Le pasa los bloques a ese consumidor. */
        public static BodySubscriber<Void> ofByteArrayConsumer(
                Consumer<Optional<byte[]>> consumer) {
            return declinar();
        }

        /** El cuerpo como flujo que se lee despues. */
        public static BodySubscriber<InputStream> ofInputStream() {
            return declinar();
        }

        /** El cuerpo como flujo de lineas. */
        public static BodySubscriber<Stream<String>> ofLines(Charset charset) {
            return declinar();
        }

        /** El cuerpo como publicador. */
        public static BodySubscriber<Flow.Publisher<List<ByteBuffer>>> ofPublisher() {
            return declinar();
        }

        /** Descarta el cuerpo y devuelve ese valor. */
        public static <U> BodySubscriber<U> replacing(U value) {
            return declinar();
        }

        /** Descarta el cuerpo. */
        public static BodySubscriber<Void> discarding() {
            return declinar();
        }

        /** Agrupa antes de entregar. */
        public static <T> BodySubscriber<T> buffering(BodySubscriber<T> downstream,
                int bufferSize) {
            return declinar();
        }

        /**
         * Transforma el resultado de otro lector.
         *
         * <p>La funcion corre cuando el cuerpo ya esta entero, asi que puede bloquear sin trabar la
         * lectura — al reves que un {@code map} sobre el flujo de bloques.
         */
        public static <T, U> BodySubscriber<U> mapping(BodySubscriber<T> upstream,
                Function<? super T, ? extends U> mapper) {
            return declinar();
        }

        /** Corta si el cuerpo pasa de ese tamano. */
        public static <T> BodySubscriber<T> limiting(BodySubscriber<T> downstream, long capacity) {
            return declinar();
        }
    }
}
