package javax.swing;

import java.awt.Component;
import java.util.Vector;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.DesktopPaneUI;

/**
 * El contenedor de ventanas internas.
 *
 * <h2>Es un panel por capas</h2>
 *
 * <p>Hereda de {@link JLayeredPane} porque las ventanas se superponen y hay que saber cual esta
 * arriba. Lo que agrega es la nocion de <em>ventana activa</em> y un {@link DesktopManager} que
 * decide como se comportan.
 *
 * <h2>Los iconos tambien son hijos</h2>
 *
 * <p>Una ventana minimizada se saca del escritorio y se pone su {@code JDesktopIcon} en su lugar.
 * Por eso {@link #getAllFrames} mira las dos cosas: los hijos que son ventanas y los que son
 * iconos, preguntandole a cada icono por su ventana. Recorrer los hijos buscando solo
 * {@code JInternalFrame} perderia las minimizadas.
 *
 * <h2>Arrastre en vivo o con contorno</h2>
 *
 * <p>{@link #LIVE_DRAG_MODE} redibuja la ventana entera mientras se la mueve;
 * {@link #OUTLINE_DRAG_MODE} dibuja solo un rectangulo y mueve al soltar. El segundo existe para
 * escritorios con muchas ventanas, donde redibujar en vivo se arrastra.
 */
public class JDesktopPane extends JLayeredPane implements Accessible {

    private static final String uiClassID = "DesktopPaneUI";

    /** Se redibuja la ventana entera mientras se la mueve. */
    public static final int LIVE_DRAG_MODE = 0;

    /** Se dibuja solo el contorno y se mueve al soltar. */
    public static final int OUTLINE_DRAG_MODE = 1;

    transient DesktopManager desktopManager;

    private transient JInternalFrame selectedFrame = null;
    private int dragMode = LIVE_DRAG_MODE;
    private boolean dragModeSet = false;

    /** Un escritorio vacio. */
    public JDesktopPane() {
        setFocusCycleRoot(true);
        setOpaque(true);
        updateUI();
    }

    public DesktopPaneUI getUI() {
        return (DesktopPaneUI) ui;
    }

    public void setUI(DesktopPaneUI ui) {
        super.setUI(ui);
    }

    /**
     * Como se ve el arrastre de una ventana.
     *
     * <p>No valida nada, y esto esta medido contra el JDK: su documentacion promete un
     * {@link IllegalArgumentException} para un modo desconocido y el codigo no lo lanza. Se copia
     * el codigo y no la promesa -- un modo raro se guarda y lo ignora el aspecto.
     */
    public void setDragMode(int dragMode) {
        int oldDragMode = this.dragMode;
        this.dragMode = dragMode;
        firePropertyChange("dragMode", oldDragMode, this.dragMode);
        dragModeSet = true;
    }

    public int getDragMode() {
        return dragMode;
    }

    /** Quien decide como se comportan las ventanas; ver {@link DesktopManager}. */
    public DesktopManager getDesktopManager() {
        return desktopManager;
    }

    public void setDesktopManager(DesktopManager d) {
        DesktopManager oldValue = desktopManager;
        desktopManager = d;
        firePropertyChange("desktopManager", oldValue, desktopManager);
    }

    public void updateUI() {
    }

    public String getUIClassID() {
        return uiClassID;
    }

    /** Todas las ventanas, minimizadas incluidas; ver la nota de la clase. */
    public JInternalFrame[] getAllFrames() {
        Vector<JInternalFrame> vResults = new Vector<JInternalFrame>(10);
        int count = getComponentCount();
        for (int i = 0; i < count; i++) {
            Component next = getComponent(i);
            if (next instanceof JInternalFrame) {
                vResults.addElement((JInternalFrame) next);
            } else if (next instanceof JInternalFrame.JDesktopIcon) {
                JInternalFrame.JDesktopIcon icono = (JInternalFrame.JDesktopIcon) next;
                JInternalFrame tmp = icono.getInternalFrame();
                if (tmp != null) {
                    vResults.addElement(tmp);
                }
            }
        }
        JInternalFrame[] results = new JInternalFrame[vResults.size()];
        vResults.copyInto(results);
        return results;
    }

