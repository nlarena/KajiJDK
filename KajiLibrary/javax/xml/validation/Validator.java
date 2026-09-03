package javax.xml.validation;

import java.io.IOException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

/**
 * KajiLibrary's javax.xml.validation.Validator -- valida un documento contra un esquema.
 *
 * <p>Se obtiene de {@link Schema#newValidator} y <b>no</b> es seguro entre hilos. Ver la nota de
 * {@link Schema} sobre que conviene guardar y que conviene fabricar.
 *
 * <h2>El segundo argumento no es la salida del error</h2>
 *
 * <p>{@link #validate(Source, Result)} confunde la primera vez: el {@code Result} no es adonde van
 * los errores --eso es el {@link ErrorHandler}-- sino <b>el mismo documento, aumentado</b>. Validar
 * con XML Schema agrega informacion que no estaba en el original: los valores por omision de los
 * atributos que faltaban, y el tipo de cada elemento. Ese resultado es lo que se llama el conjunto
 * de informacion post-validacion, y sin este parametro se perderia.
 *
 * <h2>Sin manejador de errores no se entera nadie</h2>
 *
 * <p>Sin un {@link ErrorHandler}, un error de validacion se lanza como {@link SAXException} y corta
 * ahi. Con uno, se reportan todos y el que decide si seguir es el manejador. Para revisar un
 * documento eso es la diferencia entre saber cual es el primer problema y saber cuales son todos.
 *
 * <p>La otra mitad es que un {@code warning} sin manejador se <b>descarta en silencio</b>. Poner uno
 * que al menos registre es lo minimo razonable.
 */
public abstract class Validator {

    /** Para las subclases. */
    protected Validator() {
    }

    /**
     * Deja el validador como recien fabricado.
     *
     * <p>Es abstracto y no tiene default, al reves que en {@code DocumentBuilder}: aca reusar es lo
     * normal y una implementacion tiene que poder limpiarse.
     */
    public abstract void reset();

    /**
     * Valida y descarta el resultado aumentado.
     *
     * @throws SAXException si el documento no valida y no hay manejador de errores que lo absorba
     */
    public void validate(Source source) throws SAXException, IOException {
        validate(source, null);
    }

    /**
     * Valida y deja el resultado aumentado en {@code result}.
     *
     * @param result adonde va el documento con los valores por omision y los tipos; null para
     *     descartarlo. Ver la nota de la clase: no es adonde van los errores
     */
    public abstract void validate(Source source, Result result) throws SAXException, IOException;

    /** Quien recibe los errores y advertencias. Ver la nota de la clase. */
    public abstract void setErrorHandler(ErrorHandler errorHandler);

    /** Ver {@link #setErrorHandler}. */
    public abstract ErrorHandler getErrorHandler();

    /** Quien resuelve los recursos externos que el esquema o el documento nombren. */
    public abstract void setResourceResolver(LSResourceResolver resourceResolver);

    /** Ver {@link #setResourceResolver}. */
    public abstract LSResourceResolver getResourceResolver();

    /**
     * El valor de una bandera.
     *
     * <p>Por omision no conoce ninguna. La que toda implementacion tiene que reconocer es
     * {@code javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING}.
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
}
