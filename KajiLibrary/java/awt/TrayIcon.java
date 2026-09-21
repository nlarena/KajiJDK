package java.awt;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

/**
 * An icon in the system tray, next to the clock.
 *
 * <p>It is not a {@link Component}, and that is the first thing that surprises: it lives in an area
 * the operating system manages, not in the program's tree of windows. That is why it has listeners
 * of its own instead of inheriting them, has no parent, and its menu is a loose {@link PopupMenu}
 * that is not added to anything.
 *
 * <p>The {@link ActionEvent} it fires is the **double click** one (or the single click, depending
 * on the system), not any click at all: ordinary clicks arrive as {@link MouseEvent}. It is the
 * difference between "I was pressed" and "I was chosen".
 *
 * <p><strong>Without a tray it cannot be built.</strong> The three constructors throw
 * {@link HeadlessException}, just as in the JDK. It would be tempting to let it be built —its state
 * is all memory and needs no screen to be kept— but it would buy nothing: an icon nobody can add to
 * any tray is good for nothing and holds up no other class. Where diverging is worth it is in
 * {@link Window}, which does build, because the whole tree of components hangs off it; here nothing
 * hangs off this.
 *
 * <p>The instance methods are declared because they are part of the class, but there is no way to
 * reach them: no instance exists.
 */
public class TrayIcon {

    /** Which kind of notification balloon to show. */
    public static enum MessageType {

        /** An error: error icon. */
        ERROR,

        /** A warning. */
        WARNING,

        /** Information. */
        INFO,

        /** No icon. */
        NONE
    }

    /** The icon's drawing. */
    private Image image;

    /** The menu that comes up with the right button. */
    private PopupMenu popup;

    /** The text that comes up when the mouse passes over it. */
    private String tooltip;

    /** Whether the drawing is scaled to the size of the tray. */
    private boolean autosize;

    /** The command it sends when chosen. */
    private String actionCommand;

    /** The mouse listeners, chained. */
    transient MouseListener mouseListener;

    /** The motion ones. */
    transient MouseMotionListener mouseMotionListener;

    /** The action ones. */
    transient ActionListener actionListener;

    /**
     * An icon with that drawing.
     *
     * <p>The lack of a screen is checked **before** the image, and that is the JDK's order: without
     * a tray there is no icon to build, so the check on the drawing never gets to run. With a
     * screen and a `null` image, on the other hand, the missing drawing is reported.
     *
     * @throws HeadlessException if there is no screen, that is to say always here
     * @throws IllegalArgumentException if there is a screen and the image is `null`
     */
    public TrayIcon(Image image) {
        if (GraphicsEnvironment.isHeadless()) {
            throw new HeadlessException();
        }
        if (image == null) {
            throw new IllegalArgumentException("creating TrayIcon with null Image");
        }
        this.image = image;
    }

    /**
     * An icon with that drawing and that tooltip.
     *
     * @throws HeadlessException if there is no screen
     */
    public TrayIcon(Image image, String tooltip) {
        this(image);
        this.tooltip = tooltip;
    }

    /**
     * An icon with that drawing, that tooltip and that menu.
     *
     * @throws HeadlessException if there is no screen
     */
    public TrayIcon(Image image, String tooltip, PopupMenu popup) {
        this(image, tooltip);
        this.popup = popup;
    }

    /**
     * Changes the drawing.
     *
     * <p>The previous image is **not** released: whoever created it is still its owner and may be
     * using it somewhere else.
     *
     * @throws NullPointerException if the image is `null`
     */
    public void setImage(Image image) {
        if (image == null) {
            throw new NullPointerException("setting null Image");
        }
        this.image = image;
    }

    /** The drawing. */
    public Image getImage() {
        return this.image;
    }

    /**
     * Changes the right-button menu.
     *
     * <p>A menu that already belongs to **another** icon is ignored: a `PopupMenu` cannot be in two
     * places, and stealing it from the other one would be worse than doing nothing.
     *
     * @param popup the menu, or `null` to take it away
     */
    public void setPopupMenu(PopupMenu popup) {
        if (popup == this.popup) {
            return;
        }
        synchronized (TrayIcon.class) {
            if (popup != null && popup.trayOwner != null && popup.trayOwner != this) {
                return;
            }
            if (this.popup != null) {
                this.popup.trayOwner = null;
            }
            if (popup != null) {
                popup.trayOwner = this;
            }
            this.popup = popup;
        }
    }

    /**
     * The right-button menu.
     *
     * @return the menu, or `null` if it has none
     */
    public PopupMenu getPopupMenu() {
        return this.popup;
    }

    /**
     * Changes the tooltip.
     *
     * @param tooltip the text, or `null` to show none
     */
    public void setToolTip(String tooltip) {
        this.tooltip = tooltip;
    }

    /**
     * The tooltip.
     *
     * @return the text, or `null`
     */
    public String getToolTip() {
        return this.tooltip;
    }

    /**
     * Says whether to scale the drawing to the size of the tray.
     *
     * <p>With `false` —the default— the image is cropped or padded, which is what is right when it
     * already comes at the exact size: scaling an image that already fits only dirties it.
     */
    public void setImageAutoSize(boolean autosize) {
        this.autosize = autosize;
    }

