package javax.xml.parsers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.validation.Schema;
import org.xml.sax.HandlerBase;
import org.xml.sax.InputSource;
import org.xml.sax.Parser;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * KajiLibrary's javax.xml.parsers.SAXParser -- lee un XML avisando por evento.
 *
 * <p>La contracara de {@link DocumentBuilder}: no arma nada en memoria, va llamando a un manejador a
 * medida que lee. Por eso puede leer un archivo mas grande que la memoria disponible, y por eso
 * quien lo usa tiene que quedarse con lo que le interesa mientras pasa -- lo que no se guarda, se
 * perdio.
 *
 * <h2>Las dos familias de {@code parse}</h2>
 *
 * <p>Las que reciben {@link HandlerBase} son de SAX 1 y estan obsoletas; las que reciben
 * {@link DefaultHandler} son de SAX 2 y son las que hay que usar. No es solo un cambio de nombre: en
 * SAX 1 los elementos no tienen espacio de nombres, asi que un documento con prefijos llega con los
 * prefijos pegados al nombre y hay que separarlos a mano. Las dos familias siguen porque
 * {@code javax.xml.parsers} salio cuando SAX 1 todavia se usaba.
 *
 * <p>Las dos hacen lo mismo con el manejador: se lo enchufan a los <b>cuatro</b> puntos --contenido,
 * entidades, errores y DTD-- porque {@link DefaultHandler} implementa las cuatro interfaces. De ahi
 * viene la comodidad de la clase: una sola instancia y solo se redefinen los metodos que importan.
 */
public abstract class SAXParser {

    /** Para las subclases. */
    protected SAXParser() {
    }

    /**
     * Deja el analizador como recien creado.
     *
     * @throws UnsupportedOperationException por omision, igual que en {@link DocumentBuilder#reset}
     */
    public void reset() {
        throw new UnsupportedOperationException(
            "This SAXParser, \"" + this.getClass().getName()
                + "\", does not support the reset functionality.");
    }

    /**
     * Lee de un flujo con un manejador de SAX 1.
     *
     * @throws IllegalArgumentException si el flujo es null
     * @deprecated ver la nota de la clase; usar la version con {@link DefaultHandler}
     */
    @Deprecated
    public void parse(InputStream is, HandlerBase hb) throws SAXException, IOException {
        if (is == null) {
            throw new IllegalArgumentException("InputStream cannot be null");
        }
        parse(new InputSource(is), hb);
    }

    /**
     * Idem, diciendo desde donde vino.
     *
     * @throws IllegalArgumentException si el flujo es null
     * @deprecated ver la nota de la clase
     */
    @Deprecated
    public void parse(InputStream is, HandlerBase hb, String systemId)
        throws SAXException, IOException {
        if (is == null) {
            throw new IllegalArgumentException("InputStream cannot be null");
        }
        InputSource in = new InputSource(is);
        in.setSystemId(systemId);
        parse(in, hb);
    }

    /**
     * Lee de un flujo.
     *
     * @throws IllegalArgumentException si el flujo es null
     */
    public void parse(InputStream is, DefaultHandler dh) throws SAXException, IOException {
        if (is == null) {
            throw new IllegalArgumentException("InputStream cannot be null");
        }
        parse(new InputSource(is), dh);
    }

    /**
     * Idem, diciendo desde donde vino.
     *
     * @param systemId contra el que se resuelven las referencias relativas
     * @throws IllegalArgumentException si el flujo es null
     */
    public void parse(InputStream is, DefaultHandler dh, String systemId)
        throws SAXException, IOException {
        if (is == null) {
            throw new IllegalArgumentException("InputStream cannot be null");
        }
        InputSource in = new InputSource(is);
        in.setSystemId(systemId);
        parse(in, dh);
    }

    /**
     * Lee de un URI con un manejador de SAX 1.
     *
     * @throws IllegalArgumentException si el URI es null
     * @deprecated ver la nota de la clase
     */
    @Deprecated
    public void parse(String uri, HandlerBase hb) throws SAXException, IOException {
        if (uri == null) {
            throw new IllegalArgumentException("uri cannot be null");
        }
        parse(new InputSource(uri), hb);
    }

