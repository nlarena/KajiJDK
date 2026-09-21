package javax.xml.stream.events;

/**
 * KajiLibrary's javax.xml.stream.events.StartDocument -- the start of the document and what its XML
 * declaration says.
 *
 * <h2>The {@code xxxSet()} pairs and why they are needed</h2>
 *
 * <p>The declaration {@code <?xml version="1.0" encoding="UTF-8" standalone="yes"?>} has two
 * optional parts, and for each there are two different questions: what the value is, and whether it
 * was written. {@link #getCharacterEncodingScheme()} against {@link #encodingSet()}, {@link
 * #isStandalone()} against {@link #standaloneSet()}.
 *
 * <p>It is not redundancy. A document without {@code encoding} still has an encoding --the one the
 * parser detected, UTF-8 by default-- and {@link #getCharacterEncodingScheme()} reports it; what
 * {@link #encodingSet()} adds is whether that encoding was declared or deduced. Whoever rewrites
 * the document needs the difference so as not to invent a declaration that was not there, and
 * whoever diagnoses an encoding problem needs to know whether they are being lied to or guessed at.
 *
 * <p>With {@code standalone} the same happens, more strongly: the default value is {@code no}, so
 * {@link #isStandalone()} returns false both for a document that said {@code no} and for one that
 * said nothing, and only {@link #standaloneSet()} tells them apart.
 */
public interface StartDocument extends XMLEvent {

    /**
     * Where the document came from.
     *
     * @return the system identifier, or the empty string if not known
     */
    String getSystemId();

    /**
     * The encoding of the document: the declared one, or the one detected.
     *
     * @return the name of the encoding
     */
    String getCharacterEncodingScheme();

    /**
     * Whether the encoding was written in the XML declaration.
     *
     * @return true if the document declared it
     */
    boolean encodingSet();

    /**
     * The value of {@code standalone}, false by default.
     *
     * @return true only if the document declared {@code standalone="yes"}
     */
    boolean isStandalone();

    /**
     * Whether {@code standalone} was written in the XML declaration.
     *
     * @return true if the document declared it
     */
    boolean standaloneSet();

    /**
     * The declared version.
     *
     * @return {@code "1.0"} if there is no declaration, or whatever the declaration says
     */
    String getVersion();
}
