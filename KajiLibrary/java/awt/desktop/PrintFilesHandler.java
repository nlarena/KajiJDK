package java.awt.desktop;

/**
 * KajiLibrary's java.awt.desktop.PrintFilesHandler -- imprime los archivos que le pasa el sistema.
 *
 * <p>Se registra con {@code Desktop.setPrintFilesHandler}. Solo puede haber uno: a diferencia de los
 * {@link SystemEventListener}, esto no es un aviso sino una responsabilidad, y no tendria sentido que
 * dos partes del programa la tomaran.
 */
public interface PrintFilesHandler {

    /** Imprime esos archivos. */
    void printFiles(PrintFilesEvent e);
}
