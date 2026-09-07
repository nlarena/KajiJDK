package javax.swing;

/**
 * Quien decide como se comportan las ventanas internas de un escritorio.
 *
 * <h2>Por que existe</h2>
 *
 * <p>Un {@link JInternalFrame} no se cierra, no se agranda ni se mueve por su cuenta: le pide al
 * administrador del escritorio que lo haga. Asi el escritorio puede imponer su politica -- una
 * ventana maximizada que tapa a las demas, iconos ordenados en una fila, arrastre con contorno en
 * vez de en vivo -- sin que cada ventana sepa nada de eso.
 *
 * <h2>Las tres tandas de metodos</h2>
 *
 * <p>Los primeros ocho son cambios de estado que pide la ventana. Los seis del medio son las tres
 * etapas -- empezar, seguir, terminar -- de arrastrar y de redimensionar; estan separadas porque el
 * modo de contorno solo dibuja durante el medio y recien mueve al terminar. El ultimo,
 * {@link #setBoundsForFrame}, es el que finalmente mueve algo.
 */
public interface DesktopManager {

    /** La ventana se agrego al escritorio y hay que mostrarla. */
    void openFrame(JInternalFrame f);

    /** Saca la ventana del escritorio. */
    void closeFrame(JInternalFrame f);

    /** Agranda la ventana a todo el escritorio. */
    void maximizeFrame(JInternalFrame f);

    /** Devuelve la ventana a su tamano anterior. */
    void minimizeFrame(JInternalFrame f);

    /** Reemplaza la ventana por su icono. */
    void iconifyFrame(JInternalFrame f);

    /** Devuelve la ventana en lugar de su icono. */
    void deiconifyFrame(JInternalFrame f);

    /** La ventana paso a ser la activa. */
    void activateFrame(JInternalFrame f);

    /** La ventana dejo de ser la activa. */
    void deactivateFrame(JInternalFrame f);

    /** Empieza un arrastre; ver la nota de la interfaz. */
    void beginDraggingFrame(JComponent f);

    /** El arrastre va por esa posicion. */
    void dragFrame(JComponent f, int newX, int newY);

    /** Termina el arrastre. */
    void endDraggingFrame(JComponent f);

    /** Empieza a redimensionar desde ese borde. */
    void beginResizingFrame(JComponent f, int direction);

    /** El redimensionado va por ese rectangulo. */
    void resizeFrame(JComponent f, int newX, int newY, int newWidth, int newHeight);

    /** Termina el redimensionado. */
    void endResizingFrame(JComponent f);

    /** Mueve y redimensiona la ventana; es el unico que cambia algo de verdad. */
    void setBoundsForFrame(JComponent f, int newX, int newY, int newWidth, int newHeight);
}
