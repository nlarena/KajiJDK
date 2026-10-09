package org.w3c.dom.ls;

import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMException;
import org.w3c.dom.Node;

/**
 * KajiLibrary's org.w3c.dom.ls.LSSerializer -- it turns a tree back into text.
 *
 * <p>The way back of {@link LSParser}. The three {@code write} differ in the destination and in one
 * more important thing: {@link #writeToString} has to build the whole output in memory, so for a
 * large document one of the other two has to be used.
 *
 * <h2>Serialising is not the inverse of parsing</h2>
 *
 * <p>It is worth keeping in mind because it is surprising: reading and writing back a document does
 * <b>not</b> return the same bytes. The order of the attributes is not kept --the DOM model does
 * not store it--, the quotes may change, the space between attributes is normalised and the
 * entities may be left expanded. What is kept is the document in the XML sense, not its form.
 * Comparing two XMLs by comparing text is, for that reason, almost always the wrong method.
 *
 * <p>{@link #getNewLine} is the useful exception to that: it is the only part of the form that can
 * be fixed, and it exists precisely because the end of line is the first thing that breaks a
 * comparison between systems.
 */
public interface LSSerializer {

    /** The parameters of the output, by name. */
    DOMConfiguration getDomConfig();

    /** The end of line in use, or null for the system's one. */
    String getNewLine();

    /** See {@link #getNewLine}; null goes back to the system's one. */
    void setNewLine(String newLine);

    /** The filter that decides which nodes go out, or null. */
    LSSerializerFilter getFilter();

    /** Ver {@link #getFilter}. */
    void setFilter(LSSerializerFilter filter);

    /**
     * It writes the node to that destination.
     *
     * @return whether it was written; false when the destination could not be resolved
     * @throws LSException with {@link LSException#SERIALIZE_ERR} if something could not be
     *     serialised
     */
    boolean write(Node nodeArg, LSOutput destination) throws LSException;

    /** The same, to a URI. */
    boolean writeToURI(Node nodeArg, String uri) throws LSException;

    /**
     * The same, to a string.
     *
     * <p>It builds everything in memory; see the note of the class.
     *
     * @throws DOMException if the result does not fit in a {@code String}
     */
    String writeToString(Node nodeArg) throws DOMException, LSException;
}
