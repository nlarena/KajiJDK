package javax.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.beans.PropertyVetoException;
import java.io.Serializable;

/**
 * El administrador de escritorio que viene puesto.
 *
 * <h2>Donde se guarda el estado</h2>
 *
 * <p>Un administrador atiende a todas las ventanas de un escritorio, asi que no puede guardar en
 * campos propios cosas que son de cada ventana. Las guarda en la ventana misma: el rectangulo de
 * antes de maximizar va en {@link JInternalFrame#setNormalBounds}, y la marca de "ya estuvo
 * minimizada" en una propiedad de cliente. De ahi los cuatro metodos protegidos
 * {@link #setPreviousBounds}, {@link #getPreviousBounds}, {@link #setWasIcon} y {@link #wasIcon}:
 * son el punto donde una subclase puede cambiar ese guardado.
 *
 * <h2>Por que importa "ya estuvo minimizada"</h2>
 *
 * <p>La primera vez que una ventana se minimiza hay que elegirle un lugar al icono; las siguientes
 * hay que respetar el lugar donde el usuario lo dejo. Sin esa marca, cada minimizada le devolveria
 * el icono al rincon.
 *
 * <h2>Como se ubica un icono</h2>
 *
 * <p>{@link #getBoundsForIconOf} va probando lugares en una fila al pie del escritorio y se queda
 * con el primero que no pisa a otro icono; cuando se llena la fila sube una. Es un acomodo simple y
 * a proposito: cualquier cosa mas fina depende del aspecto.
 */
public class DefaultDesktopManager implements DesktopManager, Serializable {

    static final String HAS_BEEN_ICONIFIED_PROPERTY = "wasIconOnce";

    static final int DEFAULT_DRAG_MODE = 0;
    static final int OUTLINE_DRAG_MODE = 1;
    static final int FASTER_DRAG_MODE = 2;

    int dragMode = DEFAULT_DRAG_MODE;

    private Rectangle currentBounds = null;

    /** El administrador de siempre. */
    public DefaultDesktopManager() {
    }

    /**
     * Muestra la ventana en lugar de su icono.
     *
     * <p>Solo hace algo si el icono estaba puesto: abrir una ventana que ya esta abierta no tiene
     * que sacarla de donde esta.
     */
    public void openFrame(JInternalFrame f) {
        if (f.getDesktopIcon().getParent() != null) {
            f.getDesktopIcon().getParent().add(f);
            removeIconFor(f);
        }
    }

    /**
     * Saca la ventana del escritorio.
     *
     * <p>Si era la activa, primero la desactiva: dejar el escritorio apuntando a una ventana que ya
     * no esta seria un fantasma. Tambien se olvida el rectangulo guardado y la marca de minimizada,
     * porque una ventana cerrada que se vuelva a agregar empieza de cero.
     */
    public void closeFrame(JInternalFrame f) {
        JDesktopPane d = f.getDesktopPane();
        boolean estabaActiva = f.isSelected();
        Container c = f.getParent();
        if (estabaActiva) {
            try {
                f.setSelected(false);
            } catch (PropertyVetoException e2) {
                // Se cierra igual: la ventana se va del escritorio.
            }
        }
        if (c != null) {
            Rectangle r = f.getBounds();
            c.remove(f);
            c.repaint(r.x, r.y, r.width, r.height);
        }
        removeIconFor(f);
        if (getPreviousBounds(f) != null) {
            setPreviousBounds(f, null);
        }
        if (wasIcon(f)) {
            setWasIcon(f, null);
        }
        if (estabaActiva && d != null) {
            d.setSelectedFrame(null);
        }
    }

    /**
     * Agranda la ventana a todo el escritorio.
     *
     * <p>Una minimizada primero se restituye: no se puede maximizar un icono.
     */
    public void maximizeFrame(JInternalFrame f) {
        if (f.isIcon()) {
            try {
                f.setIcon(false);
            } catch (PropertyVetoException e2) {
                // Si no se puede restituir tampoco se puede maximizar.
                return;
            }
        } else {
            setPreviousBounds(f, f.getBounds());
            Container padre = f.getParent();
            if (padre != null) {
                Rectangle limites = padre.getBounds();
                setBoundsForFrame(f, 0, 0, limites.width, limites.height);
            }
        }
        try {
            f.setSelected(true);
        } catch (PropertyVetoException e2) {
            // Queda maximizada aunque no se pueda activar.
        }
    }

    /** La devuelve al rectangulo que tenia antes de maximizarse. */
    public void minimizeFrame(JInternalFrame f) {
        Rectangle r = getPreviousBounds(f);
        if (r != null) {
            setPreviousBounds(f, null);
            try {
                f.setSelected(true);
            } catch (PropertyVetoException e2) {
                // Vuelve a su tamano aunque no se pueda activar.
            }
            setBoundsForFrame(f, r.x, r.y, r.width, r.height);
        }
    }

