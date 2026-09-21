package javax.xml.transform;

/**
 * KajiLibrary's javax.xml.transform.URIResolver -- who decides what is on the other side of an
 * `href`.
 *
 * <p>A stylesheet carries references to other documents: `&lt;xsl:import href="base.xsl"/&gt;`,
 * `&lt;xsl:include&gt;`, and XPath's `document()` function. The processor does not resolve them by
 * itself: it passes them to this object, and uses what it returns. That is the whole point of the
 * interface -- **standing between a URI and its content**.
 *
 * <p>What that indirection buys, which is what justifies its existence: the stylesheet can be
 * served from an in-memory catalog or from the application's jar instead of the network; it can be
 * cached; and above all access can be **denied**, which is the usual defence against a hostile
 * document pulling files from disk or opening outgoing connections.
 *
 * <p>The contract of {@link #resolve} has a detail that gets overlooked: returning {@code null}
 * **is not an error**. It means "resolve it yourself as you know how", and the processor goes back
 * to its default mechanism. To really forbid, {@link TransformerException} has to be thrown.
 */
public interface URIResolver {

    /**
     * The document that corresponds to {@code href} resolved against {@code base}.
     *
     * @param href the URI as it appears in the document, possibly relative
     * @param base the base URI against which to resolve it
     * @return the source to use, or null to let the processor resolve on its own
     * @throws TransformerException if the reference cannot or must not be followed
     */
    Source resolve(String href, String base) throws TransformerException;
}
