package java.awt.print;

import java.io.IOException;

/**
 * KajiLibrary's java.awt.print.PrinterIOException -- el trabajo fallo por un problema de
 * entrada/salida.
 *
 * <p>Envuelve la {@link IOException} original para que pueda salir por una firma que solo declara
 * {@link PrinterException}.
 *
 * <h2>Dos formas de sacar la misma excepcion</h2>
 *
 * <p>{@link #getIOException} y {@link #getCause} devuelven <b>lo mismo</b>. La primera es de 1998; la
 * segunda aparecio en 1.4 con el mecanismo general de causas encadenadas, y se redefinio para que las
 * herramientas que imprimen trazas encontraran la causa por el camino estandar.
 *
 * <p>La consecuencia es que {@code getMessage()} devuelve null: el constructor no toma mensaje, y no
 * hereda el de la causa.
 */
public class PrinterIOException extends PrinterException {

    private static final long serialVersionUID = 5850870712125932846L;

    /** La original. */
    private final IOException mException;

    /** @param exception la que hay que envolver */
    public PrinterIOException(IOException exception) {
        initCause(null);
        this.mException = exception;
    }

    /** La original. Lo mismo que {@link #getCause}. */
    public IOException getIOException() {
        return this.mException;
    }

    /** La original. Ver la nota de la clase. */
    @Override
    public Throwable getCause() {
        return this.mException;
    }
}
