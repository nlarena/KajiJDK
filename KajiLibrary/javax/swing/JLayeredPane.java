package javax.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.Hashtable;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;

/**
 * A container with layers: what is in a high layer covers what is in a low one.
 *
 * <h2>The layer is not a separate list</h2>
 *
 * <p>Every child goes in the container's same list; what the layer does is decide at which
 * position of that list each one goes in. Those of a high layer go in front -- a small index --,
 * which in AWT is what is drawn on top.
 *
 * <p>That is why {@link #getIndexOf} and {@link #getPosition} are different: the first is the
 * absolute index among every child, the second the position within its layer.
 *
 * <h2>The six named layers</h2>
 *
 * <p>They are not the only ones -- the layer is any integer --, but they are the ones Swing
 * uses and it is worth respecting them: a drop-down menu drawn in the internal frames' layer is
 * going to end up covered by the first frame that moves.
 *
 * <h2>Why it draws itself</h2>
 *
 * <p>{@link #isOptimizedDrawingEnabled} returns false: the layers overlap by definition, and
 * the repainting system has to draw them in order instead of choosing one.
 */
public class JLayeredPane extends JComponent implements Accessible {

    /** The layer of ordinary things. */
    public static final Integer DEFAULT_LAYER = Integer.valueOf(0);

    /** The floating palettes'. */
    public static final Integer PALETTE_LAYER = Integer.valueOf(100);

    /** The modal dialogs'. */
    public static final Integer MODAL_LAYER = Integer.valueOf(200);

    /** The drop-down menus'. */
    public static final Integer POPUP_LAYER = Integer.valueOf(300);

    /** That of what is being dragged; it is the highest. */
    public static final Integer DRAG_LAYER = Integer.valueOf(400);

    /** That of a window's content; it is the lowest. */
    public static final Integer FRAME_CONTENT_LAYER = Integer.valueOf(-30000);

    /** The property a component may carry its layer with. */
    public static final String LAYER_PROPERTY = "layeredContainerLayer";

    private Hashtable<Component, Integer> componentToLayer;
    private boolean optimizedDrawingPossible = true;

    /** An empty layered pane. */
    public JLayeredPane() {
        setLayout(null);
    }

    /**
     * It adds a child in the layer that applies.
     *
     * <p>The layer comes from the constraint if it is an {@link Integer}, from the component's
     * property if it has it, or from the usual layer. That order matters: it is what allows the
     * layer to be set in the component and it to be added afterwards without repeating it.
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

    /** Always false; see the class note. */
    public boolean isOptimizedDrawingEnabled() {
        return false;
    }

    /**
     * It keeps the layer in the component itself.
     *
     * <p>It is static and changes nothing of the pane: it serves to mark a component
     * <em>before</em> adding it. Adding it afterwards puts it where the mark says.
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

    /** The nearest layered pane above that component, or null. */
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

    /** It puts the component in that layer, at the front of it. */
    public void setLayer(Component c, int layer) {
        setLayer(c, layer, -1);
    }

    /**
     * It puts the component in that layer and at that position within it.
     *
     * <p>If it was already added, it is taken out and added again: the layer decides the index
     * among the children, and changing it is changing place in the list.
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

    /** That component's layer. */
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

    /** The index among every child, or -1; see the class note. */
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

    /** It takes it to the front of its layer; it does not change its layer. */
    public void moveToFront(Component c) {
        setPosition(c, 0);
    }

    public void moveToBack(Component c) {
        setPosition(c, -1);
    }

    /** It puts it at that position within its layer; -1 is at the back. */
    public void setPosition(Component c, int position) {
        setLayer(c, getLayer(c), position);
    }

    /** The position within its layer, or -1 if it is not there. */
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

    /** The highest layer in use, or the usual one if there are no children. */
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
                // The children are sorted by layer: past the layer, there are no more.
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

    /**
     * It draws the background and afterwards the children, from the lowest layer to the highest.
     */
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
     * The table of layers of the components that are not Swing's.
     *
     * <p>Swing's carry it as a client property. AWT's have nowhere to, and that is why this
     * separate table is needed.
     */
    protected Hashtable<Component, Integer> getComponentToLayer() {
        if (componentToLayer == null) {
            componentToLayer = new Hashtable<Component, Integer>(4);
        }
        return componentToLayer;
    }

    /** That layer's {@link Integer}, reusing the six named ones. */
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
     * At which index among the children something of that layer goes in.
     *
     * <p>The children are sorted by layer from highest to lowest, so it is enough to walk until
     * the first lower layer is found.
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
                    // The new layer is the highest: it goes first.
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
