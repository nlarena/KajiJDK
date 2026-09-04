package java.net.http;

import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Flow;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

/**
 * Un pedido HTTP ya armado. Inmutable, y por lo tanto reusable entre hilos.
 *
 * <h2>Por que el cuerpo es un {@code Publisher} y no un arreglo de bytes</h2>
 *
 * <p>Porque un cuerpo puede no caber en memoria, o no existir todavia cuando el pedido se arma. Un
 * {@link BodyPublisher} es un {@link Flow.Publisher} de {@link ByteBuffer}: el cliente le pide los
 * datos <em>cuando los va a mandar</em> y al ritmo que la red los acepta.
 *
 * <p>Eso es contrapresion real, y es lo que permite subir un archivo de gigabytes sin cargarlo. El
 * precio es que un {@code BodyPublisher} puede ser consultado <strong>mas de una vez</strong> —al
 * seguir una redireccion, al reintentar— y por eso {@link BodyPublishers#ofInputStream} recibe un
 * {@link Supplier} y no un flujo: un flujo ya consumido no se puede volver a leer, y un proveedor
 * puede dar otro.
 *
 * @since 11
 */
public abstract class HttpRequest {

    /** Para las implementaciones. */
    protected HttpRequest() {
    }

    /** Un constructor de pedidos con esa URI. */
    public static Builder newBuilder(URI uri) {
        return newBuilder().uri(uri);
    }

    /**
     * Un constructor que arranca copiando otro pedido, con sus encabezados filtrados.
     *
     * <p>Sirve para reescribir un pedido —sacarle una autorizacion antes de seguir una redireccion
     * a otro host, por ejemplo— sin volver a armarlo entero.
     */
    public static Builder newBuilder(HttpRequest request, BiPredicate<String, String> filter) {
        throw new UnsupportedOperationException(
                "esta VM no trae implementacion del cliente HTTP; ver HttpClient");
    }

    /** Un constructor vacio. */
    public static Builder newBuilder() {
        throw new UnsupportedOperationException(
                "esta VM no trae implementacion del cliente HTTP; ver HttpClient");
    }

    /** El cuerpo, si el pedido lleva uno. */
    public abstract Optional<BodyPublisher> bodyPublisher();

    /** El metodo, en mayusculas. */
    public abstract String method();

    /** El plazo total del pedido, si se puso uno. */
    public abstract Optional<Duration> timeout();

    /**
     * Si se pidio {@code Expect: 100-continue}.
     *
     * <p>Es preguntarle al servidor si va a aceptar el cuerpo <em>antes</em> de mandarlo. Vale la
     * pena cuando el cuerpo es grande y el rechazo probable —una autorizacion que puede fallar—, y
     * cuesta un viaje de mas cuando no.
     */
    public abstract boolean expectContinue();

    /** A donde va. */
    public abstract URI uri();

    /** La version pedida, si se fijo una. */
    public abstract Optional<HttpClient.Version> version();

    /** Los encabezados. */
    public abstract HttpHeaders headers();

