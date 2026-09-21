package javax.xml.crypto;

import java.io.InputStream;

/**
 * KajiLibrary's javax.xml.crypto.OctetStreamData -- a stream of bytes, as data to sign.
 *
 * <p>The other half of {@link Data}. What is really signed is bytes: every chain of transforms ends
 * up turning nodes into octets --with a canonicalization-- because a cryptographic digest knows
 * nothing about trees.
 *
 * <p>It also carries the URI it came from and its content type, both optional. They serve to decide
 * how to interpret it when the stream is not XML: a signature can cover an image or a binary file,
 * and there the type is the only thing that says what it is.
 *
 * <p>It is a stream and not an array, so it <b>gets consumed</b>: reading it twice does not work.
 * It is right for something that can be huge, and it has to be kept in mind when debugging a
 * signature that does not validate.
 */
public class OctetStreamData implements Data {

    /** The stream. */
    private final InputStream octetStream;

    /** Where it came from, or null. */
    private final String uri;

    /** Its content type, or null. */
    private final String mimeType;

    /** The stream only. */
    public OctetStreamData(InputStream octetStream) {
        this(octetStream, null, null);
    }

    /**
     * With the origin and the type.
     *
     * @param uri where it came from, or null
     * @param mimeType its content type, or null
     * @throws NullPointerException if the stream is null
     */
    public OctetStreamData(InputStream octetStream, String uri, String mimeType) {
        if (octetStream == null) {
            throw new NullPointerException("octetStream is null");
        }
        this.octetStream = octetStream;
        this.uri = uri;
        this.mimeType = mimeType;
    }

    /** The stream. It gets consumed; see the class note. */
    public InputStream getOctetStream() {
        return this.octetStream;
    }

    /** Where it came from, or null. */
    public String getURI() {
        return this.uri;
    }

    /** Its content type, or null. */
    public String getMimeType() {
        return this.mimeType;
    }
}
