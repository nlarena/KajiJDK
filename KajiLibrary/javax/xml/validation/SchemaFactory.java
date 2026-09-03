package javax.xml.validation;

import java.io.File;
import java.net.URL;
import java.util.Iterator;
import java.util.ServiceLoader;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

/**
 * KajiLibrary's javax.xml.validation.SchemaFactory -- lee esquemas y los compila.
 *
 * <p>Una fabrica <b>por lenguaje de esquema</b>: se pide con el URI del lenguaje --XML Schema, RELAX
 * NG-- y devuelve una implementacion que lo entienda. Es la diferencia con
 * {@code DocumentBuilderFactory}, que tiene un solo {@code newInstance} sin argumento: ahi hay un
 * solo XML posible y aca hay varios lenguajes de esquema en competencia.
 *
 * <h2>Como se elige</h2>
 *
 * <p>{@link #newInstance(String)} busca en orden: la propiedad de sistema
 * {@code javax.xml.validation.SchemaFactory:<lenguaje>} --con el URI del lenguaje pegado al nombre,
 * que es lo que permite configurar cada uno por separado--, despues los proveedores registrados como
 * servicio, quedandose con el primero que <b>diga que soporta</b> ese lenguaje, y por ultimo la
 * implementacion incluida en la plataforma.
 *
 * <p>Que no haya ninguna es un {@link IllegalArgumentException} y no un error de configuracion. Tiene
 * sentido: el argumento fue un lenguaje que nadie sabe leer, y eso es un problema del pedido.
 *
 * <h2>Los cuatro {@code newSchema}</h2>
 *
 * <p>Los tres con argumento arman un {@code Source} y llaman al que recibe un arreglo. El del arreglo
 * es el interesante: compila <b>varios documentos como un solo esquema</b>, que es lo que hace falta
 * cuando un esquema esta partido en archivos que se importan entre si. No es lo mismo que compilar
 * cada uno por separado -- las referencias cruzadas solo cierran si estan todos juntos.
 *
 * <p>{@link #newSchema()} sin argumentos es el mas raro y el mas util a veces: devuelve un esquema
 * "especial" que valida cada documento contra <b>lo que el documento mismo declare</b> con
 * {@code xsi:schemaLocation}. Es comodo y es exactamente lo que no hay que hacer si el documento
 * viene de afuera: deja que el documento elija sus propias reglas.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>{@link #newDefaultInstance} lanza {@link SchemaFactoryConfigurationError} porque esta biblioteca
 * no trae una implementacion de esquemas incluida, y {@link #newInstance(String)} termina en
 * {@link IllegalArgumentException} mientras no haya ninguna registrada. Las dos son salidas que esos
 * metodos ya declaran; con un proveedor registrado, funcionan como en el JDK.
 */
public abstract class SchemaFactory {

    /** El prefijo de la propiedad de sistema; se le pega el URI del lenguaje. */
    private static final String PROPERTY_PREFIX = "javax.xml.validation.SchemaFactory:";

    /** Para las subclases. */
    protected SchemaFactory() {
    }

    /**
     * La implementacion incluida en la plataforma.
     *
     * @throws SchemaFactoryConfigurationError siempre en KajiLibrary; ver la nota de la clase
     */
    public static SchemaFactory newDefaultInstance() {
        throw new SchemaFactoryConfigurationError(
            "KajiLibrary does not include a built-in schema implementation; set the system property "
                + PROPERTY_PREFIX + "<schemaLanguage> or register a SchemaFactory service");
    }

    /**
     * La fabrica que entiende ese lenguaje.
     *
     * <p>Ver el orden de busqueda en la nota de la clase.
     *
     * @throws IllegalArgumentException si ninguna lo soporta
     * @throws NullPointerException si el lenguaje es null
     */
    public static SchemaFactory newInstance(String schemaLanguage) {
        if (schemaLanguage == null) {
            throw new NullPointerException("schemaLanguage cannot be null");
        }
        String configured = null;
        try {
            configured = System.getProperty(PROPERTY_PREFIX + schemaLanguage);
        } catch (SecurityException e) {
            // Sin permiso para leerla: se sigue con los servicios.
        }
        if (configured != null && configured.length() > 0) {
            return newInstance(schemaLanguage, configured, null);
        }
        ServiceLoader<SchemaFactory> loader = ServiceLoader.load(SchemaFactory.class);
        Iterator<SchemaFactory> it = loader.iterator();
        while (it.hasNext()) {
            SchemaFactory candidate = it.next();
            // Se le pregunta a cada uno: un proveedor registrado no tiene por que saber todos los
            // lenguajes, y quedarse con el primero a ciegas daria una fabrica que no sirve.
            if (candidate.isSchemaLanguageSupported(schemaLanguage)) {
                return candidate;
            }
        }
        throw new IllegalArgumentException(
            "No SchemaFactory that implements the schema language specified by: " + schemaLanguage
                + " could be loaded");
    }

