package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Dictionary;
import java.util.Enumeration;

import javax.swing.JComponent;
import javax.swing.JSlider;
import javax.swing.LookAndFeel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MouseInputAdapter;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.SliderUI;
import javax.swing.plaf.UIResource;

/**
 * El aspecto basico de un deslizador.
 *
 * <h2>Seis rectangulos, uno adentro del otro</h2>
 *
 * <p>Toda la clase gira alrededor de seis rectangulos que se calculan en cadena, cada uno a partir
 * del anterior: {@link #focusRect} es el componente menos sus margenes;
 * {@link #contentRect} es ese menos el aire del foco; {@link #trackRect} es la franja por donde
 * corre el pulgar; {@link #tickRect} y {@link #labelRect} son las dos franjas de abajo, que miden
 * cero si el deslizador no muestra marcas ni etiquetas; y {@link #thumbRect} es el pulgar.
 *
 * <p>La cadena se recalcula entera cada vez que cambia algo que la afecta -- el tamano, la
 * orientacion, los margenes, si muestra marcas --, y no de a partes: recalcular la mitad seria mas
 * rapido y dejaria pares de rectangulos que no se corresponden.
 *
 * <h2>Los rectangulos pueden tener ancho negativo, y esta bien</h2>
 *
 * <p>Un deslizador que todavia no tiene tamano da un {@code trackRect} de ancho -10: el buffer de
 * cinco pixeles de cada lado se resta de un contenido que mide cero. Esta medido, y no se corrige:
 * corregirlo escondiendo el negativo daria posiciones de pulgar distintas de las del JDK apenas el
 * deslizador tenga tamano.
 *
 * <h2>De valor a pixel y de vuelta</h2>
 *
 * <p>{@link #xPositionForValue} y {@link #yPositionForValue} son una regla de tres entre el rango
 * del modelo y el largo de la franja, con los dos extremos recortados. El recorte es lo que hace
 * que el pulgar no se salga cuando el rango es raro, y es tambien lo que hace que en un deslizador
 * sin tamano las tres posiciones den el mismo numero.
 *
 * <h2>El reloj del desplazamiento</h2>
 *
 * <p>Apretar en la franja --no en el pulgar-- lo hace avanzar de a un bloque, y seguir apretando lo
 * repite: {@link #scrollTimer} es lo que repite. La direccion sale de que lado del pulgar se
 * apreto.
 *
 * <h2>Lo que queda dicho</h2>
 *
 * <p>El dibujo del pulgar y de la franja es el del aspecto de verdad; el basico dibuja rectangulos
 * planos. Las etiquetas son componentes del programa y se pintan como estan.
 */
public class BasicSliderUI extends SliderUI {

    /** Un bloque hacia adelante. */
    public static final int POSITIVE_SCROLL = +1;

    /** Un bloque hacia atras. */
    public static final int NEGATIVE_SCROLL = -1;

    /** Hasta el minimo. */
    public static final int MIN_SCROLL = -2;

    /** Hasta el maximo. */
    public static final int MAX_SCROLL = +2;

    protected Timer scrollTimer;
    protected JSlider slider;

    protected Insets focusInsets = null;
    protected Insets insetCache = null;
    protected boolean leftToRightCache = true;
    protected Rectangle focusRect = null;
    protected Rectangle contentRect = null;
    protected Rectangle labelRect = null;
    protected Rectangle tickRect = null;
    protected Rectangle trackRect = null;
    protected Rectangle thumbRect = null;

    /** El aire que se le deja al pulgar en cada punta de la franja. */
    protected int trackBuffer = 0;

    protected ChangeListener changeListener;
    protected ComponentListener componentListener;
    protected FocusListener focusListener;
    protected ScrollListener scrollListener;
    protected PropertyChangeListener propertyChangeListener;
    protected TrackListener trackListener;

    private Color shadowColor;
    private Color highlightColor;
    private Color focusColor;
    private boolean dragging;

    private static final ColorUIResource FONDO = new ColorUIResource(238, 238, 238);
    private static final ColorUIResource FRENTE = new ColorUIResource(163, 184, 204);
    private static final ColorUIResource FOCO = new ColorUIResource(163, 184, 204);
    private static final ColorUIResource BRILLO = new ColorUIResource(255, 255, 255);
    private static final ColorUIResource SOMBRA = new ColorUIResource(184, 207, 229);
    private static final FontUIResource FUENTE = new FontUIResource("Dialog", Font.BOLD, 12);

    /** Para ese deslizador; los rectangulos se arman vacios y los llena {@code installUI}. */
    public BasicSliderUI(JSlider b) {
        focusRect = new Rectangle();
        contentRect = new Rectangle();
        labelRect = new Rectangle();
        tickRect = new Rectangle();
        trackRect = new Rectangle();
        thumbRect = new Rectangle();
        insetCache = new Insets(0, 0, 0, 0);
        focusInsets = new Insets(0, 0, 0, 0);
    }

    /** Uno nuevo por deslizador: guarda los seis rectangulos. */
    public static ComponentUI createUI(JComponent b) {
        return new BasicSliderUI((JSlider) b);
    }

    public void installUI(JComponent c) {
        slider = (JSlider) c;
        dragging = false;
        installDefaults(slider);
        installListeners(slider);
        installKeyboardActions(slider);
        insetCache = slider.getInsets();
        leftToRightCache = slider.getComponentOrientation().isLeftToRight();
        calculateGeometry();
    }

