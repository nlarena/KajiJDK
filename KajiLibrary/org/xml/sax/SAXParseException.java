package org.xml.sax;

/**
 * KajiLibrary's org.xml.sax.SAXParseException -- a `SAXException` that also knows **where** it
 * happened.
 *
 * <p>What it adds over `SAXException` are the four coordinates of a {@link Locator} --public
 * identifier, system identifier, line and column-- but **copied**, not the reference to the
 * locator. That copy is the reason for the class: the `Locator` the parser gives is alive and keeps
 * moving, so keeping it inside an exception that is going to be looked at later would give the
 * wrong position. Here they are frozen in the constructor and do not change any more.
 *
 * <p>It is the type the three methods of {@link ErrorHandler} receive, and that is why it arrives
 * both for fatal errors and for warnings: that the instance is an exception does not mean it was
 * thrown. A `warning` builds it, passes it to the handler and goes on parsing.
 *
 * <p><strong>A detail of `toString` that looks like a typo and is not.</strong> The JDK writes the
 * name of the class and **glues** `publicId: ...` on with no separator, while the other three
 * fields do go preceded by `"; "`. What comes out is `org.xml.sax.SAXParseExceptionpublicId: p;
 * systemId: s; lineNumber: 3; columnNumber: 7; message`. It is reproduced as it is because the
 * format is observable and there are tool outputs that already have it written down; "fixing" it
 * would be changing the behaviour, not correcting it.
 */
public class SAXParseException extends SAXException {

    static final long serialVersionUID = -5651165872476709336L;

    private String publicId;
    private String systemId;
    private int lineNumber;
    private int columnNumber;

    /**
     * @param locator if it is `null`, the four coordinates are left at "unknown" instead of
     *        failing: a parser may report a problem before it has a position.
     */
    public SAXParseException(String message, Locator locator) {
        super(message);
        if (locator != null) {
            init(locator.getPublicId(), locator.getSystemId(),
                    locator.getLineNumber(), locator.getColumnNumber());
        } else {
            init(null, null, -1, -1);
        }
    }

    public SAXParseException(String message, Locator locator, Exception e) {
        super(message, e);
        if (locator != null) {
            init(locator.getPublicId(), locator.getSystemId(),
                    locator.getLineNumber(), locator.getColumnNumber());
        } else {
            init(null, null, -1, -1);
        }
    }

    /** The explicit form, for whoever does not have a `Locator` at hand. */
    public SAXParseException(String message, String publicId, String systemId,
            int lineNumber, int columnNumber) {
        super(message);
        init(publicId, systemId, lineNumber, columnNumber);
    }

    public SAXParseException(String message, String publicId, String systemId,
            int lineNumber, int columnNumber, Exception e) {
        super(message, e);
        init(publicId, systemId, lineNumber, columnNumber);
    }

    private void init(String publicId, String systemId, int lineNumber, int columnNumber) {
        this.publicId = publicId;
        this.systemId = systemId;
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
    }

    public String getPublicId() {
        return this.publicId;
    }

    public String getSystemId() {
        return this.systemId;
    }

    /**
     * Base 1, and -1 if it is not known. It points at the **end** of the construct, not its start.
     */
    public int getLineNumber() {
        return this.lineNumber;
    }

    /** Base 1, and -1 if it is not known. */
    public int getColumnNumber() {
        return this.columnNumber;
    }

    /**
     * See the comment of the header: the gluing of the name of the class and `publicId` is the
     * JDK's and is copied on purpose. The fields at -1 or `null` are not written, so an exception
     * with no position comes out as `org.xml.sax.SAXParseException; message`.
     */
    public String toString() {
        StringBuilder buf = new StringBuilder(getClass().getName());
        String message = getLocalizedMessage();
        if (this.publicId != null) {
            buf.append("publicId: ").append(this.publicId);
        }
        if (this.systemId != null) {
            buf.append("; systemId: ").append(this.systemId);
        }
        if (this.lineNumber != -1) {
            buf.append("; lineNumber: ").append(this.lineNumber);
        }
        if (this.columnNumber != -1) {
            buf.append("; columnNumber: ").append(this.columnNumber);
        }
        if (message != null) {
            buf.append("; ").append(message);
        }
        return buf.toString();
    }
}
