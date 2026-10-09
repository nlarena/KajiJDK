package org.w3c.dom.ls;

import org.w3c.dom.DOMException;

/**
 * KajiLibrary's org.w3c.dom.ls.DOMImplementationLS -- the factory of this whole package.
 *
 * <p>It is obtained by asking a {@code DOMImplementation} for the {@code "LS"} feature, and from it
 * come the four pieces: parser, serialiser, input and output.
 *
 * <p>The last two --{@link #createLSInput} and {@link #createLSOutput}-- are factories of objects
 * that <b>have no public constructor</b>. It is what makes {@link LSResourceResolver} usable:
 * whoever writes one needs to return an {@code LSInput}, and without this they would have to
 * implement the interface by hand every time.
 *
 * <p>The asynchronous mode is chosen when creating the parser and not afterwards, because it
 * changes how it is built inside and not only how it is called.
 */
public interface DOMImplementationLS {

    /** The parser blocks until it finishes. */
    short MODE_SYNCHRONOUS = 1;

    /** The parser returns at once and notifies through events. */
    short MODE_ASYNCHRONOUS = 2;

    /**
     * A parser.
     *
     * @param mode one of the two above
     * @param schemaType the namespace of the schema language to validate with, or null
     * @throws DOMException if this implementation does not support that mode or that type of schema
     */
    LSParser createLSParser(short mode, String schemaType) throws DOMException;

    /** A serialiser. */
    LSSerializer createLSSerializer();

    /** An empty input, to be filled in. See the note of the class. */
    LSInput createLSInput();

    /** An empty output, to be filled in. */
    LSOutput createLSOutput();
}
