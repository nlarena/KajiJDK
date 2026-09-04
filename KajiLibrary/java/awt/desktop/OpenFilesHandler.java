package java.awt.desktop;

/**
 * KajiLibrary's java.awt.desktop.OpenFilesHandler -- abre los archivos que le pasa el sistema.
 *
 * <p>Se registra con {@code Desktop.setOpenFilesHandler}. Solo puede haber uno: a diferencia de los
 * {@link SystemEventListener}, esto no es un aviso sino una responsabilidad, y no tendria sentido que
 * dos partes del programa la tomaran.
 */
public interface OpenFilesHandler {

    /** Abre esos archivos. */
    void openFiles(OpenFilesEvent e);
}
