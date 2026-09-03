package javax.xml.validation;

import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

/**
 * KajiLibrary's javax.xml.validation.ValidatorHandler -- valida mientras se lee.
 *
 * <p>Es un {@link ContentHandler} que valida lo que le llega y se lo pasa a otro
 * {@code ContentHandler}. Se enchufa en el medio de una cadena SAX, y de ahi vienen sus dos
 * ventajas sobre {@link Validator}: no necesita el documento entero en memoria, y quien recibe la
 * salida ya la recibe validada.
 *
 * <h2>Lo que sale no es lo mismo que entro</h2>
 *
 * <p>Al manejador de mas abajo le llegan los atributos con los <b>valores por omision</b> que puso el
 * esquema, no solo los que estaban escritos. Es lo que se quiere --construir el arbol ya completo--
 * y es la razon por la que conviene poner el validador antes y no despues del que construye.
 *
 * <p>{@link #getTypeInfoProvider} es lo que hace que esto valga la pena de verdad: deja saber, para
 * cada elemento y atributo que pasa, de que tipo era segun el esquema. Ver ahi las reglas de cuando
 * se puede preguntar.
 *
 * <h2>Un detalle que muerde</h2>
 *
 * <p>Esta clase implementa {@code ContentHandler} y ademas tiene {@link #setContentHandler}. No es lo
 * mismo: lo que implementa es la <b>entrada</b> --lo que le manda el lector-- y lo que se le pone con
 * el setter es la <b>salida</b>. Pasarse a si mismo como salida arma un lazo infinito.
 */
public abstract class ValidatorHandler implements ContentHandler {

    /** Para las subclases. */
    protected ValidatorHandler() {
    }

    /** Adonde va lo validado. Ver la nota de la clase: no es la entrada. */
    public abstract void setContentHandler(ContentHandler receiver);

    /** Ver {@link #setContentHandler}. */
    public abstract ContentHandler getContentHandler();

    /** Quien recibe los errores y advertencias de validacion. */
    public abstract void setErrorHandler(ErrorHandler errorHandler);

    /** Ver {@link #setErrorHandler}. */
    public abstract ErrorHandler getErrorHandler();

    /** Quien resuelve los recursos externos que el esquema nombre. */
    public abstract void setResourceResolver(LSResourceResolver resourceResolver);

    /** Ver {@link #setResourceResolver}. */
    public abstract LSResourceResolver getResourceResolver();

    /** Los tipos de lo que esta pasando ahora mismo; ver {@link TypeInfoProvider}. */
    public abstract TypeInfoProvider getTypeInfoProvider();

    /**
     * El valor de una bandera.
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
