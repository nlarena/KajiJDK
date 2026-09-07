package javax.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.Hashtable;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;

/**
 * Un contenedor con capas: lo de una capa alta tapa a lo de una baja.
 *
 * <h2>La capa no es una lista aparte</h2>
 *
 * <p>Todos los hijos van en la misma lista del contenedor; lo que hace la capa es decidir en que
 * posicion de esa lista entra cada uno. Los de capa alta van adelante -- indice chico --, que en
 * AWT es lo que se dibuja encima.
 *
 * <p>Por eso {@link #getIndexOf} y {@link #getPosition} son distintos: el primero es el indice
 * absoluto entre todos los hijos, el segundo la posicion dentro de su capa.
 *
 * <h2>Las seis capas con nombre</h2>
 *
 * <p>No son las unicas -- la capa es cualquier entero --, pero son las que Swing usa y conviene
 * respetar: un menu desplegable que se dibuje en la capa de las ventanas internas va a quedar
 * tapado por la primera ventana que se mueva.
 *
 * <h2>Por que dibuja el mismo</h2>
 *
 * <p>{@link #isOptimizedDrawingEnabled} devuelve falso: las capas se pisan por definicion, y el
 * sistema de repintado tiene que dibujarlas en orden en lugar de elegir una.
 */
public class JLayeredPane extends JComponent implements Accessible {

    /** La capa de lo normal. */
    public static final Integer DEFAULT_LAYER = Integer.valueOf(0);

    /** La de las paletas flotantes. */
    public static final Integer PALETTE_LAYER = Integer.valueOf(100);

    /** La de los dialogos modales. */
    public static final Integer MODAL_LAYER = Integer.valueOf(200);

    /** La de los menus desplegables. */
    public static final Integer POPUP_LAYER = Integer.valueOf(300);

    /** La de lo que se esta arrastrando; es la mas alta. */
    public static final Integer DRAG_LAYER = Integer.valueOf(400);

    /** La del contenido de una ventana; es la mas baja. */
    public static final Integer FRAME_CONTENT_LAYER = Integer.valueOf(-30000);

    /** La propiedad con la que un componente puede llevar su capa. */
    public static final String LAYER_PROPERTY = "layeredContainerLayer";

    private Hashtable<Component, Integer> componentToLayer;
    private boolean optimizedDrawingPossible = true;
    private AccessibleContext accessibleContext;

    /** Un panel de capas vacio. */
    public JLayeredPane() {
        setLayout(null);
    }

    /**
     * Agrega un hijo en la capa que corresponda.
     *
     * <p>La capa sale de la restriccion si es un {@link Integer}, de la propiedad del componente si
     * la tiene, o de la capa de siempre. Ese orden importa: es lo que permite poner la capa en el
     * componente y despues agregarlo sin repetirla.
     */
    protected void addImpl(Component comp, Object constraints, int index) {
        int layer;
        int pos;
        if (constraints instanceof Integer) {
            layer = ((Integer) constraints).intValue();
            setLayer(comp, layer);
        } else {
            layer = getLayer(comp);
        }
        pos = insertIndexForLayer(layer, index);
        super.addImpl(comp, constraints, pos);
        comp.validate();
        comp.repaint();
    }

    public void remove(int index) {
        Component c = getComponent(index);
        super.remove(index);
        if (c != null && !(c instanceof JComponent)) {
            getComponentToLayer().remove(c);
        }
    }

    public void removeAll() {
        Component[] children = getComponents();
        Hashtable<Component, Integer> cToL = getComponentToLayer();
        for (int counter = children.length - 1; counter >= 0; counter--) {
            Component c = children[counter];
            if (c != null && !(c instanceof JComponent)) {
                cToL.remove(c);
            }
        }
        super.removeAll();
    }

    /** Siempre falso; ver la nota de la clase. */
    public boolean isOptimizedDrawingEnabled() {
        return false;
    }

    /**
     * Guarda la capa en el componente mismo.
     *
     * <p>Es estatico y no cambia nada del panel: sirve para marcar un componente <em>antes</em> de
     * agregarlo. Agregarlo despues lo pone donde dice la marca.
     */
    public static void putLayer(JComponent c, int layer) {
        c.putClientProperty(LAYER_PROPERTY, Integer.valueOf(layer));
    }

