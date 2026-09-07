package javax.swing.plaf.basic;

import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.Color;
import java.awt.Component;
import java.awt.Component$BaselineResizeBehavior;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.LookAndFeel;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.ScrollPaneUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.metal.MetalBorders$ScrollPaneBorder;

/**
 * El aspecto basico de un panel con barras: mantiene los tres modelos de acuerdo.
 *
 * <h2>Un triangulo de escuchas</h2>
 *
 * <p>Tres cosas pueden cambiar y las tres tienen que reflejarse en las otras:
 *
 * <ul>
 * <li>Cambio la ventana —se movio, o cambio de tamano, o le pusieron otro contenido—: se recalculan
 * el valor y la extension de las dos barras ({@link #syncScrollPaneWithViewport}).
 * <li>Cambio el modelo de una barra —alguien la arrastro—: se mueve la posicion de la ventana.
 * <li>Cambio una propiedad del panel —otra ventana, otra barra, otra politica—: se reengancha lo
 * que corresponda.
 * </ul>
 *
 * <p>El circuito no se realimenta porque cada paso escribe un valor que ya es el que corresponde:
 * al segundo aviso, nada cambia y el modelo no vuelve a avisar.
 *
 * <h2>Lo que no esta</h2>
 *
 * <p>Las acciones por teclado necesitan la tabla del aspecto para saber que tecla hace que, y esa
 * tabla todavia no esta.
 */
public class BasicScrollPaneUI extends ScrollPaneUI implements ScrollPaneConstants {

    protected JScrollPane scrollpane;

    /** La rueda; se registra en el panel, no en la vista. */
    private MouseWheelListener mouseWheelListener;

    /** El escucha compartido; ver {@link #createMouseWheelListener}. */
    private Handler handler;

    protected ChangeListener vsbChangeListener;

    protected ChangeListener hsbChangeListener;

    protected ChangeListener viewportChangeListener;

    protected PropertyChangeListener spPropertyChangeListener;

    private static final ColorUIResource FONDO_POR_OMISION = new ColorUIResource(238, 238, 238);
    private static final ColorUIResource FRENTE_POR_OMISION = new ColorUIResource(51, 51, 51);
    private static final Font FUENTE_POR_OMISION = new FontUIResource("Dialog", Font.PLAIN, 12);

    public BasicScrollPaneUI() {
    }

    /** Un aspecto por panel: guarda el panel y sus cuatro escuchas. */
    public static ComponentUI createUI(JComponent x) {
        return new BasicScrollPaneUI();
    }

    /** Pinta el borde de la ventana, si hay; lo demas lo pintan las piezas. */
    public void paint(Graphics g, JComponent c) {
        Border vpBorder = scrollpane.getViewportBorder();
        if (vpBorder != null) {
            Rectangle r = scrollpane.getViewportBorderBounds();
            vpBorder.paintBorder(scrollpane, g, r.x, r.y, r.width, r.height);
        }
    }

