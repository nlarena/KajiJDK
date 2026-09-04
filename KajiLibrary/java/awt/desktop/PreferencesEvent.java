package java.awt.desktop;

/**
 * KajiLibrary's java.awt.desktop.PreferencesEvent -- el usuario pidio abrir las preferencias.
 *
 * <p>Lo entrega {@link PreferencesHandler}. En los escritorios donde el menu de preferencias esta
 * deshabilitado por omision, registrar un manejador es lo que lo habilita.
 */
public final class PreferencesEvent extends AppEvent {

    private static final long serialVersionUID = -6398607097086476160L;

    /** Sin datos: el evento es el aviso. */
    public PreferencesEvent() {
    }
}
