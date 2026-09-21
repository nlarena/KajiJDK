package org.w3c.dom.ls;

import java.io.InputStream;
import java.io.Reader;

/**
 * KajiLibrary's org.w3c.dom.ls.LSInput -- where to read a document from.
 *
 * <p>Four ways of saying the same thing --characters, bytes, a string, a URI-- and an order of
 * preference that <b>does</b> matter: first {@link #getCharacterStream} is looked at, then
 * {@link #getByteStream}, then {@link #getStringData} and lastly {@link #getSystemId}. The first
 * that is not null is used and the rest are ignored.
 *
 * <p>The order is not arbitrary: it goes from the most resolved to the least. A character stream
 * already has its encoding decided by whoever opened it; a byte stream still has to be decoded; a
 * URI has not even been read. Whoever implements an {@link LSResourceResolver} usually returns the
 * highest one they have at hand, and that is why it is as well to keep the order in mind: setting a
 * character stream and also a {@code systemId} does not offer two options, it silences the second.
 *
 * <p>It is an interface with setters, which in Java is not usual. It comes from the specification
 * being the W3C's and written in IDL, where these are read-write <b>attributes</b>.
 */
public interface LSInput {

    /** The character stream, or null. It is the one that wins if it is there. */
    Reader getCharacterStream();

    /** Ver {@link #getCharacterStream}. */
    void setCharacterStream(Reader characterStream);

    /** The byte stream, or null. It is decoded with {@link #getEncoding}. */
    InputStream getByteStream();

    /** Ver {@link #getByteStream}. */
    void setByteStream(InputStream byteStream);

    /** The whole document as a string, or null. */
    String getStringData();

    /** Ver {@link #getStringData}. */
    void setStringData(String stringData);

    /**
     * Where it came from, so that relative things can be resolved.
     *
     * <p>It serves for two different things: as the <b>last</b> resort for reading, and as the base
     * of the relative references of the document even if the contents came by another road.
     */
    String getSystemId();

    /** Ver {@link #getSystemId}. */
    void setSystemId(String systemId);

    /** The public identifier, if it has one. */
    String getPublicId();

    /** Ver {@link #getPublicId}. */
    void setPublicId(String publicId);

    /** The base a relative {@link #getSystemId} is resolved against. */
    String getBaseURI();

    /** Ver {@link #getBaseURI}. */
    void setBaseURI(String baseURI);

    /** The encoding of the byte stream; it is ignored with a character stream or a string. */
    String getEncoding();

    /** Ver {@link #getEncoding}. */
    void setEncoding(String encoding);

    /**
     * Whether whoever hands it over certifies that it is already well formed.
     *
     * <p>With this at true the parser may skip encoding checks. It is a promise of the provider,
     * not a check: if it is a lie, the result is undefined.
     */
    boolean getCertifiedText();

    /** Ver {@link #getCertifiedText}. */
    void setCertifiedText(boolean certifiedText);
}
