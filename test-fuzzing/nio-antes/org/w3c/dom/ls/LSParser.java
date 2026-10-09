package org.w3c.dom.ls;

import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * KajiLibrary's org.w3c.dom.ls.LSParser -- the parser of the W3C specification.
 *
 * <p>It does the same as {@code javax.xml.parsers.DocumentBuilder} and it is not a duplicate by
 * oversight: this one comes from the DOM Level 3 specification and that one from the Java platform.
 * They coexist because code written against the W3C has to be able to run on Java without being
 * rewritten.
 *
 * <p>What this one has and the other does not is {@link #parseWithContext}, which parses a fragment
 * and inserts it <b>inside</b> a document that already exists. It is not sugar: parsing a loose
 * fragment and then importing it loses the context --the namespaces declared in the ancestors, the
 * entities of the document-- and may give a different tree.
 *
 * <p>The configuration is not methods but a {@link DOMConfiguration} with parameters by name. It is
 * looser on types and in exchange it lets an implementation add options without changing the
 * interface.
 */
public interface LSParser {

    /** The fragment is appended at the end of the children of the context node. */
    short ACTION_APPEND_AS_CHILDREN = 1;

    /** It replaces all the children of the context node. */
    short ACTION_REPLACE_CHILDREN = 2;

    /** It is inserted before the context node, as a sibling. */
    short ACTION_INSERT_BEFORE = 3;

    /** It is inserted after the context node, as a sibling. */
    short ACTION_INSERT_AFTER = 4;

    /** It replaces the context node. */
    short ACTION_REPLACE = 5;

    /** The parameters of the parser, by name. See the note of the class. */
    DOMConfiguration getDomConfig();

    /** The filter that decides what gets into the tree, or null. */
    LSParserFilter getFilter();

    /** Ver {@link #getFilter}. */
    void setFilter(LSParserFilter filter);

    /**
     * Whether it works in the background.
     *
     * <p>It is a property of the parser and not of the call: it is chosen when creating it with
     * {@link DOMImplementationLS#createLSParser} and does not change afterwards.
     */
    boolean getAsync();

    /**
     * Whether it is busy with an analysis.
     *
     * <p>Calling {@code parse} on a busy one is an error, and this is the way of asking without
     * causing it.
     */
    boolean getBusy();

    /**
     * It parses and returns a new document.
     *
     * @throws LSException with {@link LSException#PARSE_ERR} if it could not
     */
    Document parse(LSInput input) throws DOMException, LSException;

    /** The same, reading from a URI. */
    Document parseURI(String uri) throws DOMException, LSException;

    /**
     * It parses a fragment and puts it into a document that already exists.
     *
     * <p>See the note of the class on why this is not the same as parsing apart and importing.
     *
     * @param contextArg the node relative to which what was read is placed
     * @param action one of the five constants above
     * @return the resulting node, which may not be the same one that was read
     */
    Node parseWithContext(LSInput input, Node contextArg, short action)
        throws DOMException, LSException;

    /**
     * It cuts an analysis in progress short.
     *
     * <p>It makes sense above all in asynchronous mode; on a synchronous one only the filter or an
     * event handler can call it, which are the only ones that run while parsing.
     */
    void abort();
}
