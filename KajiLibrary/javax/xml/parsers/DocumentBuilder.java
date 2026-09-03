package javax.xml.parsers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.validation.Schema;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * KajiLibrary's javax.xml.parsers.DocumentBuilder -- lee un XML y devuelve un arbol.
 *
 * <p>Es la cara DOM del analisis: se lee todo el documento y queda en memoria como un
 * {@link Document} que se puede recorrer en cualquier direccion. La otra cara es {@link SAXParser},
 * que avisa por evento y no guarda nada. La eleccion entre las dos no es de gusto: DOM necesita
 * varias veces el tamaño del archivo en memoria, asi que para un documento grande que solo se
 * recorre una vez, SAX es la unica opcion.
 *
 * <h2>Un solo metodo abstracto y seis atajos</h2>
 *
 * <p>Los {@code parse} que reciben flujo, archivo o URI arman un {@link InputSource} y llaman al
 * abstracto. Vale la pena mirar que hacen con el <b>identificador de sistema</b>: es lo que despues
 * permite resolver una referencia relativa dentro del documento, y por eso {@code parse(File)} lo
 * pone a partir del camino absoluto y no del que se paso. Un XML leido de un flujo sin identificador
 * no puede resolver nada relativo, y ese es el motivo de la sobrecarga que recibe uno aparte.
 *
 * <h2>Los defaults que lanzan</h2>
 *
 * <p>{@link #reset} y {@link #isXIncludeAware} tienen cuerpo y lanzan
 * {@link UnsupportedOperationException}. Es deliberado y es lo que hace el JDK: llegaron despues de
 * la version 1 de la clase, y una implementacion vieja que no las conoce no puede contestarlas.
 * Devolver false en {@code isXIncludeAware} seria peor -- afirmaria algo que nadie verifico.
 */
public abstract class DocumentBuilder {

    /** Para las subclases. */
    protected DocumentBuilder() {
    }

    /**
     * Deja el analizador como recien creado.
     *
     * @throws UnsupportedOperationException por omision; ver la nota de la clase
     */
    public void reset() {
        throw new UnsupportedOperationException(
            "This DocumentBuilder, \"" + this.getClass().getName()
                + "\", does not support the reset functionality.");
    }

    /**
     * Lee de un flujo, sin identificador de sistema.
     *
     * <p>Un documento leido asi no puede resolver referencias relativas; ver la nota de la clase.
     *
     * @throws IllegalArgumentException si el flujo es null
     */
    public Document parse(InputStream is) throws SAXException, IOException {
        if (is == null) {
            throw new IllegalArgumentException("InputStream cannot be null");
        }
        return parse(new InputSource(is));
    }

    /**
     * Lee de un flujo, diciendo desde donde vino.
     *
     * @param systemId contra el que se resuelven las referencias relativas
     * @throws IllegalArgumentException si el flujo es null
     */
    public Document parse(InputStream is, String systemId) throws SAXException, IOException {
        if (is == null) {
            throw new IllegalArgumentException("InputStream cannot be null");
        }
        InputSource in = new InputSource(is);
        in.setSystemId(systemId);
        return parse(in);
    }

    /**
     * Lee de un URI.
     *
     * @throws IllegalArgumentException si el URI es null
     */
    public Document parse(String uri) throws SAXException, IOException {
        if (uri == null) {
            throw new IllegalArgumentException("URI cannot be null");
        }
        return parse(new InputSource(uri));
    }

    /**
     * Lee de un archivo.
     *
     * <p>El identificador de sistema sale del <b>camino absoluto</b>, no del que se paso: si no, un
     * documento abierto con un camino relativo no podria resolver los suyos.
     *
     * @throws IllegalArgumentException si el archivo es null
     */
    public Document parse(File f) throws SAXException, IOException {
        if (f == null) {
            throw new IllegalArgumentException("File cannot be null");
        }
        return parse(new InputSource(f.toURI().toString()));
    }

    /** El unico que hay que escribir: todos los demas terminan aca. */
    public abstract Document parse(InputSource is) throws SAXException, IOException;

    /** Si distingue espacios de nombres. */
    public abstract boolean isNamespaceAware();

    /** Si valida contra la DTD del documento. */
    public abstract boolean isValidating();

    /**
     * Quien resuelve las entidades externas.
     *
     * <p>Ponerle uno que las rechace es la defensa contra XXE, que es el ataque clasico de este API:
     * un documento que declara una entidad apuntando a un archivo local y lo hace aparecer en la
     * salida.
     */
    public abstract void setEntityResolver(EntityResolver er);

    /** Quien decide que hacer con los errores; sin uno, van a la salida de error. */
    public abstract void setErrorHandler(ErrorHandler eh);

    /** Un documento vacio, para construir uno a mano. */
    public abstract Document newDocument();

    /** La implementacion DOM detras de este analizador. */
    public abstract DOMImplementation getDOMImplementation();

    /**
     * El esquema contra el que valida, o null.
     *
     * <p>Lo pone la fabrica con {@code DocumentBuilderFactory.setSchema}, no se pone aca: un
     * analizador ya construido no puede cambiar de esquema, porque el esquema decide como se
     * construye.
     *
     * @throws UnsupportedOperationException por omision; ver la nota de la clase
     */
    public Schema getSchema() {
        throw new UnsupportedOperationException(
            "This parser does not support specification \"XML Schema\".");
    }

    /**
     * Si resuelve XInclude.
     *
     * @throws UnsupportedOperationException por omision; ver la nota de la clase
     */
    public boolean isXIncludeAware() {
        throw new UnsupportedOperationException(
            "This parser does not support specification \"XInclude\".");
    }
}
