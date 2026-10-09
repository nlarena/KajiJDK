package java.awt.print;

/**
 * KajiLibrary's java.awt.print.PrinterGraphics -- the canvas knows which job it belongs to.
 *
 * <p>A single method. The {@code Graphics} that {@link Printable#print} receives implements it, so
 * whoever draws can reach the job --to cancel it, typically-- without having received it as an
 * argument.
 *
 * <p>It is checked with {@code instanceof}: the signature of {@code print} declares a plain
 * {@code Graphics}.
 */
public interface PrinterGraphics {

    /** The job this canvas is part of. */
    PrinterJob getPrinterJob();
}
