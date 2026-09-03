package javax.xml.parsers;

import java.util.Iterator;
import java.util.ServiceLoader;
import javax.xml.validation.Schema;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

/**
 * KajiLibrary's javax.xml.parsers.SAXParserFactory -- de donde salen los {@link SAXParser}.
 *
 * <p>Misma idea y mismos seis {@code newInstance} que {@link DocumentBuilderFactory}, con el que
 * conviene leerla en paralelo: la busqueda por propiedad de sistema, servicio o implementacion de la
 * plataforma esta explicada alla y aca es identica, cambiando el nombre de la propiedad.
 *
 * <p>Lo que si es distinto es cuanto se puede configurar: aca no hay banderas de comentarios, de
 * espacio en blanco ni de CDATA. No es una omision -- son opciones sobre <b>que se guarda en el
 * arbol</b>, y SAX no arma ningun arbol. Lo que en DOM es una bandera, en SAX es simplemente un
 * metodo del manejador que uno no escribe.
 *
 * <h2>Por que {@code setFeature} lanza tres excepciones</h2>
 *
 * <p>Son tres respuestas distintas y vale distinguirlas: {@link SAXNotRecognizedException} es "no se
 * que es eso", {@link SAXNotSupportedException} es "se que es pero no lo hago", y
 * {@link ParserConfigurationException} es "lo hago, pero no con el resto de lo que ya me pediste".
 * La ultima es la unica que se arregla cambiando otra cosa.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>{@link #newDefaultInstance} lanza {@link FactoryConfigurationError}; ver la nota equivalente en
 * {@link DocumentBuilderFactory}.
 */
public abstract class SAXParserFactory {

    /** La propiedad de sistema que nombra la fabrica. */
    private static final String PROPERTY = "javax.xml.parsers.SAXParserFactory";

    private boolean namespaceAware = false;
    private boolean validating = false;

    /** Para las subclases. */
    protected SAXParserFactory() {
    }

    /**
     * La fabrica de la plataforma, ya puesta en {@code namespaceAware}.
     *
     * @throws FactoryConfigurationError siempre en KajiLibrary; ver la nota de la clase
     */
    public static SAXParserFactory newDefaultNSInstance() {
        SAXParserFactory factory = newDefaultInstance();
        factory.setNamespaceAware(true);
        return factory;
    }

    /** Como {@link #newInstance()}, ya puesta en {@code namespaceAware}. */
    public static SAXParserFactory newNSInstance() {
        SAXParserFactory factory = newInstance();
        factory.setNamespaceAware(true);
        return factory;
    }

    /** Como {@link #newInstance(String, ClassLoader)}, ya puesta en {@code namespaceAware}. */
    public static SAXParserFactory newNSInstance(String factoryClassName,
                                                 ClassLoader classLoader) {
        SAXParserFactory factory = newInstance(factoryClassName, classLoader);
        factory.setNamespaceAware(true);
        return factory;
    }

    /**
     * La fabrica de la plataforma, sin mirar propiedades ni servicios.
     *
     * @throws FactoryConfigurationError siempre en KajiLibrary; ver la nota de la clase
     */
    public static SAXParserFactory newDefaultInstance() {
        throw new FactoryConfigurationError(
            "KajiLibrary does not include a built-in XML implementation; set the system property "
                + PROPERTY + " or register a SAXParserFactory service");
    }

    /**
     * La fabrica configurada, buscandola en orden.
     *
     * @throws FactoryConfigurationError si no hay ninguna
     */
    public static SAXParserFactory newInstance() {
        String configured = null;
        try {
            configured = System.getProperty(PROPERTY);
        } catch (SecurityException e) {
            // Sin permiso para leerla: se sigue con los servicios.
        }
        if (configured != null && configured.length() > 0) {
            return newInstance(configured, null);
        }
        ServiceLoader<SAXParserFactory> loader = ServiceLoader.load(SAXParserFactory.class);
        Iterator<SAXParserFactory> it = loader.iterator();
        if (it.hasNext()) {
            return it.next();
        }
        return newDefaultInstance();
    }

