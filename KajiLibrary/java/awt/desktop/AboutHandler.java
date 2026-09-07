package java.awt.desktop;

/**
 * KajiLibrary's java.awt.desktop.AboutHandler -- answers the {@code About} request.
 *
 * <p>It is registered with {@code Desktop.setAboutHandler}. There can be only one: unlike the
 * {@link SystemEventListener}s, this is not a notice but a responsibility, and it would make no sense
 * for two parts of the program to take it.
 */
public interface AboutHandler {

    /** Shows the program's own box. */
    void handleAbout(AboutEvent e);
}