    /**
     * Lee de un URI.
     *
     * @throws IllegalArgumentException si el URI es null
     */
    public void parse(String uri, DefaultHandler dh) throws SAXException, IOException {
        if (uri == null) {
            throw new IllegalArgumentException("uri cannot be null");
        }
        parse(new InputSource(uri), dh);
    }

    /**
     * Lee de un archivo con un manejador de SAX 1.
     *
     * @throws IllegalArgumentException si el archivo es null
     * @deprecated ver la nota de la clase
     */
    @Deprecated
    public void parse(File f, HandlerBase hb) throws SAXException, IOException {
        if (f == null) {
            throw new IllegalArgumentException("File cannot be null");
        }
        parse(new InputSource(f.toURI().toString()), hb);
    }

    /**
     * Lee de un archivo.
     *
     * <p>El identificador de sistema sale del camino absoluto; ver
     * {@link DocumentBuilder#parse(File)}.
     *
     * @throws IllegalArgumentException si el archivo es null
     */
    public void parse(File f, DefaultHandler dh) throws SAXException, IOException {
        if (f == null) {
            throw new IllegalArgumentException("File cannot be null");
        }
        parse(new InputSource(f.toURI().toString()), dh);
    }

    /**
     * Lee de una fuente con un manejador de SAX 1.
     *
     * @throws IllegalArgumentException si la fuente es null
     * @deprecated ver la nota de la clase
     */
    @Deprecated
    public void parse(InputSource is, HandlerBase hb) throws SAXException, IOException {
        if (is == null) {
            throw new IllegalArgumentException("InputSource cannot be null");
        }
        Parser parser = this.getParser();
        if (hb != null) {
            parser.setDocumentHandler(hb);
            parser.setEntityResolver(hb);
            parser.setErrorHandler(hb);
            parser.setDTDHandler(hb);
        }
        parser.parse(is);
    }

    /**
     * Lee de una fuente.
     *
     * <p>Es donde terminan todos los demas {@code parse} de SAX 2: el manejador se enchufa en los
     * cuatro puntos y recien ahi se lee.
     *
     * @throws IllegalArgumentException si la fuente es null
     */
    public void parse(InputSource is, DefaultHandler dh) throws SAXException, IOException {
        if (is == null) {
            throw new IllegalArgumentException("InputSource cannot be null");
        }
        XMLReader reader = this.getXMLReader();
        if (dh != null) {
            reader.setContentHandler(dh);
            reader.setEntityResolver(dh);
            reader.setErrorHandler(dh);
            reader.setDTDHandler(dh);
        }
        reader.parse(is);
    }

    /**
     * El analizador de SAX 1 que hay detras.
     *
     * @deprecated ver la nota de la clase; usar {@link #getXMLReader}
     */
    @Deprecated
    public abstract Parser getParser() throws SAXException;

    /** El de SAX 2, que es el que de verdad hace el trabajo. */
    public abstract XMLReader getXMLReader() throws SAXException;

    /** Si distingue espacios de nombres. */
    public abstract boolean isNamespaceAware();

    /** Si valida contra la DTD del documento. */
    public abstract boolean isValidating();

    /**
     * Cambia una propiedad del lector.
     *
     * @throws SAXNotRecognizedException si no conoce ese nombre
     * @throws SAXNotSupportedException si lo conoce pero no lo puede cambiar ahora
     */
    public abstract void setProperty(String name, Object value)
        throws SAXNotRecognizedException, SAXNotSupportedException;

    /** El valor de una propiedad. */
    public abstract Object getProperty(String name)
        throws SAXNotRecognizedException, SAXNotSupportedException;

    /**
     * El esquema contra el que valida, o null.
     *
     * <p>Lo pone la fabrica; ver {@link DocumentBuilder#getSchema}.
     *
     * @throws UnsupportedOperationException por omision
     */
    public Schema getSchema() {
        throw new UnsupportedOperationException(
            "This parser does not support specification \"XML Schema\".");
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
