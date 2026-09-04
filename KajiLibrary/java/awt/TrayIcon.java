package java.awt;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

/**
 * Un ícono en la bandeja del sistema, al lado del reloj.
 *
 * <p>No es un {@link Component}, y eso es lo primero que sorprende: vive en una zona que maneja el
 * sistema operativo, no en el árbol de ventanas del programa. Por eso tiene sus propios oyentes en
 * vez de heredarlos, no tiene padre, y su menú es un {@link PopupMenu} suelto que no está agregado a
 * nada.
 *
 * <p>El {@link ActionEvent} que dispara es el del **doble clic** (o el clic simple, según el
 * sistema), no el de cualquier clic: los clics comunes llegan como {@link MouseEvent}. Es la
 * diferencia entre "me apretaron" y "me eligieron".
 *
 * <p><strong>Sin bandeja no se puede construir.</strong> Los tres constructores tiran
 * {@link HeadlessException}, igual que en el JDK. Sería tentador dejarlo construir —su estado es todo
 * memoria y no necesita pantalla para guardarse— pero no compraría nada: un ícono que nadie puede
 * agregar a ninguna bandeja no sirve para nada ni sostiene a ninguna otra clase. Donde sí vale la
 * pena divergir es en {@link Window}, que construye igual porque de ella cuelga todo el árbol de
 * componentes; acá no cuelga nadie.
 *
 * <p>Los métodos de instancia están declarados porque son parte de la clase, pero no hay forma de
 * llegar a ellos: no existe ninguna instancia.
 */
public class TrayIcon {

    /** Qué tipo de globo de aviso mostrar. */
    public static enum MessageType {

        /** Un error: ícono de error. */
        ERROR,

        /** Una advertencia. */
        WARNING,

        /** Información. */
        INFO,

        /** Sin ícono. */
        NONE
    }

    /** El dibujo del ícono. */
    private Image image;

    /** El menú que sale con el botón derecho. */
    private PopupMenu popup;

    /** El texto que sale al pasar el mouse por encima. */
    private String tooltip;

    /** Si el dibujo se escala al tamaño de la bandeja. */
    private boolean autosize;

    /** El comando que manda al elegirlo. */
    private String actionCommand;

    /** Los oyentes de mouse, encadenados. */
    transient MouseListener mouseListener;

    /** Los de movimiento. */
    transient MouseMotionListener mouseMotionListener;

    /** Los de acción. */
    transient ActionListener actionListener;

    /**
     * Un ícono con ese dibujo.
     *
     * <p>La falta de pantalla se comprueba **antes** que la imagen, y ése es el orden del JDK: sin
     * bandeja no hay ícono que armar, así que la validación del dibujo ni llega a correr. Con
     * pantalla y una imagen `null`, en cambio, sí se avisa que el dibujo falta.
     *
     * @throws HeadlessException si no hay pantalla, o sea siempre acá
     * @throws IllegalArgumentException si hay pantalla y la imagen es `null`
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
     * Un ícono con ese dibujo y ese texto emergente.
     *
     * @throws HeadlessException si no hay pantalla
     */
    public TrayIcon(Image image, String tooltip) {
        this(image);
        this.tooltip = tooltip;
    }

    /**
     * Un ícono con ese dibujo, ese texto emergente y ese menú.
     *
     * @throws HeadlessException si no hay pantalla
     */
    public TrayIcon(Image image, String tooltip, PopupMenu popup) {
        this(image, tooltip);
        this.popup = popup;
    }

    /**
     * Cambia el dibujo.
     *
     * <p>La imagen anterior **no** se libera: quien la creó sigue siendo su dueño y puede estar
     * usándola en otro lado.
     *
     * @throws NullPointerException si la imagen es `null`
     */
    public void setImage(Image image) {
        if (image == null) {
            throw new NullPointerException("setting null Image");
        }
        this.image = image;
    }

    /** El dibujo. */
    public Image getImage() {
        return this.image;
    }

    /**
     * Cambia el menú del botón derecho.
     *
     * <p>Un menú que ya es de **otro** ícono se ignora: un `PopupMenu` no puede estar en dos lugares,
     * y robárselo al otro sería peor que no hacer nada.
     *
     * @param popup el menú, o `null` para sacarlo
     */
    public void setPopupMenu(PopupMenu popup) {
        if (popup == this.popup) {
            return;
        }
        synchronized (TrayIcon.class) {
            if (popup != null && popup.duenoDeBandeja != null && popup.duenoDeBandeja != this) {
                return;
            }
            if (this.popup != null) {
                this.popup.duenoDeBandeja = null;
            }
            if (popup != null) {
                popup.duenoDeBandeja = this;
            }
            this.popup = popup;
        }
    }

