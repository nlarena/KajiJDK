package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BoundedRangeModel;
import javax.swing.JComponent;
import javax.swing.JProgressBar;
import javax.swing.LookAndFeel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.DimensionUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.ProgressBarUI;
import javax.swing.plaf.UIResource;

/**
 * El aspecto basico de una barra de progreso.
 *
 * <h2>Dos barras distintas en una clase</h2>
 *
 * <p>Una barra <em>determinada</em> sabe cuanto falta y se llena de a poco: es una regla de tres
 * entre el valor del modelo y el ancho disponible. Una <em>indeterminada</em> no sabe nada y lo
 * unico que dice es "sigo trabajando": un bloque que va y viene. Las dos comparten el borde, los
 * colores y el texto, y ahi se acaba el parecido, por eso hay dos metodos de pintado.
 *
 * <h2>El bloque que rebota</h2>
 *
 * <p>El bloque mide un sexto del largo disponible --{@link #getBoxLength}, redondeado-- y recorre el
 * largo de ida y de vuelta en {@link #getFrameCount} cuadros. El numero de cuadros no es una
 * constante: sale de dividir cuanto dura el ciclo entre cada cuanto se repinta, y en Metal eso da
 * sesenta.
 *
 * <p><strong>{@link #getBox} revienta si la barra todavia no fue indeterminada.</strong> Las medidas
 * internas que necesita se calculan cuando la barra <em>entra</em> en modo indeterminado, y antes de
 * eso son nulas. Esta medido: el JDK tira {@code NullPointerException} ahi, y se copia -- una
 * subclase que llame a {@code getBox} fuera de {@code paintIndeterminate} tiene que romperse igual
 * en las dos bibliotecas.
 *
 * <h2>Las celdas que ya no se usan</h2>
 *
 * <p>{@link #getCellLength} y {@link #getCellSpacing} valen 1 y 0, y nadie los mira. Son de cuando
 * la barra se dibujaba como una fila de bloquecitos separados; el aspecto basico la dibuja llena.
 * Quedan porque una subclase puede volver a ese estilo.
 *
 * <h2>El minimo no depende del borde</h2>
 *
 * <p>El ancho minimo de una barra horizontal es diez pixeles, y punto: no se le suman los margenes.
 * Es raro --el preferido si los suma-- y esta medido con dos bordes distintos.
 *
 * <h2>Linea de base</h2>
 *
 * <p>Solo la tiene si la barra muestra su texto. Sin texto no hay nada que apoyar y la respuesta es
 * -1 con comportamiento {@code OTHER}.
 */
public class BasicProgressBarUI extends ProgressBarUI {

    protected JProgressBar progressBar;
    protected ChangeListener changeListener;

    /** El rectangulo del bloque que rebota; nulo hasta que la barra sea indeterminada. */
    protected Rectangle boxRect;

    private PropertyChangeListener propertyListener;
    private Timer animator;
    private int animationIndex = 0;
    private int numFrames = 0;
    private int cellLength = 1;
    private int cellSpacing = 0;
    private Color selectionForeground;
    private Color selectionBackground;

    /** El area de adentro del borde; nula hasta el primer modo indeterminado. */
    private Rectangle componentInnards;

    private static final ColorUIResource FONDO = new ColorUIResource(238, 238, 238);
    private static final ColorUIResource FRENTE = new ColorUIResource(163, 184, 204);
    private static final ColorUIResource SEL_FONDO = new ColorUIResource(99, 130, 191);
    private static final ColorUIResource SEL_FRENTE = new ColorUIResource(238, 238, 238);
    private static final FontUIResource FUENTE = new FontUIResource("Dialog", Font.BOLD, 12);
    private static final DimensionUIResource INTERNO_H = new DimensionUIResource(146, 12);
    private static final DimensionUIResource INTERNO_V = new DimensionUIResource(12, 146);

