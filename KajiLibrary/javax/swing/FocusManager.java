package javax.swing;

import java.awt.DefaultKeyboardFocusManager;
import java.awt.KeyboardFocusManager;

/**
 * El administrador de foco de Swing.
 *
 * <h2>Una clase que quedo de otra epoca</h2>
 *
 * <p>Antes de Java 1.4, Swing tenia su propio administrador de foco y esta era la puerta de entrada.
 * Desde entonces el foco lo maneja AWT con {@link KeyboardFocusManager}, y esta clase quedo como una
 * fachada: {@link #getCurrentManager} y {@link #setCurrentManager} son el administrador de AWT visto
 * a traves de un tipo mas viejo.
 *
 * <p>{@link #disableSwingFocusManager} y {@link #isFocusManagerEnabled} ya no hacen nada util --
 * estan marcadas obsoletas en el JDK y se conservan porque son publicas --. Vale la pena saberlo
 * antes de escribir codigo que dependa de ellas.
 */
public abstract class FocusManager extends DefaultKeyboardFocusManager {

    /**
     * La clave con la que se pedia otro administrador.
     *
     * <p>Ya no la lee nadie; ver la nota de la clase.
     */
    public static final String FOCUS_MANAGER_CLASS_PROPERTY = "FocusManagerClassName";

    /** Para las subclases. */
    protected FocusManager() {
    }

    /**
     * El administrador de foco de este contexto.
     *
     * <p>Si el que hay no es de este tipo -- lo normal, porque el de AWT no lo es -- se lo devuelve
     * envuelto: el metodo promete un {@code FocusManager} y hay que cumplirlo.
     */
    public static FocusManager getCurrentManager() {
        KeyboardFocusManager m = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        if (m instanceof FocusManager) {
            return (FocusManager) m;
        }
        return new DelegadoDeAwt();
    }

    /**
     * Cambia el administrador.
     *
     * <p>Uno envuelto vuelve a ser el de AWT: pasarle a AWT su propia envoltura lo dejaria
     * apuntandose a si mismo.
     *
     * @throws SecurityException si el contexto no lo permite
     */
    public static void setCurrentManager(FocusManager aFocusManager) {
        KeyboardFocusManager toSet = aFocusManager;
        if (aFocusManager instanceof DelegadoDeAwt) {
            toSet = null;
        }
        KeyboardFocusManager.setCurrentKeyboardFocusManager(toSet);
    }

    /**
     * No hace nada.
     *
     * @deprecated Como en el JDK: el administrador de Swing ya no existe, asi que no hay nada que
     *     apagar.
     */
    @Deprecated
    public static void disableSwingFocusManager() {
    }

    /**
     * Siempre cierto.
     *
     * @deprecated Ver {@link #disableSwingFocusManager}.
     */
    @Deprecated
    public static boolean isFocusManagerEnabled() {
        return true;
    }

    /**
     * Envuelve el administrador de AWT para poder devolverlo con este tipo.
     *
     * <p>No agrega comportamiento: todo lo hereda de {@link DefaultKeyboardFocusManager}.
     */
    private static class DelegadoDeAwt extends FocusManager {

        DelegadoDeAwt() {
            super();
        }
    }
}
