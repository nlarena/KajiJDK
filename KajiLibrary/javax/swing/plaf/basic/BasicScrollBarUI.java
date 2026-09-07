package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BoundedRangeModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.LookAndFeel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.DimensionUIResource;
import javax.swing.plaf.ScrollBarUI;

/**
 * El aspecto basico de una barra de desplazamiento: dos botones, una pista y un pulgar.
 *
 * <h2>El UI es tambien la distribucion</h2>
 *
 * <p>Implementa {@link LayoutManager} y se instala como distribucion de la barra. Tiene sentido:
 * donde va cada pieza depende del modelo —el pulgar se ubica y se dimensiona por valor y
 * extension—, y eso lo sabe el aspecto, no un layout generico. De ahi que
 * {@link #layoutVScrollbar} sea el metodo mas largo de la clase.
 *
 * <h2>Como se ubica el pulgar</h2>
 *
 * <p>Dos cuentas. El <em>largo</em> es la parte de la pista que corresponde a lo que se ve:
 * {@code pista * extension / rango}, nunca menos que el minimo ni mas que el maximo. La
 * <em>posicion</em> reparte el sobrante de la pista segun cuanto del recorrido util se lleva
 * andado: {@code (pista - pulgar) * (valor - minimo) / (rango - extension)}. El caso de estar al
 * final se trata aparte, pegando el pulgar contra el boton de abajo, para que no quede un pixel de
 * pista por un redondeo.
 *
 * <p>Si el pulgar no entra en la pista se le da tamano cero, que es como esta familia de aspectos
 * dice "no hay nada que desplazar".
 *
 * <h2>Lo que no esta</h2>
 *
 * <p>No estan las acciones por teclado: para atarlas hace falta la tabla del aspecto, que esta
 * biblioteca todavia no tiene, asi que no habria de donde sacar que tecla hace que.
 */
public class BasicScrollBarUI extends ScrollBarUI implements LayoutManager, SwingConstants {

    /** Ni resaltado. */
    protected static final int NO_HIGHLIGHT = 0;

    /** La parte de pista de antes del pulgar, resaltada. */
    protected static final int DECREASE_HIGHLIGHT = 1;

    /** La parte de pista de despues del pulgar, resaltada. */
    protected static final int INCREASE_HIGHLIGHT = 2;

    protected Dimension minimumThumbSize;
    protected Dimension maximumThumbSize;

    protected Color thumbHighlightColor;
    protected Color thumbLightShadowColor;
    protected Color thumbDarkShadowColor;
    protected Color thumbColor;
    protected Color trackColor;
    protected Color trackHighlightColor;

    protected JScrollBar scrollbar;
    protected JButton incrButton;
    protected JButton decrButton;

    protected boolean isDragging;
    protected TrackListener trackListener;
    protected ArrowButtonListener buttonListener;

    /** El que repite mientras el boton sigue apretado; ver {@link ScrollListener}. */
    protected ScrollListener scrollListener;

    /** El reloj que lo llama: 300 ms hasta el primer repique y 60 entre repiques. */
    protected Timer scrollTimer;
    protected ModelListener modelListener;

    protected Rectangle thumbRect;
    protected Rectangle trackRect;

    protected int trackHighlight;

    protected PropertyChangeListener propertyChangeListener;

    /** El ancho de la barra a lo angosto; 17 en Metal. */
    protected int scrollBarWidth;

    /** El hueco entre el pulgar y el boton de abajo o de la derecha; cero en Metal. */
    protected int incrGap;

    /** El hueco entre el pulgar y el boton de arriba o de la izquierda; cero en Metal. */
    protected int decrGap;

    private boolean supportsAbsolutePositioning;
    private boolean thumbActive;

    public BasicScrollBarUI() {
    }

    /** Un aspecto por barra: guarda la barra, sus botones y los rectangulos. */
    public static ComponentUI createUI(JComponent c) {
        return new BasicScrollBarUI();
    }

