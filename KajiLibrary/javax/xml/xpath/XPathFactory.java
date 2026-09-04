package javax.xml.xpath;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * KajiLibrary's javax.xml.xpath.XPathFactory -- de donde salen los {@link XPath}.
 *
 * <p>Se pide por <b>modelo de objetos</b>: el URI de la representacion del documento sobre la que se
 * va a evaluar. La plataforma trae uno solo, {@link #DEFAULT_OBJECT_MODEL_URI}, que es DOM. La
 * indireccion existe porque XPath no depende de DOM en principio -- se puede evaluar sobre otras
 * representaciones-- y este es el punto donde eso se elige.
 *
 * <p>La propiedad de sistema que la configura no es un nombre fijo: es
 * {@link #DEFAULT_PROPERTY_NAME} <b>mas dos puntos y el URI del modelo</b>, asi que cada modelo se
 * configura por separado. Ese detalle no se adivina.
 *
 * <h2>Tres formas de fallar, y son distintas</h2>
 *
 * <ul>
 *   <li>{@link #newInstance(String)} con un modelo que nadie soporta lanza
 *       {@link XPathFactoryConfigurationException}, que es <b>comprobada</b>: no hay soporte para eso
 *       en particular y el programa puede probar otra cosa;
 *   <li>con null lanza {@link NullPointerException} y con la cadena vacia
 *       {@link IllegalArgumentException}: no son modelos, son argumentos mal formados;
 *   <li>{@link #newInstance()} --el que no toma modelo-- lanza una {@link RuntimeException} si no
 *       encuentra DOM, porque no tiene forma de declarar una comprobada.
 * </ul>
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>Esta biblioteca no trae un evaluador de XPath: hacerlo pide un parser de expresiones, un motor
 * de ejes y una implementacion de DOM viva, y ninguna de las tres esta. Sin una fabrica registrada,
 * {@link #newDefaultInstance} y {@link #newInstance()} lanzan, y {@link #newInstance(String)} lanza
 * la comprobada que ya declara. La busqueda por propiedad de sistema y por servicio esta
 * implementada de verdad, asi que registrar una implementacion alcanza.
 */
public abstract class XPathFactory {

    /** El prefijo de la propiedad de sistema; se le pega {@code ":"} y el URI del modelo. */
    public static final String DEFAULT_PROPERTY_NAME = "javax.xml.xpath.XPathFactory";

    /** El modelo DOM, que es el que trae la plataforma. */
    public static final String DEFAULT_OBJECT_MODEL_URI = "http://java.sun.com/jaxp/xpath/dom";

    /** Para las subclases. */
    protected XPathFactory() {
    }

    /**
     * La implementacion incluida en la plataforma.
     *
     * @throws RuntimeException siempre en KajiLibrary; ver la nota de la clase
     */
    public static XPathFactory newDefaultInstance() {
        throw new RuntimeException(
            "KajiLibrary does not include a built-in XPath implementation; set the system property "
                + DEFAULT_PROPERTY_NAME + ":" + DEFAULT_OBJECT_MODEL_URI
                + " or register an XPathFactory service");
    }

    /**
     * La fabrica para DOM.
     *
     * @throws RuntimeException si no hay ninguna
     */
    public static XPathFactory newInstance() {
        try {
            return newInstance(DEFAULT_OBJECT_MODEL_URI);
        } catch (XPathFactoryConfigurationException e) {
            // El metodo no declara comprobadas: se envuelve, que es lo que hace el JDK.
            throw new RuntimeException(
                "XPathFactory#newInstance() failed to create an XPathFactory for the default "
                    + "object model: " + DEFAULT_OBJECT_MODEL_URI, e);
        }
    }

    /**
     * La fabrica para ese modelo de objetos.
     *
     * <p>Busca en orden: la propiedad de sistema de ese modelo, los proveedores registrados como
     * servicio --quedandose con el primero que <b>diga que soporta</b> el modelo-- y la
     * implementacion de la plataforma.
     *
     * @throws NullPointerException si el URI es null
     * @throws IllegalArgumentException si es la cadena vacia
     * @throws XPathFactoryConfigurationException si nadie soporta ese modelo
     */
    public static XPathFactory newInstance(String uri) throws XPathFactoryConfigurationException {
        if (uri == null) {
            throw new NullPointerException(
                "XPathFactory#newInstance(String uri) cannot be called with uri == null");
        }
        if (uri.length() == 0) {
            throw new IllegalArgumentException(
                "XPathFactory#newInstance(String uri) cannot be called with uri == \"\"");
        }
        String configured = null;
        try {
            configured = System.getProperty(DEFAULT_PROPERTY_NAME + ":" + uri);
        } catch (SecurityException e) {
            // Sin permiso para leerla: se sigue con los servicios.
        }
        if (configured != null && configured.length() > 0) {
            return newInstance(uri, configured, null);
        }
        ServiceLoader<XPathFactory> loader = ServiceLoader.load(XPathFactory.class);
        Iterator<XPathFactory> it = loader.iterator();
        while (it.hasNext()) {
            XPathFactory candidate = it.next();
            if (candidate.isObjectModelSupported(uri)) {
                return candidate;
            }
        }
        throw new XPathFactoryConfigurationException(
            "No XPathFactory implementation found for the object model: " + uri);
    }

    /**
     * Esa clase y ninguna otra, para ese modelo.
     *
     * @param classLoader con el que se carga; null significa el del contexto o el de esta clase
     * @throws XPathFactoryConfigurationException si no se puede construir, o si la construida no
     *     soporta ese modelo
     */
    public static XPathFactory newInstance(String uri, String factoryClassName,
                                           ClassLoader classLoader)
        throws XPathFactoryConfigurationException {
        if (uri == null) {
            throw new NullPointerException(
                "XPathFactory#newInstance(String uri) cannot be called with uri == null");
        }
        if (uri.length() == 0) {
            throw new IllegalArgumentException(
                "XPathFactory#newInstance(String uri) cannot be called with uri == \"\"");
        }
        if (factoryClassName == null) {
            throw new XPathFactoryConfigurationException("factoryClassName cannot be null");
        }
        XPathFactory made;
        try {
            ClassLoader loader = classLoader;
            if (loader == null) {
                loader = Thread.currentThread().getContextClassLoader();
            }
            if (loader == null) {
                loader = XPathFactory.class.getClassLoader();
            }
            Class<?> found = Class.forName(factoryClassName, false, loader);
            made = (XPathFactory) found.getConstructor(new Class<?>[0]).newInstance(new Object[0]);
        } catch (Exception e) {
            throw new XPathFactoryConfigurationException(
                "Provider " + factoryClassName + " could not be instantiated: " + e);
        }
        if (!made.isObjectModelSupported(uri)) {
            throw new XPathFactoryConfigurationException(
                factoryClassName + " does not support the object model " + uri);
        }
        return made;
    }

    /** Si esta fabrica trabaja sobre ese modelo de objetos. */
    public abstract boolean isObjectModelSupported(String objectModel);

    /**
     * Cambia una bandera.
     *
     * <p>La que toda implementacion tiene que reconocer es
     * {@code javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING}, que entre otras cosas apaga las
     * funciones de extension.
     */
    public abstract void setFeature(String name, boolean value)
        throws XPathFactoryConfigurationException;

    /** El valor de una bandera. */
    public abstract boolean getFeature(String name) throws XPathFactoryConfigurationException;

    /** El resolvedor de variables que llevaran los {@link XPath} que salgan de aca. */
    public abstract void setXPathVariableResolver(XPathVariableResolver resolver);

    /** Idem para las funciones. */
    public abstract void setXPathFunctionResolver(XPathFunctionResolver resolver);

    /** Un evaluador con la configuracion que tiene ahora la fabrica. */
    public abstract XPath newXPath();

    /**
     * Una propiedad de la implementacion.
     *
     * @throws UnsupportedOperationException por omision: llego despues que la clase, y una
     *     implementacion vieja no la conoce
     */
    public void setProperty(String name, String value) {
        throw new UnsupportedOperationException(
            "This XPathFactory does not support the setProperty method.");
    }

    /**
     * El valor de una propiedad.
     *
     * @throws UnsupportedOperationException por omision
     */
    public String getProperty(String name) {
        throw new UnsupportedOperationException(
            "This XPathFactory does not support the getProperty method.");
    }
}
