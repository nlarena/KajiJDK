package javax.xml.parsers;

import java.util.Iterator;
import java.util.ServiceLoader;
import javax.xml.validation.Schema;

/**
 * KajiLibrary's javax.xml.parsers.DocumentBuilderFactory -- de donde salen los {@link DocumentBuilder}.
 *
 * <p>Es una fabrica y no un constructor porque la implementacion de XML es reemplazable: quien
 * escribe el programa pide "un analizador DOM" y quien arma el despliegue decide cual. Los seis
 * {@code newInstance} son las variantes de esa eleccion.
 *
 * <h2>Las tres formas de elegir</h2>
 *
 * <ul>
 *   <li>{@link #newInstance()} <b>busca</b>: primero la propiedad de sistema
 *       {@code javax.xml.parsers.DocumentBuilderFactory}, despues los proveedores registrados como
 *       servicio, y si no hay nada, la implementacion incluida en la plataforma;
 *   <li>{@link #newInstance(String, ClassLoader)} no busca: usa esa clase o falla. Sirve cuando un
 *       programa necesita <b>una</b> implementacion concreta y no quiere que una propiedad de
 *       sistema se la cambie por atras;
 *   <li>{@link #newDefaultInstance()} saltea la busqueda al reves: va derecho a la de la plataforma,
 *       ignorando propiedades y servicios.
 * </ul>
 *
 * <p>Las variantes {@code NS} son iguales pero devuelven la fabrica ya puesta en
 * {@code namespaceAware}. Existen porque ese valor por omision es <b>false</b> por razones
 * historicas, es casi siempre el equivocado, y olvidarse de cambiarlo da un sintoma confuso: los
 * elementos aparecen con el prefijo pegado al nombre y las busquedas por espacio de nombres no
 * encuentran nada.
 *
 * <h2>Las banderas y sus valores por omision</h2>
 *
 * <p>Todas arrancan en false salvo {@link #isExpandEntityReferences}, que arranca en true. Esa es la
 * que conviene mirar: con entidades expandidas, un documento que declara una entidad externa hace
 * que el analizador la vaya a buscar, y de ahi salen tanto la lectura de archivos locales como los
 * pedidos de red que el programa nunca pidio.
 *
 * <h2>Validar por esquema o por DTD</h2>
 *
 * <p>{@link #setSchema} y {@link #setValidating} son dos mecanismos <b>distintos</b> y no hay que
 * mezclarlos: el segundo valida contra la DTD que el documento declara, el primero contra un esquema
 * que elige la aplicacion. Poner los dos es un error de configuracion, y la diferencia de fondo es
 * quien manda: con DTD, el documento; con esquema, quien lo lee.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>{@link #newDefaultInstance} lanza {@link FactoryConfigurationError}, porque KajiLibrary no trae
 * una implementacion de XML incluida. Es la salida que ese metodo ya declara para el caso "no hay
 * fabrica", y por eso {@link #newInstance()} funciona igual que en el JDK mientras alguien registre
 * una: solo falla cuando no hay ninguna, que es la verdad.
 */
public abstract class DocumentBuilderFactory {

    /** La propiedad de sistema que nombra la fabrica. */
    private static final String PROPERTY = "javax.xml.parsers.DocumentBuilderFactory";

    private boolean namespaceAware = false;
    private boolean validating = false;
    private boolean whitespace = false;
    private boolean expandEntityRef = true;
    private boolean ignoreComments = false;
    private boolean coalescing = false;

    /** Para las subclases. */
    protected DocumentBuilderFactory() {
    }

    /**
     * La fabrica incluida en la plataforma, ya puesta en {@code namespaceAware}.
     *
     * @throws FactoryConfigurationError siempre en KajiLibrary; ver la nota de la clase
     */
    public static DocumentBuilderFactory newDefaultNSInstance() {
        DocumentBuilderFactory factory = newDefaultInstance();
        factory.setNamespaceAware(true);
        return factory;
    }

    /** Como {@link #newInstance()}, ya puesta en {@code namespaceAware}. */
    public static DocumentBuilderFactory newNSInstance() {
        DocumentBuilderFactory factory = newInstance();
        factory.setNamespaceAware(true);
        return factory;
    }

    /** Como {@link #newInstance(String, ClassLoader)}, ya puesta en {@code namespaceAware}. */
    public static DocumentBuilderFactory newNSInstance(String factoryClassName,
                                                       ClassLoader classLoader) {
        DocumentBuilderFactory factory = newInstance(factoryClassName, classLoader);
        factory.setNamespaceAware(true);
        return factory;
    }

    /**
     * La fabrica incluida en la plataforma, sin mirar propiedades ni servicios.
     *
     * @throws FactoryConfigurationError siempre en KajiLibrary; ver la nota de la clase
     */
    public static DocumentBuilderFactory newDefaultInstance() {
        throw new FactoryConfigurationError(
            "KajiLibrary does not include a built-in XML implementation; set the system property "
                + PROPERTY + " or register a DocumentBuilderFactory service");
    }