    /** Sobre la URI, el metodo, los encabezados, el plazo, {@code expectContinue} y la version. */
    public final boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof HttpRequest)) {
            return false;
        }
        HttpRequest o = (HttpRequest) obj;
        return uri().equals(o.uri())
                && method().equals(o.method())
                && headers().equals(o.headers())
                && timeout().equals(o.timeout())
                && expectContinue() == o.expectContinue()
                && version().equals(o.version());
    }

    public final int hashCode() {
        return uri().hashCode() * 31 + method().hashCode() * 17 + headers().hashCode();
    }

    /**
     * Arma un {@link HttpRequest}.
     *
     * <p>Todos los metodos devuelven {@code this}, asi que se encadenan. La diferencia entre
     * {@link #header} y {@link #setHeader} es la de siempre y conviene no confundirla: el primero
     * <em>agrega</em> un valor mas, el segundo <em>reemplaza</em> los que hubiera.
     */
    public interface Builder {

        /** A donde va el pedido. */
        Builder uri(URI uri);

        /** Pide {@code Expect: 100-continue}; ver {@link HttpRequest#expectContinue}. */
        Builder expectContinue(boolean enable);

        /** Fija la version a pedir. */
        Builder version(HttpClient.Version version);

        /** Agrega un valor a ese encabezado, sin sacar los que haya. */
        Builder header(String name, String value);

        /**
         * Agrega varios, como pares nombre/valor.
         *
         * @throws IllegalArgumentException si la cantidad es impar
         */
        Builder headers(String... headers);

        /** El plazo total; vencerlo da {@link HttpTimeoutException}. */
        Builder timeout(Duration duration);

        /** Deja ese encabezado con ese unico valor. */
        Builder setHeader(String name, String value);

        /** Metodo {@code GET}, sin cuerpo. */
        Builder GET();

        /** Metodo {@code POST} con ese cuerpo. */
        Builder POST(BodyPublisher bodyPublisher);

        /** Metodo {@code PUT} con ese cuerpo. */
        Builder PUT(BodyPublisher bodyPublisher);

        /** Metodo {@code DELETE}, sin cuerpo. */
        Builder DELETE();

        /**
         * Metodo {@code HEAD}, sin cuerpo.
         *
         * <p>Llego con cuerpo por omision —es {@code method("HEAD", noBody())}— porque agregarlo
         * como abstracto habria roto a quien ya implementaba esta interfaz.
         */
        default Builder HEAD() {
            return method("HEAD", BodyPublishers.noBody());
        }

        /**
         * Cualquier metodo.
         *
         * @throws IllegalArgumentException si el metodo no es un token HTTP valido, o si es uno de
         *     los que el cliente no deja mandar por seguridad ({@code CONNECT}, {@code TRACE})
         */
        Builder method(String method, BodyPublisher bodyPublisher);

        /** El pedido armado. */
        HttpRequest build();

        /**
         * Una copia independiente de este constructor.
         *
         * <p>Sirve para armar varios pedidos que comparten la mayor parte de la configuracion sin
         * que tocar uno afecte a los otros.
         */
        Builder copy();
    }

    /**
     * De donde salen los bytes del cuerpo.
     *
     * <p>Un {@link Flow.Publisher} con una cosa mas: {@link #contentLength}, que el cliente necesita
     * <strong>antes</strong> de empezar a publicar para poder mandar {@code Content-Length} en vez
     * de trocear.
     */
    public interface BodyPublisher extends Flow.Publisher<ByteBuffer> {

        /**
         * Cuantos bytes va a publicar.
         *
         * @return el largo, o un negativo si no se sabe — y ahi el cuerpo va por trozos
         */
        long contentLength();
    }

    /**
     * Los {@link BodyPublisher} que trae el JDK.
     *
     * <p>En esta VM declinan: no hay implementacion del cliente HTTP. Ver {@link HttpClient}.
     */
    public static class BodyPublishers {

        private BodyPublishers() {
        }

        private static BodyPublisher declinar() {
            throw new UnsupportedOperationException(
                    "esta VM no trae implementacion del cliente HTTP; ver HttpClient");
        }

        /** Envuelve un publicador propio, con largo desconocido. */
        public static BodyPublisher fromPublisher(
                Flow.Publisher<? extends ByteBuffer> publisher) {
            return declinar();
        }

        /** Igual, declarando el largo. */
        public static BodyPublisher fromPublisher(
                Flow.Publisher<? extends ByteBuffer> publisher, long contentLength) {
            return declinar();
        }

        /** El cuerpo es esa cadena, en UTF-8. */
        public static BodyPublisher ofString(String body) {
            return declinar();
        }

        /** Igual, con ese juego de caracteres. */
        public static BodyPublisher ofString(String s, Charset charset) {
            return declinar();
        }

        /**
         * El cuerpo sale de un flujo que da el proveedor.
         *
         * <p>Un {@link Supplier} y no un flujo directo: ver la nota de {@link HttpRequest} sobre por
         * que un cuerpo puede pedirse mas de una vez.
         */
        public static BodyPublisher ofInputStream(Supplier<? extends InputStream> streamSupplier) {
            return declinar();
        }

        /** El cuerpo son esos bytes. */
        public static BodyPublisher ofByteArray(byte[] buf) {
            return declinar();
        }

        /** Un tramo de esos bytes. */
        public static BodyPublisher ofByteArray(byte[] buf, int offset, int length) {
            return declinar();
        }

        /** El cuerpo es el contenido de ese archivo. */
        public static BodyPublisher ofFile(Path path) throws java.io.FileNotFoundException {
            return declinar();
        }

        /** El cuerpo son esos bloques, uno detras de otro. */
        public static BodyPublisher ofByteArrays(Iterable<byte[]> iter) {
            return declinar();
        }

        /** Sin cuerpo. Es lo que usan {@code GET}, {@code DELETE} y {@code HEAD}. */
        public static BodyPublisher noBody() {
            return declinar();
        }

        /**
         * Varios cuerpos, uno detras de otro.
         *
         * <p>El largo total se conoce solo si <strong>todos</strong> lo conocen; con uno solo
         * desconocido, el resultado tambien lo es.
         */
        public static BodyPublisher concat(BodyPublisher... publishers) {
            return declinar();
        }
    }
}
