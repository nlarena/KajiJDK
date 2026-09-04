package javax.print;

import java.io.Serializable;
import java.nio.charset.Charset;

/**
 * KajiLibrary's javax.print.DocFlavor -- de que tipo es un documento y en que forma esta el dato.
 *
 * <p>Son <b>dos</b> cosas, y ese es todo el diseno de la clase:
 *
 * <ul>
 *   <li>el <b>tipo MIME</b>, que dice que es el documento: PDF, PostScript, texto plano en UTF-8;
 *   <li>la <b>clase de representacion</b>, que dice como se le entrega al servicio: un
 *       {@code byte[]}, un {@code InputStream}, una {@code URL}.
 * </ul>
 *
 * <p>Un PDF en un arreglo de bytes y el mismo PDF detras de una URL son dos formatos distintos aunque
 * el documento sea el mismo, y una impresora puede aceptar uno y no el otro. Por eso las constantes
 * estan agrupadas en clases anidadas por representacion y no por tipo.
 *
 * <h2>Las variantes {@code _HOST}</h2>
 *
 * <p>{@link #hostEncoding} es la codificacion por omision de <b>esta</b> maquina virtual, y las
 * constantes que la usan heredan su problema: el mismo formato significa una cosa aca y otra en otra
 * maquina. Sirven para imprimir algo que se acaba de leer del sistema local; para cualquier cosa que
 * cruce la red o se guarde, las variantes explicitas.
 *
 * <h2>La comparacion es sobre la forma canonica</h2>
 *
 * <p>{@code "Text/Plain; CharSet=Utf-8"} y {@code "text/plain;charset=utf-8"} son iguales: tipo,
 * subtipo y nombres de parametro se bajan a minusculas y los parametros se ordenan. Los valores se
 * dejan como estan, salvo el de {@code charset}, que es insensible a mayusculas por definicion.
 *
 * <h2>{@code AUTOSENSE}</h2>
 *
 * <p>Es {@code application/octet-stream}: bytes sin declarar, que la impresora deduzca. Funciona
 * seguido y falla en silencio cuando no -- sale una pagina de basura. Es lo ultimo que hay que probar,
 * no lo primero.
 */
public class DocFlavor implements Serializable, Cloneable {

    private static final long serialVersionUID = -4512080796965449721L;

    /**
     * La codificacion por omision de esta maquina virtual. Ver la nota de la clase.
     *
     * <p>No es constante de compilacion: se calcula al cargar la clase.
     */
    public static final String hostEncoding;

    static {
        hostEncoding = Charset.defaultCharset().name();
    }

    /** El tipo MIME, ya normalizado. */
    private transient MimeType myMimeType;

    /** El nombre de la clase de representacion. */
    private final String myClassName;

    /**
     * @param mimeType el tipo MIME
     * @param className el nombre completo de la clase de representacion
     * @throws NullPointerException si alguno es null
     * @throws IllegalArgumentException si el tipo MIME no es valido
     */
    public DocFlavor(String mimeType, String className) {
        if (className == null) {
            throw new NullPointerException();
        }
        this.myMimeType = new MimeType(mimeType);
        this.myClassName = className;
    }

    /** El tipo MIME en forma canonica. Ver la nota de la clase. */
    public String getMimeType() {
        return this.myMimeType.getMimeType();
    }

    /** El tipo, en minusculas. */
    public String getMediaType() {
        return this.myMimeType.getMediaType();
    }

    /** El subtipo, en minusculas. */
    public String getMediaSubtype() {
        return this.myMimeType.getMediaSubtype();
    }

    /**
     * El valor de ese parametro, o null.
     *
     * <p>El nombre se busca sin distinguir mayusculas, porque ya estan normalizados.
     */
    public String getParameter(String paramName) {
        return this.myMimeType.getParameterMap().get(paramName.toLowerCase());
    }

    /** El nombre de la clase de representacion. */
    public String getRepresentationClassName() {
        return this.myClassName;
    }

    /** El tipo canonico mas {@code class="..."}. */
    @Override
    public String toString() {
        return getStringValue();
    }

    /** Sobre el tipo canonico y la clase de representacion, los dos. */
    @Override
    public int hashCode() {
        return getStringValue().hashCode();
    }

