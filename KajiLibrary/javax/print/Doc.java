package javax.print;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import javax.print.attribute.DocAttributeSet;

/**
 * KajiLibrary's javax.print.Doc -- un documento para imprimir.
 *
 * <p>Junta tres cosas: el dato, el {@link DocFlavor} que dice de que tipo es, y los atributos que
 * valen solo para este documento.
 *
 * <h2>Los tres accesores al dato</h2>
 *
 * <p>{@link #getPrintData} devuelve el dato en su forma declarada. Los otros dos son atajos para el
 * servicio, y <b>devuelven null si no aplican</b>:
 *
 * <ul>
 *   <li>{@link #getReaderForText} solo si el dato es texto de caracteres;
 *   <li>{@link #getStreamForBytes} solo si el dato es bytes.
 * </ul>
 *
 * <p>Devolver null es lo correcto, no un error. Un servicio prueba el que le sirve y si le dan null usa
 * {@code getPrintData}.
 *
 * <h2>Se lee una sola vez</h2>
 *
 * <p>Los tres metodos tienen que devolver <b>el mismo</b> objeto en cada llamada, no uno nuevo. Es lo
 * que permite que el dato sea un flujo que no se puede rebobinar. La contracara es que un {@code Doc}
 * se puede imprimir una sola vez.
 */
public interface Doc {

    /** De que tipo es el dato. */
    DocFlavor getDocFlavor();

    /**
     * El dato, en la clase que declara el formato.
     *
     * @throws IOException si el dato es un flujo y no se pudo abrir
     */
    Object getPrintData() throws IOException;

    /** Los atributos propios de este documento, o null. */
    DocAttributeSet getAttributes();

    /**
     * El dato como caracteres, o null si no es texto. Siempre el mismo lector.
     *
     * @throws IOException si no se pudo abrir
     */
    Reader getReaderForText() throws IOException;

    /**
     * El dato como bytes, o null si no lo es. Siempre el mismo flujo.
     *
     * @throws IOException si no se pudo abrir
     */
    InputStream getStreamForBytes() throws IOException;
}
