package javax.xml.transform.sax;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

/**
 * KajiLibrary's javax.xml.transform.sax.SAXSource -- una fuente que se lee por eventos.
 *
 * <p>Junta dos cosas: <b>de donde</b> leer --el {@link InputSource}-- y <b>quien</b> lo lee --el
 * {@link XMLReader}--. El lector es opcional: sin uno, el transformador usa el suyo. Ponerlo sirve
 * para meter un filtro en el medio, o un analizador configurado de una forma particular, sin que el
 * transformador se entere.
 *
 * <p>{@link #sourceToInputSource} es la utilidad interesante y esta aca por comodidad: convierte
 * cualquier {@code Source} en un {@code InputSource} cuando se puede. Puede con este y con
 * {@link StreamSource}; con los demas devuelve <b>null</b> en vez de lanzar, porque un
 * {@code DOMSource} no tiene nada que leer por eventos y eso no es un error sino una respuesta.
 */
public class SAXSource implements Source {

    /** Con esto se le pregunta a un {@code TransformerFactory} si acepta esta fuente. */
    public static final String FEATURE = "http://javax.xml.transform.sax.SAXSource/feature";

    private XMLReader reader;

    private InputSource inputSource;

    /** Vacia, para llenarla. */
    public SAXSource() {
    }

    /** Con lector propio. */
    public SAXSource(XMLReader reader, InputSource inputSource) {
        this.reader = reader;
        this.inputSource = inputSource;
    }

    /** Sin lector: lo pone el transformador. */
    public SAXSource(InputSource inputSource) {
        this.inputSource = inputSource;
    }

    /** El lector, o null para que lo elija el transformador. */
    public void setXMLReader(XMLReader reader) {
        this.reader = reader;
    }

    /** Ver {@link #setXMLReader}. */
    public XMLReader getXMLReader() {
        return this.reader;
    }

    /** De donde leer. */
    public void setInputSource(InputSource inputSource) {
        this.inputSource = inputSource;
    }

    /** Ver {@link #setInputSource}. */
    public InputSource getInputSource() {
        return this.inputSource;
    }

    /**
     * De donde salio.
     *
     * <p>Si todavia no hay {@link InputSource}, <b>lo crea</b> con ese identificador. Es lo que hace
     * que poner solo el identificador alcance para tener una fuente utilizable.
     */
    public void setSystemId(String systemId) {
        if (this.inputSource == null) {
            this.inputSource = new InputSource(systemId);
        } else {
            this.inputSource.setSystemId(systemId);
        }
    }

    /** El del {@link InputSource}, o null si no hay. */
    public String getSystemId() {
        if (this.inputSource == null) {
            return null;
        }
        return this.inputSource.getSystemId();
    }

    /**
     * Un {@code InputSource} equivalente, si se puede.
     *
     * @return null si esa fuente no se puede leer por eventos; ver la nota de la clase
     */
    public static InputSource sourceToInputSource(Source source) {
        if (source instanceof SAXSource) {
            return ((SAXSource) source).getInputSource();
        }
        if (source instanceof StreamSource) {
            StreamSource stream = (StreamSource) source;
            InputSource made = new InputSource(stream.getSystemId());
            made.setByteStream(stream.getInputStream());
            made.setCharacterStream(stream.getReader());
            made.setPublicId(stream.getPublicId());
            return made;
        }
        return null;
    }

    /**
     * Si no hay nada que leer.
     *
     * <p>No alcanza con tener un {@link InputSource}: uno recien construido esta tan vacio como
     * ninguno. Lo que cuenta es que tenga alguna de las tres vias --identificador, flujo de bytes o
     * flujo de caracteres--.
     */
    public boolean isEmpty() {
        InputSource where = getInputSource();
        if (where == null) {
            return true;
        }
        return where.getSystemId() == null
            && where.getByteStream() == null
            && where.getCharacterStream() == null;
    }
}
