package java.awt;

import java.awt.image.BufferedImage;
import java.util.Locale;

/**
 * Lo que hay para dibujar: las pantallas, las impresoras y las tipografías.
 *
 * <p>Es el punto de entrada a todo lo que depende del hardware gráfico, y por eso es el que sabe
 * responder la pregunta que decide la mitad del comportamiento de AWT: {@link #isHeadless}. Si no hay
 * pantalla, todo lo que la necesite tiene que tirar {@link HeadlessException} en vez de inventar.
 *
 * <p><strong>Acá siempre es sin pantalla.</strong> Esta implementación no tiene forma de hablar con
 * un sistema de ventanas, así que {@link #isHeadless} da `true` y los métodos de pantalla tiran.
 *
 * <p>La lista de tipografías queda **vacía**, y no por falta de ganas. Una tipografía sólo se puede
 * registrar si se la creó con {@link Font#createFont} —así lo pide el JDK y así se comporta acá— y
 * ese método necesita un motor de tipografías que esta biblioteca no tiene. Leer las instaladas del
 * sistema tampoco se puede. Una lista vacía es entonces la respuesta correcta; devolver nombres
 * conocidos —"Dialog", "SansSerif"— sería inventar tipografías que después no se pueden medir ni
 * dibujar.
 */
public abstract class GraphicsEnvironment {

    /** El único entorno, armado la primera vez que lo piden. */
    private static GraphicsEnvironment local;

    /** Para las subclases. */
    protected GraphicsEnvironment() {
    }

    /**
     * El entorno de esta máquina.
     *
     * <p>Es único y se arma una sola vez: preguntar por las pantallas dos veces no puede dar dos
     * conjuntos distintos de pantallas.
     */
    public static GraphicsEnvironment getLocalGraphicsEnvironment() {
        synchronized (GraphicsEnvironment.class) {
            if (local == null) {
                local = new HeadlessGraphicsEnvironment();
            }
            return local;
        }
    }

    /**
     * Si esta máquina no tiene pantalla, teclado ni mouse.
     *
     * @return `true` siempre: esta implementación no habla con ningún sistema de ventanas
     */
    public static boolean isHeadless() {
        return true;
    }

    /** El mensaje que lleva la {@link HeadlessException}, o `null` si sí hay pantalla. */
    static String getHeadlessMessage() {
        return "\nNo X11 DISPLAY variable was set, "
                + "or no headful library support was found, "
                + "but this program performed an operation which requires it.";
    }

    /**
     * Tira si no hay pantalla.
     *
     * @throws HeadlessException siempre
     */
    static void checkHeadless() throws HeadlessException {
        throw new HeadlessException(getHeadlessMessage());
    }

    /**
     * Si **este** entorno no tiene pantalla.
     *
     * <p>Es distinto de {@link #isHeadless}, que habla de la máquina: un entorno puede ser sin
     * pantalla en una máquina que sí la tiene. Acá dan lo mismo.
     */
    public boolean isHeadlessInstance() {
        return true;
    }

    /**
     * Todas las pantallas.
     *
     * @throws HeadlessException si no hay ninguna
     */
    public abstract GraphicsDevice[] getScreenDevices() throws HeadlessException;

    /**
     * La pantalla principal.
     *
     * @throws HeadlessException si no hay ninguna
     */
    public abstract GraphicsDevice getDefaultScreenDevice() throws HeadlessException;

    /** Un contexto de dibujo sobre esa imagen. */
    public abstract Graphics2D createGraphics(BufferedImage img);

    /** Todas las tipografías, cada una en tamaño 1. */
    public abstract Font[] getAllFonts();

    /** Los nombres de familia de todas las tipografías. */
    public abstract String[] getAvailableFontFamilyNames();

    /** Lo mismo, con los nombres traducidos a ese idioma. */
    public abstract String[] getAvailableFontFamilyNames(Locale l);

    /**
     * Registra una tipografía para que la vean {@link #getAllFonts} y quien la pida por nombre.
     *
     * <p>Es cómo se usa una tipografía que viene en un archivo y no está instalada en el sistema.
     * Sólo acepta las **creadas** con {@link Font#createFont}: una armada con `new Font(nombre, ...)`
     * no trae glifos consigo, sólo un nombre, así que registrarla no agregaría nada.
     *
     * @return `false` siempre acá: {@link Font#createFont} necesita un motor de tipografías que esta
     *     biblioteca no tiene, así que ninguna tipografía llega a ser una creada
     * @throws NullPointerException si la tipografía es `null`
     */
    public boolean registerFont(Font font) {
        if (font == null) {
            throw new NullPointerException("font cannot be null.");
        }
        return false;
    }

    /**
     * Pide que se prefieran las tipografías del idioma actual.
     *
     * <p>No hace nada: es una preferencia sobre cómo elegir un sustituto cuando falta un glifo, y sin
     * tipografías instaladas no hay nada entre qué elegir. El JDK también la ignora cuando su gestor
     * de tipografías no la admite.
     */
    public void preferLocaleFonts() {
    }

    /** Pide que se prefieran las proporcionales; no hace nada, por lo mismo. */
    public void preferProportionalFonts() {
    }

    /**
     * El centro de la zona útil de la pantalla, que es donde se centra una ventana.
     *
     * @throws HeadlessException si no hay pantalla
     */
    public Point getCenterPoint() throws HeadlessException {
        Rectangle r = this.getMaximumWindowBounds();
        return new Point(r.x + r.width / 2, r.y + r.height / 2);
    }

    /**
     * La zona de la pantalla donde puede ir una ventana maximizada: todo menos la barra de tareas.
     *
     * @throws HeadlessException si no hay pantalla
     */
    public Rectangle getMaximumWindowBounds() throws HeadlessException {
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        return new Rectangle(d);
    }
}