    /**
     * Esa clase y ninguna otra, para ese lenguaje.
     *
     * @param classLoader con el que se carga; null significa el del contexto o el de esta clase
     * @throws IllegalArgumentException si no se puede construir, o si la construida no soporta ese
     *     lenguaje
     */
    public static SchemaFactory newInstance(String schemaLanguage, String factoryClassName,
                                            ClassLoader classLoader) {
        if (schemaLanguage == null) {
            throw new NullPointerException("schemaLanguage cannot be null");
        }
        if (factoryClassName == null) {
            throw new IllegalArgumentException("factoryClassName cannot be null");
        }
        SchemaFactory made;
        try {
            ClassLoader loader = classLoader;
            if (loader == null) {
                loader = Thread.currentThread().getContextClassLoader();
            }
            if (loader == null) {
                loader = SchemaFactory.class.getClassLoader();
            }
            Class<?> found = Class.forName(factoryClassName, false, loader);
            made = (SchemaFactory) found.getConstructor(new Class<?>[0]).newInstance(new Object[0]);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                "Provider " + factoryClassName + " could not be instantiated: " + e);
        }
        if (!made.isSchemaLanguageSupported(schemaLanguage)) {
            throw new IllegalArgumentException(
                factoryClassName + " does not support the schema language " + schemaLanguage);
        }
        return made;
    }

    /**
     * Si esta fabrica entiende ese lenguaje.
     *
     * @throws NullPointerException si es null
     * @throws IllegalArgumentException si es la cadena vacia
     */
    public abstract boolean isSchemaLanguageSupported(String schemaLanguage);

    /**
     * El valor de una bandera.
     *
     * <p>Por omision no conoce ninguna. La que toda implementacion tiene que reconocer es
     * {@code javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING}, que es la que apaga el acceso a
     * recursos externos.
     *
     * @throws SAXNotRecognizedException si no conoce ese nombre
     */
    public boolean getFeature(String name)
        throws SAXNotRecognizedException, SAXNotSupportedException {
        if (name == null) {
            throw new NullPointerException("the name parameter is null");
        }
        throw new SAXNotRecognizedException(name);
    }

    /**
     * Cambia una bandera.
     *
     * @throws SAXNotRecognizedException si no conoce ese nombre
     */
    public void setFeature(String name, boolean value)
        throws SAXNotRecognizedException, SAXNotSupportedException {
        if (name == null) {
            throw new NullPointerException("the name parameter is null");
        }
        throw new SAXNotRecognizedException(name);
    }

    /**
     * Cambia una propiedad.
     *
     * @throws SAXNotRecognizedException si no conoce ese nombre
     */
    public void setProperty(String name, Object object)
        throws SAXNotRecognizedException, SAXNotSupportedException {
        if (name == null) {
            throw new NullPointerException("the name parameter is null");
        }
        throw new SAXNotRecognizedException(name);
    }

    /**
     * El valor de una propiedad.
     *
     * @throws SAXNotRecognizedException si no conoce ese nombre
     */
    public Object getProperty(String name)
        throws SAXNotRecognizedException, SAXNotSupportedException {
        if (name == null) {
            throw new NullPointerException("the name parameter is null");
        }
        throw new SAXNotRecognizedException(name);
    }

    /** Quien recibe los errores al <b>compilar el esquema</b>, no al validar documentos. */
    public abstract void setErrorHandler(ErrorHandler errorHandler);

    /** Ver {@link #setErrorHandler}. */
    public abstract ErrorHandler getErrorHandler();

    /** Quien resuelve lo que el esquema importe o incluya. */
    public abstract void setResourceResolver(LSResourceResolver resourceResolver);

    /** Ver {@link #setResourceResolver}. */
    public abstract LSResourceResolver getResourceResolver();

    /**
     * Compila un esquema de una fuente.
     *
     * @throws SAXException si el esquema esta mal
     * @throws NullPointerException si la fuente es null
     */
    public Schema newSchema(Source schema) throws SAXException {
        if (schema == null) {
            throw new NullPointerException("schema cannot be null");
        }
        return newSchema(new Source[] {schema});
    }

    /**
     * Idem, de un archivo.
     *
     * @throws NullPointerException si el archivo es null
     */
    public Schema newSchema(File schema) throws SAXException {
        if (schema == null) {
            throw new NullPointerException("schema cannot be null");
        }
        return newSchema(new StreamSource(schema));
    }

    /**
     * Idem, de un URL.
     *
     * @throws NullPointerException si el URL es null
     */
    public Schema newSchema(URL schema) throws SAXException {
        if (schema == null) {
            throw new NullPointerException("schema cannot be null");
        }
        return newSchema(new StreamSource(schema.toExternalForm()));
    }

    /**
     * Compila <b>varias</b> fuentes como un solo esquema.
     *
     * <p>Ver la nota de la clase sobre por que no es lo mismo que compilarlas por separado.
     */
    public abstract Schema newSchema(Source[] schemas) throws SAXException;

    /**
     * El esquema que valida cada documento contra lo que el documento declare.
     *
     * <p>Comodo y peligroso; ver la nota de la clase.
     */
    public abstract Schema newSchema() throws SAXException;
}