    /**
     * La fabrica configurada, buscandola en orden.
     *
     * <p>Ver los tres pasos en la nota de la clase.
     *
     * @throws FactoryConfigurationError si no hay ninguna
     */
    public static DocumentBuilderFactory newInstance() {
        String configured = null;
        try {
            configured = System.getProperty(PROPERTY);
        } catch (SecurityException e) {
            // Sin permiso para leerla: se sigue con los servicios, que es lo mismo que hacer nada.
        }
        if (configured != null && configured.length() > 0) {
            return newInstance(configured, null);
        }
        ServiceLoader<DocumentBuilderFactory> loader =
            ServiceLoader.load(DocumentBuilderFactory.class);
        Iterator<DocumentBuilderFactory> it = loader.iterator();
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
    public static DocumentBuilderFactory newInstance(String factoryClassName,
                                                     ClassLoader classLoader) {
        if (factoryClassName == null) {
            throw new FactoryConfigurationError("factoryClassName cannot be null");
        }
        try {
            ClassLoader loader = classLoader;
            if (loader == null) {
                loader = Thread.currentThread().getContextClassLoader();
            }
            if (loader == null) {
                loader = DocumentBuilderFactory.class.getClassLoader();
            }
            Class<?> found = Class.forName(factoryClassName, false, loader);
            return (DocumentBuilderFactory) found.getConstructor(new Class<?>[0])
                .newInstance(new Object[0]);
        } catch (ClassCastException e) {
            throw new FactoryConfigurationError(
                e, factoryClassName + " is not a DocumentBuilderFactory");
        } catch (Exception e) {
            throw new FactoryConfigurationError(e, "Provider " + factoryClassName + " not found");
        }
    }

    /**
     * Un analizador con la configuracion que tiene ahora la fabrica.
     *
     * <p>Los cambios posteriores a la fabrica no lo afectan: lo que se lee al construirlo queda
     * fijado.
     *
     * @throws ParserConfigurationException si esta implementacion no puede dar lo que se pidio
     */
    public abstract DocumentBuilder newDocumentBuilder() throws ParserConfigurationException;

    /** Ver la nota de la clase sobre por que casi siempre hay que ponerla en true. */
    public void setNamespaceAware(boolean awareness) {
        this.namespaceAware = awareness;
    }

    /** Si los analizadores validan contra la DTD del documento. */
    public void setValidating(boolean validating) {
        this.validating = validating;
    }

    /**
     * Si se descarta el espacio en blanco que la DTD declara como relleno.
     *
     * <p>Solo hace algo con validacion prendida: sin DTD no hay forma de saber cual espacio es
     * significativo y cual es sangria.
     */
    public void setIgnoringElementContentWhitespace(boolean whitespace) {
        this.whitespace = whitespace;
    }

    /** Ver la nota de la clase sobre entidades externas. */
    public void setExpandEntityReferences(boolean expandEntityRef) {
        this.expandEntityRef = expandEntityRef;
    }

    /** Si los comentarios no llegan al arbol. */
    public void setIgnoringComments(boolean ignoreComments) {
        this.ignoreComments = ignoreComments;
    }

    /**
     * Si las secciones CDATA se funden con el texto que las rodea.
     *
     * <p>Conviene: sin esto, un mismo texto puede llegar partido en varios nodos segun donde el
     * autor haya abierto un CDATA, y hay que juntarlo a mano en cada lectura.
     */
    public void setCoalescing(boolean coalescing) {
        this.coalescing = coalescing;
    }

    /** Ver {@link #setNamespaceAware}. */
    public boolean isNamespaceAware() {
        return this.namespaceAware;
    }

    /** Ver {@link #setValidating}. */
    public boolean isValidating() {
        return this.validating;
    }

    /** Ver {@link #setIgnoringElementContentWhitespace}. */
    public boolean isIgnoringElementContentWhitespace() {
        return this.whitespace;
    }

    /** Ver {@link #setExpandEntityReferences}. Arranca en <b>true</b>. */
    public boolean isExpandEntityReferences() {
        return this.expandEntityRef;
    }

    /** Ver {@link #setIgnoringComments}. */
    public boolean isIgnoringComments() {
        return this.ignoreComments;
    }

    /** Ver {@link #setCoalescing}. */
    public boolean isCoalescing() {
        return this.coalescing;
    }

    /**
     * Un atributo especifico de la implementacion.
     *
     * @throws IllegalArgumentException si no lo reconoce
     */
    public abstract void setAttribute(String name, Object value) throws IllegalArgumentException;

    /** El valor de un atributo especifico de la implementacion. */
    public abstract Object getAttribute(String name) throws IllegalArgumentException;

    /**
     * Una bandera especifica de la implementacion.
     *
     * <p>La unica que toda implementacion tiene que reconocer es
     * {@code javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING}.
     *
     * @throws ParserConfigurationException si no la reconoce o no la puede dar
     */
    public abstract void setFeature(String name, boolean value)
        throws ParserConfigurationException;

    /** El valor de una bandera. */
    public abstract boolean getFeature(String name) throws ParserConfigurationException;

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
     * Pone el esquema. Ver la nota de la clase sobre no mezclarlo con {@link #setValidating}.
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
     * <p>Pedirlo en false no hace nada, porque no pedirlo es el estado por omision. Pedirlo en true
     * lanza si esta implementacion no lo sabe hacer -- que es lo correcto: seguir en silencio
     * dejaria un documento a medio armar sin que nadie se entere.
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
     * @throws UnsupportedOperationException por omision; ver {@link DocumentBuilder#isXIncludeAware}
     */
    public boolean isXIncludeAware() {
        throw new UnsupportedOperationException(
            "This parser does not support specification \"XInclude\".");
    }
}
