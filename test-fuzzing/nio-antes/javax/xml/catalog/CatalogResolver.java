package javax.xml.catalog;

import java.io.InputStream;
import javax.xml.stream.XMLResolver;
import javax.xml.transform.Source;
import javax.xml.transform.URIResolver;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

/**
 * KajiLibrary's javax.xml.catalog.CatalogResolver -- plugs a {@link Catalog} into the four XML
 * APIs.
 *
 * <p>It extends the four resolution interfaces there are in the platform --SAX, StAX,
 * transformation and DOM-- so that the same object serves as resolver in any of them. It is what
 * makes configuring a catalog one line and not four implementations.
 *
 * <h2>What happens when there is no match</h2>
 *
 * <p>{@link NotFoundAction} decides it, and it comes from the {@code RESOLVE} feature. It is the
 * part to understand before using this in production; see there.
 *
 * <h2>The two four-argument {@code resolveEntity}s</h2>
 *
 * <p>Only one is declared, returning {@link InputStream}; the other that appears in the bytecode is
 * the bridge the compiler generates because {@link XMLResolver} declares it returning {@code
 * Object}. It is a compilation detail, not two methods.
 */
public interface CatalogResolver
    extends EntityResolver, XMLResolver, URIResolver, LSResourceResolver {

    /**
     * What to do when the catalog does not have the entry.
     *
     * <p>The three options are very different and choosing wrong is paid for late:
     *
     * <ul>
     *   <li>{@link #STRICT} --the default-- throws {@link CatalogException}. It is what to use in a
     *       closed deployment: if the catalog does not cover something, one wants to know;
     *   <li>{@link #CONTINUE} returns null, which in all these APIs means "resolve it yourself as
     *       usual". That is: <b>it goes out to the network</b>. It is what is wanted when the
     *       catalog is a cache and not a restriction;
     *   <li>{@link #IGNORE} returns something empty. The parser goes on as if the resource existed
     *       and were blank -- useful to skip a DTD that only declares entities that are not used,
     *       and dangerous if that DTD defined default values.
     * </ul>
     */
    enum NotFoundAction {

        /** Return null and let the API resolve by itself. See the note. */
        CONTINUE("continue"),

        /** Return something empty. See the note. */
        IGNORE("ignore"),

        /** Throw {@link CatalogException}. It is the default. */
        STRICT("strict");

        /** The name the {@code RESOLVE} feature uses. */
        private final String literal;

        NotFoundAction(String literal) {
            this.literal = literal;
        }

        /** The name in lower case, not the constant's. */
        @Override
        public String toString() {
            return this.literal;
        }

        /**
         * The action with that name.
         *
         * @throws IllegalArgumentException if it is none; case-sensitive
         */
        public static NotFoundAction getType(String literal) {
            NotFoundAction[] all = values();
            int i = 0;
            while (i < all.length) {
                if (all[i].literal.equals(literal)) {
                    return all[i];
                }
                i = i + 1;
            }
            throw CatalogMessages.invalidArgument(literal, "RESOLVE");
        }
    }

    /**
     * SAX's resolution.
     *
     * @return the source, or null; see {@link NotFoundAction}
     * @throws CatalogException in strict mode without a match
     */
    InputSource resolveEntity(String publicId, String systemId);

    /**
     * The transformations' resolution.
     *
     * @return the source, or null
     * @throws CatalogException in strict mode without a match
     */
    Source resolve(String href, String base);

    /**
     * StAX's resolution.
     *
     * <p>It returns {@link InputStream} and not {@code Object} like {@link XMLResolver}: it is a
     * narrowing of the return type, allowed and more useful.
     *
     * @return the stream, or null
     * @throws CatalogException in strict mode without a match
     */
    InputStream resolveEntity(String publicId, String systemId, String baseURI, String namespace);

    /**
     * DOM's resolution.
     *
     * @return the input, or null
     * @throws CatalogException in strict mode without a match
     */
    LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId,
                            String baseURI);
}
