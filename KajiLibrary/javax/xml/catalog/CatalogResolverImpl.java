package javax.xml.catalog;

import java.io.InputStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import org.w3c.dom.ls.LSInput;
import org.xml.sax.InputSource;

/**
 * The resolver {@link CatalogManager} returns.
 *
 * <p>Package access: it is not API. It queries the catalog and, when there is no match, applies the
 * {@link CatalogResolver.NotFoundAction} it was given.
 *
 * <p>The four methods do the same thing in a different type, and the differences between them are
 * in how each API expresses "I found nothing" and "here is something empty".
 */
final class CatalogResolverImpl implements CatalogResolver {

    /** What is queried. */
    private final Catalog catalog;

    /** What to do when it does not match. */
    private final NotFoundAction action;

    CatalogResolverImpl(Catalog catalog, NotFoundAction action) {
        this.catalog = catalog;
        this.action = action;
    }

    /**
     * SAX's resolution.
     *
     * <p>Without a match: null to continue, a source with an empty reader to ignore.
     */
    public InputSource resolveEntity(String publicId, String systemId) {
        String resolved = match(publicId, systemId);
        if (resolved != null) {
            InputSource source = new InputSource(resolved);
            source.setPublicId(publicId);
            return source;
        }
        if (this.action == NotFoundAction.STRICT) {
            throw CatalogMessages.noMatch(publicId, systemId);
        }
        if (this.action == NotFoundAction.IGNORE) {
            return new InputSource(new StringReader(""));
        }
        return null;
    }

    /**
     * The transformations' resolution.
     *
     * <p>It is the only one that does <b>not</b> return null to continue: the API would read that
     * null as "resolve it yourself", and what is due there is to hand it the address already
     * resolved against the base. It is what the JDK does, checked against JDK 25.
     */
    public Source resolve(String href, String base) {
        if (href == null) {
            throw new NullPointerException();
        }
        String resolved = this.catalog.matchURI(href);
        if (resolved != null) {
            return new SAXSource(new InputSource(resolved));
        }
        if (this.action == NotFoundAction.STRICT) {
            throw CatalogMessages.noMatch(null, href);
        }
        if (this.action == NotFoundAction.IGNORE) {
            return new SAXSource(new InputSource(new StringReader("")));
        }
        return new SAXSource(new InputSource(absolutize(href, base)));
    }

    /**
     * StAX's resolution.
     *
     * <p>It returns null to continue <b>and</b> to ignore: there is no way of handing over an empty
     * stream without opening one, and the {@code XMLResolver} contract already admits null.
     */
    public InputStream resolveEntity(String publicId, String systemId, String baseURI,
                                     String namespace) {
        String resolved = match(publicId, systemId);
        if (resolved == null && this.action == NotFoundAction.STRICT) {
            throw CatalogMessages.noMatch(publicId, systemId);
        }
        return null;
    }

    /**
     * DOM's resolution.
     *
     * <p>It returns null unless there is a match; see {@link #resolveEntity(String, String, String,
     * String)}.
     */
    public LSInput resolveResource(String type, String namespaceURI, String publicId,
                                   String systemId, String baseURI) {
        String resolved = match(publicId, systemId);
        if (resolved == null && this.action == NotFoundAction.STRICT) {
            throw CatalogMessages.noMatch(publicId, systemId);
        }
        return null;
    }

    /**
     * The query to the catalog, in the order that corresponds.
     *
     * <p>The system identifier first: it is the one that identifies the concrete resource. The
     * public one is a formal name and can be ambiguous between versions.
     */
    private String match(String publicId, String systemId) {
        if (systemId != null) {
            String bySystem = this.catalog.matchSystem(systemId);
            if (bySystem != null) {
                return bySystem;
            }
        }
        if (publicId != null) {
            return this.catalog.matchPublic(publicId);
        }
        return null;
    }

    /** {@code href} resolved against {@code base}, or as is if that cannot be done. */
    private static String absolutize(String href, String base) {
        if (base == null) {
            return href;
        }
        try {
            return new URL(new URL(base), href).toString();
        } catch (MalformedURLException e) {
            return href;
        }
    }
}
