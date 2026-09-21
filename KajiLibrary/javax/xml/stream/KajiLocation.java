package javax.xml.stream;

/**
 * An immutable location within the document.
 *
 * <p>Package-private: {@link Location} is the public interface and this is nothing more than five
 * fields.
 *
 * <p>The three numeric coordinates are -1 when not known, which is what the interface dictates.
 * Here they are always known because the parser keeps them as it consumes characters; the constant
 * {@link #NONE} is the one used for the events {@link XMLEventFactory} makes by hand, which did not
 * come out of any document.
 */
final class KajiLocation implements Location {

    /** The one given to an event that did not come from a document. */
    static final KajiLocation NONE = new KajiLocation(-1, -1, -1, null, null);

    private final int line;
    private final int column;
    private final int offset;
    private final String publicId;
    private final String systemId;

    KajiLocation(int line, int column, int offset, String publicId, String systemId) {
        this.line = line;
        this.column = column;
        this.offset = offset;
        this.publicId = publicId;
        this.systemId = systemId;
    }

    public int getLineNumber() {
        return line;
    }

    public int getColumnNumber() {
        return column;
    }

    public int getCharacterOffset() {
        return offset;
    }

    public String getPublicId() {
        return publicId;
    }

    public String getSystemId() {
        return systemId;
    }

    public String toString() {
        return "Line number = " + line
                + "\nColumn number = " + column
                + "\nSystem Id = " + systemId
                + "\nPublic Id = " + publicId
                + "\nLocation Uri= " + systemId
                + "\nCharacterOffset = " + offset
                + "\n";
    }
}