    public void uninstallUI(JComponent c) {
        if (c != slider) {
            throw new IllegalComponentStateException(this + " was asked to deinstall() "
                    + c + " when it only knows about " + slider + ".");
        }
        if (scrollTimer != null) {
            scrollTimer.stop();
        }
        scrollTimer = null;
        uninstallDefaults(slider);
        uninstallListeners(slider);
        uninstallKeyboardActions(slider);
        insetCache = null;
        leftToRightCache = true;
        focusRect = null;
        contentRect = null;
        labelRect = null;
        tickRect = null;
        trackRect = null;
        thumbRect = null;
        slider = null;
    }

    /** Colores y fuente; los valores son los de {@code Slider.*} en Metal. */
    protected void installDefaults(JSlider slider) {
        Color fondo = slider.getBackground();
        if (fondo == null || fondo instanceof UIResource) {
            slider.setBackground(FONDO);
        }
        Color frente = slider.getForeground();
        if (frente == null || frente instanceof UIResource) {
            slider.setForeground(FRENTE);
        }
        Font fuente = slider.getFont();
        if (fuente == null || fuente instanceof UIResource) {
            slider.setFont(FUENTE);
        }
        LookAndFeel.installProperty(slider, "opaque", Boolean.TRUE);
        focusInsets = new Insets(0, 0, 0, 0);
        focusColor = FOCO;
        highlightColor = BRILLO;
        shadowColor = SOMBRA;
    }

    /** No saca nada; ver {@link BasicPanelUI#uninstallDefaults}. */
    protected void uninstallDefaults(JSlider slider) {
    }

    protected void installListeners(JSlider slider) {
        trackListener = createTrackListener(slider);
        changeListener = createChangeListener(slider);
        componentListener = createComponentListener(slider);
        focusListener = createFocusListener(slider);
        scrollListener = createScrollListener(slider);
        propertyChangeListener = createPropertyChangeListener(slider);

        slider.addMouseListener(trackListener);
        slider.addMouseMotionListener(trackListener);
        slider.addFocusListener(focusListener);
        slider.addComponentListener(componentListener);
        slider.addPropertyChangeListener(propertyChangeListener);
        slider.getModel().addChangeListener(changeListener);

        scrollTimer = new Timer(100, scrollListener);
        scrollTimer.setInitialDelay(300);
    }

    protected void uninstallListeners(JSlider slider) {
        slider.removeMouseListener(trackListener);
        slider.removeMouseMotionListener(trackListener);
        slider.removeFocusListener(focusListener);
        slider.removeComponentListener(componentListener);
        slider.removePropertyChangeListener(propertyChangeListener);
        slider.getModel().removeChangeListener(changeListener);
        trackListener = null;
        changeListener = null;
        componentListener = null;
        focusListener = null;
        scrollListener = null;
        propertyChangeListener = null;
    }

    /** Sin atajos propios: las flechas las ata la tabla del aspecto. */
    protected void installKeyboardActions(JSlider slider) {
    }

    protected void uninstallKeyboardActions(JSlider slider) {
    }

    protected TrackListener createTrackListener(JSlider slider) {
        return new TrackListener(this);
    }

    protected ChangeListener createChangeListener(JSlider slider) {
        return new Handler(this);
    }

    protected ComponentListener createComponentListener(JSlider slider) {
        return new Handler(this);
    }

    protected FocusListener createFocusListener(JSlider slider) {
        return new Handler(this);
    }

    protected ScrollListener createScrollListener(JSlider slider) {
        return new ScrollListener(this);
    }

    protected PropertyChangeListener createPropertyChangeListener(JSlider slider) {
        return new Handler(this);
    }

    protected Color getShadowColor() {
        return shadowColor;
    }

    protected Color getHighlightColor() {
        return highlightColor;
    }

    protected Color getFocusColor() {
        return focusColor;
    }

    /** Si el pulgar se esta arrastrando. */
    protected boolean isDragging() {
        return dragging;
    }

    /** Si el deslizador esta dado vuelta -- el maximo del lado del minimo --. */
    protected boolean drawInverted() {
        if (slider.getOrientation() == SwingConstants.HORIZONTAL) {
            if (leftToRightCache) {
                return slider.getInverted();
            }
            return !slider.getInverted();
        }
        return slider.getInverted();
    }

    /** El valor mas chico de la tabla de etiquetas, o {@code null} si no hay tabla. */
    protected Integer getLowestValue() {
        Dictionary<?, ?> dictionary = slider.getLabelTable();
        if (dictionary == null) {
            return null;
        }
        Enumeration<?> keys = dictionary.keys();
        Integer min = null;
        while (keys.hasMoreElements()) {
            Object k = keys.nextElement();
            if (k instanceof Integer) {
                Integer i = (Integer) k;
                if (min == null || i.intValue() < min.intValue()) {
                    min = i;
                }
            }
        }
        return min;
    }

    /** Y el mas grande. */
    protected Integer getHighestValue() {
        Dictionary<?, ?> dictionary = slider.getLabelTable();
        if (dictionary == null) {
            return null;
        }
        Enumeration<?> keys = dictionary.keys();
        Integer max = null;
        while (keys.hasMoreElements()) {
            Object k = keys.nextElement();
            if (k instanceof Integer) {
                Integer i = (Integer) k;
                if (max == null || i.intValue() > max.intValue()) {
                    max = i;
                }
            }
        }
        return max;
    }

    /** La etiqueta del valor mas chico, o {@code null}. */
    protected Component getLowestValueLabel() {
        Integer min = getLowestValue();
        if (min == null) {
            return null;
        }
        Object o = slider.getLabelTable().get(min);
        return (o instanceof Component) ? (Component) o : null;
    }

    /** Y la del mas grande. */
    protected Component getHighestValueLabel() {
        Integer max = getHighestValue();
        if (max == null) {
            return null;
        }
        Object o = slider.getLabelTable().get(max);
        return (o instanceof Component) ? (Component) o : null;
    }

