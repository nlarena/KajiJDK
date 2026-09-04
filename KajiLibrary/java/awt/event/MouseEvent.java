package java.awt.event;

import java.awt.Component;
import java.awt.Point;

/**
 * Alguien usó el ratón sobre un componente.
 *
 * <p>Las coordenadas son **relativas al componente**, no a la pantalla: el (0,0) es su ángulo
 * superior izquierdo. Es lo que hace que un componente pueda atender clics sin saber dónde está.
 *
 * <p>{@link #getButton} y los modificadores dicen cosas distintas y las dos hacen falta. El botón es
 * **cuál cambió** en este evento; los modificadores son **cuáles estaban apretados**. Al soltar el
 * botón 1 mientras el 2 sigue apretado, el botón es 1 y los modificadores traen el 2.
 *
 * <p>{@link #isPopupTrigger} existe porque el gesto del menú contextual no es el mismo en todos
 * lados: en Windows es soltar el botón derecho y en macOS es apretar con Control. Preguntarle al
 * evento evita escribir esa diferencia en cada aplicación — y evita que llegue **dos veces** al
 * comprobarlo tanto al apretar como al soltar.
 */
public class MouseEvent extends InputEvent {

    private static final long serialVersionUID = -991214153494842848L;

    /** El botón 1, normalmente el izquierdo. */
    public static final int BUTTON1 = 1;

    /** El botón 2, normalmente el del medio. */
    public static final int BUTTON2 = 2;

    /** El botón 3, normalmente el derecho. */
    public static final int BUTTON3 = 3;

    /** Se apretó y se soltó sin mover. */
    public static final int MOUSE_CLICKED = 500;

    /** Se movió con un botón apretado. */
    public static final int MOUSE_DRAGGED = 506;

    /** El ratón entró al componente. */
    public static final int MOUSE_ENTERED = 504;

    /** El ratón salió del componente. */
    public static final int MOUSE_EXITED = 505;

    /** El primer identificador de la familia. */
    public static final int MOUSE_FIRST = 500;

    /** El último identificador de la familia. */
    public static final int MOUSE_LAST = 507;

    /** Se movió sin botones apretados. */
    public static final int MOUSE_MOVED = 503;

    /** Se apretó un botón. */
    public static final int MOUSE_PRESSED = 501;

    /** Se soltó un botón. */
    public static final int MOUSE_RELEASED = 502;

    /** Se movió la rueda. */
    public static final int MOUSE_WHEEL = 507;

    /** Ningún botón cambió en este evento. */
    public static final int NOBUTTON = 0;

    private int x;
    private int y;
    private final int xAbs;
    private final int yAbs;
    private final int clickCount;
    private final boolean popupTrigger;
    private final int button;

    /**
     * Con todo dado, incluida la posición en pantalla.
     *
     * @throws IllegalArgumentException si la fuente es `null`
     */
    public MouseEvent(Component source, int id, long when, int modifiers, int x, int y,
            int xAbs, int yAbs, int clickCount, boolean popupTrigger, int button) {
        super(source, id, when, modifiers);
        this.x = x;
        this.y = y;
        this.xAbs = xAbs;
        this.yAbs = yAbs;
        this.clickCount = clickCount;
        this.popupTrigger = popupTrigger;
        this.button = button;
    }

    /**
     * Sin la posición en pantalla, que se deduce del componente.
     *
     * @throws IllegalArgumentException si la fuente es `null`
     */
    public MouseEvent(Component source, int id, long when, int modifiers, int x, int y,
            int clickCount, boolean popupTrigger, int button) {
        this(source, id, when, modifiers, x, y, 0, 0, clickCount, popupTrigger, button);
    }

    /**
     * Sin decir qué botón cambió.
     *
     * @throws IllegalArgumentException si la fuente es `null`
     */
    public MouseEvent(Component source, int id, long when, int modifiers, int x, int y,
            int clickCount, boolean popupTrigger) {
        this(source, id, when, modifiers, x, y, 0, 0, clickCount, popupTrigger, NOBUTTON);
    }

    /** La X, relativa al componente. */
    public int getX() {
        return this.x;
    }

    /** La Y, relativa al componente. */
    public int getY() {
        return this.y;
    }

    /** La X, relativa a la pantalla. */
    public int getXOnScreen() {
        return this.xAbs;
    }

    /** La Y, relativa a la pantalla. */
    public int getYOnScreen() {
        return this.yAbs;
    }

    /** El punto, relativo al componente. */
    public Point getPoint() {
        return new Point(this.x, this.y);
    }

    /** El punto, relativo a la pantalla. */
    public Point getLocationOnScreen() {
        return new Point(this.xAbs, this.yAbs);
    }

    /**
     * Corre las coordenadas relativas al componente.
     *
     * <p>Sirve para volver a despachar el mismo evento desde otro componente, sin fabricar uno
     * nuevo.
     */
    public synchronized void translatePoint(int x, int y) {
        this.x = this.x + x;
        this.y = this.y + y;
    }

    /** Cuántos clics seguidos van. */
    public int getClickCount() {
        return this.clickCount;
    }

    /** Cuál botón cambió, o {@link #NOBUTTON}. */
    public int getButton() {
        return this.button;
    }

    /** Si este evento es el gesto del menú contextual de la plataforma. */
    public boolean isPopupTrigger() {
        return this.popupTrigger;
    }

    /** Los modificadores en la codificación nueva. */
    public int getModifiersEx() {
        return super.getModifiersEx();
    }

    /**
     * Los modificadores escritos para una persona.
     *
     * @deprecated trabaja con la codificación vieja. Usar
     *     {@link InputEvent#getModifiersExText(int)}.
     */
    @Deprecated
    public static String getMouseModifiersText(int modifiers) {
        return InputEvent.getModifiersExText(modifiers);
    }

    public String paramString() {
        String tipo;
        if (this.id == MOUSE_PRESSED) {
            tipo = "MOUSE_PRESSED";
        } else if (this.id == MOUSE_RELEASED) {
            tipo = "MOUSE_RELEASED";
        } else if (this.id == MOUSE_CLICKED) {
            tipo = "MOUSE_CLICKED";
        } else if (this.id == MOUSE_ENTERED) {
            tipo = "MOUSE_ENTERED";
        } else if (this.id == MOUSE_EXITED) {
            tipo = "MOUSE_EXITED";
        } else if (this.id == MOUSE_MOVED) {
            tipo = "MOUSE_MOVED";
        } else if (this.id == MOUSE_DRAGGED) {
            tipo = "MOUSE_DRAGGED";
        } else if (this.id == MOUSE_WHEEL) {
            tipo = "MOUSE_WHEEL";
        } else {
            tipo = "unknown type";
        }
        return tipo + ",(" + this.x + "," + this.y + "),absolute(" + this.xAbs + ","
                + this.yAbs + "),button=" + this.button + ",clickCount=" + this.clickCount;
    }
}