    /**
     * El menú del botón derecho.
     *
     * @return el menú, o `null` si no tiene
     */
    public PopupMenu getPopupMenu() {
        return this.popup;
    }

    /**
     * Cambia el texto emergente.
     *
     * @param tooltip el texto, o `null` para no mostrar ninguno
     */
    public void setToolTip(String tooltip) {
        this.tooltip = tooltip;
    }

    /**
     * El texto emergente.
     *
     * @return el texto, o `null`
     */
    public String getToolTip() {
        return this.tooltip;
    }

    /**
     * Dice si escalar el dibujo al tamaño de la bandeja.
     *
     * <p>Con `false` —lo de fábrica— la imagen se recorta o se rellena, que es lo correcto cuando ya
     * viene del tamaño justo: escalar una imagen que ya encaja sólo la ensucia.
     */
    public void setImageAutoSize(boolean autosize) {
        this.autosize = autosize;
    }

    /** Si el dibujo se escala. */
    public boolean isImageAutoSize() {
        return this.autosize;
    }

    /** Agrega un oyente de mouse; `null` no hace nada. */
    public synchronized void addMouseListener(MouseListener listener) {
        if (listener == null) {
            return;
        }
        this.mouseListener = AWTEventMulticaster.add(this.mouseListener, listener);
    }

    /** Saca un oyente de mouse. */
    public synchronized void removeMouseListener(MouseListener listener) {
        if (listener == null) {
            return;
        }
        this.mouseListener = AWTEventMulticaster.remove(this.mouseListener, listener);
    }

    /** Los oyentes de mouse. */
    public synchronized MouseListener[] getMouseListeners() {
        return AWTEventMulticaster.getListeners(this.mouseListener, MouseListener.class);
    }

    /** Agrega un oyente de movimiento; `null` no hace nada. */
    public synchronized void addMouseMotionListener(MouseMotionListener listener) {
        if (listener == null) {
            return;
        }
        this.mouseMotionListener = AWTEventMulticaster.add(this.mouseMotionListener, listener);
    }

    /** Saca un oyente de movimiento. */
    public synchronized void removeMouseMotionListener(MouseMotionListener listener) {
        if (listener == null) {
            return;
        }
        this.mouseMotionListener = AWTEventMulticaster.remove(this.mouseMotionListener, listener);
    }

    /** Los oyentes de movimiento. */
    public synchronized MouseMotionListener[] getMouseMotionListeners() {
        return AWTEventMulticaster.getListeners(this.mouseMotionListener,
                MouseMotionListener.class);
    }

    /**
     * El comando que manda al elegirlo.
     *
     * @return el comando, o `null` si no se fijó ninguno
     */
    public String getActionCommand() {
        return this.actionCommand;
    }

    /** Fija el comando que manda al elegirlo. */
    public void setActionCommand(String command) {
        this.actionCommand = command;
    }

    /** Agrega un oyente de acción; `null` no hace nada. */
    public synchronized void addActionListener(ActionListener listener) {
        if (listener == null) {
            return;
        }
        this.actionListener = AWTEventMulticaster.add(this.actionListener, listener);
    }

    /** Saca un oyente de acción. */
    public synchronized void removeActionListener(ActionListener listener) {
        if (listener == null) {
            return;
        }
        this.actionListener = AWTEventMulticaster.remove(this.actionListener, listener);
    }

    /** Los oyentes de acción. */
    public synchronized ActionListener[] getActionListeners() {
        return AWTEventMulticaster.getListeners(this.actionListener, ActionListener.class);
    }

    /**
     * Muestra un globo de aviso al lado del ícono.
     *
     * <p>No hace nada: el globo lo dibuja el sistema operativo en su bandeja, y no hay bandeja.
     * Tampoco tira, porque el JDK tampoco lo hace cuando el sistema no admite globos —un aviso que no
     * se ve no es motivo para romper el programa—.
     *
     * @throws NullPointerException si el título y el texto son los dos `null`
     */
    public void displayMessage(String caption, String text, MessageType messageType) {
        if (caption == null && text == null) {
            throw new NullPointerException("displaying the message with both caption and text being null");
        }
    }

    /**
     * Cuánto mide el ícono en la bandeja.
     *
     * @return lo que diga {@link SystemTray#getTrayIconSize}
     */
    public Dimension getSize() {
        return SystemTray.getSystemTray().getTrayIconSize();
    }

    /** Les avisa a los oyentes que corresponda. */
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

    /** Les avisa a los oyentes de mouse. */
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

    /** Les avisa a los de movimiento. */
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

    /** Les avisa a los de acción. */
    void processActionEvent(ActionEvent e) {
        ActionListener l = this.actionListener;
        if (l != null) {
            l.actionPerformed(e);
        }
    }
}