    protected int getWidthOfLowValueLabel() {
        Component label = getLowestValueLabel();
        return (label == null) ? 0 : label.getPreferredSize().width;
    }

    protected int getWidthOfHighValueLabel() {
        Component label = getHighestValueLabel();
        return (label == null) ? 0 : label.getPreferredSize().width;
    }

    protected int getHeightOfHighValueLabel() {
        Component label = getHighestValueLabel();
        return (label == null) ? 0 : label.getPreferredSize().height;
    }

    protected int getHeightOfLowValueLabel() {
        Component label = getLowestValueLabel();
        return (label == null) ? 0 : label.getPreferredSize().height;
    }

    /**
     * Si todas las etiquetas apoyan el texto a la misma altura.
     *
     * <p>Sirve para una sola cosa, y es la que justifica el metodo: si comparten linea de base, la
     * fila de etiquetas se puede alinear por ella y los numeros quedan derechos aunque una etiqueta
     * sea mas alta que otra. Si no la comparten -- una etiqueta es un icono, o tiene dos renglones
     * -- hay que centrarlas verticalmente, que es lo unico que queda.
     *
     * <p>Sin tabla de etiquetas la respuesta es que no, aunque suene al reves: no hay etiquetas que
     * alinear, asi que no hay linea de base compartida de la cual colgarlas. Una tabla vacia, en
     * cambio, contesta que si -- no hay ninguna que desmienta --. Las dos, medidas.
     *
     * <p>El JDK guarda el resultado y lo recalcula cuando cambia la tabla; aca se calcula cada vez.
     * Es la misma respuesta y no puede quedar vieja.
     */
    protected boolean labelsHaveSameBaselines() {
        Dictionary<?, ?> dictionary = slider.getLabelTable();
        if (dictionary == null) {
            return false;
        }
        int base = -1;
        Enumeration<?> elements = dictionary.elements();
        while (elements.hasMoreElements()) {
            Object o = elements.nextElement();
            if (!(o instanceof Component)) {
                return false;
            }
            Component label = (Component) o;
            Dimension pref = label.getPreferredSize();
            int suya = label.getBaseline(pref.width, pref.height);
            if (suya < 0) {
                return false;
            }
            if (base == -1) {
                base = suya;
            } else if (base != suya) {
                return false;
            }
        }
        return true;
    }

    /** El alto de la etiqueta mas alta; cero si no hay ninguna. */
    protected int getHeightOfTallestLabel() {
        Dictionary<?, ?> dictionary = slider.getLabelTable();
        if (dictionary == null) {
            return 0;
        }
        int alto = 0;
        Enumeration<?> elements = dictionary.elements();
        while (elements.hasMoreElements()) {
            Object o = elements.nextElement();
            if (o instanceof Component) {
                alto = Math.max(alto, ((Component) o).getPreferredSize().height);
            }
        }
        return alto;
    }

    /** Ocho pixeles; es lo que miden las marcas grandes. */
    protected int getTickLength() {
        return 8;
    }

    /** El tamano del pulgar; el basico lo hace de 11 x 20, y de 20 x 11 acostado. */
    protected Dimension getThumbSize() {
        if (slider.getOrientation() == SwingConstants.HORIZONTAL) {
            return new Dimension(11, 20);
        }
        return new Dimension(20, 11);
    }

    public Dimension getPreferredHorizontalSize() {
        return new Dimension(200, 21);
    }

    public Dimension getPreferredVerticalSize() {
        return new Dimension(21, 200);
    }

    public Dimension getMinimumHorizontalSize() {
        return new Dimension(36, 21);
    }

    public Dimension getMinimumVerticalSize() {
        return new Dimension(21, 36);
    }

    /** El de referencia, con el alto --o el ancho-- que piden las tres franjas. */
    public Dimension getPreferredSize(JComponent c) {
        recalculateIfInsetsChanged();
        Dimension d;
        if (slider.getOrientation() == SwingConstants.VERTICAL) {
            d = new Dimension(getPreferredVerticalSize());
            d.width = insetCache.left + insetCache.right;
            d.width += focusInsets.left + focusInsets.right;
            d.width += trackRect.width + tickRect.width + labelRect.width;
        } else {
            d = new Dimension(getPreferredHorizontalSize());
            d.height = insetCache.top + insetCache.bottom;
            d.height += focusInsets.top + focusInsets.bottom;
            d.height += trackRect.height + tickRect.height + labelRect.height;
        }
        return d;
    }

    /** Idem, a partir del minimo de referencia. */
    public Dimension getMinimumSize(JComponent c) {
        recalculateIfInsetsChanged();
        Dimension d;
        if (slider.getOrientation() == SwingConstants.VERTICAL) {
            d = new Dimension(getMinimumVerticalSize());
            d.width = insetCache.left + insetCache.right;
            d.width += focusInsets.left + focusInsets.right;
            d.width += trackRect.width + tickRect.width + labelRect.width;
        } else {
            d = new Dimension(getMinimumHorizontalSize());
            d.height = insetCache.top + insetCache.bottom;
            d.height += focusInsets.top + focusInsets.bottom;
            d.height += trackRect.height + tickRect.height + labelRect.height;
        }
        return d;
    }

    /** Se estira a lo largo y nada a lo ancho. */
    public Dimension getMaximumSize(JComponent c) {
        Dimension d = getPreferredSize(c);
        if (slider.getOrientation() == SwingConstants.VERTICAL) {
            d.height = Short.MAX_VALUE;
        } else {
            d.width = Short.MAX_VALUE;
        }
        return d;
    }

