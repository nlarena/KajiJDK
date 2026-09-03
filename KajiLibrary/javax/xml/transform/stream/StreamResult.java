package javax.xml.transform.stream;

import java.io.File;
import java.io.OutputStream;
import java.io.Writer;

import javax.xml.transform.Result;

/**
 * KajiLibrary's javax.xml.transform.stream.StreamResult -- el resultado se escribe serializado.
 *
 * <p>El espejo de {@link StreamSource}, y la diferencia con las otras implementaciones de
 * {@link Result} no es de forma sino de **quien hace el trabajo**: escribir en un arbol o en un
 * manejador de eventos es entregar nodos, escribir aca es serializar --elegir la codificacion,
 * escapar los `&amp;`, decidir si se indenta--. Por eso las propiedades de {@link
 * javax.xml.transform.OutputKeys} recien tienen efecto cuando el destino es este.
 *
 * <p>Los tres destinos se miran en el mismo orden que en la fuente: el {@link Writer}, despues el
 * {@link OutputStream}, y al final el identificador de sistema, que el procesador abre el mismo.
 * Y la misma advertencia, invertida y peor: un `Writer` ya fijo la codificacion, asi que
 * {@code OutputKeys.ENCODING} **no la puede cambiar**, y encima la declaracion XML que se emita va
 * a anunciar una codificacion que el `Writer` no esta usando. Si la codificacion importa, el
 * destino es el flujo de bytes.
 */
public class StreamResult implements Result {

    /**
     * El nombre con el que se le pregunta a una fabrica si acepta esta clase de destino.
     *
     * <p>De solo lectura, como el de {@link StreamSource#FEATURE}: no es una opcion que se prenda.
     */
    public static final String FEATURE = "http://javax.xml.transform.stream.StreamResult/feature";

    private String systemId;
    private OutputStream outputStream;
    private Writer writer;

    /** Vacio, para llenarlo despues con los `set`. */
    public StreamResult() {
    }

    /**
     * Hacia un flujo de bytes. Es el destino que respeta {@code OutputKeys.ENCODING}.
     *
     * @param outputStream donde escribir
     */
    public StreamResult(OutputStream outputStream) {
        setOutputStream(outputStream);
    }

    /**
     * Hacia un flujo de caracteres. Ojo con la codificacion: ver el encabezado.
     *
     * @param writer donde escribir
     */
    public StreamResult(Writer writer) {
        setWriter(writer);
    }

    /**
     * Hacia una URI, que el procesador abre el mismo.
     *
     * @param systemId la URI del destino
     */
    public StreamResult(String systemId) {
        this.systemId = systemId;
    }

    /**
     * Hacia un archivo. La URI se arma con {@link #setSystemId(File)}.
     *
     * @param f el archivo
     */
    public StreamResult(File f) {
        setSystemId(f);
    }

    // ---- a donde van los bytes ---------------------------------------------------------------

    /**
     * Fija el flujo de bytes.
     *
     * @param outputStream donde escribir, o null
     */
    public void setOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    /** El flujo de bytes, o null. */
    public OutputStream getOutputStream() {
        return outputStream;
    }

    /**
     * Fija el flujo de caracteres, que le gana al de bytes.
     *
     * @param writer donde escribir, o null
     */
    public void setWriter(Writer writer) {
        this.writer = writer;
    }

    /** El flujo de caracteres, o null. */
    public Writer getWriter() {
        return writer;
    }

    // ---- identificacion ----------------------------------------------------------------------

    /**
     * Fija la URI del destino.
     *
     * @param systemId la URI, o null
     */
    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    /**
     * Fija la URI del destino a partir de un archivo, convertido a {@code file:}.
     *
     * <p>Misma conversion que en {@link StreamSource#setSystemId(File)}, y por la misma razon: sin
     * percent-encoding, una ruta con espacios no es una URI. Alla esta anotado el techo que las dos
     * comparten --lo que falta esta en {@code java.net.URI} y en {@code File}, no aca--.
     *
     * @param f el archivo
     */
    public void setSystemId(File f) {
        this.systemId = f.toURI().toASCIIString();
    }

    /** La URI del destino, o null. */
    public String getSystemId() {
        return systemId;
    }
}