    /** La ventana activa, o nulo si ninguna lo esta. */
    public JInternalFrame getSelectedFrame() {
        return selectedFrame;
    }

    /**
     * Anota cual es la ventana activa.
     *
     * <p>Solo anota: no la activa. Quien activa es {@link JInternalFrame#setSelected}, y este
     * metodo esta para que el escritorio se entere.
     */
    public void setSelectedFrame(JInternalFrame f) {
        selectedFrame = f;
    }

    /** Las ventanas de esa capa, minimizadas incluidas. */
    public JInternalFrame[] getAllFramesInLayer(int layer) {
        Vector<JInternalFrame> vResults = new Vector<JInternalFrame>(10);
        int count = getComponentCount();
        for (int i = 0; i < count; i++) {
            Component next = getComponent(i);
            if (next instanceof JInternalFrame) {
                JInternalFrame v = (JInternalFrame) next;
                if (v.getLayer() == layer) {
                    vResults.addElement(v);
                }
            } else if (next instanceof JInternalFrame.JDesktopIcon) {
                JInternalFrame.JDesktopIcon icono = (JInternalFrame.JDesktopIcon) next;
                JInternalFrame tmp = icono.getInternalFrame();
                if (tmp != null && tmp.getLayer() == layer) {
                    vResults.addElement(tmp);
                }
            }
        }
        JInternalFrame[] results = new JInternalFrame[vResults.size()];
        vResults.copyInto(results);
        return results;
    }

    /**
     * Activa la ventana siguiente o la anterior.
     *
     * <p>Es lo que hace Ctrl+F6. Da la vuelta al llegar al final: en un escritorio no hay una
     * ventana final en la que quedarse trabado.
     *
     * @return la que quedo activa, o nulo si no hay ninguna.
     */
    public JInternalFrame selectFrame(boolean forward) {
        JInternalFrame[] marcos = getAllFrames();
        if (marcos.length == 0) {
            return null;
        }
        int actual = -1;
        JInternalFrame sel = getSelectedFrame();
        for (int i = 0; i < marcos.length; i++) {
            if (marcos[i] == sel) {
                actual = i;
            }
        }
        int siguiente;
        if (actual < 0) {
            siguiente = forward ? 0 : marcos.length - 1;
        } else {
            siguiente = forward ? actual + 1 : actual - 1;
            if (siguiente >= marcos.length) {
                siguiente = 0;
            } else if (siguiente < 0) {
                siguiente = marcos.length - 1;
            }
        }
        JInternalFrame elegido = marcos[siguiente];
        try {
            elegido.setSelected(true);
            elegido.moveToFront();
        } catch (java.beans.PropertyVetoException e) {
            // Si se veta, la seleccion no cambia; se devuelve igual cual se intento.
        }
        return elegido;
    }

    /**
     * Saca un hijo.
     *
     * <p>Sacar la ventana activa <em>no</em> deja al escritorio sin activa: {@link #getSelectedFrame}
     * sigue devolviendo la que se fue. Parece un descuido y es lo que hace el JDK -- esta medido --,
     * asi que se copia. Quien saca una ventana a mano y quiere el escritorio limpio tiene que
     * llamar el mismo a {@link #setSelectedFrame} con nulo; el administrador de escritorio ya lo
     * hace al cerrar.
     */
    public void remove(Component comp) {
        super.remove(comp);
    }

    /** Saca el hijo de esa posicion; ver {@link #remove(Component)}. */
    public void remove(int index) {
        super.remove(index);
    }

    /** Saca todos; tampoco olvida la activa. Ver {@link #remove(Component)}. */
    public void removeAll() {
        super.removeAll();
    }

    public void setComponentZOrder(Component comp, int index) {
        super.setComponentZOrder(comp, index);
    }

    protected void addImpl(Component comp, Object constraints, int index) {
        super.addImpl(comp, constraints, index);
    }

    protected String paramString() {
        return super.paramString();
    }

    public AccessibleContext getAccessibleContext() {
        return accessibleContext;
    }
}
