package org.w3c.dom.ls;

import java.io.OutputStream;
import java.io.Writer;

/**
 * KajiLibrary's org.w3c.dom.ls.LSOutput -- where to write a document to.
 *
 * <p>The mirror of {@link LSInput}, with the same order of preference:
 * {@link #getCharacterStream}, then {@link #getByteStream}, and lastly {@link #getSystemId}. The
 * first that is not null is used.
 *
 * <p>There is an asymmetry with the input worth noting: there is no {@code stringData}. It makes
 * sense -- writing to a string is not a destination but a result-- and
 * {@link LSSerializer#writeToString} is there for that.
 */
public interface LSOutput {

    /** The character stream, or null. It is the one that wins if it is there. */
    Writer getCharacterStream();

    /** Ver {@link #getCharacterStream}. */
    void setCharacterStream(Writer characterStream);

    /** The byte stream, or null. It is encoded with {@link #getEncoding}. */
    OutputStream getByteStream();

    /** Ver {@link #getByteStream}. */
    void setByteStream(OutputStream byteStream);

    /** The URI to write to, if no stream was given. */
    String getSystemId();

    /** Ver {@link #getSystemId}. */
    void setSystemId(String systemId);

    /**
     * The output encoding.
     *
     * <p>It only applies to the byte stream: a character stream already has it fixed by whoever
     * opened it, and setting it here does not change it.
     */
    String getEncoding();

    /** Ver {@link #getEncoding}. */
    void setEncoding(String encoding);
}