    /** Rehace la cadena entera de rectangulos; ver la nota de la clase. */
    private void calculateGeometry() {
        calculateFocusRect();
        calculateContentRect();
        calculateThumbSize();
        calculateTrackBuffer();
        calculateTrackRect();
        calculateTickRect();
        calculateLabelRect();
        calculateThumbLocation();
    }

    protected void calculateFocusRect() {
        focusRect.x = insetCache.left;
        focusRect.y = insetCache.top;
        focusRect.width = slider.getWidth() - (insetCache.left + insetCache.right);
        focusRect.height = slider.getHeight() - (insetCache.top + insetCache.bottom);
    }

    protected void calculateContentRect() {
        contentRect.x = focusRect.x + focusInsets.left;
        contentRect.y = focusRect.y + focusInsets.top;
        contentRect.width = focusRect.width - (focusInsets.left + focusInsets.right);
        contentRect.height = focusRect.height - (focusInsets.top + focusInsets.bottom);
    }

    protected void calculateThumbSize() {
        Dimension size = getThumbSize();
        thumbRect.setSize(size.width, size.height);
    }

    /** La mitad del pulgar de cada lado, para que no se salga en los extremos. */
    protected void calculateTrackBuffer() {
        if (slider.getPaintLabels() && slider.getLabelTable() != null) {
            Component highLabel = getHighestValueLabel();
            Component lowLabel = getLowestValueLabel();
            if (slider.getOrientation() == SwingConstants.HORIZONTAL) {
                trackBuffer = Math.max((highLabel == null) ? 0 : highLabel.getBounds().width / 2,
                        (lowLabel == null) ? 0 : lowLabel.getBounds().width / 2);
                trackBuffer = Math.max(trackBuffer, thumbRect.width / 2);
            } else {
                trackBuffer = Math.max((highLabel == null) ? 0 : highLabel.getBounds().height / 2,
                        (lowLabel == null) ? 0 : lowLabel.getBounds().height / 2);
                trackBuffer = Math.max(trackBuffer, thumbRect.height / 2);
            }
        } else {
            if (slider.getOrientation() == SwingConstants.HORIZONTAL) {
                trackBuffer = thumbRect.width / 2;
            } else {
                trackBuffer = thumbRect.height / 2;
            }
        }
    }

    protected void calculateTrackRect() {
        int centerSpacing;
        if (slider.getOrientation() == SwingConstants.HORIZONTAL) {
            centerSpacing = thumbRect.height;
            if (slider.getPaintTicks()) {
                centerSpacing += getTickLength();
            }
            if (slider.getPaintLabels()) {
                centerSpacing += getHeightOfTallestLabel();
            }
            trackRect.x = contentRect.x + trackBuffer;
            trackRect.y = contentRect.y + (contentRect.height - centerSpacing - 1) / 2;
            trackRect.width = contentRect.width - (trackBuffer * 2);
            trackRect.height = thumbRect.height;
        } else {
            centerSpacing = thumbRect.width;
            if (leftToRightCache) {
                if (slider.getPaintTicks()) {
                    centerSpacing += getTickLength();
                }
                if (slider.getPaintLabels()) {
                    centerSpacing += getWidthOfWidestLabel();
                }
            } else {
                if (slider.getPaintTicks()) {
                    centerSpacing -= getTickLength();
                }
                if (slider.getPaintLabels()) {
                    centerSpacing -= getWidthOfWidestLabel();
                }
            }
            trackRect.x = contentRect.x + (contentRect.width - centerSpacing - 1) / 2;
            trackRect.y = contentRect.y + trackBuffer;
            trackRect.width = thumbRect.width;
            trackRect.height = contentRect.height - (trackBuffer * 2);
        }
    }

    private int getWidthOfWidestLabel() {
        Dictionary<?, ?> dictionary = slider.getLabelTable();
        if (dictionary == null) {
            return 0;
        }
        int ancho = 0;
        Enumeration<?> elements = dictionary.elements();
        while (elements.hasMoreElements()) {
            Object o = elements.nextElement();
            if (o instanceof Component) {
                ancho = Math.max(ancho, ((Component) o).getPreferredSize().width);
            }
        }
        return ancho;
    }

    private void calculateTickRect() {
        if (slider.getOrientation() == SwingConstants.HORIZONTAL) {
            tickRect.x = trackRect.x;
            tickRect.y = trackRect.y + trackRect.height;
            tickRect.width = trackRect.width;
            tickRect.height = slider.getPaintTicks() ? getTickLength() : 0;
        } else {
            tickRect.width = slider.getPaintTicks() ? getTickLength() : 0;
            if (leftToRightCache) {
                tickRect.x = trackRect.x + trackRect.width;
            } else {
                tickRect.x = trackRect.x - tickRect.width;
            }
            tickRect.y = trackRect.y;
            tickRect.height = trackRect.height;
        }
    }

    protected void calculateLabelRect() {
        if (slider.getPaintLabels()) {
            if (slider.getOrientation() == SwingConstants.HORIZONTAL) {
                labelRect.x = tickRect.x - trackBuffer;
                labelRect.y = tickRect.y + tickRect.height;
                labelRect.width = tickRect.width + (trackBuffer * 2);
                labelRect.height = getHeightOfTallestLabel();
            } else {
                labelRect.width = getWidthOfWidestLabel();
                if (leftToRightCache) {
                    labelRect.x = tickRect.x + tickRect.width;
                } else {
                    labelRect.x = tickRect.x - labelRect.width;
                }
                labelRect.y = tickRect.y - trackBuffer;
                labelRect.height = tickRect.height + (trackBuffer * 2);
            }
        } else {
            if (slider.getOrientation() == SwingConstants.HORIZONTAL) {
                labelRect.x = tickRect.x;
                labelRect.y = tickRect.y + tickRect.height;
                labelRect.width = tickRect.width;
                labelRect.height = 0;
            } else {
                if (leftToRightCache) {
                    labelRect.x = tickRect.x + tickRect.width;
                } else {
                    labelRect.x = tickRect.x;
                }
                labelRect.y = tickRect.y;
                labelRect.width = 0;
                labelRect.height = tickRect.height;
            }
        }
    }

