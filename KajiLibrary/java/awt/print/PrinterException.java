package java.awt.print;

/**
 * KajiLibrary's java.awt.print.PrinterException -- algo salio mal al imprimir.
 *
 * <p>La base de las excepciones de este paquete. Es comprobada, y con razon: que una impresora falle
 * es una condicion normal del entorno, no un error de programa.
 *
 * <p>No confundirla con {@code javax.print.PrintException}, que es la del otro sistema de impresion.
 * Los dos paquetes conviven --este es el viejo, orientado a dibujar; el otro es el nuevo, orientado a
 * documentos-- y sus excepciones no tienen relacion de herencia.
 */
public class PrinterException extends Exception {

    private static final long serialVersionUID = -3757589981158265819L;

    /** Sin detalle. */
    public PrinterException() {
        super();
    }

    /** Con mensaje. */
    public PrinterException(String msg) {
        super(msg);
    }
}
