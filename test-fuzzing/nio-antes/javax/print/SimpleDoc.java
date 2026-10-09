package javax.print;

import java.io.ByteArrayInputStream;
import java.io.CharArrayReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import javax.print.attribute.AttributeSetUtilities;
import javax.print.attribute.DocAttributeSet;

/**
 * KajiLibrary's javax.print.SimpleDoc -- the common implementation of {@link Doc}.
 *
 * <p>It covers the normal case: data already in memory or an open stream, with its format and its
 * attributes. One only needs to write a {@code Doc} of one's own when the data is produced on the
 * fly.
 *
 * <h2>It checks that the data is of the type it says</h2>
 *
 * <p>The constructor checks that the data is an instance of the format's representation class, and
 * throws {@link IllegalArgumentException} if not. It is worth failing here and not inside the
 * service, where the error would come out as a {@code ClassCastException} without context.
 *
 * <h2>It is read only once</h2>
 *
 * <p>{@link #getReaderForText} and {@link #getStreamForBytes} keep what they return and always
 * return the same, as {@link Doc} asks. The consequence is that a {@code SimpleDoc} serves for a
 * single printing even if the data is a {@code String}.
 */
public final class SimpleDoc implements Doc {

    /** The data. */
    private final Object printData;

    /** What type it is. */
    private final DocFlavor flavor;

    /** The attributes, already read-only. */
    private final DocAttributeSet attributes;

    /** The reader, once created. See the class note. */
    private Reader reader;

    /** The stream, once created. */
    private InputStream inputStream;

    /**
     * @param printData the data, which has to be of the class the format declares
     * @param flavor what type it is
     * @param attributes the document's own attributes, or null
     * @throws IllegalArgumentException if the data or the format is null, or if the data is not of
     *     the declared class
     */
    public SimpleDoc(Object printData, DocFlavor flavor, DocAttributeSet attributes) {
        if (flavor == null || printData == null) {
            throw new IllegalArgumentException("null argument(s)");
        }
        Class<?> repClass;
        try {
            repClass = Class.forName(flavor.getRepresentationClassName(), false,
                                     getClass().getClassLoader());
        } catch (Throwable e) {
            throw new IllegalArgumentException("unknown representation class");
        }
        if (!repClass.isInstance(printData)) {
            throw new IllegalArgumentException("data is not of declared type");
        }
        this.printData = printData;
        this.flavor = flavor;
        if (attributes != null) {
            this.attributes = AttributeSetUtilities.unmodifiableView(attributes);
        } else {
            this.attributes = null;
        }
    }

    /** What type it is. */
    public DocFlavor getDocFlavor() {
        return this.flavor;
    }

    /** The attributes, or null if none were passed. */
    public DocAttributeSet getAttributes() {
        return this.attributes;
    }

    /** The data. */
    public Object getPrintData() throws IOException {
        return this.printData;
    }

    /**
     * The data as characters, or null if it is not text.
     *
     * <p>It recognises {@code char[]}, {@link String} and {@link Reader}. Always the same reader.
     */
    public synchronized Reader getReaderForText() throws IOException {
        if (this.printData instanceof char[]) {
            if (this.reader == null) {
                this.reader = new CharArrayReader((char[]) this.printData);
            }
        } else if (this.printData instanceof String) {
            if (this.reader == null) {
                this.reader = new StringReader((String) this.printData);
            }
        } else if (this.printData instanceof Reader) {
            this.reader = (Reader) this.printData;
        }
        return this.reader;
    }

    /**
     * The data as bytes, or null if it is not.
     *
     * <p>It recognises {@code byte[]} and {@link InputStream}. Always the same stream.
     */
    public synchronized InputStream getStreamForBytes() throws IOException {
        if (this.printData instanceof byte[]) {
            if (this.inputStream == null) {
                this.inputStream = new ByteArrayInputStream((byte[]) this.printData);
            }
        } else if (this.printData instanceof InputStream) {
            this.inputStream = (InputStream) this.printData;
        }
        return this.inputStream;
    }
}