    protected void calculateThumbLocation() {
        if (slider.getSnapToTicks()) {
            // Con marcas, el pulgar salta de una a la otra en vez de quedar en el medio.
            int sliderValue = slider.getValue();
            int snappedValue = sliderValue;
            int tickSpacing = getTickSpacing();
            if (tickSpacing != 0) {
                int min = slider.getMinimum();
                if ((sliderValue - min) % tickSpacing != 0) {
                    float temp = (float) (sliderValue - min) / (float) tickSpacing;
                    snappedValue = min + (Math.round(temp) * tickSpacing);
                }
                if (snappedValue != sliderValue) {
                    slider.setValue(snappedValue);
                }
            }
        }
        if (slider.getOrientation() == SwingConstants.HORIZONTAL) {
            int valuePosition = xPositionForValue(slider.getValue());
            thumbRect.x = valuePosition - (thumbRect.width / 2);
            thumbRect.y = trackRect.y;
        } else {
            int valuePosition = yPositionForValue(slider.getValue());
            thumbRect.x = trackRect.x;
            thumbRect.y = valuePosition - (thumbRect.height / 2);
        }
    }

    private int getTickSpacing() {
        int majorTickSpacing = slider.getMajorTickSpacing();
        int minorTickSpacing = slider.getMinorTickSpacing();
        if (minorTickSpacing > 0) {
            return minorTickSpacing;
        }
        if (majorTickSpacing > 0) {
            return majorTickSpacing;
        }
        return 0;
    }

    /** Pone el pulgar ahi y redibuja lo que hace falta. */
    public void setThumbLocation(int x, int y) {
        Rectangle unionRect = new Rectangle();
        unionRect.setBounds(thumbRect);
        thumbRect.setLocation(x, y);
        javax.swing.SwingUtilities.computeUnion(thumbRect.x, thumbRect.y,
                thumbRect.width, thumbRect.height, unionRect);
        slider.repaint(unionRect.x, unionRect.y, unionRect.width, unionRect.height);
    }

    /** El pixel que le toca a ese valor; ver la nota de la clase. */
    protected int xPositionForValue(int value) {
        int min = slider.getMinimum();
        int max = slider.getMaximum();
        int trackLength = trackRect.width;
        double valueRange = (double) max - (double) min;
        double pixelsPerValue = (double) trackLength / valueRange;
        int trackLeft = trackRect.x;
        int trackRight = trackRect.x + (trackRect.width - 1);
        int xPosition;
        if (!drawInverted()) {
            xPosition = trackLeft;
            xPosition += Math.round(pixelsPerValue * ((double) value - min));
        } else {
            xPosition = trackRight;
            xPosition -= Math.round(pixelsPerValue * ((double) value - min));
        }
        xPosition = Math.max(trackLeft, xPosition);
        xPosition = Math.min(trackRight, xPosition);
        return xPosition;
    }

    /** Idem en el otro eje. */
    protected int yPositionForValue(int value) {
        return yPositionForValue(value, trackRect.y, trackRect.height);
    }

    /** Idem, con una franja dada; util para dibujar en un rectangulo prestado. */
    protected int yPositionForValue(int value, int trackY, int trackHeight) {
        int min = slider.getMinimum();
        int max = slider.getMaximum();
        double valueRange = (double) max - (double) min;
        double pixelsPerValue = (double) trackHeight / valueRange;
        int trackBottom = trackY + (trackHeight - 1);
        int yPosition;
        if (!drawInverted()) {
            yPosition = trackY;
            yPosition += Math.round(pixelsPerValue * ((double) max - value));
        } else {
            yPosition = trackY;
            yPosition += Math.round(pixelsPerValue * ((double) value - min));
        }
        yPosition = Math.max(trackY, yPosition);
        yPosition = Math.min(trackBottom, yPosition);
        return yPosition;
    }

    /** Rehace la geometria si cambiaron los margenes. */
    protected void recalculateIfInsetsChanged() {
        Insets newInsets = slider.getInsets();
        if (!newInsets.equals(insetCache)) {
            insetCache = newInsets;
            calculateGeometry();
        }
    }

    /** Idem si cambio la orientacion del idioma. */
    protected void recalculateIfOrientationChanged() {
        boolean ltr = slider.getComponentOrientation().isLeftToRight();
        if (ltr != leftToRightCache) {
            leftToRightCache = ltr;
            calculateGeometry();
        }
    }

    /** Avanza un bloque en ese sentido. */
    public void scrollByBlock(int direction) {
        synchronized (slider) {
            int blockIncrement = (slider.getMaximum() - slider.getMinimum()) / 10;
            if (blockIncrement == 0) {
                blockIncrement = 1;
            }
            if (slider.getSnapToTicks()) {
                int tickSpacing = getTickSpacing();
                if (blockIncrement < tickSpacing) {
                    blockIncrement = tickSpacing;
                }
            }
            int delta = blockIncrement * ((direction > 0) ? POSITIVE_SCROLL : NEGATIVE_SCROLL);
            slider.setValue(slider.getValue() + delta);
        }
    }