    /** Sin tope: un panel con barras se estira todo lo que le den. */
    public Dimension getMaximumSize(JComponent c) {
        return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    /**
     * Colores, fuente y borde.
     *
     * <p>Son los de {@code ScrollPane.*} medidos en Metal (JDK 25): fondo (238, 238, 238), frente
     * (51, 51, 51), Dialog 12 y el borde de {@code MetalBorders.ScrollPaneBorder}. El borde de la
     * ventana queda en {@code null}, como en Metal.
     */
    protected void installDefaults(JScrollPane scrollpane) {
        Color fondo = scrollpane.getBackground();
        if (fondo == null || fondo instanceof UIResource) {
            scrollpane.setBackground(FONDO_POR_OMISION);
        }
        Color frente = scrollpane.getForeground();
        if (frente == null || frente instanceof UIResource) {
            scrollpane.setForeground(FRENTE_POR_OMISION);
        }
        Font fuente = scrollpane.getFont();
        if (fuente == null || fuente instanceof UIResource) {
            scrollpane.setFont(FUENTE_POR_OMISION);
        }
        Border borde = scrollpane.getBorder();
        if (borde == null || borde instanceof UIResource) {
            scrollpane.setBorder(new MetalBorders$ScrollPaneBorder());
        }
        Border bordeVentana = scrollpane.getViewportBorder();
        if (bordeVentana == null || bordeVentana instanceof UIResource) {
            scrollpane.setViewportBorder(null);
        }
        LookAndFeel.installProperty(scrollpane, "opaque", Boolean.TRUE);
    }

    protected void installListeners(JScrollPane c) {
        vsbChangeListener = createVSBChangeListener();
        hsbChangeListener = createHSBChangeListener();
        viewportChangeListener = createViewportChangeListener();
        spPropertyChangeListener = createPropertyChangeListener();
        mouseWheelListener = createMouseWheelListener();
        scrollpane.addMouseWheelListener(mouseWheelListener);

        JViewport viewport = scrollpane.getViewport();
        JScrollBar vsb = scrollpane.getVerticalScrollBar();
        JScrollBar hsb = scrollpane.getHorizontalScrollBar();

        if (viewport != null) {
            viewport.addChangeListener(viewportChangeListener);
        }
        if (vsb != null) {
            vsb.getModel().addChangeListener(vsbChangeListener);
        }
        if (hsb != null) {
            hsb.getModel().addChangeListener(hsbChangeListener);
        }
        scrollpane.addPropertyChangeListener(spPropertyChangeListener);
    }

    /** Nada: sin {@code InputMap} no hay donde registrar teclas. */
    protected void installKeyboardActions(JScrollPane c) {
    }

    public void installUI(JComponent x) {
        super.installUI(x);
        scrollpane = (JScrollPane) x;
        installDefaults(scrollpane);
        installListeners(scrollpane);
        installKeyboardActions(scrollpane);
    }

    /** Lo instalado queda en el componente; el JDK tampoco lo borra. */
    protected void uninstallDefaults(JScrollPane c) {
        LookAndFeel.uninstallBorder(scrollpane);
        if (scrollpane.getViewportBorder() instanceof UIResource) {
            scrollpane.setViewportBorder(null);
        }
    }

    protected void uninstallListeners(JComponent c) {
        JViewport viewport = scrollpane.getViewport();
        JScrollBar vsb = scrollpane.getVerticalScrollBar();
        JScrollBar hsb = scrollpane.getHorizontalScrollBar();

        if (viewport != null) {
            viewport.removeChangeListener(viewportChangeListener);
        }
        if (vsb != null) {
            vsb.getModel().removeChangeListener(vsbChangeListener);
        }
        if (hsb != null) {
            hsb.getModel().removeChangeListener(hsbChangeListener);
        }
        scrollpane.removePropertyChangeListener(spPropertyChangeListener);

        vsbChangeListener = null;
        hsbChangeListener = null;
        viewportChangeListener = null;
        spPropertyChangeListener = null;
    }

    protected void uninstallKeyboardActions(JScrollPane c) {
    }

    public void uninstallUI(JComponent c) {
        super.uninstallUI(c);
        uninstallDefaults(scrollpane);
        uninstallListeners(c);
        uninstallKeyboardActions(scrollpane);
        scrollpane = null;
    }

    /**
     * Copia a las barras lo que la ventana muestra.
     *
     * <p>El valor es la posicion, la extension es lo que se ve, el maximo es el tamano del
     * contenido y el minimo es cero. Las cabeceras se acompanan en su unico eje: la de filas sigue
     * el desplazamiento vertical y la de columnas el horizontal, y por eso no se van de la
     * pantalla.
     */
    protected void syncScrollPaneWithViewport() {
        JViewport viewport = scrollpane.getViewport();
        JScrollBar vsb = scrollpane.getVerticalScrollBar();
        JScrollBar hsb = scrollpane.getHorizontalScrollBar();
        JViewport rowHead = scrollpane.getRowHeader();
        JViewport colHead = scrollpane.getColumnHeader();
        boolean ltr = scrollpane.getComponentOrientation().isLeftToRight();

        if (viewport != null) {
            Dimension extentSize = viewport.getExtentSize();
            Dimension viewSize = viewport.getViewSize();
            Point viewPosition = viewport.getViewPosition();

            if (vsb != null) {
                int extent = extentSize.height;
                int max = viewSize.height;
                int value = Math.max(0, Math.min(viewPosition.y, max - extent));
                vsb.setValues(value, extent, 0, max);
            }

            if (hsb != null) {
                int extent = extentSize.width;
                int max = viewSize.width;
                int value = Math.max(0, Math.min(viewPosition.x, max - extent));
                if (!ltr) {
                    value = Math.max(0, Math.min(max - extent, max - extent - viewPosition.x));
                }
                hsb.setValues(value, extent, 0, max);
            }

            if (rowHead != null) {
                Point p = rowHead.getViewPosition();
                p.y = viewport.getViewPosition().y;
                p.x = 0;
                rowHead.setViewPosition(p);
            }

            if (colHead != null) {
                Point p = colHead.getViewPosition();
                if (ltr) {
                    p.x = viewport.getViewPosition().x;
                } else {
                    p.x = Math.max(0, viewport.getViewPosition().x);
                }
                p.y = 0;
                colHead.setViewPosition(p);
            }
        }
    }

    /**
     * La linea de base del panel: la de su cabecera de columnas, y nada mas.
     *
     * <p>Sin cabecera devuelve {@code -1}, aunque el contenido tenga una. Tiene sentido: el
     * contenido se desplaza, asi que su linea de base no esta en un lugar fijo del panel, y
     * alinear contra ella dejaria de valer en cuanto alguien mueva la barra. La cabecera, en
     * cambio, no se mueve en vertical.
     *
     * <p>Medido en el JDK 25: con borde da 14 y sin borde 13, para una cabecera cuya propia linea
     * esta en 13; o sea, la de la cabecera mas el inset de arriba del panel.
     */
    public int getBaseline(JComponent c, int width, int height) {
        super.getBaseline(c, width, height);
        JViewport columnHeader = scrollpane.getColumnHeader();
        if (columnHeader != null && columnHeader.isVisible()) {
            Component headerView = columnHeader.getView();
            if (headerView instanceof JComponent) {
                java.awt.Insets insets = scrollpane.getInsets();
                Dimension headerPref = columnHeader.getPreferredSize();
                int baseline = ((JComponent) headerView).getBaseline(headerPref.width,
                        headerPref.height);
                if (baseline >= 0) {
                    return insets.top + baseline;
                }
            }
        }
        return -1;
    }

    public Component$BaselineResizeBehavior getBaselineResizeBehavior(JComponent c) {
        super.getBaselineResizeBehavior(c);
        return Component$BaselineResizeBehavior.CONSTANT_ASCENT;
    }

    protected ChangeListener createViewportChangeListener() {
        return new ViewportChangeHandler();
    }

    /**
     * El escucha de la rueda.
     *
     * <p>Devuelve siempre el mismo, y no un {@link MouseWheelHandler} nuevo: es lo que hace el JDK
     * desde que junto a todos sus escuchas internos en una sola clase. {@link MouseWheelHandler}
     * sigue estando -- es publica y alguien pudo haberla extendido -- pero ya no es lo que sale de
     * aca; lo unico que hace es delegar en este. Medido.
     */
    protected MouseWheelListener createMouseWheelListener() {
        return elEscucha();
    }

    /** El escucha compartido, creado la primera vez que hace falta. */
    private Handler elEscucha() {
        if (handler == null) {
            handler = new Handler();
        }
        return handler;
    }

    protected ChangeListener createHSBChangeListener() {
        return new HSBChangeListener();
    }

    protected ChangeListener createVSBChangeListener() {
        return new VSBChangeListener();
    }

    /** Cambio la politica de una barra: hay que volver a acomodar. */
    protected void updateScrollBarDisplayPolicy(PropertyChangeEvent e) {
        scrollpane.revalidate();
        scrollpane.repaint();
    }

    /** Cambio la ventana: se reengancha el escucha y se resincroniza todo. */
    protected void updateViewport(PropertyChangeEvent e) {
        JViewport oldViewport = (JViewport) (e.getOldValue());
        JViewport newViewport = (JViewport) (e.getNewValue());

        if (oldViewport != null) {
            oldViewport.removeChangeListener(viewportChangeListener);
        }
        if (newViewport != null) {
            Point p = newViewport.getViewPosition();
            if (scrollpane.getComponentOrientation().isLeftToRight()) {
                p.x = Math.max(p.x, 0);
            } else {
                p.x = Math.min(p.x, newViewport.getViewSize().width
                        - newViewport.getExtentSize().width);
            }
            p.y = Math.max(p.y, 0);
            newViewport.setViewPosition(p);
            newViewport.addChangeListener(viewportChangeListener);
        }
        syncScrollPaneWithViewport();
    }

    protected void updateRowHeader(PropertyChangeEvent e) {
        JViewport newRowHead = (JViewport) (e.getNewValue());
        if (newRowHead != null) {
            JViewport viewport = scrollpane.getViewport();
            Point p = newRowHead.getViewPosition();
            p.y = (viewport != null) ? viewport.getViewPosition().y : 0;
            p.x = 0;
            newRowHead.setViewPosition(p);
        }
    }

    protected void updateColumnHeader(PropertyChangeEvent e) {
        JViewport newColHead = (JViewport) (e.getNewValue());
        if (newColHead != null) {
            JViewport viewport = scrollpane.getViewport();
            Point p = newColHead.getViewPosition();
            if (viewport == null) {
                p.x = 0;
            } else {
                p.x = viewport.getViewPosition().x;
            }
            p.y = 0;
            newColHead.setViewPosition(p);
            scrollpane.add(newColHead, COLUMN_HEADER);
        }
    }

    protected PropertyChangeListener createPropertyChangeListener() {
        return new PropertyChangeHandler();
    }

    /** Cambio la ventana: hay que copiarles a las barras lo que ahora se ve. */
    /**
     * La rueda del mouse.
     *
     * <p>Mueve la barra vertical; si no hay o no se ve, la horizontal. Que la rueda mueva en
     * horizontal cuando no hay nada que mover en vertical es lo que hace usable un panel ancho y
     * bajo, donde la unica barra es la de abajo.
     *
     * <p>El evento se consume apenas se decide cual barra mover, aun antes de moverla: consumirlo
     * es lo que evita que el panel de mas afuera tambien se desplace con el mismo giro.
     */
    protected class MouseWheelHandler implements MouseWheelListener {

        protected MouseWheelHandler() {
        }

        public void mouseWheelMoved(MouseWheelEvent e) {
            elEscucha().mouseWheelMoved(e);
        }
    }

    /** Donde vive de verdad la logica de la rueda; ver {@link MouseWheelHandler}. */
    private class Handler implements MouseWheelListener {

        public void mouseWheelMoved(MouseWheelEvent e) {
            if (!scrollpane.isWheelScrollingEnabled() || e.getWheelRotation() == 0) {
                return;
            }
            JScrollBar barra = scrollpane.getVerticalScrollBar();
            if (barra == null || !barra.isVisible()) {
                barra = scrollpane.getHorizontalScrollBar();
                if (barra == null || !barra.isVisible()) {
                    return;
                }
            }
            e.consume();
            if (e.getScrollType() == MouseWheelEvent.WHEEL_BLOCK_SCROLL) {
                porBloques(barra, e.getWheelRotation() < 0 ? -1 : 1);
            } else {
                porPasos(barra, e.getUnitsToScroll());
            }
        }

        /** Una pantalla en esa direccion. */
        private void porBloques(JScrollBar barra, int direccion) {
            int salto = barra.getBlockIncrement(direccion);
            if (salto == 0) {
                salto = barra.getVisibleAmount();
            }
            barra.setValue(barra.getValue() + salto * direccion);
        }

        /** Tantos pasos como pida el evento, sumando el paso de cada uno. */
        private void porPasos(JScrollBar barra, int pasos) {
            if (pasos == 0) {
                return;
            }
            int direccion = (pasos < 0) ? -1 : 1;
            int total = 0;
            for (int i = Math.abs(pasos); i > 0; i--) {
                total += barra.getUnitIncrement(direccion) * direccion;
            }
            barra.setValue(barra.getValue() + total);
        }
    }

    protected class ViewportChangeHandler implements ChangeListener {

        public ViewportChangeHandler() {
        }

        public void stateChanged(ChangeEvent e) {
            syncScrollPaneWithViewport();
        }
    }

    /** Cambio el modelo de la barra horizontal: se mueve la ventana. */
    protected class HSBChangeListener implements ChangeListener {

        public HSBChangeListener() {
        }

        public void stateChanged(ChangeEvent e) {
            JViewport viewport = scrollpane.getViewport();
            if (viewport != null) {
                JScrollBar scrollbar = scrollpane.getHorizontalScrollBar();
                if (scrollbar == null) {
                    return;
                }
                int value = scrollbar.getValue();
                Point p = viewport.getViewPosition();
                if (scrollpane.getComponentOrientation().isLeftToRight()) {
                    p.x = value;
                } else {
                    int max = viewport.getViewSize().width;
                    int extent = viewport.getExtentSize().width;
                    p.x = max - extent - value;
                }
                viewport.setViewPosition(p);
            }
        }
    }

    /** Cambio el modelo de la barra vertical: se mueve la ventana. */
    protected class VSBChangeListener implements ChangeListener {

        public VSBChangeListener() {
        }

        public void stateChanged(ChangeEvent e) {
            JViewport viewport = scrollpane.getViewport();
            if (viewport != null) {
                JScrollBar scrollbar = scrollpane.getVerticalScrollBar();
                if (scrollbar == null) {
                    return;
                }
                Point p = viewport.getViewPosition();
                p.y = scrollbar.getValue();
                viewport.setViewPosition(p);
            }
        }
    }

    /** Cambio una pieza o una politica del panel; reengancha lo que haga falta. */
    public class PropertyChangeHandler implements PropertyChangeListener {

        public PropertyChangeHandler() {
        }

        public void propertyChange(PropertyChangeEvent e) {
            String propertyName = e.getPropertyName();

            if (propertyName.equals("verticalScrollBarDisplayPolicy")
                    || propertyName.equals("verticalScrollBarPolicy")
                    || propertyName.equals("horizontalScrollBarDisplayPolicy")
                    || propertyName.equals("horizontalScrollBarPolicy")) {
                updateScrollBarDisplayPolicy(e);
            } else if (propertyName.equals("viewport")) {
                updateViewport(e);
            } else if (propertyName.equals("rowHeader")) {
                updateRowHeader(e);
            } else if (propertyName.equals("columnHeader")) {
                updateColumnHeader(e);
            } else if (propertyName.equals("verticalScrollBar")) {
                actualizarBarra(e, vsbChangeListener);
            } else if (propertyName.equals("horizontalScrollBar")) {
                actualizarBarra(e, hsbChangeListener);
            } else if (propertyName.equals("componentOrientation")) {
                syncScrollPaneWithViewport();
            }
        }
    }

    /** Cambio una barra: el escucha se muda del modelo viejo al nuevo. */
    private void actualizarBarra(PropertyChangeEvent e, ChangeListener listener) {
        JScrollBar vieja = (JScrollBar) e.getOldValue();
        JScrollBar nueva = (JScrollBar) e.getNewValue();
        if (vieja != null) {
            vieja.getModel().removeChangeListener(listener);
        }
        if (nueva != null) {
            nueva.getModel().addChangeListener(listener);
        }
        syncScrollPaneWithViewport();
    }
}
