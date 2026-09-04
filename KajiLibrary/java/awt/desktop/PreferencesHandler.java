package java.awt.desktop;

/**
 * KajiLibrary's java.awt.desktop.PreferencesHandler -- responde al pedido de preferencias.
 *
 * <p>Se registra con {@code Desktop.setPreferencesHandler}. Solo puede haber uno: a diferencia de los
 * {@link SystemEventListener}, esto no es un aviso sino una responsabilidad, y no tendria sentido que
 * dos partes del programa la tomaran.
 */
public interface PreferencesHandler {

    /** Abre las preferencias del programa. */
    void handlePreferences(PreferencesEvent e);
}
