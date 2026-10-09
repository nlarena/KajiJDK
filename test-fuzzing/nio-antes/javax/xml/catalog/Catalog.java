package javax.xml.catalog;

import java.util.stream.Stream;

/**
 * KajiLibrary's javax.xml.catalog.Catalog -- a table that translates identifiers into local
 * addresses.
 *
 * <p>It solves XML's classic problem: a document says its DTD is at {@code
 * http://example.com/foo.dtd}, and one does not want each parse to go out to the internet to fetch
 * it --nor to fail when there is no network, nor to let a third party decide what is on the other
 * side--. A catalog says "that identifier means this file here".
 *
 * <h2>The three kinds of match</h2>
 *
 * <ul>
 *   <li>{@link #matchSystem} by <b>system</b> identifier: the literal URL the document carries;
 *   <li>{@link #matchPublic} by <b>public</b> identifier: the formal name, in the style of
 *       {@code -//W3C//DTD XHTML 1.0//EN};
 *   <li>{@link #matchURI} for references by URI, which is what XSLT and schemas use.
 * </ul>
 *
 * <p>Which one wins when both match is decided by the {@code PREFER} feature; see
 * {@link CatalogFeatures}.
 *
 * <p>All three return <b>null</b> when there is no match. It is the {@link CatalogResolver} that
 * decides what to do about that, not the catalog.
 *
 * <p>{@link #catalogs} returns the alternative and next catalogs -- a catalog can point to others,
 * and that is how a chain is built.
 */
public interface Catalog {

    /** The local address for that system identifier, or null. */
    String matchSystem(String systemId);

    /** The local address for that public identifier, or null. */
    String matchPublic(String publicId);

    /** The local address for that URI, or null. */
    String matchURI(String uri);

    /** The catalogs chained to this one. */
    Stream<Catalog> catalogs();
}