    /** Y una unidad. */
    public void scrollByUnit(int direction) {
        synchronized (slider) {
            int delta = ((direction > 0) ? POSITIVE_SCROLL : NEGATIVE_SCROLL);
            if (slider.getSnapToTicks()) {
                delta *= getTickSpacing();
            }
            slider.setValue(slider.getValue() + delta);
        }
    }

    /** Apretar en la franja: avanza un bloque hacia donde se apreto. */
    protected void scrollDueToClickInTrack(int dir) {
        scrollByBlock(dir);
    }

    public void paint(Graphics g, JComponent c) {
        recalculateIfInsetsChanged();
        recalculateIfOrientationChanged();
        Rectangle clip = g.getClipBounds();
        if (slider.getPaintTrack() && clip.intersects(trackRect)) {
            paintTrack(g);
        }
        if (slider.getPaintTicks() && clip.intersects(tickRect)) {
            paintTicks(g);
        }
        if (slider.getPaintLabels() && clip.intersects(labelRect)) {
            paintLabels(g);
        }
        if (slider.hasFocus() && clip.intersects(focusRect)) {
            paintFocus(g);
        }
        if (clip.intersects(thumbRect)) {
            paintThumb(g);
        }
    }

    /** Un rectangulo punteado alrededor; ver la nota de la clase. */
    public void paintFocus(Graphics g) {
        g.setColor(getFocusColor());
        g.drawRect(focusRect.x, focusRect.y, focusRect.width - 1, focusRect.height - 1);
    }

    /** La franja: una hendidura fina en el medio del alto disponible. */
    public void paintTrack(Graphics g) {
        Rectangle trackBounds = trackRect;
        if (slider.getOrientation() == SwingConstants.HORIZONTAL) {
            int cy = (trackBounds.height / 2) - 2;
            int cw = trackBounds.width;
            g.translate(trackBounds.x, trackBounds.y + cy);
            g.setColor(getShadowColor());
            g.drawLine(0, 0, cw - 1, 0);
            g.drawLine(0, 1, 0, 2);
            g.setColor(getHighlightColor());
            g.drawLine(0, 3, cw, 3);
            g.drawLine(cw, 0, cw, 3);
            g.translate(-trackBounds.x, -(trackBounds.y + cy));
        } else {
            int cx = (trackBounds.width / 2) - 2;
            int ch = trackBounds.height;
            g.translate(trackBounds.x + cx, trackBounds.y);
            g.setColor(getShadowColor());
            g.drawLine(0, 0, 0, ch - 1);
            g.drawLine(1, 0, 2, 0);
            g.setColor(getHighlightColor());
            g.drawLine(3, 0, 3, ch);
            g.drawLine(0, ch, 3, ch);
            g.translate(-(trackBounds.x + cx), -trackBounds.y);
        }
    }

    /** Las marcas grandes y chicas. */
    public void paintTicks(Graphics g) {
        Rectangle tickBounds = tickRect;
        g.setColor(getShadowColor());
        if (slider.getOrientation() == SwingConstants.HORIZONTAL) {
            g.translate(0, tickBounds.y);
            if (slider.getMinorTickSpacing() > 0) {
                int value = slider.getMinimum();
                while (value <= slider.getMaximum()) {
                    int xPos = xPositionForValue(value);
                    paintMinorTickForHorizSlider(g, tickBounds, xPos);
                    if (Integer.MAX_VALUE - slider.getMinorTickSpacing() < value) {
                        break;
                    }
                    value += slider.getMinorTickSpacing();
                }
            }
            if (slider.getMajorTickSpacing() > 0) {
                int value = slider.getMinimum();
                while (value <= slider.getMaximum()) {
                    int xPos = xPositionForValue(value);
                    paintMajorTickForHorizSlider(g, tickBounds, xPos);
                    if (Integer.MAX_VALUE - slider.getMajorTickSpacing() < value) {
                        break;
                    }
                    value += slider.getMajorTickSpacing();
                }
            }
            g.translate(0, -tickBounds.y);
        } else {
            g.translate(tickBounds.x, 0);
            if (slider.getMinorTickSpacing() > 0) {
                int value = slider.getMinimum();
                while (value <= slider.getMaximum()) {
                    int yPos = yPositionForValue(value);
                    paintMinorTickForVertSlider(g, tickBounds, yPos);
                    if (Integer.MAX_VALUE - slider.getMinorTickSpacing() < value) {
                        break;
                    }
                    value += slider.getMinorTickSpacing();
                }
            }
            if (slider.getMajorTickSpacing() > 0) {
                int value = slider.getMinimum();
                while (value <= slider.getMaximum()) {
                    int yPos = yPositionForValue(value);
                    paintMajorTickForVertSlider(g, tickBounds, yPos);
                    if (Integer.MAX_VALUE - slider.getMajorTickSpacing() < value) {
                        break;
                    }
                    value += slider.getMajorTickSpacing();
                }
            }
            g.translate(-tickBounds.x, 0);
        }
    }

    protected void paintMinorTickForHorizSlider(Graphics g, Rectangle tickBounds, int x) {
        g.drawLine(x, 0, x, tickBounds.height / 2 - 1);
    }

    protected void paintMajorTickForHorizSlider(Graphics g, Rectangle tickBounds, int x) {
        g.drawLine(x, 0, x, tickBounds.height - 2);
    }

    protected void paintMinorTickForVertSlider(Graphics g, Rectangle tickBounds, int y) {
        g.drawLine(0, y, tickBounds.width / 2 - 1, y);
    }

    protected void paintMajorTickForVertSlider(Graphics g, Rectangle tickBounds, int y) {
        g.drawLine(0, y, tickBounds.width - 2, y);
    }

