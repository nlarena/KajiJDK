package java.awt.desktop;

/**
 * KajiLibrary's java.awt.desktop.QuitStrategy -- que hacer cuando el sistema pide cerrar.
 *
 * <p>Se fija con {@code Desktop.setQuitStrategy} y decide que pasa <b>despues</b> de que un
 * {@link QuitHandler} acepta el cierre.
 *
 * <p>La diferencia entre las dos importa: {@link #CLOSE_ALL_WINDOWS} manda un evento de cierre a cada
 * ventana, asi que cada una puede guardar lo suyo; {@link #NORMAL_EXIT} llama a {@code System.exit(0)}
 * directo y las ventanas no se enteran.
 */
public enum QuitStrategy {

    /** Llama a {@code System.exit(0)}. Las ventanas no se enteran. */
    NORMAL_EXIT,

    /** Manda un evento de cierre a cada ventana. */
    CLOSE_ALL_WINDOWS
}
