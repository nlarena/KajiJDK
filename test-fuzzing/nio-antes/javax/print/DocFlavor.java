package javax.print;

import java.io.Serializable;
import java.nio.charset.Charset;

/**
 * KajiLibrary's javax.print.DocFlavor -- what type a document is and in what form the data is.
 *
 * <p>They are <b>two</b> things, and that is the whole design of the class:
 *
 * <ul>
 *   <li>the <b>MIME type</b>, which says what the document is: PDF, PostScript, plain text in
 *     UTF-8;
 *   <li>the <b>representation class</b>, which says how it is handed to the service: a
 *       {@code byte[]}, an {@code InputStream}, a {@code URL}.
 * </ul>
 *
 * <p>A PDF in a byte array and the same PDF behind a URL are two different formats even if the
 * document is the same, and a printer may accept one and not the other. That is why the constants
 * are grouped in nested classes by representation and not by type.
 *
 * <h2>The {@code _HOST} variants</h2>
 *
 * <p>{@link #hostEncoding} is <b>this</b> virtual machine's default encoding, and the constants
 * that use it inherit its problem: the same format means one thing here and another on another
 * machine. They serve to print something just read from the local system; for anything that crosses
 * the network or is stored, the explicit variants.
 *
 * <h2>The comparison is on the canonical form</h2>
 *
 * <p>{@code "Text/Plain; CharSet=Utf-8"} and {@code "text/plain;charset=utf-8"} are equal: type,
 * subtype and parameter names are lowered and the parameters are sorted. The values are left as
 * they are, except the {@code charset} one, which is case-insensitive by definition.
 *
 * <h2>{@code AUTOSENSE}</h2>
 *
 * <p>It is {@code application/octet-stream}: undeclared bytes, for the printer to work out. It
 * works often and fails silently when it does not -- a page of garbage comes out. It is the last
 * thing to try, not the first.
 */
public class DocFlavor implements Serializable, Cloneable {

    private static final long serialVersionUID = -4512080796965449721L;

    /**
     * This virtual machine's default encoding. See the class note.
     *
     * <p>It is not a compile-time constant: it is computed when the class loads.
     */
    public static final String hostEncoding;

    static {
        hostEncoding = Charset.defaultCharset().name();
    }

    /** The MIME type, already normalised. */
    private transient MimeType myMimeType;

    /** The name of the representation class. */
    private final String myClassName;

    /**
     * @param mimeType the MIME type
     * @param className the full name of the representation class
     * @throws NullPointerException if either is null
     * @throws IllegalArgumentException if the MIME type is not valid
     */
    public DocFlavor(String mimeType, String className) {
        if (className == null) {
            throw new NullPointerException();
        }
        this.myMimeType = new MimeType(mimeType);
        this.myClassName = className;
    }

    /** The MIME type in canonical form. See the class note. */
    public String getMimeType() {
        return this.myMimeType.getMimeType();
    }

    /** The type, in lower case. */
    public String getMediaType() {
        return this.myMimeType.getMediaType();
    }

    /** The subtype, in lower case. */
    public String getMediaSubtype() {
        return this.myMimeType.getMediaSubtype();
    }

    /**
     * The value of that parameter, or null.
     *
     * <p>The name is looked up case-insensitively, because they are already normalised.
     */
    public String getParameter(String paramName) {
        return this.myMimeType.getParameterMap().get(paramName.toLowerCase());
    }

    /** The name of the representation class. */
    public String getRepresentationClassName() {
        return this.myClassName;
    }

    /** The canonical type plus {@code class="..."}. */
    @Override
    public String toString() {
        return getStringValue();
    }

    /** On the canonical type and the representation class, both. */
    @Override
    public int hashCode() {
        return getStringValue().hashCode();
    }