    /**
     * Esa fabrica y ninguna otra.
     *
     * @param classLoader con el que se carga; null significa el del contexto o el de esta clase
     * @throws FactoryConfigurationError si no se puede construir
     */
    public static SAXParserFactory newInstance(String factoryClassName, ClassLoader classLoader) {
        if (factoryClassName == null) {
            throw new FactoryConfigurationError("factoryClassName cannot be null");
        }
        try {
            ClassLoader loader = classLoader;
            if (loader == null) {
                loader = Thread.currentThread().getContextClassLoader();
            }
            if (loader == null) {
                loader = SAXParserFactory.class.getClassLoader();
            }
            Class<?> found = Class.forName(factoryClassName, false, loader);
            return (SAXParserFactory) found.getConstructor(new Class<?>[0])
                .newInstance(new Object[0]);
        } catch (ClassCastException e) {
            throw new FactoryConfigurationError(
                e, factoryClassName + " is not a SAXParserFactory");
        } catch (Exception e) {
            throw new FactoryConfigurationError(e, "Provider " + factoryClassName + " not found");
        }
    }

    /**
     * Un analizador con la configuracion que tiene ahora la fabrica.
     *
     * @throws ParserConfigurationException si esta implementacion no puede dar lo que se pidio
     * @throws SAXException si el analizador de abajo falla al construirse
     */
    public abstract SAXParser newSAXParser() throws ParserConfigurationException, SAXException;

    /** Ver {@link DocumentBuilderFactory#setNamespaceAware}. */
    public void setNamespaceAware(boolean awareness) {
        this.namespaceAware = awareness;
    }

    /** Si los analizadores validan contra la DTD del documento. */
    public void setValidating(boolean validating) {
        this.validating = validating;
    }

    /** Ver {@link #setNamespaceAware}. */
    public boolean isNamespaceAware() {
        return this.namespaceAware;
    }

    /** Ver {@link #setValidating}. */
    public boolean isValidating() {
        return this.validating;
    }

    /**
     * Una bandera de SAX o de la implementacion.
     *
     * <p>Ver la nota de la clase sobre las tres excepciones.
     */
    public abstract void setFeature(String name, boolean value)
        throws ParserConfigurationException, SAXNotRecognizedException, SAXNotSupportedException;

    /** El valor de una bandera. */
    public abstract boolean getFeature(String name)
        throws ParserConfigurationException, SAXNotRecognizedException, SAXNotSupportedException;

    /**
     * El esquema con el que validan los analizadores que salgan de aca, o null.
     *
     * @throws UnsupportedOperationException por omision
     */
    public Schema getSchema() {
        throw new UnsupportedOperationException(
            "This parser does not support specification \"XML Schema\".");
    }

    /**
     * Pone el esquema.
     *
     * <p>No mezclarlo con {@link #setValidating}; ver
     * {@link DocumentBuilderFactory#setSchema}.
     *
     * @param schema null lo quita
     * @throws UnsupportedOperationException por omision
     */
    public void setSchema(Schema schema) {
        throw new UnsupportedOperationException(
            "This parser does not support specification \"XML Schema\".");
    }

    /**
     * Pide resolver XInclude.
     *
     * <p>Pedirlo en false no hace nada; ver {@link DocumentBuilderFactory#setXIncludeAware}.
     *
     * @throws UnsupportedOperationException al pedir true en una implementacion que no lo soporta
     */
    public void setXIncludeAware(boolean state) {
        if (state) {
            throw new UnsupportedOperationException(
                "This parser does not support specification \"XInclude\".");
        }
    }

    /**
     * Si resuelve XInclude.
     *
     * @throws UnsupportedOperationException por omision
     */
    public boolean isXIncludeAware() {
        throw new UnsupportedOperationException(
            "This parser does not support specification \"XInclude\".");
    }
}
