package jdk.javadoc.doclet;

import java.io.PrintWriter;

import javax.lang.model.element.Element;
import javax.tools.Diagnostic;
import javax.tools.FileObject;

import com.sun.source.util.DocTreePath;

/**
 * Por donde un complemento informa problemas, para que salgan como los de la herramienta.
 *
 * <h2>Por que no alcanza con imprimir</h2>
 *
 * <p>Porque un diagnostico tiene un lugar. Escribir en la salida "falta @param" no le sirve a nadie;
 * lo que sirve es que salga con el archivo y la linea, en el mismo formato que los demas mensajes,
 * de modo que un editor pueda saltar hasta ahi. Las sobrecargas de {@code print} se distinguen
 * justamente por cuanta precision hay disponible sobre el lugar.
 *
 * <p>Ademas cuenta: la herramienta sabe cuantos errores hubo y termina en consecuencia. Un
 * complemento que imprime por su cuenta no participa de eso.
 *
 * <h2>Las dos salidas</h2>
 *
 * <p>{@link #getStandardWriter} es para lo que el complemento produce; {@link #getDiagnosticWriter}
 * para lo que sale mal. Estan separadas para que la salida se pueda redirigir sin llevarse los
 * errores puestos.
 *
 * @since 9
 */
public interface Reporter {

    /**
     * Un diagnostico sin lugar.
     *
     * @param kind la severidad
     * @param msg el mensaje
     */
    void print(Diagnostic.Kind kind, String msg);

    /**
     * Un diagnostico ubicado en un nodo de la documentacion.
     *
     * @param kind la severidad
     * @param path donde, dentro del comentario
     * @param msg el mensaje
     */
    void print(Diagnostic.Kind kind, DocTreePath path, String msg);

    /**
     * Un diagnostico ubicado en un rango dentro de un nodo de la documentacion.
     *
     * <p>Las tres posiciones son las de un diagnostico del compilador: donde empieza lo senalado,
     * donde esta el caracter que se marca, y donde termina. El del medio no es redundante — es el
     * que decide adonde apunta la flecha cuando el rango abarca varias lineas.
     *
     * <p>Por omision descarta las posiciones y delega en la version sin ellas: una implementacion
     * que no las sepa usar no deberia por eso perder el mensaje.
     *
     * @param kind la severidad
     * @param path donde, dentro del comentario
     * @param start donde empieza
     * @param pos donde apunta
     * @param end donde termina
     * @param msg el mensaje
     */
    default void print(Diagnostic.Kind kind, DocTreePath path, int start, int pos, int end,
            String msg) {
        print(kind, path, msg);
    }

    /**
     * Un diagnostico ubicado en un elemento del programa.
     *
     * @param kind la severidad
     * @param e el elemento
     * @param msg el mensaje
     */
    void print(Diagnostic.Kind kind, Element e, String msg);

    /**
     * Un diagnostico ubicado en un rango de un archivo cualquiera.
     *
     * <p>Sirve para lo que no es codigo Java: un archivo de recursos, una plantilla. Por omision
     * pierde el lugar y emite solo el mensaje.
     *
     * @param kind la severidad
     * @param file el archivo
     * @param start donde empieza
     * @param pos donde apunta
     * @param end donde termina
     * @param msg el mensaje
     */
    default void print(Diagnostic.Kind kind, FileObject file, int start, int pos, int end,
            String msg) {
        print(kind, msg);
    }

    /**
     * Por donde escribir la salida normal del complemento.
     *
     * @return el escritor
     * @throws UnsupportedOperationException si esta implementacion no lo ofrece
     */
    default PrintWriter getStandardWriter() {
        throw new UnsupportedOperationException("este Reporter no expone la salida estandar");
    }

    /**
     * Por donde escribir los diagnosticos.
     *
     * @return el escritor
     * @throws UnsupportedOperationException si esta implementacion no lo ofrece
     */
    default PrintWriter getDiagnosticWriter() {
        throw new UnsupportedOperationException(
                "este Reporter no expone la salida de diagnosticos");
    }
}
