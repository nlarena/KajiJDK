package javax.swing;

import javax.swing.plaf.UIResource;

/**
 * Un aspecto: el conjunto de {@code ComponentUI} y valores por omision que dan a Swing una cara.
 *
 * <h2>Lo que hay y lo que no</h2>
 *
 * <p>Esta biblioteca tiene <strong>un solo aspecto</strong>: el basico, con los valores por
 * omision medidos en Metal, instalados directamente por cada {@code updateUI}. No hay
 * {@code UIManager} donde registrar otro, y por eso tampoco estan aca los metodos que consultan sus
 * tablas: {@code installColors}, {@code installColorsAndFont}, {@code installBorder},
 * {@code getDefaults}, {@code makeKeyBindings}, {@code makeInputMap}, {@code loadKeyBindings},
 * {@code makeIcon}, {@code getLayoutStyle}. Cada uno de ellos, sin tablas, solo podria mentir.
 *
 * <p>Lo que si esta es lo que no depende de tablas: {@link #installProperty}, que es como un
 * aspecto pone una propiedad <em>sin pisar lo que el usuario puso</em>, y
 * {@link #uninstallBorder}, que quita un borde solo si es del aspecto. Los dos se apoyan en
 * {@link UIResource}, que es la manera de distinguir lo uno de lo otro.
 */
public abstract class LookAndFeel {

    public LookAndFeel() {
    }

    /**
     * Pone una propiedad en el componente, salvo que el usuario ya la haya puesto.
     *
     * <p>Es la regla de convivencia entre aspecto y programador: el aspecto propone, el programador
     * dispone. El componente recuerda cuales propiedades le puso el programador, y esta llamada
     * respeta esas. Las propiedades admitidas dependen del componente; una que no admite es un
     * {@code IllegalArgumentException}.
     */
    public static void installProperty(JComponent c, String propertyName, Object propertyValue) {
        c.setUIProperty(propertyName, propertyValue);
    }

    /** Quita el borde del componente si lo puso un aspecto; uno del usuario se queda. */
    public static void uninstallBorder(JComponent c) {
        if (c.getBorder() instanceof UIResource) {
            c.setBorder(null);
        }
    }

    /**
     * El icono deshabilitado que corresponde a ese icono: ninguno.
     *
     * <p>El JDK fabrica uno agrisado cuando el icono es un {@code ImageIcon}, y {@code null} para
     * cualquier otro. Sin {@code ImageIcon}, la respuesta es siempre la segunda, y quien la recibe
     * —{@code AbstractButton}, {@code JLabel}— pinta el icono normal.
     */
    public Icon getDisabledIcon(JComponent component, Icon icon) {
        return null;
    }

    /** El icono deshabilitado y seleccionado: ninguno, por lo mismo que {@link #getDisabledIcon}. */
    public Icon getDisabledSelectedIcon(JComponent component, Icon icon) {
        return null;
    }

    /** Un nombre corto para mostrar, como "Metal". */
    public abstract String getName();

    /** Un identificador estable, como "Metal"; el nombre puede cambiar, este no. */
    public abstract String getID();

    /** Una linea que lo describe. */
    public abstract String getDescription();

    /** Si este aspecto puede decorar las ventanas el mismo: no, este no. */
    public boolean getSupportsWindowDecorations() {
        return false;
    }

    /** Si es el aspecto nativo de la plataforma. */
    public abstract boolean isNativeLookAndFeel();

    /** Si este aspecto puede usarse en esta plataforma. */
    public abstract boolean isSupportedLookAndFeel();

    /** Se llama al instalarlo; no hay nada que preparar. */
    public void initialize() {
    }

    /** Se llama al desinstalarlo; no hay nada que soltar. */
    public void uninitialize() {
    }

    public String toString() {
        return "[" + getDescription() + " - " + getClass().getName() + "]";
    }
}
