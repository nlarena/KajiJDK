package java.awt.print;

/**
 * KajiLibrary's java.awt.print.PrinterGraphics -- el lienzo sabe de que trabajo es.
 *
 * <p>Un solo metodo. El {@code Graphics} que recibe {@link Printable#print} la implementa, asi que
 * quien dibuja puede llegar al trabajo --para cancelarlo, tipicamente-- sin haberlo recibido como
 * argumento.
 *
 * <p>Se consulta con {@code instanceof}: la firma de {@code print} declara {@code Graphics} a secas.
 */
public interface PrinterGraphics {

    /** El trabajo del que este lienzo es parte. */
    PrinterJob getPrinterJob();
}
