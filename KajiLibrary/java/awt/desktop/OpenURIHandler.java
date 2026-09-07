package java.awt.desktop;

/**
 * KajiLibrary's java.awt.desktop.OpenURIHandler -- opens an address the system hands it.
 *
 * <p>It is registered with {@code Desktop.setOpenURIHandler}. There can be only one: unlike the
 * {@link SystemEventListener}s, this is not a notice but a responsibility, and it would make no sense
 * for two parts of the program to take it.
 */
public interface OpenURIHandler {

    /** Opens that address. */
    void openURI(OpenURIEvent e);
}
