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
 * El resolutor que devuelve {@link CatalogManager}.
 *
 * <p>De acceso de paquete: no es API. Consulta el catalogo y, cuando no hay coincidencia, aplica la
 * {@link CatalogResolver.NotFoundAction} que le toco.
 *
 * <p>Los cuatro metodos hacen lo mismo en distinto tipo, y las diferencias entre ellos estan en como
 * cada API expresa "no encontre nada" y "aca tenes algo vacio".
 */
final class CatalogResolverImpl implements CatalogResolver {

    /** Contra que se consulta. */
    private final Catalog catalog;

    /** Que hacer cuando no coincide. */
    private final NotFoundAction action;

    CatalogResolverImpl(Catalog catalog, NotFoundAction action) {
        this.catalog = catalog;
        this.action = action;
    }

    /**
     * La resolucion de SAX.
     *
     * <p>Sin coincidencia: null para continuar, una fuente con un lector vacio para ignorar.
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
     * La resolucion de las transformaciones.
     *
     * <p>Es la unica que <b>no</b> devuelve null para continuar: la API interpretaria ese null como
     * "resolvelo vos", y lo que corresponde ahi es entregarle la direccion ya resuelta contra la base.
     * Es lo que hace el JDK y se comprobo contra el JDK 25.
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
     * La resolucion de StAX.
     *
     * <p>Devuelve null para continuar <b>y</b> para ignorar: no hay forma de entregar un flujo vacio
     * sin abrir uno, y el contrato de {@code XMLResolver} ya admite null.
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
     * La resolucion de DOM.
     *
     * <p>Devuelve null salvo que haya coincidencia; ver {@link #resolveEntity(String, String, String,
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
     * La consulta al catalogo, en el orden que corresponde.
     *
     * <p>El identificador de sistema primero: es el que identifica el recurso concreto. El publico es
     * un nombre formal y puede ser ambiguo entre versiones.
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

    /** {@code href} resuelto contra {@code base}, o tal cual si no se puede. */
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
