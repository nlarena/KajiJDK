package javax.accessibility;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.FocusListener;

/**
 * La parte gráfica de un objeto accesible: dónde está, de qué tamaño, de qué color.
 *
 * <p>Es casi un espejo de {@code java.awt.Component}, y esa duplicación es deliberada. Un objeto
 * accesible **no tiene por qué ser** un componente de AWT: puede ser una celda de una planilla
 * dibujada a mano o un elemento de un motor de interfaz propio. Declarar la geometría acá permite
 * que una ayuda técnica dibuje un recuadro alrededor de cualquiera de los dos.
 *
 * <p>{@link #getAccessibleAt} es la que hace posible "¿qué hay debajo del puntero?", que es la
 * pregunta con la que empieza casi toda inspección.
 */
public interface AccessibleComponent {

    /** El color de fondo, o `null` si no lo admite. */
    Color getBackground();

    /** Cambia el color de fondo. */
    void setBackground(Color c);

    /** El color del texto, o `null` si no lo admite. */
    Color getForeground();

    /** Cambia el color del texto. */
    void setForeground(Color c);

    /** El cursor, o `null` si no lo admite. */
    Cursor getCursor();

    /** Cambia el cursor. */
    void setCursor(Cursor cursor);

    /** La fuente, o `null` si no lo admite. */
    Font getFont();

    /** Cambia la fuente. */
    void setFont(Font f);

    /** Las medidas de esa fuente, o `null` si no lo admite. */
    FontMetrics getFontMetrics(Font f);

    /** Si responde a la entrada del usuario. */
    boolean isEnabled();

    /** Lo habilita o lo deshabilita. */
    void setEnabled(boolean b);

    /** Si está declarado visible. */
    boolean isVisible();

    /** Lo muestra o lo oculta. */
    void setVisible(boolean b);

    /** Si se ve de verdad, contando a sus ancestros. */
    boolean isShowing();

    /** Si ese punto, relativo al objeto, cae adentro. */
    boolean contains(Point p);

    /**
     * Dónde está en la pantalla.
     *
     * @return el punto, o `null` si no está en pantalla
     */
    Point getLocationOnScreen();

    /** Dónde está, relativo a su padre. */
    Point getLocation();

    /** Lo mueve. */
    void setLocation(Point p);

    /** Su rectángulo, relativo al padre. */
    Rectangle getBounds();

    /** Cambia su rectángulo. */
    void setBounds(Rectangle r);

    /** Su tamaño. */
    Dimension getSize();

    /** Cambia su tamaño. */
    void setSize(Dimension d);

    /**
     * Qué hijo accesible cae en ese punto.
     *
     * @return el hijo, o `null` si ninguno
     */
    Accessible getAccessibleAt(Point p);

    /** Si puede recibir el foco. */
    boolean isFocusTraversable();

    /** Le pide el foco. */
    void requestFocus();

    /** Suma alguien a quien avisarle de los cambios de foco. */
    void addFocusListener(FocusListener l);

    /** Saca a ese oyente. */
    void removeFocusListener(FocusListener l);
}
