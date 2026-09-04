package java.awt.desktop;

/**
 * KajiLibrary's java.awt.desktop.AboutHandler -- responde al pedido de {@code Acerca de}.
 *
 * <p>Se registra con {@code Desktop.setAboutHandler}. Solo puede haber uno: a diferencia de los
 * {@link SystemEventListener}, esto no es un aviso sino una responsabilidad, y no tendria sentido que
 * dos partes del programa la tomaran.
 */
public interface AboutHandler {

    /** Muestra el cuadro propio del programa. */
    void handleAbout(AboutEvent e);
}
