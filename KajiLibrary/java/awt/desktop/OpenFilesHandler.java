package java.awt.desktop;

/**
 * KajiLibrary's java.awt.desktop.OpenFilesHandler -- opens the files the system hands it.
 *
 * <p>It is registered with {@code Desktop.setOpenFilesHandler}. There can be only one: unlike the
 * {@link SystemEventListener}s, this is not a notice but a responsibility, and it would make no sense
 * for two parts of the program to take it.
 */
public interface OpenFilesHandler {

    /** Opens those files. */
    void openFiles(OpenFilesEvent e);
}
