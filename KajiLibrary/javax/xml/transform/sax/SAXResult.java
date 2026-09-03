package javax.xml.transform.sax;

import javax.xml.transform.Result;
import org.xml.sax.ContentHandler;
import org.xml.sax.ext.LexicalHandler;

/**
 * KajiLibrary's javax.xml.transform.sax.SAXResult -- la salida sale por eventos.
 *
 * <p>En vez de escribir texto o armar un arbol, el transformador le va avisando a un
 * {@link ContentHandler} lo que produce. Es la via para encadenar transformaciones sin materializar
 * el paso intermedio, y para consumir una salida grande sin tenerla entera en memoria.
 *
 * <h2>Por que hay un segundo manejador</h2>
 *
 * <p>{@link #setLexicalHandler} es opcional y no es un duplicado: {@code ContentHandler} no tiene
 * metodos para <b>comentarios</b>, secciones CDATA ni entidades. Sin un manejador lexico, todo eso
 * pasa sin que nadie se entere -- que casi siempre esta bien, y que es un problema cuando lo que se
 * esta armando tiene que conservar los comentarios del original.
 *
 * <p>Los dos se ponen por separado aunque un mismo objeto implemente las dos interfaces: poner el
 * de contenido <b>no</b> instala el lexico, y {@link #getLexicalHandler} sigue devolviendo null
 * hasta que alguien lo ponga. Conviene saberlo porque invita al error de esperar comentarios que
 * nunca llegan. El JDK hace lo mismo -- la deduccion, cuando la hay, la hace el transformador y no
 * esta clase.
 */
public class SAXResult implements Result {

    /** Con esto se le pregunta a un {@code TransformerFactory} si acepta este destino. */
    public static final String FEATURE = "http://javax.xml.transform.sax.SAXResult/feature";

    private ContentHandler handler;

    private LexicalHandler lexicalHandler;

    private String systemId;

    /** Vacio, para llenarlo. */
    public SAXResult() {
    }

    /** Con el manejador que va a recibir la salida. */
    public SAXResult(ContentHandler handler) {
        setHandler(handler);
    }

    /** Quien recibe los eventos de la salida. */
    public void setHandler(ContentHandler handler) {
        this.handler = handler;
    }

    /** Ver {@link #setHandler}. */
    public ContentHandler getHandler() {
        return this.handler;
    }

    /** Quien recibe comentarios, CDATA y entidades. Ver la nota de la clase. */
    public void setLexicalHandler(LexicalHandler handler) {
        this.lexicalHandler = handler;
    }

    /** Ver {@link #setLexicalHandler}; null si nadie puso uno. */
    public LexicalHandler getLexicalHandler() {
        return this.lexicalHandler;
    }

    /** De donde sale el resultado; informativo. */
    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    /** Ver {@link #setSystemId}. */
    public String getSystemId() {
        return this.systemId;
    }
}
