package java.awt.event;

import java.awt.Component;
import java.awt.Point;

/**
 * Someone used the mouse over a component.
 *
 * <p>The coordinates are **relative to the component**, not to the screen: the (0,0) is its top left
 * corner. That is what lets a component attend to clicks without knowing where it is.
 *
 * <p>{@link #getButton} and the modifiers say different things and both are needed. The button is
 * **which one changed** in this event; the modifiers are **which ones were down**. On releasing
 * button 1 while 2 is still down, the button is 1 and the modifiers carry the 2.
 *
 * <p>{@link #isPopupTrigger} exists because the context-menu gesture is not the same everywhere: on
 * Windows it is releasing the right button and on macOS it is pressing with Control. Asking the
 * event avoids writing that difference into every application — and avoids it arriving **twice** by
 * checking it both on the press and on the release.
 */
public class MouseEvent extends InputEvent {

    private static final long serialVersionUID = -991214153494842848L;

    /** Button 1, usually the left one. */
    public static final int BUTTON1 = 1;

    /** Button 2, usually the middle one. */
    public static final int BUTTON2 = 2;

    /** Button 3, usually the right one. */
    public static final int BUTTON3 = 3;

    /** It was pressed and released without moving. */
    public static final int MOUSE_CLICKED = 500;

    /** It moved with a button down. */
    public static final int MOUSE_DRAGGED = 506;

    /** The mouse entered the component. */
    public static final int MOUSE_ENTERED = 504;

    /** The mouse left the component. */
    public static final int MOUSE_EXITED = 505;

    /** The family's first identifier. */
    public static final int MOUSE_FIRST = 500;

    /** The family's last identifier. */
    public static final int MOUSE_LAST = 507;

    /** It moved with no button down. */
    public static final int MOUSE_MOVED = 503;

    /** A button was pressed. */
    public static final int MOUSE_PRESSED = 501;

    /** A button was released. */
    public static final int MOUSE_RELEASED = 502;

    /** The wheel was moved. */
    public static final int MOUSE_WHEEL = 507;

    /** No button changed in this event. */
    public static final int NOBUTTON = 0;

    private int x;
    private int y;
    private final int xAbs;
    private final int yAbs;
    private final int clickCount;
    private final boolean popupTrigger;
    private final int button;

    /**
     * With everything given, the position on screen included.
     *
     * @throws IllegalArgumentException if the source is `null`
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
     * Without the position on screen, which is worked out from the component.
     *
     * @throws IllegalArgumentException if the source is `null`
     */
    public MouseEvent(Component source, int id, long when, int modifiers, int x, int y,
            int clickCount, boolean popupTrigger, int button) {
        this(source, id, when, modifiers, x, y, 0, 0, clickCount, popupTrigger, button);
    }

    /**
     * Without saying which button changed.
     *
     * @throws IllegalArgumentException if the source is `null`
     */
    public MouseEvent(Component source, int id, long when, int modifiers, int x, int y,
            int clickCount, boolean popupTrigger) {
        this(source, id, when, modifiers, x, y, 0, 0, clickCount, popupTrigger, NOBUTTON);
    }

    /** The X, relative to the component. */
    public int getX() {
        return this.x;
    }

    /** The Y, relative to the component. */
    public int getY() {
        return this.y;
    }

    /** The X, relative to the screen. */
    public int getXOnScreen() {
        return this.xAbs;
    }

    /** The Y, relative to the screen. */
    public int getYOnScreen() {
        return this.yAbs;
    }

    /** The point, relative to the component. */
    public Point getPoint() {
        return new Point(this.x, this.y);
    }

    /** The point, relative to the screen. */
    public Point getLocationOnScreen() {
        return new Point(this.xAbs, this.yAbs);
    }

    /**
     * Shifts the coordinates relative to the component.
     *
     * <p>It serves to dispatch the same event again from another component, without building a new
     * one.
     */
    public synchronized void translatePoint(int x, int y) {
        this.x = this.x + x;
        this.y = this.y + y;
    }

    /** How many clicks in a row have gone by. */
    public int getClickCount() {
        return this.clickCount;
    }

    /** Which button changed, or {@link #NOBUTTON}. */
    public int getButton() {
        return this.button;
    }

    /** Whether this event is the platform's context-menu gesture. */
    public boolean isPopupTrigger() {
        return this.popupTrigger;
    }

    /** The modifiers in the new encoding. */
    public int getModifiersEx() {
        return super.getModifiersEx();
    }

    /**
     * The modifiers written out for a person.
     *
     * @deprecated it works with the old encoding. Use
     *     {@link InputEvent#getModifiersExText(int)}.
     */
    @Deprecated
    public static String getMouseModifiersText(int modifiers) {
        return InputEvent.getModifiersExText(modifiers);
    }

    public String paramString() {
        String type;
        if (this.id == MOUSE_PRESSED) {
            type = "MOUSE_PRESSED";
        } else if (this.id == MOUSE_RELEASED) {
            type = "MOUSE_RELEASED";
        } else if (this.id == MOUSE_CLICKED) {
            type = "MOUSE_CLICKED";
        } else if (this.id == MOUSE_ENTERED) {
            type = "MOUSE_ENTERED";
        } else if (this.id == MOUSE_EXITED) {
            type = "MOUSE_EXITED";
        } else if (this.id == MOUSE_MOVED) {
            type = "MOUSE_MOVED";
        } else if (this.id == MOUSE_DRAGGED) {
            type = "MOUSE_DRAGGED";
        } else if (this.id == MOUSE_WHEEL) {
            type = "MOUSE_WHEEL";
        } else {
            type = "unknown type";
        }
        return type + ",(" + this.x + "," + this.y + "),absolute(" + this.xAbs + ","
                + this.yAbs + "),button=" + this.button + ",clickCount=" + this.clickCount;
    }
}
