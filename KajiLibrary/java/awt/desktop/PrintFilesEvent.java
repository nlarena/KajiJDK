package java.awt.desktop;

import java.io.File;
import java.util.List;

/**
 * KajiLibrary's java.awt.desktop.PrintFilesEvent -- el sistema pide imprimir archivos.
 *
 * <p>Lo entrega {@link PrintFilesHandler}. Llega cuando el usuario elige "Imprimir" sobre archivos en
 * el escritorio sin abrir el programa.
 *
 * <p>Lo que se espera de un manejador es que imprima <b>sin interfaz</b>: el usuario ya dijo que
 * queria imprimir, y abrir una ventana ahi es un estorbo.
 */
public final class PrintFilesEvent extends FilesEvent {

    private static final long serialVersionUID = -5752560876153618618L;

    /** @param files los archivos a imprimir */
    public PrintFilesEvent(final List<File> files) {
        super(files);
    }
}
