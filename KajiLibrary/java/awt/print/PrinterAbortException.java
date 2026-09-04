package java.awt.print;

/**
 * KajiLibrary's java.awt.print.PrinterAbortException -- el trabajo se corto a proposito.
 *
 * <p>La diferencia con {@link PrinterException} a secas es la <b>causa</b>: esta significa que alguien
 * lo cancelo --el usuario, o el programa con {@code PrinterJob.cancel()}--, no que algo se rompio. Un
 * programa que la atrapa no deberia reintentar ni avisar de un error.
 */
public class PrinterAbortException extends PrinterException {

    private static final long serialVersionUID = 4725169026278854136L;

    /** Sin detalle. */
    public PrinterAbortException() {
        super();
    }

    /** Con mensaje. */
    public PrinterAbortException(String msg) {
        super(msg);
    }
}
