package javax.swing;

import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.HeadlessException;

/**
 * Una ventana con decoracion, del lado de Swing.
 *
 * <p>Es un {@link Frame} de AWT con un panel raiz adentro; de ahi sale casi toda su API --el panel
 * de contenido, el panel de vidrio, las capas-- y tambien su rareza mas conocida: agregarle un
 * componente directamente no lo agrega al `JFrame` sino a su panel de contenido, porque el `JFrame`
 * mismo no admite mas hijo que el panel raiz.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>Esta la parte que es **estado**: los cuatro constructores, la politica de cierre y la
 * decoracion por omision. Lo que falta es lo que cuelga de `JRootPane` --que esta biblioteca no
 * tiene-- y todo lo que necesita un `LookAndFeel` instalado.
 *
 * <p>La politica de cierre se guarda y se devuelve, pero **nadie la aplica**: aplicarla es trabajo
 * del despacho de eventos de ventana, que aca no corre. Es una propiedad honesta --lee lo que se
 * escribio-- y no una promesa de comportamiento.
 *
 * @see WindowConstants
 */
public class JFrame extends Frame implements WindowConstants {


    /** Si las ventanas nuevas se decoran con el `LookAndFeel` en vez de con el sistema. */
    private static boolean defaultLookAndFeelDecorated = false;

    /** Que hacer al cerrarla; una de las constantes de {@link WindowConstants}. */
    private int defaultCloseOperation = HIDE_ON_CLOSE;

    /**
     * Una ventana sin titulo, todavia invisible.
     *
     * @throws HeadlessException si el entorno no tiene pantalla
     */
    public JFrame() throws HeadlessException {
        super();
    }

    /**
     * Una ventana sin titulo en esa configuracion grafica.
     *
     * @param gc la pantalla y el modo, o `null` para los de siempre
     */
    public JFrame(GraphicsConfiguration gc) {
        super(gc);
    }

    /**
     * Una ventana con ese titulo, todavia invisible.
     *
     * @param title el titulo, o `null` para ninguno
     * @throws HeadlessException si el entorno no tiene pantalla
     */
    public JFrame(String title) throws HeadlessException {
        super(title);
    }

    /**
     * Una ventana con ese titulo en esa configuracion grafica.
     *
     * @param title el titulo, o `null` para ninguno
     * @param gc la pantalla y el modo, o `null` para los de siempre
     */
    public JFrame(String title, GraphicsConfiguration gc) {
        super(title, gc);
    }

    /**
     * Fija que hacer cuando el usuario la cierre.
     *
     * <p>Ver la nota de la clase: aca se guarda, pero no hay quien lo ejecute.
     *
     * @param operation una de las constantes de {@link WindowConstants}
     * @throws IllegalArgumentException si no es una de las cuatro
     */
    public void setDefaultCloseOperation(int operation) {
        if (operation != DO_NOTHING_ON_CLOSE && operation != HIDE_ON_CLOSE
                && operation != DISPOSE_ON_CLOSE && operation != EXIT_ON_CLOSE) {
            throw new IllegalArgumentException("defaultCloseOperation must be one of: "
                    + "DO_NOTHING_ON_CLOSE, HIDE_ON_CLOSE, DISPOSE_ON_CLOSE, or EXIT_ON_CLOSE");
        }
        this.defaultCloseOperation = operation;
    }

    /** Que se haria al cerrarla. Por omision, {@link WindowConstants#HIDE_ON_CLOSE}. */
    public int getDefaultCloseOperation() {
        return this.defaultCloseOperation;
    }

    /**
     * Si las ventanas creadas de aca en mas se decoran con el `LookAndFeel`.
     *
     * <p>Solo afecta a las que se creen despues: la decoracion se elige al construirlas.
     */
    public static void setDefaultLookAndFeelDecorated(boolean defaultLookAndFeelDecorated) {
        JFrame.defaultLookAndFeelDecorated = defaultLookAndFeelDecorated;
    }

    /** Si las ventanas nuevas se decoran con el `LookAndFeel`. Por omision, no. */
    public static boolean isDefaultLookAndFeelDecorated() {
        return defaultLookAndFeelDecorated;
    }
}