    /** Cuanto dura una ida y vuelta, y cada cuanto se repinta; de ahi salen los cuadros. */
    private static final int DURACION_DEL_CICLO = 3000;
    private static final int INTERVALO_DE_REPINTADO = 50;

    public BasicProgressBarUI() {
    }

    /** Uno nuevo por barra: guarda el componente y el estado de la animacion. */
    public static ComponentUI createUI(JComponent x) {
        return new BasicProgressBarUI();
    }

    public void installUI(JComponent c) {
        progressBar = (JProgressBar) c;
        installDefaults();
        installListeners();
        if (progressBar.isIndeterminate()) {
            arrancarIndeterminado();
        }
    }

    public void uninstallUI(JComponent c) {
        if (progressBar.isIndeterminate()) {
            pararIndeterminado();
        }
        uninstallDefaults();
        uninstallListeners();
        progressBar = null;
    }

    /** Colores, fuente, borde y opacidad; los valores son los de {@code ProgressBar.*} en Metal. */
    protected void installDefaults() {
        Color fondo = progressBar.getBackground();
        if (fondo == null || fondo instanceof UIResource) {
            progressBar.setBackground(FONDO);
        }
        Color frente = progressBar.getForeground();
        if (frente == null || frente instanceof UIResource) {
            progressBar.setForeground(FRENTE);
        }
        Font fuente = progressBar.getFont();
        if (fuente == null || fuente instanceof UIResource) {
            progressBar.setFont(FUENTE);
        }
        Border borde = progressBar.getBorder();
        if (borde == null || borde instanceof UIResource) {
            progressBar.setBorder(new BorderUIResource.LineBorderUIResource(SEL_FONDO, 1));
        }
        LookAndFeel.installProperty(progressBar, "opaque", Boolean.TRUE);
        selectionBackground = SEL_FONDO;
        selectionForeground = SEL_FRENTE;
        cellLength = 1;
        cellSpacing = 0;
    }

    /** No saca nada; ver {@link BasicPanelUI#uninstallDefaults}. */
    protected void uninstallDefaults() {
    }

    protected void installListeners() {
        changeListener = new Handler();
        progressBar.addChangeListener(changeListener);
        propertyListener = new Handler();
        progressBar.addPropertyChangeListener(propertyListener);
    }

    protected void uninstallListeners() {
        progressBar.removeChangeListener(changeListener);
        progressBar.removePropertyChangeListener(propertyListener);
        changeListener = null;
        propertyListener = null;
    }

    /** Cuantos cuadros dura una ida y vuelta; cero hasta que la barra sea indeterminada. */
    protected final int getFrameCount() {
        return numFrames;
    }

    /** En que cuadro va la animacion. */
    protected int getAnimationIndex() {
        return animationIndex;
    }

    /** Lo pone y repinta. */
    protected void setAnimationIndex(int newValue) {
        if (animationIndex != newValue) {
            animationIndex = newValue;
            if (progressBar != null) {
                progressBar.repaint();
            }
        }
    }

    /** Pasa al cuadro siguiente, volviendo a cero al terminar el ciclo. */
    protected void incrementAnimationIndex() {
        int newValue = getAnimationIndex() + 1;
        setAnimationIndex((numFrames > 0 && newValue < numFrames) ? newValue : 0);
    }