    /**
     * Los seis colores de la barra.
     *
     * <p>Son los de {@code ScrollBar.*} medidos en Metal (JDK 25): pulgar (163, 184, 204), su
     * brillo (184, 207, 229), su sombra (99, 130, 191) y su sombra oscura (122, 138, 153); pista
     * (238, 238, 238) y su resaltado (122, 138, 153).
     */
    protected void configureScrollBarColors() {
        if (scrollbar.getBackground() == null
                || scrollbar.getBackground() instanceof javax.swing.plaf.UIResource) {
            scrollbar.setBackground(new javax.swing.plaf.ColorUIResource(238, 238, 238));
        }
        if (scrollbar.getForeground() == null
                || scrollbar.getForeground() instanceof javax.swing.plaf.UIResource) {
            scrollbar.setForeground(new javax.swing.plaf.ColorUIResource(238, 238, 238));
        }
        thumbHighlightColor = new Color(184, 207, 229);
        thumbLightShadowColor = new Color(99, 130, 191);
        thumbDarkShadowColor = new Color(122, 138, 153);
        thumbColor = new Color(163, 184, 204);
        trackColor = new Color(238, 238, 238);
        trackHighlightColor = new Color(122, 138, 153);
    }

    public void installUI(JComponent c) {
        scrollbar = (JScrollBar) c;
        thumbRect = new Rectangle(0, 0, 0, 0);
        trackRect = new Rectangle(0, 0, 0, 0);
        installDefaults();
        installComponents();
        installListeners();
        installKeyboardActions();
    }

    public void uninstallUI(JComponent c) {
        scrollbar = (JScrollBar) c;
        uninstallListeners();
        uninstallDefaults();
        uninstallComponents();
        uninstallKeyboardActions();
        thumbRect = null;
        scrollbar = null;
        incrButton = null;
        decrButton = null;
    }

    /** Ver la nota de {@link #configureScrollBarColors} para de donde salen los numeros. */
    protected void installDefaults() {
        scrollBarWidth = 17;
        minimumThumbSize = new DimensionUIResource(8, 8);
        maximumThumbSize = new DimensionUIResource(4096, 4096);
        supportsAbsolutePositioning = true;
        incrGap = 0;
        decrGap = 0;

        trackHighlight = NO_HIGHLIGHT;
        if (scrollbar.getLayout() == null
                || (scrollbar.getLayout() instanceof javax.swing.plaf.UIResource)) {
            scrollbar.setLayout(this);
        }
        configureScrollBarColors();
        LookAndFeel.installProperty(scrollbar, "opaque", Boolean.TRUE);
    }