    public static int getLayer(JComponent c) {
        Integer i = (Integer) c.getClientProperty(LAYER_PROPERTY);
        if (i != null) {
            return i.intValue();
        }
        return DEFAULT_LAYER.intValue();
    }

    /** El panel de capas mas cercano arriba de ese componente, o nulo. */
    public static JLayeredPane getLayeredPaneAbove(Component c) {
        if (c == null) {
            return null;
        }
        Component parent = c.getParent();
        while (parent != null && !(parent instanceof JLayeredPane)) {
            parent = parent.getParent();
        }
        return (JLayeredPane) parent;
    }

    /** Pone el componente en esa capa, al frente de ella. */
    public void setLayer(Component c, int layer) {
        setLayer(c, layer, -1);
    }

    /**
     * Pone el componente en esa capa y en esa posicion dentro de ella.
     *
     * <p>Si ya estaba agregado, se lo saca y se lo vuelve a agregar: la capa decide el indice entre
     * los hijos, y cambiarla es cambiar de lugar en la lista.
     */
    public void setLayer(Component c, int layer, int position) {
        Integer layerObj = getObjectForLayer(layer);
        if (layer == getLayer(c) && position == getPosition(c)) {
            repaint(c.getBounds());
            return;
        }
        if (c instanceof JComponent) {
            ((JComponent) c).putClientProperty(LAYER_PROPERTY, layerObj);
        } else {
            getComponentToLayer().put(c, layerObj);
        }
        if (c.getParent() == null || c.getParent() != this) {
            repaint(c.getBounds());
            return;
        }
        int index = insertIndexForLayer(c, layer, position);
        setComponentZOrder(c, index);
        repaint(c.getBounds());
    }

    /** La capa de ese componente. */
    public int getLayer(Component c) {
        Integer i;
        if (c instanceof JComponent) {
            i = (Integer) ((JComponent) c).getClientProperty(LAYER_PROPERTY);
        } else {
            i = getComponentToLayer().get(c);
        }
        if (i == null) {
            return DEFAULT_LAYER.intValue();
        }
        return i.intValue();
    }

    /** El indice entre todos los hijos, o -1; ver la nota de la clase. */
    public int getIndexOf(Component c) {
        int i;
        int count = getComponentCount();
        for (i = 0; i < count; i++) {
            if (c == getComponent(i)) {
                return i;
            }
        }
        return -1;
    }

    /** Lo lleva al frente de su capa; no lo cambia de capa. */
    public void moveToFront(Component c) {
        setPosition(c, 0);
    }

    public void moveToBack(Component c) {
        setPosition(c, -1);
    }

    /** Lo pone en esa posicion dentro de su capa; -1 es al fondo. */
    public void setPosition(Component c, int position) {
        setLayer(c, getLayer(c), position);
    }

    /** La posicion dentro de su capa, o -1 si no esta. */
    public int getPosition(Component c) {
        int i;
        int startLayer;
        int curLayer;
        int startLocation;
        int pos = 0;
        int count = getComponentCount();
        startLocation = getIndexOf(c);
        if (startLocation == -1) {
            return -1;
        }
        startLayer = getLayer(c);
        for (i = startLocation - 1; i >= 0; i--) {
            curLayer = getLayer(getComponent(i));
            if (curLayer == startLayer) {
                pos++;
            } else {
                return pos;
            }
        }
        return pos;
    }

    /** La capa mas alta en uso, o la de siempre si no hay hijos. */
    public int highestLayer() {
        if (getComponentCount() > 0) {
            return getLayer(getComponent(0));
        }
        return 0;
    }

    public int lowestLayer() {
        int count = getComponentCount();
        if (count > 0) {
            return getLayer(getComponent(count - 1));
        }
        return 0;
    }

    public int getComponentCountInLayer(int layer) {
        int i;
        int count = getComponentCount();
        int curLayer;
        int layerCount = 0;
        for (i = 0; i < count; i++) {
            curLayer = getLayer(getComponent(i));
            if (curLayer == layer) {
                layerCount++;
            } else if (layerCount > 0 || curLayer < layer) {
                // Los hijos estan ordenados por capa: pasada la capa, no hay mas.
                break;
            }
        }
        return layerCount;
    }