    /** Likewise: two formats are equal only if they match on both things. */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof DocFlavor)) {
            return false;
        }
        DocFlavor other = (DocFlavor) obj;
        return this.myClassName.equals(other.myClassName)
            && this.myMimeType.equals(other.myMimeType);
    }

    /** The text {@code toString}, {@code hashCode} and {@code equals} use. */
    private String getStringValue() {
        return getMimeType() + "; class=\"" + this.myClassName + "\"";
    }

    /**
     * KajiLibrary's javax.print.DocFlavor.BYTE_ARRAY -- the data is a {@code byte[]}.
     *
     * <p>Here the character set does matter, because bytes without declaring it mean nothing. See
     * the note of {@link DocFlavor} on the {@code _HOST} variants.
     */
    public static class BYTE_ARRAY extends DocFlavor {

        private static final long serialVersionUID = -9065578006593857475L;

        /**
         * @param mimeType the MIME type
         * @throws NullPointerException if it is null
         * @throws IllegalArgumentException if it is not valid
         */
        public BYTE_ARRAY(String mimeType) {
            super(mimeType, "[B");
        }

        /** Plain text, the platform's encoding. */
        public static final BYTE_ARRAY TEXT_PLAIN_HOST = new BYTE_ARRAY("text/plain; charset=" + hostEncoding);

        /** Plain text in UTF-8. */
        public static final BYTE_ARRAY TEXT_PLAIN_UTF_8 = new BYTE_ARRAY("text/plain; charset=utf-8");

        /** Plain text in UTF-16, with byte order mark. */
        public static final BYTE_ARRAY TEXT_PLAIN_UTF_16 = new BYTE_ARRAY("text/plain; charset=utf-16");

        /** Plain text in big-endian UTF-16. */
        public static final BYTE_ARRAY TEXT_PLAIN_UTF_16BE = new BYTE_ARRAY("text/plain; charset=utf-16be");

        /** Plain text in little-endian UTF-16. */
        public static final BYTE_ARRAY TEXT_PLAIN_UTF_16LE = new BYTE_ARRAY("text/plain; charset=utf-16le");

        /** Plain text in ASCII. */
        public static final BYTE_ARRAY TEXT_PLAIN_US_ASCII = new BYTE_ARRAY("text/plain; charset=us-ascii");

        /** HTML, the platform's encoding. */
        public static final BYTE_ARRAY TEXT_HTML_HOST = new BYTE_ARRAY("text/html; charset=" + hostEncoding);

        /** HTML in UTF-8. */
        public static final BYTE_ARRAY TEXT_HTML_UTF_8 = new BYTE_ARRAY("text/html; charset=utf-8");

        /** HTML in UTF-16, with byte order mark. */
        public static final BYTE_ARRAY TEXT_HTML_UTF_16 = new BYTE_ARRAY("text/html; charset=utf-16");

        /** HTML in big-endian UTF-16. */
        public static final BYTE_ARRAY TEXT_HTML_UTF_16BE = new BYTE_ARRAY("text/html; charset=utf-16be");

        /** HTML in little-endian UTF-16. */
        public static final BYTE_ARRAY TEXT_HTML_UTF_16LE = new BYTE_ARRAY("text/html; charset=utf-16le");

        /** HTML in ASCII. */
        public static final BYTE_ARRAY TEXT_HTML_US_ASCII = new BYTE_ARRAY("text/html; charset=us-ascii");

        /** PDF. */
        public static final BYTE_ARRAY PDF = new BYTE_ARRAY("application/pdf");

        /** PostScript. */
        public static final BYTE_ARRAY POSTSCRIPT = new BYTE_ARRAY("application/postscript");

        /** HP's PCL. */
        public static final BYTE_ARRAY PCL = new BYTE_ARRAY("application/vnd.hp-pcl");

        /** GIF. */
        public static final BYTE_ARRAY GIF = new BYTE_ARRAY("image/gif");

        /** JPEG. */
        public static final BYTE_ARRAY JPEG = new BYTE_ARRAY("image/jpeg");

        /** PNG. */
        public static final BYTE_ARRAY PNG = new BYTE_ARRAY("image/png");

        /**
         * Undeclared bytes: for the printer to work out what they are. It is the last thing to try.
         */
        public static final BYTE_ARRAY AUTOSENSE = new BYTE_ARRAY("application/octet-stream");

    }

    /**
     * KajiLibrary's javax.print.DocFlavor.INPUT_STREAM -- the data is a {@link
     * java.io.InputStream}.
     *
     * <p>Here the character set does matter, because bytes without declaring it mean nothing. See
     * the note of {@link DocFlavor} on the {@code _HOST} variants.
     */
    public static class INPUT_STREAM extends DocFlavor {

        private static final long serialVersionUID = -7045842700749194127L;

        /**
         * @param mimeType the MIME type
         * @throws NullPointerException if it is null
         * @throws IllegalArgumentException if it is not valid
         */
        public INPUT_STREAM(String mimeType) {
            super(mimeType, "java.io.InputStream");
        }

        /** Plain text, the platform's encoding. */
        public static final INPUT_STREAM TEXT_PLAIN_HOST = new INPUT_STREAM("text/plain; charset=" + hostEncoding);

        /** Plain text in UTF-8. */
        public static final INPUT_STREAM TEXT_PLAIN_UTF_8 = new INPUT_STREAM("text/plain; charset=utf-8");

        /** Plain text in UTF-16, with byte order mark. */
        public static final INPUT_STREAM TEXT_PLAIN_UTF_16 = new INPUT_STREAM("text/plain; charset=utf-16");

        /** Plain text in big-endian UTF-16. */
        public static final INPUT_STREAM TEXT_PLAIN_UTF_16BE = new INPUT_STREAM("text/plain; charset=utf-16be");

        /** Plain text in little-endian UTF-16. */
        public static final INPUT_STREAM TEXT_PLAIN_UTF_16LE = new INPUT_STREAM("text/plain; charset=utf-16le");

        /** Plain text in ASCII. */
        public static final INPUT_STREAM TEXT_PLAIN_US_ASCII = new INPUT_STREAM("text/plain; charset=us-ascii");

        /** HTML, the platform's encoding. */
        public static final INPUT_STREAM TEXT_HTML_HOST = new INPUT_STREAM("text/html; charset=" + hostEncoding);

        /** HTML in UTF-8. */
        public static final INPUT_STREAM TEXT_HTML_UTF_8 = new INPUT_STREAM("text/html; charset=utf-8");

        /** HTML in UTF-16, with byte order mark. */
        public static final INPUT_STREAM TEXT_HTML_UTF_16 = new INPUT_STREAM("text/html; charset=utf-16");

        /** HTML in big-endian UTF-16. */
        public static final INPUT_STREAM TEXT_HTML_UTF_16BE = new INPUT_STREAM("text/html; charset=utf-16be");

        /** HTML in little-endian UTF-16. */
        public static final INPUT_STREAM TEXT_HTML_UTF_16LE = new INPUT_STREAM("text/html; charset=utf-16le");

        /** HTML in ASCII. */
        public static final INPUT_STREAM TEXT_HTML_US_ASCII = new INPUT_STREAM("text/html; charset=us-ascii");

        /** PDF. */
        public static final INPUT_STREAM PDF = new INPUT_STREAM("application/pdf");

        /** PostScript. */
        public static final INPUT_STREAM POSTSCRIPT = new INPUT_STREAM("application/postscript");

        /** HP's PCL. */
        public static final INPUT_STREAM PCL = new INPUT_STREAM("application/vnd.hp-pcl");

        /** GIF. */
        public static final INPUT_STREAM GIF = new INPUT_STREAM("image/gif");

        /** JPEG. */
        public static final INPUT_STREAM JPEG = new INPUT_STREAM("image/jpeg");

        /** PNG. */
        public static final INPUT_STREAM PNG = new INPUT_STREAM("image/png");

        /**
         * Undeclared bytes: for the printer to work out what they are. It is the last thing to try.
         */
        public static final INPUT_STREAM AUTOSENSE = new INPUT_STREAM("application/octet-stream");

    }

    /**
     * KajiLibrary's javax.print.DocFlavor.URL -- the data is a {@link java.net.URL}.
     *
     * <p>It is the only representation where the data does <b>not</b> travel: the address is passed
     * to the service and it fetches it. That means the printer has to be able to reach that URL,
     * which is not obvious if it is on another network.
     *
     * <p>Here the character set does matter, because bytes without declaring it mean nothing. See
     * the note of {@link DocFlavor} on the {@code _HOST} variants.
     */
    public static class URL extends DocFlavor {

        private static final long serialVersionUID = 2936725788144902062L;

        /**
         * @param mimeType the MIME type
         * @throws NullPointerException if it is null
         * @throws IllegalArgumentException if it is not valid
         */
        public URL(String mimeType) {
            super(mimeType, "java.net.URL");
        }

        /** Plain text, the platform's encoding. */
        public static final URL TEXT_PLAIN_HOST = new URL("text/plain; charset=" + hostEncoding);

        /** Plain text in UTF-8. */
        public static final URL TEXT_PLAIN_UTF_8 = new URL("text/plain; charset=utf-8");

        /** Plain text in UTF-16, with byte order mark. */
        public static final URL TEXT_PLAIN_UTF_16 = new URL("text/plain; charset=utf-16");

        /** Plain text in big-endian UTF-16. */
        public static final URL TEXT_PLAIN_UTF_16BE = new URL("text/plain; charset=utf-16be");

        /** Plain text in little-endian UTF-16. */
        public static final URL TEXT_PLAIN_UTF_16LE = new URL("text/plain; charset=utf-16le");

        /** Plain text in ASCII. */
        public static final URL TEXT_PLAIN_US_ASCII = new URL("text/plain; charset=us-ascii");

        /** HTML, the platform's encoding. */
        public static final URL TEXT_HTML_HOST = new URL("text/html; charset=" + hostEncoding);

        /** HTML in UTF-8. */
        public static final URL TEXT_HTML_UTF_8 = new URL("text/html; charset=utf-8");

        /** HTML in UTF-16, with byte order mark. */
        public static final URL TEXT_HTML_UTF_16 = new URL("text/html; charset=utf-16");

        /** HTML in big-endian UTF-16. */
        public static final URL TEXT_HTML_UTF_16BE = new URL("text/html; charset=utf-16be");

        /** HTML in little-endian UTF-16. */
        public static final URL TEXT_HTML_UTF_16LE = new URL("text/html; charset=utf-16le");

        /** HTML in ASCII. */
        public static final URL TEXT_HTML_US_ASCII = new URL("text/html; charset=us-ascii");

        /** PDF. */
        public static final URL PDF = new URL("application/pdf");

        /** PostScript. */
        public static final URL POSTSCRIPT = new URL("application/postscript");

        /** HP's PCL. */
        public static final URL PCL = new URL("application/vnd.hp-pcl");

        /** GIF. */
        public static final URL GIF = new URL("image/gif");

        /** JPEG. */
        public static final URL JPEG = new URL("image/jpeg");

        /** PNG. */
        public static final URL PNG = new URL("image/png");

        /**
         * Undeclared bytes: for the printer to work out what they are. It is the last thing to try.
         */
        public static final URL AUTOSENSE = new URL("application/octet-stream");

    }

    /**
     * KajiLibrary's javax.print.DocFlavor.CHAR_ARRAY -- the data is a {@code char[]}.
     *
     * <p>The character set is always {@code utf-16}, and it cannot be changed for a concrete
     * reason: Java {@code char}s <b>already are</b> UTF-16. There is no decoding to do, so
     * declaring something else would be lying about what is in memory.
     */
    public static class CHAR_ARRAY extends DocFlavor {

        private static final long serialVersionUID = -8720590903724405128L;

        /**
         * @param mimeType the MIME type
         * @throws NullPointerException if it is null
         * @throws IllegalArgumentException if it is not valid
         */
        public CHAR_ARRAY(String mimeType) {
            super(mimeType, "[C");
        }

        /** Plain text. */
        public static final CHAR_ARRAY TEXT_PLAIN = new CHAR_ARRAY("text/plain; charset=utf-16");

        /** HTML. */
        public static final CHAR_ARRAY TEXT_HTML = new CHAR_ARRAY("text/html; charset=utf-16");

    }

    /**
     * KajiLibrary's javax.print.DocFlavor.STRING -- the data is a {@link String}.
     *
     * <p>The character set is always {@code utf-16}, and it cannot be changed for a concrete
     * reason: Java {@code char}s <b>already are</b> UTF-16. There is no decoding to do, so
     * declaring something else would be lying about what is in memory.
     */
    public static class STRING extends DocFlavor {

        private static final long serialVersionUID = 4414407504887034035L;

        /**
         * @param mimeType the MIME type
         * @throws NullPointerException if it is null
         * @throws IllegalArgumentException if it is not valid
         */
        public STRING(String mimeType) {
            super(mimeType, "java.lang.String");
        }

        /** Plain text. */
        public static final STRING TEXT_PLAIN = new STRING("text/plain; charset=utf-16");

        /** HTML. */
        public static final STRING TEXT_HTML = new STRING("text/html; charset=utf-16");

    }

    /**
     * KajiLibrary's javax.print.DocFlavor.READER -- the data is a {@link java.io.Reader}.
     *
     * <p>The character set is always {@code utf-16}, and it cannot be changed for a concrete
     * reason: Java {@code char}s <b>already are</b> UTF-16. There is no decoding to do, so
     * declaring something else would be lying about what is in memory.
     */
    public static class READER extends DocFlavor {

        private static final long serialVersionUID = 7100295812579351567L;

        /**
         * @param mimeType the MIME type
         * @throws NullPointerException if it is null
         * @throws IllegalArgumentException if it is not valid
         */
        public READER(String mimeType) {
            super(mimeType, "java.io.Reader");
        }

        /** Plain text. */
        public static final READER TEXT_PLAIN = new READER("text/plain; charset=utf-16");

        /** HTML. */
        public static final READER TEXT_HTML = new READER("text/html; charset=utf-16");

    }

    /**
     * KajiLibrary's javax.print.DocFlavor.SERVICE_FORMATTED -- the data is an object that draws.
     *
     * <p>The three share the same MIME type --{@code application/x-java-jvm-local-objectref}-- and
     * are told apart only by the representation class. It is not an oversight: that type literally
     * means "a reference to an object of this virtual machine", and there is nothing more to say
     * about the bytes because there are no bytes.
     *
     * <p>The practical consequence is that these formats <b>cannot be sent over the network</b>.
     * The print service has to be in the same process, because what it receives is a call to a
     * method that draws, not a document.
     */
    public static class SERVICE_FORMATTED extends DocFlavor {

        private static final long serialVersionUID = 6181337766266637256L;

        /**
         * @param className the name of the representation class
         * @throws NullPointerException if it is null
         */
        public SERVICE_FORMATTED(String className) {
            super("application/x-java-jvm-local-objectref", className);
        }

        /** An image rendered at whatever resolution the printer asks for. */
        public static final SERVICE_FORMATTED RENDERABLE_IMAGE =
            new SERVICE_FORMATTED("java.awt.image.renderable.RenderableImage");

        /** An object that draws one page at a time. */
        public static final SERVICE_FORMATTED PRINTABLE =
            new SERVICE_FORMATTED("java.awt.print.Printable");

        /** An object that also knows how many pages there are and with which format each goes. */
        public static final SERVICE_FORMATTED PAGEABLE =
            new SERVICE_FORMATTED("java.awt.print.Pageable");

    }
}