    /** Las etiquetas, cada una centrada en su valor. */
    public void paintLabels(Graphics g) {
        Rectangle labelBounds = labelRect;
        Dictionary<?, ?> dictionary = slider.getLabelTable();
        if (dictionary == null) {
            return;
        }
        Enumeration<?> keys = dictionary.keys();
        int minValue = slider.getMinimum();
        int maxValue = slider.getMaximum();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = dictionary.get(key);
            if (!(key instanceof Integer) || !(value instanceof Component)) {
                continue;
            }
            int labelValue = ((Integer) key).intValue();
            if (labelValue < minValue || labelValue > maxValue) {
                continue;
            }
            Component label = (Component) value;
            if (slider.getOrientation() == SwingConstants.HORIZONTAL) {
                g.translate(0, labelBounds.y);
                paintHorizontalLabel(g, labelValue, label);
                g.translate(0, -labelBounds.y);
            } else {
                int offset = 0;
                if (!leftToRightCache) {
                    offset = labelBounds.width - label.getPreferredSize().width;
                }
                g.translate(labelBounds.x + offset, 0);
                paintVerticalLabel(g, labelValue, label);
                g.translate(-labelBounds.x - offset, 0);
            }
        }
    }

    protected void paintHorizontalLabel(Graphics g, int value, Component label) {
        int labelCenter = xPositionForValue(value);
        int labelLeft = labelCenter - (label.getPreferredSize().width / 2);
        g.translate(labelLeft, 0);
        label.paint(g);
        g.translate(-labelLeft, 0);
    }

    protected void paintVerticalLabel(Graphics g, int value, Component label) {
        int labelCenter = yPositionForValue(value);
        int labelTop = labelCenter - (label.getPreferredSize().height / 2);
        g.translate(0, labelTop);
        label.paint(g);
        g.translate(0, -labelTop);
    }

    /** El pulgar: un rectangulo plano; ver la nota de la clase. */
    public void paintThumb(Graphics g) {
        Rectangle knobBounds = thumbRect;
        g.translate(knobBounds.x, knobBounds.y);
        g.setColor(slider.getForeground());
        g.fillRect(0, 0, knobBounds.width, knobBounds.height);
        g.setColor(getShadowColor());
        g.drawRect(0, 0, knobBounds.width - 1, knobBounds.height - 1);
        g.translate(-knobBounds.x, -knobBounds.y);
    }

    /**
     * Donde apoya el texto de las etiquetas.
     *
     * @throws NullPointerException si el componente es nulo
     * @throws IllegalArgumentException si el ancho o el alto son negativos
     */
    public int getBaseline(JComponent c, int width, int height) {
        super.getBaseline(c, width, height);
        if (slider.getPaintLabels() && labelRect.height > 0) {
            return labelRect.y;
        }
        return 0;
    }

    /** El que sigue el mouse sobre la franja y sobre el pulgar. */
    public class TrackListener extends MouseInputAdapter {

        private final BasicSliderUI ui;
        protected transient int offset;
        protected transient int currentMouseX;
        protected transient int currentMouseY;

        /** El parametro es el UI; ver el hallazgo #518. */
        public TrackListener(BasicSliderUI ui) {
            this.ui = ui;
        }

        public void mousePressed(MouseEvent e) {
            if (!ui.slider.isEnabled()) {
                return;
            }
            currentMouseX = e.getX();
            currentMouseY = e.getY();
            if (ui.slider.isRequestFocusEnabled()) {
                ui.slider.requestFocus();
            }
            if (ui.thumbRect.contains(currentMouseX, currentMouseY)) {
                ui.dragging = true;
                offset = (ui.slider.getOrientation() == SwingConstants.HORIZONTAL)
                        ? (currentMouseX - ui.thumbRect.x)
                        : (currentMouseY - ui.thumbRect.y);
                return;
            }
            ui.dragging = false;
            int direction = direccion();
            if (direction != 0) {
                ui.scrollDueToClickInTrack(direction);
                if (ui.scrollTimer != null) {
                    ui.scrollListener.setDirection(direction);
                    ui.scrollTimer.start();
                }
            }
        }

        private int direccion() {
            if (ui.slider.getOrientation() == SwingConstants.HORIZONTAL) {
                if (currentMouseX < ui.thumbRect.x) {
                    return ui.drawInverted() ? POSITIVE_SCROLL : NEGATIVE_SCROLL;
                }
                if (currentMouseX > ui.thumbRect.x + ui.thumbRect.width) {
                    return ui.drawInverted() ? NEGATIVE_SCROLL : POSITIVE_SCROLL;
                }
                return 0;
            }
            if (currentMouseY < ui.thumbRect.y) {
                return ui.drawInverted() ? NEGATIVE_SCROLL : POSITIVE_SCROLL;
            }
            if (currentMouseY > ui.thumbRect.y + ui.thumbRect.height) {
                return ui.drawInverted() ? POSITIVE_SCROLL : NEGATIVE_SCROLL;
            }
            return 0;
        }

        public void mouseReleased(MouseEvent e) {
            if (!ui.slider.isEnabled()) {
                return;
            }
            if (ui.scrollTimer != null) {
                ui.scrollTimer.stop();
            }
            ui.dragging = false;
            ui.slider.setValueIsAdjusting(false);
            ui.slider.repaint();
        }

        public void mouseDragged(MouseEvent e) {
            if (!ui.slider.isEnabled() || !ui.dragging) {
                return;
            }
            ui.slider.setValueIsAdjusting(true);
            currentMouseX = e.getX();
            currentMouseY = e.getY();
            if (ui.slider.getOrientation() == SwingConstants.HORIZONTAL) {
                ui.slider.setValue(ui.valueForXPosition(currentMouseX - offset
                        + ui.thumbRect.width / 2));
            } else {
                ui.slider.setValue(ui.valueForYPosition(currentMouseY - offset
                        + ui.thumbRect.height / 2));
            }
        }

        public void mouseMoved(MouseEvent e) {
        }

        /** Si ese punto cae en el pulgar. */
        public boolean shouldScroll(int direction) {
            return true;
        }
    }

    /** El valor que corresponde a ese pixel; la vuelta de {@link #xPositionForValue}. */
    int valueForXPosition(int xPos) {
        int trackLength = trackRect.width;
        int trackLeft = trackRect.x;
        int trackRight = trackRect.x + (trackRect.width - 1);
        int minValue = slider.getMinimum();
        int maxValue = slider.getMaximum();
        if (trackLength <= 0) {
            return minValue;
        }
        int value;
        if (xPos <= trackLeft) {
            value = drawInverted() ? maxValue : minValue;
        } else if (xPos >= trackRight) {
            value = drawInverted() ? minValue : maxValue;
        } else {
            int distanceFromTrackLeft = xPos - trackLeft;
            double valueRange = (double) maxValue - (double) minValue;
            double valuePerPixel = valueRange / (double) trackLength;
            int valueFromTrackLeft = (int) Math.round(distanceFromTrackLeft * valuePerPixel);
            value = drawInverted() ? (maxValue - valueFromTrackLeft)
                    : (minValue + valueFromTrackLeft);
        }
        return value;
    }

    /** Idem en el otro eje. */
    int valueForYPosition(int yPos) {
        int trackLength = trackRect.height;
        int trackTop = trackRect.y;
        int trackBottom = trackRect.y + (trackRect.height - 1);
        int minValue = slider.getMinimum();
        int maxValue = slider.getMaximum();
        if (trackLength <= 0) {
            return minValue;
        }
        int value;
        if (yPos <= trackTop) {
            value = drawInverted() ? minValue : maxValue;
        } else if (yPos >= trackBottom) {
            value = drawInverted() ? maxValue : minValue;
        } else {
            int distanceFromTrackTop = yPos - trackTop;
            double valueRange = (double) maxValue - (double) minValue;
            double valuePerPixel = valueRange / (double) trackLength;
            int valueFromTrackTop = (int) Math.round(distanceFromTrackTop * valuePerPixel);
            value = drawInverted() ? (minValue + valueFromTrackTop)
                    : (maxValue - valueFromTrackTop);
        }
        return value;
    }

    /** El reloj que repite el avance mientras se mantiene apretado; ver la nota de la clase. */
    public class ScrollListener implements ActionListener {

        private final BasicSliderUI ui;
        private int direction = POSITIVE_SCROLL;
        private boolean useBlockIncrement = true;

        /** El parametro es el UI; ver el hallazgo #518. */
        public ScrollListener(BasicSliderUI ui) {
            this.ui = ui;
        }

        public void setDirection(int direction) {
            this.direction = direction;
        }

        public void setScrollByBlock(boolean block) {
            this.useBlockIncrement = block;
        }

        public void actionPerformed(ActionEvent e) {
            if (useBlockIncrement) {
                ui.scrollByBlock(direction);
            } else {
                ui.scrollByUnit(direction);
            }
        }
    }

    /**
     * El que escucha el modelo, el foco, el tamano y las propiedades.
     *
     * <p>Estatico y con el UI como campo, por lo mismo que en todo el paquete.
     */
    private static class Handler implements ChangeListener, ComponentListener, FocusListener,
            PropertyChangeListener {

        private final BasicSliderUI ui;

        Handler(BasicSliderUI ui) {
            this.ui = ui;
        }

        public void stateChanged(ChangeEvent e) {
            if (!ui.isDragging()) {
                ui.calculateThumbLocation();
                ui.slider.repaint();
            }
        }

        public void componentResized(ComponentEvent e) {
            ui.calculateGeometry();
            ui.slider.repaint();
        }

        public void componentMoved(ComponentEvent e) {
        }

        public void componentShown(ComponentEvent e) {
        }

        public void componentHidden(ComponentEvent e) {
        }

        public void focusGained(FocusEvent e) {
            ui.slider.repaint();
        }

        public void focusLost(FocusEvent e) {
            ui.slider.repaint();
        }

        public void propertyChange(PropertyChangeEvent e) {
            String nombre = e.getPropertyName();
            if ("orientation".equals(nombre) || "inverted".equals(nombre)
                    || "labelTable".equals(nombre) || "majorTickSpacing".equals(nombre)
                    || "minorTickSpacing".equals(nombre) || "paintTicks".equals(nombre)
                    || "paintLabels".equals(nombre) || "paintTrack".equals(nombre)
                    || "font".equals(nombre) || "componentOrientation".equals(nombre)) {
                ui.calculateGeometry();
                ui.slider.repaint();
            } else if ("model".equals(nombre)) {
                Object viejo = e.getOldValue();
                Object nuevo = e.getNewValue();
                if (viejo instanceof javax.swing.BoundedRangeModel) {
                    ((javax.swing.BoundedRangeModel) viejo)
                            .removeChangeListener(ui.changeListener);
                }
                if (nuevo instanceof javax.swing.BoundedRangeModel) {
                    ((javax.swing.BoundedRangeModel) nuevo).addChangeListener(ui.changeListener);
                }
                ui.calculateThumbLocation();
                ui.slider.repaint();
            }
        }
    }

    /** La que tira {@link #uninstallUI} si le dan otro componente. */
    private static class IllegalComponentStateException extends IllegalStateException {

        IllegalComponentStateException(String s) {
            super(s);
        }
    }
}