    /** Arranca el reloj de la animacion. */
    protected void startAnimationTimer() {
        if (animator == null) {
            animator = new Timer(INTERVALO_DE_REPINTADO, new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    incrementAnimationIndex();
                }
            });
        }
        animator.start();
    }

    protected void stopAnimationTimer() {
        if (animator != null) {
            animator.stop();
        }
        setAnimationIndex(0);
    }

    private void arrancarIndeterminado() {
        numFrames = DURACION_DEL_CICLO / INTERVALO_DE_REPINTADO;
        componentInnards = new Rectangle();
        boxRect = new Rectangle();
        actualizarInternas();
        startAnimationTimer();
    }

    private void pararIndeterminado() {
        stopAnimationTimer();
    }

    private void actualizarInternas() {
        Insets b = progressBar.getInsets();
        componentInnards.setBounds(b.left, b.top,
                progressBar.getWidth() - (b.left + b.right),
                progressBar.getHeight() - (b.top + b.bottom));
    }

    protected int getCellLength() {
        return cellLength;
    }

    protected void setCellLength(int cellLen) {
        this.cellLength = cellLen;
    }

    protected int getCellSpacing() {
        return cellSpacing;
    }

    protected void setCellSpacing(int cellSpace) {
        this.cellSpacing = cellSpace;
    }

    protected Color getSelectionForeground() {
        return selectionForeground;
    }

    protected Color getSelectionBackground() {
        return selectionBackground;
    }

    /** Un sexto del largo, redondeado; ver la nota de la clase. */
    protected int getBoxLength(int availableLength, int otherDimension) {
        return (int) Math.round(availableLength / 6.0);
    }

    /**
     * Donde esta el bloque que rebota en este cuadro.
     *
     * @throws NullPointerException si la barra todavia no fue indeterminada; ver la nota de la clase
     */
    protected Rectangle getBox(Rectangle r) {
        // Esta llamada es la que revienta antes de tiempo, y es a proposito: `componentInnards`
        // es nulo hasta que la barra entra en modo indeterminado. Ver la nota de la clase.
        actualizarInternas();
        if (r == null) {
            r = new Rectangle();
        }
        int cuadros = (numFrames > 0) ? numFrames : 1;
        boolean horizontal = progressBar.getOrientation() == SwingConstants.HORIZONTAL;
        int largo = horizontal ? componentInnards.width : componentInnards.height;
        int grueso = horizontal ? componentInnards.height : componentInnards.width;
        int bloque = getBoxLength(largo, grueso);
        int recorrido = largo - bloque;
        if (recorrido < 0) {
            recorrido = 0;
        }
        // La ida ocupa la mitad de los cuadros y la vuelta la otra mitad.
        int mitad = cuadros / 2;
        int i = getAnimationIndex();
        int paso;
        if (mitad == 0) {
            paso = 0;
        } else if (i < mitad) {
            paso = recorrido * i / mitad;
        } else {
            paso = recorrido * (cuadros - i) / mitad;
        }
        if (horizontal) {
            r.x = componentInnards.x + paso;
            r.y = componentInnards.y;
            r.width = bloque;
            r.height = componentInnards.height;
        } else {
            r.x = componentInnards.x;
            r.y = componentInnards.y + paso;
            r.width = componentInnards.width;
            r.height = bloque;
        }
        return r;
    }

    /** Cuanto de la barra esta lleno, en pixeles. */
    protected int getAmountFull(Insets b, int width, int height) {
        int amountFull = 0;
        BoundedRangeModel model = progressBar.getModel();
        if ((model.getMaximum() - model.getMinimum()) != 0) {
            if (progressBar.getOrientation() == SwingConstants.HORIZONTAL) {
                amountFull = (int) Math.round(width * progressBar.getPercentComplete());
            } else {
                amountFull = (int) Math.round(height * progressBar.getPercentComplete());
            }
        }
        return amountFull;
    }

    protected Dimension getPreferredInnerHorizontal() {
        return new DimensionUIResource(INTERNO_H.width, INTERNO_H.height);
    }

    protected Dimension getPreferredInnerVertical() {
        return new DimensionUIResource(INTERNO_V.width, INTERNO_V.height);
    }

    /** El interior mas los margenes, agrandado si el texto no entra. */
    public Dimension getPreferredSize(JComponent c) {
        Dimension size;
        Insets border = progressBar.getInsets();
        FontMetrics fontSizer = progressBar.getFontMetrics(progressBar.getFont());
        if (progressBar.getOrientation() == SwingConstants.HORIZONTAL) {
            // Una copia, y en un `Dimension` pelado: lo que devuelve `getPreferredInnerHorizontal`
            // es del aspecto, y el tamano preferido de un componente no lo es.
            size = new Dimension(getPreferredInnerHorizontal());
            if (progressBar.isStringPainted()) {
                int stringHeight = fontSizer.getHeight() + fontSizer.getDescent();
                if (stringHeight > size.height) {
                    size.height = stringHeight;
                }
                String texto = progressBar.getString();
                if (texto != null) {
                    int stringWidth = fontSizer.stringWidth(texto);
                    if (stringWidth > size.width) {
                        size.width = stringWidth;
                    }
                }
            }
        } else {
            size = new Dimension(getPreferredInnerVertical());
            if (progressBar.isStringPainted()) {
                int stringWidth = fontSizer.getHeight() + fontSizer.getDescent();
                if (stringWidth > size.width) {
                    size.width = stringWidth;
                }
                String texto = progressBar.getString();
                if (texto != null) {
                    int stringHeight = fontSizer.stringWidth(texto);
                    if (stringHeight > size.height) {
                        size.height = stringHeight;
                    }
                }
            }
        }
        size.width += border.left + border.right;
        size.height += border.top + border.bottom;
        return size;
    }

    /** Diez pixeles de largo; ver la nota de la clase. */
    public Dimension getMinimumSize(JComponent c) {
        Dimension pref = getPreferredSize(progressBar);
        if (progressBar.getOrientation() == SwingConstants.HORIZONTAL) {
            pref.width = 10;
        } else {
            pref.height = 10;
        }
        return pref;
    }

    /** Se estira a lo largo y nada a lo ancho. */
    public Dimension getMaximumSize(JComponent c) {
        Dimension pref = getPreferredSize(progressBar);
        if (progressBar.getOrientation() == SwingConstants.HORIZONTAL) {
            pref.width = Short.MAX_VALUE;
        } else {
            pref.height = Short.MAX_VALUE;
        }
        return pref;
    }

    /**
     * Donde apoya el texto, o -1 si no hay texto; ver la nota de la clase.
     *
     * @throws NullPointerException si el componente es nulo
     * @throws IllegalArgumentException si el ancho o el alto son negativos
     */
    public int getBaseline(JComponent c, int width, int height) {
        super.getBaseline(c, width, height);
        if (!progressBar.isStringPainted()) {
            return -1;
        }
        FontMetrics metrics = progressBar.getFontMetrics(progressBar.getFont());
        Insets insets = progressBar.getInsets();
        int y = insets.top;
        int alto = height - insets.top - insets.bottom;
        return y + (alto - metrics.getAscent() - metrics.getDescent()) / 2 + metrics.getAscent();
    }

    /**
     * {@code CENTER_OFFSET} con texto y {@code OTHER} sin el.
     *
     * @throws NullPointerException si el componente es nulo
     */
    public Component.BaselineResizeBehavior getBaselineResizeBehavior(JComponent c) {
        super.getBaselineResizeBehavior(c);
        if (progressBar.isStringPainted()) {
            return Component.BaselineResizeBehavior.CENTER_OFFSET;
        }
        return Component.BaselineResizeBehavior.OTHER;
    }

    public void paint(Graphics g, JComponent c) {
        if (progressBar.isIndeterminate()) {
            paintIndeterminate(g, c);
        } else {
            paintDeterminate(g, c);
        }
    }

    /** La parte llena, y el texto encima si corresponde. */
    protected void paintDeterminate(Graphics g, JComponent c) {
        Insets b = progressBar.getInsets();
        int barRectWidth = progressBar.getWidth() - (b.right + b.left);
        int barRectHeight = progressBar.getHeight() - (b.top + b.bottom);
        if (barRectWidth <= 0 || barRectHeight <= 0) {
            return;
        }
        int amountFull = getAmountFull(b, barRectWidth, barRectHeight);
        g.setColor(progressBar.getForeground());
        if (progressBar.getOrientation() == SwingConstants.HORIZONTAL) {
            g.fillRect(b.left, b.top, amountFull, barRectHeight);
        } else {
            // La barra vertical se llena de abajo hacia arriba.
            g.fillRect(b.left, b.top + (barRectHeight - amountFull),
                    barRectWidth, amountFull);
        }
        if (progressBar.isStringPainted()) {
            paintString(g, b.left, b.top, barRectWidth, barRectHeight, amountFull, b);
        }
    }

    /** El bloque que rebota, y el texto encima si corresponde. */
    protected void paintIndeterminate(Graphics g, JComponent c) {
        Insets b = progressBar.getInsets();
        int barRectWidth = progressBar.getWidth() - (b.right + b.left);
        int barRectHeight = progressBar.getHeight() - (b.top + b.bottom);
        if (barRectWidth <= 0 || barRectHeight <= 0) {
            return;
        }
        boxRect = getBox(boxRect);
        if (boxRect != null) {
            g.setColor(progressBar.getForeground());
            g.fillRect(boxRect.x, boxRect.y, boxRect.width, boxRect.height);
        }
        if (progressBar.isStringPainted()) {
            paintString(g, b.left, b.top, barRectWidth, barRectHeight, 0, b);
        }
    }

    /**
     * El texto, con el color cambiado sobre la parte llena.
     *
     * <p>El texto se pinta dos veces con recortes distintos: una con el color de siempre sobre el
     * fondo y otra con el de seleccion sobre la parte llena. Sin eso, el texto quedaria ilegible en
     * la mitad de la barra.
     */
    protected void paintString(Graphics g, int x, int y, int width, int height, int amountFull,
            Insets b) {
        String progressString = progressBar.getString();
        if (progressString == null) {
            return;
        }
        g.setFont(progressBar.getFont());
        Point renderLocation = getStringPlacement(g, progressString, x, y, width, height);
        java.awt.Rectangle oldClip = g.getClipBounds();

        if (progressBar.getOrientation() == SwingConstants.HORIZONTAL) {
            g.setColor(getSelectionBackground());
            g.drawString(progressString, renderLocation.x, renderLocation.y);
            g.setColor(getSelectionForeground());
            g.clipRect(x, y, amountFull, height);
            g.drawString(progressString, renderLocation.x, renderLocation.y);
        } else {
            g.setColor(getSelectionBackground());
            g.drawString(progressString, renderLocation.x, renderLocation.y);
            g.setColor(getSelectionForeground());
            g.clipRect(x, y + height - amountFull, width, amountFull);
            g.drawString(progressString, renderLocation.x, renderLocation.y);
        }
        if (oldClip != null) {
            g.setClip(oldClip);
        }
    }

    /** Donde arranca el texto: centrado en la barra. */
    protected Point getStringPlacement(Graphics g, String progressString, int x, int y,
            int width, int height) {
        FontMetrics fontSizer = progressBar.getFontMetrics(progressBar.getFont());
        int stringWidth = fontSizer.stringWidth(progressString);
        if (progressBar.getOrientation() == SwingConstants.HORIZONTAL) {
            return new Point(x + Math.round(width / 2 - stringWidth / 2),
                    y + ((height + fontSizer.getAscent() - fontSizer.getLeading()
                            - fontSizer.getDescent()) / 2));
        }
        return new Point(x + ((width - fontSizer.getAscent() + fontSizer.getLeading()
                + fontSizer.getDescent()) / 2),
                y + Math.round(height / 2 - stringWidth / 2));
    }

    /** Repinta al cambiar el valor, y arranca o para la animacion al cambiar de modo. */
    private class Handler implements ChangeListener, PropertyChangeListener {

        public void stateChanged(ChangeEvent e) {
            progressBar.repaint();
        }

        public void propertyChange(PropertyChangeEvent e) {
            if ("indeterminate".equals(e.getPropertyName())) {
                if (Boolean.TRUE.equals(e.getNewValue())) {
                    arrancarIndeterminado();
                } else {
                    pararIndeterminado();
                }
                progressBar.repaint();
            }
        }
    }
}
