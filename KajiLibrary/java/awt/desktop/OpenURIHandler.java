package java.awt.desktop;

/**
 * KajiLibrary's java.awt.desktop.OpenURIHandler -- abre una direccion que le pasa el sistema.
 *
 * <p>Se registra con {@code Desktop.setOpenURIHandler}. Solo puede haber uno: a diferencia de los
 * {@link SystemEventListener}, esto no es un aviso sino una responsabilidad, y no tendria sentido que
 * dos partes del programa la tomaran.
 */
public interface OpenURIHandler {

    /** Abre esa direccion. */
    void openURI(OpenURIEvent e);
}