    protected void installComponents() {
        if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
            incrButton = createIncreaseButton(SOUTH);
            decrButton = createDecreaseButton(NORTH);
        } else {
            incrButton = createIncreaseButton(EAST);
            decrButton = createDecreaseButton(WEST);
        }
        scrollbar.add(incrButton);
        scrollbar.add(decrButton);
        scrollbar.setEnabled(scrollbar.isEnabled());
    }

    protected void uninstallComponents() {
        if (incrButton != null) {
            scrollbar.remove(incrButton);
        }
        if (decrButton != null) {
            scrollbar.remove(decrButton);
        }
    }

    protected void installListeners() {
        trackListener = createTrackListener();
        buttonListener = createArrowButtonListener();
        modelListener = createModelListener();
        propertyChangeListener = createPropertyChangeListener();
        scrollListener = createScrollListener();

        scrollTimer = new Timer(60, scrollListener);
        scrollTimer.setInitialDelay(300);

        scrollbar.addMouseListener(trackListener);
        scrollbar.addMouseMotionListener(trackListener);
        scrollbar.getModel().addChangeListener(modelListener);
        scrollbar.addPropertyChangeListener(propertyChangeListener);

        if (incrButton != null) {
            incrButton.addMouseListener(buttonListener);
        }
        if (decrButton != null) {
            decrButton.addMouseListener(buttonListener);
        }
    }

    /** Nada: sin {@code InputMap} no hay donde registrar teclas; ver la nota de la clase. */
    protected void installKeyboardActions() {
    }

    protected void uninstallKeyboardActions() {
    }

    protected void uninstallListeners() {
        if (incrButton != null) {
            incrButton.removeMouseListener(buttonListener);
        }
        if (decrButton != null) {
            decrButton.removeMouseListener(buttonListener);
        }
        scrollbar.getModel().removeChangeListener(modelListener);
        scrollbar.removeMouseListener(trackListener);
        scrollbar.removeMouseMotionListener(trackListener);
        scrollbar.removePropertyChangeListener(propertyChangeListener);
        if (scrollTimer != null) {
            scrollTimer.stop();
            scrollTimer = null;
        }
    }

    /** Deja los colores puestos, como el JDK; solo suelta la distribucion. */
    protected void uninstallDefaults() {
        if (scrollbar.getLayout() == this) {
            scrollbar.setLayout(null);
        }
    }

    protected TrackListener createTrackListener() {
        return new TrackListener();
    }

    /** Prepara al escucha y larga el reloj desde cero. */
    private void arrancarReloj(int direction, boolean porBloques) {
        if (scrollTimer == null || scrollListener == null) {
            return;
        }
        scrollTimer.stop();
        scrollListener.setDirection(direction);
        scrollListener.setScrollByBlock(porBloques);
        scrollTimer.start();
    }

    protected ScrollListener createScrollListener() {
        return new ScrollListener();
    }

    protected ArrowButtonListener createArrowButtonListener() {
        return new ArrowButtonListener();
    }

    protected ModelListener createModelListener() {
        return new ModelListener();
    }

    protected PropertyChangeListener createPropertyChangeListener() {
        return new PropertyChangeHandler();
    }

    /** Si el cursor esta sobre el pulgar; algunos aspectos lo pintan distinto. */
    protected void setThumbRollover(boolean active) {
        if (thumbActive != active) {
            thumbActive = active;
            scrollbar.repaint(getThumbBounds());
        }
    }

    public boolean isThumbRollover() {
        return thumbActive;
    }

    /** Pista y pulgar, en ese orden: el pulgar va encima. */
    public void paint(Graphics g, JComponent c) {
        paintTrack(g, c, getTrackBounds());
        Rectangle thumbBounds = getThumbBounds();
        Rectangle clip = g.getClipBounds();
        if (clip == null || thumbBounds.intersects(clip)) {
            paintThumb(g, c, thumbBounds);
        }
    }

    /** Cuarenta y ocho de largo por el ancho de la barra: dos botones y algo de pista. */
    public Dimension getPreferredSize(JComponent c) {
        if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
            return new Dimension(scrollBarWidth, 48);
        }
        return new Dimension(48, scrollBarWidth);
    }

    /** Sin tope: una barra se estira todo lo que su contenedor le de. */
    public Dimension getMaximumSize(JComponent c) {
        return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    protected JButton createDecreaseButton(int orientation) {
        return new BasicArrowButton(orientation, thumbColor, thumbLightShadowColor,
                thumbDarkShadowColor, thumbHighlightColor);
    }

    protected JButton createIncreaseButton(int orientation) {
        return new BasicArrowButton(orientation, thumbColor, thumbLightShadowColor,
                thumbDarkShadowColor, thumbHighlightColor);
    }

    /** Pinta de resaltado la pista de antes del pulgar; es el clic sostenido en esa mitad. */
    protected void paintDecreaseHighlight(Graphics g) {
        Insets insets = scrollbar.getInsets();
        Rectangle thumbR = getThumbBounds();
        g.setColor(trackHighlightColor);

        if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
            int x = insets.left;
            int y = decrButton.getY() + decrButton.getHeight();
            int w = scrollbar.getWidth() - (insets.left + insets.right);
            int h = thumbR.y - y;
            g.fillRect(x, y, w, h);
        } else {
            int x = decrButton.getX() + decrButton.getWidth();
            int y = insets.top;
            int w = thumbR.x - x;
            int h = scrollbar.getHeight() - (insets.top + insets.bottom);
            g.fillRect(x, y, w, h);
        }
    }

    /** Lo mismo del otro lado del pulgar. */
    protected void paintIncreaseHighlight(Graphics g) {
        Insets insets = scrollbar.getInsets();
        Rectangle thumbR = getThumbBounds();
        g.setColor(trackHighlightColor);

        if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
            int x = insets.left;
            int y = thumbR.y + thumbR.height;
            int w = scrollbar.getWidth() - (insets.left + insets.right);
            int h = incrButton.getY() - y;
            g.fillRect(x, y, w, h);
        } else {
            int x = thumbR.x + thumbR.width;
            int y = insets.top;
            int w = incrButton.getX() - x;
            int h = scrollbar.getHeight() - (insets.top + insets.bottom);
            g.fillRect(x, y, w, h);
        }
    }

    protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
        g.setColor(trackColor);
        g.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);

        if (trackHighlight == DECREASE_HIGHLIGHT) {
            paintDecreaseHighlight(g);
        } else if (trackHighlight == INCREASE_HIGHLIGHT) {
            paintIncreaseHighlight(g);
        }
    }

    /**
     * El pulgar: un marco oscuro, el relleno, y dos lineas de relieve.
     *
     * <p>El marco se dibuja antes del relleno y el relleno lo tapa por arriba y por la izquierda:
     * queda oscuro solo el borde derecho y el de abajo. Es la misma secuencia que
     * {@link BasicArrowButton}, y por eso las tres piezas de la barra se ven de la misma familia.
     */
    protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
        if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) {
            return;
        }

        int w = thumbBounds.width;
        int h = thumbBounds.height;

        g.translate(thumbBounds.x, thumbBounds.y);

        g.setColor(thumbDarkShadowColor);
        g.drawRect(0, 0, w - 1, h - 1);
        g.setColor(thumbColor);
        g.fillRect(0, 0, w - 1, h - 1);

        g.setColor(thumbHighlightColor);
        g.drawLine(1, 1, 1, h - 2);
        g.drawLine(2, 1, w - 3, 1);

        // La linea de abajo arranca en x=2 y no en x=1: el pixel de la esquina queda para el
        // brillo, y por eso el relieve del pulgar se ve continuo en esa esquina. El boton de
        // flecha, que dibuja casi lo mismo, si la arranca en x=1; las dos formas estan medidas.
        g.setColor(thumbLightShadowColor);
        g.drawLine(2, h - 2, w - 2, h - 2);
        g.drawLine(w - 2, 1, w - 2, h - 2);

        g.translate(-thumbBounds.x, -thumbBounds.y);
    }

    protected Dimension getMinimumThumbSize() {
        return minimumThumbSize;
    }

    protected Dimension getMaximumThumbSize() {
        return maximumThumbSize;
    }

    public void addLayoutComponent(String name, Component child) {
    }

    public void removeLayoutComponent(Component child) {
    }

    public Dimension preferredLayoutSize(Container scrollbarContainer) {
        return getPreferredSize((JComponent) scrollbarContainer);
    }

    public Dimension minimumLayoutSize(Container scrollbarContainer) {
        return getMinimumSize((JComponent) scrollbarContainer);
    }

    /** Ver la nota de la clase sobre las dos cuentas del pulgar. */
    protected void layoutVScrollbar(JScrollBar sb) {
        Dimension sbSize = sb.getSize();
        Insets sbInsets = sb.getInsets();

        int itemW = sbSize.width - (sbInsets.left + sbInsets.right);
        int itemX = sbInsets.left;

        int decrButtonH = decrButton.getPreferredSize().height;
        int decrButtonY = sbInsets.top;

        int incrButtonH = incrButton.getPreferredSize().height;
        int incrButtonY = sbSize.height - (sbInsets.bottom + incrButtonH);

        int sbInsetsH = sbInsets.top + sbInsets.bottom;
        int sbButtonsH = decrButtonH + incrButtonH;
        float trackH = sbSize.height - (sbInsetsH + sbButtonsH) - (decrGap + incrGap);

        int min = sb.getMinimum();
        int max = sb.getMaximum();
        int extent = sb.getVisibleAmount();
        int range = max - min;
        int value = sb.getValue();

        int maxThumbH = getMaximumThumbSize().height;
        int minThumbH = getMinimumThumbSize().height;
        int thumbH = (range <= 0) ? maxThumbH
                : (int) (trackH * ((float) extent / (float) range));
        thumbH = Math.max(thumbH, minThumbH);
        thumbH = Math.min(thumbH, maxThumbH);

        int thumbY = incrButtonY - incrGap - thumbH;
        if (value < (max - extent)) {
            float thumbRange = trackH - thumbH;
            thumbY = (int) (0.5f
                    + (thumbRange * ((float) (value - min) / (float) (range - extent))));
            thumbY = thumbY + decrButtonY + decrButtonH + decrGap;
        }

        // Si los dos botones no entran, se reparten en partes iguales lo que hay.
        int sbAvailButtonH = (sbSize.height - sbInsetsH);
        if (sbAvailButtonH < sbButtonsH) {
            incrButtonH = sbAvailButtonH / 2;
            decrButtonH = incrButtonH;
            incrButtonY = sbSize.height - (sbInsets.bottom + incrButtonH);
        }
        decrButton.setBounds(itemX, decrButtonY, itemW, decrButtonH);
        incrButton.setBounds(itemX, incrButtonY, itemW, incrButtonH);

        int itrackY = decrButtonY + decrButtonH + decrGap;
        int itrackH = incrButtonY - incrGap - itrackY;
        trackRect.setBounds(itemX, itrackY, itemW, itrackH);

        if (thumbH >= (int) trackH) {
            setThumbBounds(0, 0, 0, 0);
        } else {
            if ((thumbY + thumbH) > incrButtonY - incrGap) {
                thumbY = incrButtonY - incrGap - thumbH;
            }
            if (thumbY < (decrButtonY + decrButtonH + decrGap)) {
                thumbY = decrButtonY + decrButtonH + decrGap + 1;
            }
            setThumbBounds(itemX, thumbY, itemW, thumbH);
        }
    }

    /** Lo mismo de {@link #layoutVScrollbar}, con los ejes cambiados. */
    protected void layoutHScrollbar(JScrollBar sb) {
        Dimension sbSize = sb.getSize();
        Insets sbInsets = sb.getInsets();

        int itemH = sbSize.height - (sbInsets.top + sbInsets.bottom);
        int itemY = sbInsets.top;

        boolean ltr = sb.getComponentOrientation().isLeftToRight();

        int leftButtonW = (ltr ? decrButton : incrButton).getPreferredSize().width;
        int rightButtonW = (ltr ? incrButton : decrButton).getPreferredSize().width;
        int leftButtonX = sbInsets.left;
        int rightButtonX = sbSize.width - (sbInsets.right + rightButtonW);
        int leftGap = ltr ? decrGap : incrGap;
        int rightGap = ltr ? incrGap : decrGap;

        int sbInsetsW = sbInsets.left + sbInsets.right;
        int sbButtonsW = leftButtonW + rightButtonW;
        float trackW = sbSize.width - (sbInsetsW + sbButtonsW) - (leftGap + rightGap);

        int min = sb.getMinimum();
        int max = sb.getMaximum();
        int extent = sb.getVisibleAmount();
        int range = max - min;
        int value = sb.getValue();

        int maxThumbW = getMaximumThumbSize().width;
        int minThumbW = getMinimumThumbSize().width;
        int thumbW = (range <= 0) ? maxThumbW
                : (int) (trackW * ((float) extent / (float) range));
        thumbW = Math.max(thumbW, minThumbW);
        thumbW = Math.min(thumbW, maxThumbW);

        int thumbX = ltr ? rightButtonX - rightGap - thumbW : leftButtonX + leftButtonW + leftGap;
        if (value < (max - extent)) {
            float thumbRange = trackW - thumbW;
            if (ltr) {
                thumbX = (int) (0.5f
                        + (thumbRange * ((float) (value - min) / (float) (range - extent))));
                thumbX = thumbX + leftButtonX + leftButtonW + leftGap;
            } else {
                thumbX = (int) (0.5f
                        + (thumbRange * ((float) (max - extent - value) / (float) (range - extent))));
                thumbX = thumbX + leftButtonX + leftButtonW + leftGap;
            }
        }

        int sbAvailButtonW = (sbSize.width - sbInsetsW);
        if (sbAvailButtonW < sbButtonsW) {
            rightButtonW = sbAvailButtonW / 2;
            leftButtonW = rightButtonW;
            rightButtonX = sbSize.width - (sbInsets.right + rightButtonW);
        }

        (ltr ? decrButton : incrButton).setBounds(leftButtonX, itemY, leftButtonW, itemH);
        (ltr ? incrButton : decrButton).setBounds(rightButtonX, itemY, rightButtonW, itemH);

        int itrackX = leftButtonX + leftButtonW + leftGap;
        int itrackW = rightButtonX - rightGap - itrackX;
        trackRect.setBounds(itrackX, itemY, itrackW, itemH);

        if (thumbW >= (int) trackW) {
            setThumbBounds(0, 0, 0, 0);
        } else {
            if (thumbX + thumbW > rightButtonX - rightGap) {
                thumbX = rightButtonX - rightGap - thumbW;
            }
            if (thumbX < itrackX) {
                thumbX = itrackX + 1;
            }
            setThumbBounds(thumbX, itemY, thumbW, itemH);
        }
    }

    public void layoutContainer(Container scrollbarContainer) {
        // Puede llegar mientras el aspecto se esta cambiando: el contenedor manda.
        JScrollBar scrollbar = (JScrollBar) scrollbarContainer;
        if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
            layoutVScrollbar(scrollbar);
        } else {
            layoutHScrollbar(scrollbar);
        }
    }

    /** Cambia el rectangulo del pulgar y repinta lo viejo y lo nuevo. */
    protected void setThumbBounds(int x, int y, int width, int height) {
        if (thumbRect.x == x && thumbRect.y == y && thumbRect.width == width
                && thumbRect.height == height) {
            return;
        }
        int minX = Math.min(x, thumbRect.x);
        int minY = Math.min(y, thumbRect.y);
        int maxX = Math.max(x + width, thumbRect.x + thumbRect.width);
        int maxY = Math.max(y + height, thumbRect.y + thumbRect.height);

        thumbRect.setBounds(x, y, width, height);
        scrollbar.repaint(minX, minY, maxX - minX, maxY - minY);

        setThumbRollover(false);
    }

    /** Una copia: quien la reciba puede modificarla sin mover el pulgar. */
    protected Rectangle getThumbBounds() {
        return thumbRect.getBounds();
    }

    protected Rectangle getTrackBounds() {
        return trackRect.getBounds();
    }

    /** Avanza una pantalla en esa direccion; el valor se acomoda solo contra los topes. */
    protected void scrollByBlock(int direction) {
        scrollbar.setValueIsAdjusting(true);
        int oldValue = scrollbar.getValue();
        int blockIncrement = scrollbar.getBlockIncrement(direction);
        if (blockIncrement == 0) {
            blockIncrement = scrollbar.getVisibleAmount();
        }
        int delta = blockIncrement * ((direction > 0) ? +1 : -1);
        int newValue = oldValue + delta;

        // Un desborde deja el valor en el tope de ese lado.
        if (delta > 0 && newValue < oldValue) {
            newValue = scrollbar.getMaximum();
        } else if (delta < 0 && newValue > oldValue) {
            newValue = scrollbar.getMinimum();
        }
        scrollbar.setValue(newValue);
        scrollbar.setValueIsAdjusting(false);
    }

    /** Avanza un paso chico en esa direccion. */
    protected void scrollByUnit(int direction) {
        int delta;
        if (direction > 0) {
            delta = scrollbar.getUnitIncrement(direction);
        } else {
            delta = -scrollbar.getUnitIncrement(direction);
        }
        int oldValue = scrollbar.getValue();
        int newValue = oldValue + delta;

        if (delta > 0 && newValue < oldValue) {
            newValue = scrollbar.getMaximum();
        } else if (delta < 0 && newValue > oldValue) {
            newValue = scrollbar.getMinimum();
        }
        if (oldValue != newValue) {
            scrollbar.setValue(newValue);
        }
    }

    /** Si un clic con el modificador del sistema salta directo a esa posicion. */
    public boolean getSupportsAbsolutePositioning() {
        return supportsAbsolutePositioning;
    }

    /**
     * Escucha el arrastre del pulgar y los clics en la pista.
     *
     * <p>Arrastrar traduce pixeles a valores con la regla de tres inversa a la que ubica al
     * pulgar; el {@code offset} es donde dentro del pulgar se lo agarro, y es lo que hace que el
     * pulgar no salte bajo el cursor al empezar a arrastrar.
     */
    protected class TrackListener extends MouseAdapter implements MouseMotionListener {

        protected transient int offset;
        protected transient int currentMouseX;
        protected transient int currentMouseY;

        public TrackListener() {
        }

        public void mouseReleased(MouseEvent e) {
            if (isDragging) {
                updateThumbState(e.getX(), e.getY());
            }
            if (scrollTimer != null) {
                scrollTimer.stop();
            }
            isDragging = false;
            offset = 0;
            trackHighlight = NO_HIGHLIGHT;
            scrollbar.setValueIsAdjusting(false);
            scrollbar.repaint();
        }

        public void mousePressed(MouseEvent e) {
            if (!scrollbar.isEnabled() || !SwingUtilities.isLeftMouseButton(e)) {
                return;
            }
            currentMouseX = e.getX();
            currentMouseY = e.getY();

            Rectangle thumbR = getThumbBounds();
            if (thumbR.contains(currentMouseX, currentMouseY)) {
                if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
                    offset = currentMouseY - thumbR.y;
                } else {
                    offset = currentMouseX - thumbR.x;
                }
                isDragging = true;
                scrollbar.setValueIsAdjusting(true);
                return;
            }
            isDragging = false;

            // Un clic en la pista avanza una pantalla hacia el lado del clic.
            int direction;
            if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
                direction = (currentMouseY < thumbR.y) ? -1 : +1;
            } else if (scrollbar.getComponentOrientation().isLeftToRight()) {
                direction = (currentMouseX < thumbR.x) ? -1 : +1;
            } else {
                direction = (currentMouseX < thumbR.x) ? +1 : -1;
            }
            trackHighlight = (direction > 0) ? INCREASE_HIGHLIGHT : DECREASE_HIGHLIGHT;
            scrollByBlock(direction);
            arrancarReloj(direction, true);
        }

        public void mouseDragged(MouseEvent e) {
            if (!isDragging || !scrollbar.isEnabled()) {
                return;
            }
            currentMouseX = e.getX();
            currentMouseY = e.getY();
            if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
                arrastreVertical(e.getY());
            } else {
                arrastreHorizontal(e.getX());
            }
        }

        /** Traduce una posicion de pixel a un valor del modelo, en vertical. */
        private void arrastreVertical(int y) {
            Rectangle thumbR = getThumbBounds();
            Rectangle trackR = getTrackBounds();
            int thumbMin = trackR.y;
            int thumbMax = trackR.y + trackR.height - thumbR.height;
            int thumbTop = Math.max(thumbMin, Math.min(thumbMax, y - offset));

            setThumbBounds(thumbR.x, thumbTop, thumbR.width, thumbR.height);

            BoundedRangeModel model = scrollbar.getModel();
            int valueMax = model.getMaximum() - model.getExtent();
            int valueRange = valueMax - model.getMinimum();
            int thumbRange = thumbMax - thumbMin;
            int valor;
            if (thumbRange <= 0) {
                valor = model.getMinimum();
            } else if (thumbTop == thumbMax) {
                valor = valueMax;
            } else {
                valor = model.getMinimum()
                        + (int) (0.5f + ((float) (thumbTop - thumbMin) * valueRange) / thumbRange);
            }
            scrollbar.setValue(valor);
        }

        /** Lo mismo en horizontal; el sentido del idioma da vuelta la regla de tres. */
        private void arrastreHorizontal(int x) {
            Rectangle thumbR = getThumbBounds();
            Rectangle trackR = getTrackBounds();
            int thumbMin = trackR.x;
            int thumbMax = trackR.x + trackR.width - thumbR.width;
            int thumbLeft = Math.max(thumbMin, Math.min(thumbMax, x - offset));

            setThumbBounds(thumbLeft, thumbR.y, thumbR.width, thumbR.height);

            BoundedRangeModel model = scrollbar.getModel();
            int valueMax = model.getMaximum() - model.getExtent();
            int valueRange = valueMax - model.getMinimum();
            int thumbRange = thumbMax - thumbMin;
            boolean ltr = scrollbar.getComponentOrientation().isLeftToRight();
            int valor;
            if (thumbRange <= 0) {
                valor = model.getMinimum();
            } else if (ltr && thumbLeft == thumbMax) {
                valor = valueMax;
            } else if (!ltr && thumbLeft == thumbMin) {
                valor = valueMax;
            } else {
                int corrido = ltr ? (thumbLeft - thumbMin) : (thumbMax - thumbLeft);
                valor = model.getMinimum()
                        + (int) (0.5f + ((float) corrido * valueRange) / thumbRange);
            }
            scrollbar.setValue(valor);
        }

        public void mouseMoved(MouseEvent e) {
            if (!isDragging) {
                updateThumbState(e.getX(), e.getY());
            }
        }

        public void mouseExited(MouseEvent e) {
            if (!isDragging) {
                setThumbRollover(false);
            }
        }
    }

    private void updateThumbState(int x, int y) {
        Rectangle rect = getThumbBounds();
        setThumbRollover(rect.contains(x, y));
    }

    /** Escucha las dos flechas: cada clic mueve un paso y, si se mantiene apretada, sigue. */
    protected class ArrowButtonListener extends MouseAdapter {

        public ArrowButtonListener() {
        }

        public void mousePressed(MouseEvent e) {
            if (!scrollbar.isEnabled() || !SwingUtilities.isLeftMouseButton(e)) {
                return;
            }
            int direction = (e.getSource() == incrButton) ? 1 : -1;
            scrollByUnit(direction);
            arrancarReloj(direction, false);
            if (!scrollbar.hasFocus() && scrollbar.isRequestFocusEnabled()) {
                scrollbar.requestFocus();
            }
        }

        public void mouseReleased(MouseEvent e) {
            if (scrollTimer != null) {
                scrollTimer.stop();
            }
        }
    }

    /**
     * El repique del desplazamiento continuo.
     *
     * <p>Es el escucha del reloj: cada vez que suena mueve un paso -- o una pantalla, si se apreto
     * la pista -- en la direccion guardada. Se para solo en dos casos, y los dos importan.
     *
     * <p>El primero es llegar al tope: seguir repicando ahi no hace nada y gasta.
     *
     * <p>El segundo es que el pulgar alcance al cursor. Al apretar la pista el pulgar viene hacia
     * el cursor de a una pantalla; si no se parara, lo pasaria de largo y el contenido seguiria
     * corriendo bajo un dedo quieto. Por eso mira {@code currentMouseX}/{@code currentMouseY} del
     * escucha de la pista y no el evento: lo que interesa es donde esta el cursor ahora.
     */
    protected class ScrollListener implements ActionListener {

        private int direction = +1;
        private boolean useBlockIncrement;

        /** Hacia adelante y de a un paso. */
        public ScrollListener() {
        }

        public ScrollListener(int dir, boolean block) {
            direction = dir;
            useBlockIncrement = block;
        }

        public void setDirection(int direction) {
            this.direction = direction;
        }

        public void setScrollByBlock(boolean block) {
            this.useBlockIncrement = block;
        }

        public void actionPerformed(ActionEvent e) {
            if (useBlockIncrement) {
                scrollByBlock(direction);
                if (alcanzoAlCursor()) {
                    pararReloj(e);
                    return;
                }
            } else {
                scrollByUnit(direction);
            }
            if (direction > 0
                    && scrollbar.getValue() + scrollbar.getVisibleAmount()
                            >= scrollbar.getMaximum()) {
                pararReloj(e);
            } else if (direction < 0 && scrollbar.getValue() <= scrollbar.getMinimum()) {
                pararReloj(e);
            }
        }

        /** Si el pulgar ya llego a donde esta el cursor. */
        private boolean alcanzoAlCursor() {
            Rectangle thumbR = getThumbBounds();
            if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
                if (direction > 0) {
                    return thumbR.y + thumbR.height >= trackListener.currentMouseY;
                }
                return thumbR.y <= trackListener.currentMouseY;
            }
            if (direction > 0) {
                return thumbR.x + thumbR.width >= trackListener.currentMouseX;
            }
            return thumbR.x <= trackListener.currentMouseX;
        }

        /** El reloj es la fuente del evento; asi funciona tambien si lo llamo otro. */
        private void pararReloj(ActionEvent e) {
            if (e.getSource() instanceof Timer) {
                ((Timer) e.getSource()).stop();
            } else if (scrollTimer != null) {
                scrollTimer.stop();
            }
        }
    }

    /** Cambio el modelo: hay que volver a ubicar el pulgar. */
    protected class ModelListener implements ChangeListener {

        public ModelListener() {
        }

        public void stateChanged(ChangeEvent e) {
            if (!useCachedValue()) {
                layoutContainer(scrollbar);
            }
        }
    }

    /** Siempre {@code false}: el JDK cachea el valor durante un arrastre, aca no hace falta. */
    private boolean useCachedValue() {
        return false;
    }

    /** Cambio una propiedad de la barra: si fue el modelo o la orientacion, hay que reacomodar. */
    public class PropertyChangeHandler implements PropertyChangeListener {

        public PropertyChangeHandler() {
        }

        public void propertyChange(PropertyChangeEvent e) {
            String propertyName = e.getPropertyName();

            if ("model".equals(propertyName)) {
                BoundedRangeModel oldModel = (BoundedRangeModel) e.getOldValue();
                BoundedRangeModel newModel = (BoundedRangeModel) e.getNewValue();
                if (oldModel != null) {
                    oldModel.removeChangeListener(modelListener);
                }
                if (newModel != null) {
                    newModel.addChangeListener(modelListener);
                }
                scrollbar.repaint();
                scrollbar.revalidate();
            } else if ("orientation".equals(propertyName)) {
                updateButtonDirections();
            } else if ("componentOrientation".equals(propertyName)) {
                updateButtonDirections();
            }
        }
    }

    /** Da vuelta las flechas cuando cambia la orientacion de la barra o del idioma. */
    private void updateButtonDirections() {
        int orient = scrollbar.getOrientation();
        if (scrollbar.getComponentOrientation().isLeftToRight()) {
            if (incrButton instanceof BasicArrowButton) {
                ((BasicArrowButton) incrButton).setDirection(
                        orient == JScrollBar.HORIZONTAL ? EAST : SOUTH);
                ((BasicArrowButton) decrButton).setDirection(
                        orient == JScrollBar.HORIZONTAL ? WEST : NORTH);
            }
        } else {
            if (incrButton instanceof BasicArrowButton) {
                ((BasicArrowButton) incrButton).setDirection(
                        orient == JScrollBar.HORIZONTAL ? WEST : SOUTH);
                ((BasicArrowButton) decrButton).setDirection(
                        orient == JScrollBar.HORIZONTAL ? EAST : NORTH);
            }
        }
    }
}