    /**
     * Reemplaza la ventana por su icono.
     *
     * <p>El icono hereda la capa de la ventana: si no, una ventana de la capa modal se minimizaria
     * a un icono que queda debajo de las demas.
     */
    public void iconifyFrame(JInternalFrame f) {
        JInternalFrame.JDesktopIcon icono = f.getDesktopIcon();
        Container c = f.getParent();
        JDesktopPane d = f.getDesktopPane();
        boolean estabaActiva = f.isSelected();
        if (c == null) {
            return;
        }
        if (!wasIcon(f)) {
            // Primera vez: hay que elegirle un lugar. Ver la nota de la clase.
            Rectangle r = getBoundsForIconOf(f);
            icono.setBounds(r.x, r.y, r.width, r.height);
            setWasIcon(f, Boolean.TRUE);
        }
        if (c instanceof JLayeredPane) {
            JLayeredPane lp = (JLayeredPane) c;
            int capa = lp.getLayer(f);
            JLayeredPane.putLayer(icono, capa);
        }
        Rectangle r = f.getBounds();
        c.remove(f);
        c.add(icono);
        c.repaint(r.x, r.y, r.width, r.height);
        if (estabaActiva) {
            try {
                f.setSelected(false);
            } catch (PropertyVetoException e2) {
                // Se minimiza igual.
            }
            if (d != null) {
                d.setSelectedFrame(null);
            }
        }
    }

    /** Devuelve la ventana en lugar de su icono. */
    public void deiconifyFrame(JInternalFrame f) {
        JInternalFrame.JDesktopIcon icono = f.getDesktopIcon();
        Container c = icono.getParent();
        if (c == null) {
            return;
        }
        c.add(f);
        removeIconFor(f);
        try {
            f.setSelected(true);
        } catch (PropertyVetoException e2) {
            // Vuelve igual aunque no se pueda activar.
        }
    }

    /**
     * La ventana paso a ser la activa.
     *
     * <p>Desactivar la anterior es parte del trabajo: dos ventanas con barra de titulo encendida
     * al mismo tiempo es exactamente lo que este metodo evita.
     */
    public void activateFrame(JInternalFrame f) {
        Container p = f.getParent();
        JDesktopPane d = f.getDesktopPane();
        JInternalFrame activa = (d == null) ? null : d.getSelectedFrame();
        if (p == null) {
            return;
        }
        if (activa == null) {
            if (d != null) {
                d.setSelectedFrame(f);
            }
        } else if (activa != f) {
            if (activa.isSelected()) {
                try {
                    activa.setSelected(false);
                } catch (PropertyVetoException e2) {
                    // Si la anterior se niega, la nueva se activa igual.
                }
            }
            if (d != null) {
                d.setSelectedFrame(f);
            }
        }
        f.moveToFront();
    }

    /** La ventana dejo de ser la activa. */
    public void deactivateFrame(JInternalFrame f) {
        JDesktopPane d = f.getDesktopPane();
        JInternalFrame activa = (d == null) ? null : d.getSelectedFrame();
        if (activa == f) {
            d.setSelectedFrame(null);
        }
    }

    /** Empieza un arrastre; toma el modo del escritorio. */
    public void beginDraggingFrame(JComponent f) {
        JDesktopPane d = getDesktopPane(f);
        if (d != null && d.getDragMode() == JDesktopPane.OUTLINE_DRAG_MODE) {
            dragMode = OUTLINE_DRAG_MODE;
        } else {
            dragMode = DEFAULT_DRAG_MODE;
        }
        currentBounds = f.getBounds();
    }

    /**
     * El arrastre va por esa posicion.
     *
     * <p>En modo contorno solo se anota a donde va: mover de verdad se hace al soltar.
     */
    public void dragFrame(JComponent f, int newX, int newY) {
        if (dragMode == OUTLINE_DRAG_MODE) {
            currentBounds = new Rectangle(newX, newY, f.getWidth(), f.getHeight());
        } else {
            setBoundsForFrame(f, newX, newY, f.getWidth(), f.getHeight());
        }
    }

    /** Termina el arrastre; en modo contorno recien aca se mueve. */
    public void endDraggingFrame(JComponent f) {
        if (dragMode == OUTLINE_DRAG_MODE && currentBounds != null) {
            setBoundsForFrame(f, currentBounds.x, currentBounds.y, currentBounds.width,
                    currentBounds.height);
        }
        currentBounds = null;
    }

    /** Empieza a redimensionar; el mismo esquema que el arrastre. */
    public void beginResizingFrame(JComponent f, int direction) {
        JDesktopPane d = getDesktopPane(f);
        if (d != null && d.getDragMode() == JDesktopPane.OUTLINE_DRAG_MODE) {
            dragMode = OUTLINE_DRAG_MODE;
        } else {
            dragMode = DEFAULT_DRAG_MODE;
        }
        currentBounds = f.getBounds();
    }