    public Component[] getComponentsInLayer(int layer) {
        int i;
        int count = getComponentCount();
        int curLayer;
        int layerCount = 0;
        Component[] results = new Component[getComponentCountInLayer(layer)];
        for (i = 0; i < count; i++) {
            curLayer = getLayer(getComponent(i));
            if (curLayer == layer) {
                results[layerCount] = getComponent(i);
                layerCount++;
            } else if (layerCount > 0 || curLayer < layer) {
                break;
            }
        }
        return results;
    }

    /** Dibuja el fondo y despues los hijos, de la capa mas baja a la mas alta. */
    public void paint(Graphics g) {
        if (isOpaque()) {
            Rectangle r = g.getClipBounds();
            java.awt.Color c = getBackground();
            if (c == null) {
                c = java.awt.Color.lightGray;
            }
            g.setColor(c);
            if (r != null) {
                g.fillRect(r.x, r.y, r.width, r.height);
            } else {
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        }
        super.paint(g);
    }

    /**
     * La tabla de capas de los componentes que no son de Swing.
     *
     * <p>Los de Swing la llevan como propiedad de cliente. Los de AWT no tienen donde, y por eso
     * hace falta esta tabla aparte.
     */
    protected Hashtable<Component, Integer> getComponentToLayer() {
        if (componentToLayer == null) {
            componentToLayer = new Hashtable<Component, Integer>(4);
        }
        return componentToLayer;
    }

    /** El {@link Integer} de esa capa, reusando los seis con nombre. */
    protected Integer getObjectForLayer(int layer) {
        if (layer == DEFAULT_LAYER.intValue()) {
            return DEFAULT_LAYER;
        }
        if (layer == PALETTE_LAYER.intValue()) {
            return PALETTE_LAYER;
        }
        if (layer == MODAL_LAYER.intValue()) {
            return MODAL_LAYER;
        }
        if (layer == POPUP_LAYER.intValue()) {
            return POPUP_LAYER;
        }
        if (layer == DRAG_LAYER.intValue()) {
            return DRAG_LAYER;
        }
        if (layer == FRAME_CONTENT_LAYER.intValue()) {
            return FRAME_CONTENT_LAYER;
        }
        return Integer.valueOf(layer);
    }

    /**
     * En que indice entre los hijos entra algo de esa capa.
     *
     * <p>Los hijos estan ordenados por capa de mayor a menor, asi que alcanza con recorrer hasta
     * encontrar la primera capa mas baja.
     */
    protected int insertIndexForLayer(int layer, int position) {
        return insertIndexForLayer(null, layer, position);
    }

    private int insertIndexForLayer(Component comp, int layer, int position) {
        int i;
        int count = getComponentCount();
        int curLayer;
        int layerStart = -1;
        int layerEnd = -1;
        int componentCount = 0;

        java.util.ArrayList<Component> compList =
                new java.util.ArrayList<Component>(count);
        for (int index = 0; index < count; index++) {
            if (getComponent(index) != comp) {
                compList.add(getComponent(index));
            }
        }
        count = compList.size();

        for (i = 0; i < count; i++) {
            curLayer = getLayer(compList.get(i));
            if (layerStart == -1 && curLayer == layer) {
                layerStart = i;
            }
            if (curLayer < layer) {
                if (i == 0) {
                    // La capa nueva es la mas alta: va primera.
                    return 0;
                }
                layerEnd = i;
                break;
            }
        }
        if (layerStart == -1 && layerEnd == -1) {
            return count;
        }
        if (layerStart != -1 && layerEnd == -1) {
            layerEnd = count;
        }
        if (layerStart == -1 && layerEnd != -1) {
            return layerEnd;
        }
        componentCount = layerEnd - layerStart;
        if (position == -1 || position > componentCount) {
            return layerEnd;
        }
        return layerStart + position;
    }

    protected String paramString() {
        return super.paramString();
    }

    public AccessibleContext getAccessibleContext() {
        return accessibleContext;
    }
}
