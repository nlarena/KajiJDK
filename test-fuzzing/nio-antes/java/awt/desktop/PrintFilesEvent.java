package java.awt.desktop;

import java.io.File;
import java.util.List;

/**
 * KajiLibrary's java.awt.desktop.PrintFilesEvent -- the system asks for files to be printed.
 *
 * <p>It is handed over by {@link PrintFilesHandler}. It arrives when the user picks "Print" over
 * files on the desktop without opening the program.
 *
 * <p>What is expected of a handler is that it print <b>with no interface</b>: the user already said
 * they wanted to print, and opening a window there is a nuisance.
 */
public final class PrintFilesEvent extends FilesEvent {

    private static final long serialVersionUID = -5752560876153618618L;

    /** @param files the files to print */
    public PrintFilesEvent(final List<File> files) {
        super(files);
    }
}