    /** El redimensionado va por ese rectangulo. */
    public void resizeFrame(JComponent f, int newX, int newY, int newWidth, int newHeight) {
        if (dragMode == OUTLINE_DRAG_MODE) {
            currentBounds = new Rectangle(newX, newY, newWidth, newHeight);
        } else {
            setBoundsForFrame(f, newX, newY, newWidth, newHeight);
        }
    }

    /** Termina el redimensionado. */
    public void endResizingFrame(JComponent f) {
        if (dragMode == OUTLINE_DRAG_MODE && currentBounds != null) {
            setBoundsForFrame(f, currentBounds.x, currentBounds.y, currentBounds.width,
                    currentBounds.height);
        }
        currentBounds = null;
    }

    /**
     * Mueve y redimensiona la ventana.
     *
     * <p>Se repinta el rectangulo viejo <em>y</em> el nuevo: repintar solo el nuevo dejaria pintado
     * el rastro del lugar de donde salio.
     */
    public void setBoundsForFrame(JComponent f, int newX, int newY, int newWidth, int newHeight) {
        boolean cambioTamano = (f.getWidth() != newWidth || f.getHeight() != newHeight);
        Rectangle antes = f.getBounds();
        f.setBounds(newX, newY, newWidth, newHeight);
        if (cambioTamano) {
            f.validate();
        }
        Container padre = f.getParent();
        if (padre != null) {
            padre.repaint(antes.x, antes.y, antes.width, antes.height);
            padre.repaint(newX, newY, newWidth, newHeight);
        }
    }

    /** Saca el icono del escritorio. */
    protected void removeIconFor(JInternalFrame f) {
        JInternalFrame.JDesktopIcon di = f.getDesktopIcon();
        Container c = di.getParent();
        if (c != null) {
            Rectangle r = di.getBounds();
            c.remove(di);
            c.repaint(r.x, r.y, r.width, r.height);
        }
    }

    /**
     * Elige lugar para el icono de esa ventana.
     *
     * <p>Prueba lugares en una fila al pie del escritorio y devuelve el primero que no pisa a otro
     * icono; al llenarse la fila sube una. Ver la nota de la clase.
     */
    protected Rectangle getBoundsForIconOf(JInternalFrame f) {
        JInternalFrame.JDesktopIcon icono = f.getDesktopIcon();
        Dimension medida = icono.getPreferredSize();
        Container c = f.getParent();
        if (c == null) {
            c = f.getDesktopIcon().getParent();
        }
        if (c == null) {
            // Todavia no esta en ningun lado: el rincon es tan bueno como cualquier otro.
            return new Rectangle(0, 0, medida.width, medida.height);
        }
        Rectangle limites = c.getBounds();
        Component[] hijos = c.getComponents();
        int w = medida.width;
        int h = medida.height;
        int x = 0;
        int y = limites.height - h;
        Rectangle libre = new Rectangle(x, y, w, h);
        boolean encontrado = false;
        while (!encontrado) {
            libre = new Rectangle(x, y, w, h);
            encontrado = true;
            for (int i = 0; i < hijos.length; i++) {
                JInternalFrame.JDesktopIcon otro = null;
                if (hijos[i] instanceof JInternalFrame) {
                    otro = ((JInternalFrame) hijos[i]).getDesktopIcon();
                } else if (hijos[i] instanceof JInternalFrame.JDesktopIcon) {
                    otro = (JInternalFrame.JDesktopIcon) hijos[i];
                } else {
                    continue;
                }
                if (icono != otro && otro.isVisible()) {
                    if (libre.intersects(otro.getBounds())) {
                        encontrado = false;
                        break;
                    }
                }
            }
            if (!encontrado) {
                x = x + w;
                if (x + w > limites.width) {
                    x = 0;
                    y = y - h;
                }
            }
        }
        return libre;
    }

    /** Guarda el rectangulo de antes de maximizar; ver la nota de la clase. */
    protected void setPreviousBounds(JInternalFrame f, Rectangle r) {
        f.setNormalBounds(r);
    }

    /** El rectangulo guardado, o nulo. */
    protected Rectangle getPreviousBounds(JInternalFrame f) {
        return f.getNormalBounds();
    }

    /** Marca que la ventana ya estuvo minimizada; ver la nota de la clase. */
    protected void setWasIcon(JInternalFrame f, Boolean value) {
        if (value != null) {
            f.putClientProperty(HAS_BEEN_ICONIFIED_PROPERTY, value);
        }
    }

    /** Si ya estuvo minimizada alguna vez. */
    protected boolean wasIcon(JInternalFrame f) {
        return (f.getClientProperty(HAS_BEEN_ICONIFIED_PROPERTY) == Boolean.TRUE);
    }

    /** El escritorio de ese componente, buscando hacia arriba. */
    JDesktopPane getDesktopPane(JComponent frame) {
        JDesktopPane pane = null;
        Component c = frame.getParent();
        while (pane == null) {
            if (c instanceof JDesktopPane) {
                pane = (JDesktopPane) c;
            } else if (c == null) {
                break;
            } else {
                c = c.getParent();
            }
        }
        return pane;
    }
}