    /** Whether the drawing is scaled. */
    public boolean isImageAutoSize() {
        return this.autosize;
    }

    /** Adds a mouse listener; `null` does nothing. */
    public synchronized void addMouseListener(MouseListener listener) {
        if (listener == null) {
            return;
        }
        this.mouseListener = AWTEventMulticaster.add(this.mouseListener, listener);
    }

    /** Removes a mouse listener. */
    public synchronized void removeMouseListener(MouseListener listener) {
        if (listener == null) {
            return;
        }
        this.mouseListener = AWTEventMulticaster.remove(this.mouseListener, listener);
    }

    /** The mouse listeners. */
    public synchronized MouseListener[] getMouseListeners() {
        return AWTEventMulticaster.getListeners(this.mouseListener, MouseListener.class);
    }

    /** Adds a motion listener; `null` does nothing. */
    public synchronized void addMouseMotionListener(MouseMotionListener listener) {
        if (listener == null) {
            return;
        }
        this.mouseMotionListener = AWTEventMulticaster.add(this.mouseMotionListener, listener);
    }

    /** Removes a motion listener. */
    public synchronized void removeMouseMotionListener(MouseMotionListener listener) {
        if (listener == null) {
            return;
        }
        this.mouseMotionListener = AWTEventMulticaster.remove(this.mouseMotionListener, listener);
    }

    /** The motion listeners. */
    public synchronized MouseMotionListener[] getMouseMotionListeners() {
        return AWTEventMulticaster.getListeners(this.mouseMotionListener,
                MouseMotionListener.class);
    }

    /**
     * The command it sends when chosen.
     *
     * @return the command, or `null` if none was set
     */
    public String getActionCommand() {
        return this.actionCommand;
    }

    /** Sets the command it sends when chosen. */
    public void setActionCommand(String command) {
        this.actionCommand = command;
    }

    /** Adds an action listener; `null` does nothing. */
    public synchronized void addActionListener(ActionListener listener) {
        if (listener == null) {
            return;
        }
        this.actionListener = AWTEventMulticaster.add(this.actionListener, listener);
    }

    /** Removes an action listener. */
    public synchronized void removeActionListener(ActionListener listener) {
        if (listener == null) {
            return;
        }
        this.actionListener = AWTEventMulticaster.remove(this.actionListener, listener);
    }

    /** The action listeners. */
    public synchronized ActionListener[] getActionListeners() {
        return AWTEventMulticaster.getListeners(this.actionListener, ActionListener.class);
    }

    /**
     * Shows a notification balloon next to the icon.
     *
     * <p>It does nothing: the balloon is drawn by the operating system in its tray, and there is no
     * tray. It does not throw either, because the JDK does not throw when the system does not
     * support balloons —a notice that is not seen is no reason to break the program—.
     *
     * @throws NullPointerException if the caption and the text are both `null`
     */
    public void displayMessage(String caption, String text, MessageType messageType) {
        if (caption == null && text == null) {
            throw new NullPointerException("displaying the message with both caption and text being null");
        }
    }

    /**
     * How big the icon is in the tray.
     *
     * @return whatever {@link SystemTray#getTrayIconSize} says
     */
    public Dimension getSize() {
        return SystemTray.getSystemTray().getTrayIconSize();
    }

    /** Tells whichever listeners correspond. */
    void processEvent(AWTEvent e) {
        if (e instanceof ActionEvent) {
            this.processActionEvent((ActionEvent) e);
        } else if (e instanceof MouseEvent) {
            MouseEvent me = (MouseEvent) e;
            int id = me.getID();
            if (id == MouseEvent.MOUSE_MOVED || id == MouseEvent.MOUSE_DRAGGED) {
                this.processMouseMotionEvent(me);
            } else {
                this.processMouseEvent(me);
            }
        }
    }

    /** Tells the mouse listeners. */
    void processMouseEvent(MouseEvent e) {
        MouseListener l = this.mouseListener;
        if (l == null) {
            return;
        }
        int id = e.getID();
        if (id == MouseEvent.MOUSE_PRESSED) {
            l.mousePressed(e);
        } else if (id == MouseEvent.MOUSE_RELEASED) {
            l.mouseReleased(e);
        } else if (id == MouseEvent.MOUSE_CLICKED) {
            l.mouseClicked(e);
        } else if (id == MouseEvent.MOUSE_ENTERED) {
            l.mouseEntered(e);
        } else if (id == MouseEvent.MOUSE_EXITED) {
            l.mouseExited(e);
        }
    }

    /** Tells the motion ones. */
    void processMouseMotionEvent(MouseEvent e) {
        MouseMotionListener l = this.mouseMotionListener;
        if (l == null) {
            return;
        }
        if (e.getID() == MouseEvent.MOUSE_MOVED) {
            l.mouseMoved(e);
        } else if (e.getID() == MouseEvent.MOUSE_DRAGGED) {
            l.mouseDragged(e);
        }
    }

    /** Tells the action ones. */
    void processActionEvent(ActionEvent e) {
        ActionListener l = this.actionListener;
        if (l != null) {
            l.actionPerformed(e);
        }
    }
}