    /** Idem: dos formatos son iguales solo si coinciden en las dos cosas. */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof DocFlavor)) {
            return false;
        }
        DocFlavor other = (DocFlavor) obj;
        return this.myClassName.equals(other.myClassName)
            && this.myMimeType.equals(other.myMimeType);
    }

    /** El texto que usan {@code toString}, {@code hashCode} y {@code equals}. */
    private String getStringValue() {
        return getMimeType() + "; class=\"" + this.myClassName + "\"";
    }

    /**
     * KajiLibrary's javax.print.DocFlavor.BYTE_ARRAY -- el dato es un {@code byte[]}.
     *
     * <p>Aca el juego de caracteres si importa, porque bytes sin declararlo no significan nada. Ver la
     * nota de {@link DocFlavor} sobre las variantes {@code _HOST}.
     *
     */
    public static class BYTE_ARRAY extends DocFlavor {

        private static final long serialVersionUID = -9065578006593857475L;

        /**
         * @param mimeType el tipo MIME
         * @throws NullPointerException si es null
         * @throws IllegalArgumentException si no es valido
         */
        public BYTE_ARRAY(String mimeType) {
            super(mimeType, "[B");
        }

        /** Texto plano, codificacion de la plataforma. */
        public static final BYTE_ARRAY TEXT_PLAIN_HOST = new BYTE_ARRAY("text/plain; charset=" + hostEncoding);

        /** Texto plano en UTF-8. */
        public static final BYTE_ARRAY TEXT_PLAIN_UTF_8 = new BYTE_ARRAY("text/plain; charset=utf-8");

        /** Texto plano en UTF-16, con marca de orden. */
        public static final BYTE_ARRAY TEXT_PLAIN_UTF_16 = new BYTE_ARRAY("text/plain; charset=utf-16");

        /** Texto plano en UTF-16 grande primero. */
        public static final BYTE_ARRAY TEXT_PLAIN_UTF_16BE = new BYTE_ARRAY("text/plain; charset=utf-16be");

        /** Texto plano en UTF-16 chico primero. */
        public static final BYTE_ARRAY TEXT_PLAIN_UTF_16LE = new BYTE_ARRAY("text/plain; charset=utf-16le");

        /** Texto plano en ASCII. */
        public static final BYTE_ARRAY TEXT_PLAIN_US_ASCII = new BYTE_ARRAY("text/plain; charset=us-ascii");

        /** HTML, codificacion de la plataforma. */
        public static final BYTE_ARRAY TEXT_HTML_HOST = new BYTE_ARRAY("text/html; charset=" + hostEncoding);

        /** HTML en UTF-8. */
        public static final BYTE_ARRAY TEXT_HTML_UTF_8 = new BYTE_ARRAY("text/html; charset=utf-8");

        /** HTML en UTF-16, con marca de orden. */
        public static final BYTE_ARRAY TEXT_HTML_UTF_16 = new BYTE_ARRAY("text/html; charset=utf-16");

        /** HTML en UTF-16 grande primero. */
        public static final BYTE_ARRAY TEXT_HTML_UTF_16BE = new BYTE_ARRAY("text/html; charset=utf-16be");

        /** HTML en UTF-16 chico primero. */
        public static final BYTE_ARRAY TEXT_HTML_UTF_16LE = new BYTE_ARRAY("text/html; charset=utf-16le");

        /** HTML en ASCII. */
        public static final BYTE_ARRAY TEXT_HTML_US_ASCII = new BYTE_ARRAY("text/html; charset=us-ascii");

        /** PDF. */
        public static final BYTE_ARRAY PDF = new BYTE_ARRAY("application/pdf");

        /** PostScript. */
        public static final BYTE_ARRAY POSTSCRIPT = new BYTE_ARRAY("application/postscript");

        /** PCL de HP. */
        public static final BYTE_ARRAY PCL = new BYTE_ARRAY("application/vnd.hp-pcl");

        /** GIF. */
        public static final BYTE_ARRAY GIF = new BYTE_ARRAY("image/gif");

        /** JPEG. */
        public static final BYTE_ARRAY JPEG = new BYTE_ARRAY("image/jpeg");

        /** PNG. */
        public static final BYTE_ARRAY PNG = new BYTE_ARRAY("image/png");

        /** Bytes sin declarar: que la impresora deduzca que son. Es lo ultimo que hay que probar. */
        public static final BYTE_ARRAY AUTOSENSE = new BYTE_ARRAY("application/octet-stream");

    }

    /**
     * KajiLibrary's javax.print.DocFlavor.INPUT_STREAM -- el dato es un {@link java.io.InputStream}.
     *
     * <p>Aca el juego de caracteres si importa, porque bytes sin declararlo no significan nada. Ver la
     * nota de {@link DocFlavor} sobre las variantes {@code _HOST}.
     *
     */
    public static class INPUT_STREAM extends DocFlavor {

        private static final long serialVersionUID = -7045842700749194127L;

        /**
         * @param mimeType el tipo MIME
         * @throws NullPointerException si es null
         * @throws IllegalArgumentException si no es valido
         */
        public INPUT_STREAM(String mimeType) {
            super(mimeType, "java.io.InputStream");
        }

        /** Texto plano, codificacion de la plataforma. */
        public static final INPUT_STREAM TEXT_PLAIN_HOST = new INPUT_STREAM("text/plain; charset=" + hostEncoding);

        /** Texto plano en UTF-8. */
        public static final INPUT_STREAM TEXT_PLAIN_UTF_8 = new INPUT_STREAM("text/plain; charset=utf-8");

        /** Texto plano en UTF-16, con marca de orden. */
        public static final INPUT_STREAM TEXT_PLAIN_UTF_16 = new INPUT_STREAM("text/plain; charset=utf-16");

        /** Texto plano en UTF-16 grande primero. */
        public static final INPUT_STREAM TEXT_PLAIN_UTF_16BE = new INPUT_STREAM("text/plain; charset=utf-16be");

        /** Texto plano en UTF-16 chico primero. */
        public static final INPUT_STREAM TEXT_PLAIN_UTF_16LE = new INPUT_STREAM("text/plain; charset=utf-16le");

        /** Texto plano en ASCII. */
        public static final INPUT_STREAM TEXT_PLAIN_US_ASCII = new INPUT_STREAM("text/plain; charset=us-ascii");

        /** HTML, codificacion de la plataforma. */
        public static final INPUT_STREAM TEXT_HTML_HOST = new INPUT_STREAM("text/html; charset=" + hostEncoding);

        /** HTML en UTF-8. */
        public static final INPUT_STREAM TEXT_HTML_UTF_8 = new INPUT_STREAM("text/html; charset=utf-8");

        /** HTML en UTF-16, con marca de orden. */
        public static final INPUT_STREAM TEXT_HTML_UTF_16 = new INPUT_STREAM("text/html; charset=utf-16");

        /** HTML en UTF-16 grande primero. */
        public static final INPUT_STREAM TEXT_HTML_UTF_16BE = new INPUT_STREAM("text/html; charset=utf-16be");

        /** HTML en UTF-16 chico primero. */
        public static final INPUT_STREAM TEXT_HTML_UTF_16LE = new INPUT_STREAM("text/html; charset=utf-16le");

        /** HTML en ASCII. */
        public static final INPUT_STREAM TEXT_HTML_US_ASCII = new INPUT_STREAM("text/html; charset=us-ascii");

        /** PDF. */
        public static final INPUT_STREAM PDF = new INPUT_STREAM("application/pdf");

        /** PostScript. */
        public static final INPUT_STREAM POSTSCRIPT = new INPUT_STREAM("application/postscript");

        /** PCL de HP. */
        public static final INPUT_STREAM PCL = new INPUT_STREAM("application/vnd.hp-pcl");

        /** GIF. */
        public static final INPUT_STREAM GIF = new INPUT_STREAM("image/gif");

        /** JPEG. */
        public static final INPUT_STREAM JPEG = new INPUT_STREAM("image/jpeg");

        /** PNG. */
        public static final INPUT_STREAM PNG = new INPUT_STREAM("image/png");

        /** Bytes sin declarar: que la impresora deduzca que son. Es lo ultimo que hay que probar. */
        public static final INPUT_STREAM AUTOSENSE = new INPUT_STREAM("application/octet-stream");

    }

    /**
     * KajiLibrary's javax.print.DocFlavor.URL -- el dato es una {@link java.net.URL}.
     *
     * <p>Es la unica representacion donde el dato <b>no</b> viaja: se le pasa la direccion al servicio y
     * el la busca. Eso significa que la impresora tiene que poder llegar a esa URL, que no es obvio si
     * esta en otra red.
     *
     * <p>Aca el juego de caracteres si importa, porque bytes sin declararlo no significan nada. Ver la
     * nota de {@link DocFlavor} sobre las variantes {@code _HOST}.
     *
     */
    public static class URL extends DocFlavor {

        private static final long serialVersionUID = 2936725788144902062L;

        /**
         * @param mimeType el tipo MIME
         * @throws NullPointerException si es null
         * @throws IllegalArgumentException si no es valido
         */
        public URL(String mimeType) {
            super(mimeType, "java.net.URL");
        }

        /** Texto plano, codificacion de la plataforma. */
        public static final URL TEXT_PLAIN_HOST = new URL("text/plain; charset=" + hostEncoding);

        /** Texto plano en UTF-8. */
        public static final URL TEXT_PLAIN_UTF_8 = new URL("text/plain; charset=utf-8");

        /** Texto plano en UTF-16, con marca de orden. */
        public static final URL TEXT_PLAIN_UTF_16 = new URL("text/plain; charset=utf-16");

        /** Texto plano en UTF-16 grande primero. */
        public static final URL TEXT_PLAIN_UTF_16BE = new URL("text/plain; charset=utf-16be");

        /** Texto plano en UTF-16 chico primero. */
        public static final URL TEXT_PLAIN_UTF_16LE = new URL("text/plain; charset=utf-16le");

        /** Texto plano en ASCII. */
        public static final URL TEXT_PLAIN_US_ASCII = new URL("text/plain; charset=us-ascii");

        /** HTML, codificacion de la plataforma. */
        public static final URL TEXT_HTML_HOST = new URL("text/html; charset=" + hostEncoding);

        /** HTML en UTF-8. */
        public static final URL TEXT_HTML_UTF_8 = new URL("text/html; charset=utf-8");

        /** HTML en UTF-16, con marca de orden. */
        public static final URL TEXT_HTML_UTF_16 = new URL("text/html; charset=utf-16");

        /** HTML en UTF-16 grande primero. */
        public static final URL TEXT_HTML_UTF_16BE = new URL("text/html; charset=utf-16be");

        /** HTML en UTF-16 chico primero. */
        public static final URL TEXT_HTML_UTF_16LE = new URL("text/html; charset=utf-16le");

        /** HTML en ASCII. */
        public static final URL TEXT_HTML_US_ASCII = new URL("text/html; charset=us-ascii");

        /** PDF. */
        public static final URL PDF = new URL("application/pdf");

        /** PostScript. */
        public static final URL POSTSCRIPT = new URL("application/postscript");

        /** PCL de HP. */
        public static final URL PCL = new URL("application/vnd.hp-pcl");

        /** GIF. */
        public static final URL GIF = new URL("image/gif");

        /** JPEG. */
        public static final URL JPEG = new URL("image/jpeg");

        /** PNG. */
        public static final URL PNG = new URL("image/png");

        /** Bytes sin declarar: que la impresora deduzca que son. Es lo ultimo que hay que probar. */
        public static final URL AUTOSENSE = new URL("application/octet-stream");

    }

    /**
     * KajiLibrary's javax.print.DocFlavor.CHAR_ARRAY -- el dato es un {@code char[]}.
     *
     * <p>El juego de caracteres siempre es {@code utf-16}, y no se puede cambiar por una razon
     * concreta: los {@code char} de Java <b>ya son</b> UTF-16. No hay decodificacion que hacer, asi
     * que declarar otra cosa seria mentir sobre lo que hay en memoria.
     *
     */
    public static class CHAR_ARRAY extends DocFlavor {

        private static final long serialVersionUID = -8720590903724405128L;

        /**
         * @param mimeType el tipo MIME
         * @throws NullPointerException si es null
         * @throws IllegalArgumentException si no es valido
         */
        public CHAR_ARRAY(String mimeType) {
            super(mimeType, "[C");
        }

        /** Texto plano. */
        public static final CHAR_ARRAY TEXT_PLAIN = new CHAR_ARRAY("text/plain; charset=utf-16");

        /** HTML. */
        public static final CHAR_ARRAY TEXT_HTML = new CHAR_ARRAY("text/html; charset=utf-16");

    }

    /**
     * KajiLibrary's javax.print.DocFlavor.STRING -- el dato es un {@link String}.
     *
     * <p>El juego de caracteres siempre es {@code utf-16}, y no se puede cambiar por una razon
     * concreta: los {@code char} de Java <b>ya son</b> UTF-16. No hay decodificacion que hacer, asi
     * que declarar otra cosa seria mentir sobre lo que hay en memoria.
     *
     */
    public static class STRING extends DocFlavor {

        private static final long serialVersionUID = 4414407504887034035L;

        /**
         * @param mimeType el tipo MIME
         * @throws NullPointerException si es null
         * @throws IllegalArgumentException si no es valido
         */
        public STRING(String mimeType) {
            super(mimeType, "java.lang.String");
        }

        /** Texto plano. */
        public static final STRING TEXT_PLAIN = new STRING("text/plain; charset=utf-16");

        /** HTML. */
        public static final STRING TEXT_HTML = new STRING("text/html; charset=utf-16");

    }

    /**
     * KajiLibrary's javax.print.DocFlavor.READER -- el dato es un {@link java.io.Reader}.
     *
     * <p>El juego de caracteres siempre es {@code utf-16}, y no se puede cambiar por una razon
     * concreta: los {@code char} de Java <b>ya son</b> UTF-16. No hay decodificacion que hacer, asi
     * que declarar otra cosa seria mentir sobre lo que hay en memoria.
     *
     */
    public static class READER extends DocFlavor {

        private static final long serialVersionUID = 7100295812579351567L;

        /**
         * @param mimeType el tipo MIME
         * @throws NullPointerException si es null
         * @throws IllegalArgumentException si no es valido
         */
        public READER(String mimeType) {
            super(mimeType, "java.io.Reader");
        }

        /** Texto plano. */
        public static final READER TEXT_PLAIN = new READER("text/plain; charset=utf-16");

        /** HTML. */
        public static final READER TEXT_HTML = new READER("text/html; charset=utf-16");

    }

    /**
     * KajiLibrary's javax.print.DocFlavor.SERVICE_FORMATTED -- el dato es un objeto que dibuja.
     *
     * <p>Los tres comparten el mismo tipo MIME --{@code application/x-java-jvm-local-objectref}-- y se
     * distinguen solo por la clase de representacion. No es un descuido: ese tipo significa
     * literalmente "una referencia a un objeto de esta maquina virtual", y no hay nada mas que decir
     * sobre los bytes porque no hay bytes.
     *
     * <p>La consecuencia practica es que estos formatos <b>no se pueden mandar por la red</b>. El
     * servicio de impresion tiene que estar en el mismo proceso, porque lo que recibe es una llamada a
     * un metodo que dibuja, no un documento.
     *
     */
    public static class SERVICE_FORMATTED extends DocFlavor {

        private static final long serialVersionUID = 6181337766266637256L;

        /**
         * @param className el nombre de la clase de representacion
         * @throws NullPointerException si es null
         */
        public SERVICE_FORMATTED(String className) {
            super("application/x-java-jvm-local-objectref", className);
        }

        /** Una imagen que se rinde a la resolucion que la impresora pida. */
        public static final SERVICE_FORMATTED RENDERABLE_IMAGE =
            new SERVICE_FORMATTED("java.awt.image.renderable.RenderableImage");

        /** Un objeto que dibuja una pagina por vez. */
        public static final SERVICE_FORMATTED PRINTABLE =
            new SERVICE_FORMATTED("java.awt.print.Printable");

        /** Un objeto que ademas sabe cuantas paginas hay y con que formato va cada una. */
        public static final SERVICE_FORMATTED PAGEABLE =
            new SERVICE_FORMATTED("java.awt.print.Pageable");

    }
}
