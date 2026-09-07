package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.LookAndFeel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.InsetsUIResource;
import javax.swing.plaf.TabbedPaneUI;
import javax.swing.plaf.UIResource;
import javax.swing.text.View;

/**
 * El aspecto basico de un panel de solapas.
 *
 * <h2>Las solapas van en corridas, no en una fila</h2>
 *
 * <p>Cuando las solapas no entran en el ancho del panel, no se achican ni aparece una barra: se
 * apilan en varias filas -- "corridas" --. {@link #tabRuns} guarda en que indice arranca cada una y
 * {@link #runCount} cuantas hay. Toda la aritmetica de la clase gira alrededor de eso: cual es la
 * solapa siguiente ({@link #getNextTabIndex}), cual es la siguiente <em>dentro de la corrida</em>
 * ({@link #getNextTabIndexInRun}), y cual corrida sigue ({@link #getNextTabRun}).
 *
 * <p>Y las corridas se <em>rotan</em>: la corrida de la solapa elegida se lleva siempre al frente,
 * pegada al contenido. Sin eso, elegir una solapa de la fila de arriba dejaria dos filas entre ella
 * y su contenido, y la linea que las une se veria cortada.
 *
 * <h2>El estado se calcula tarde</h2>
 *
 * <p>Despues de instalar, {@link #runCount} vale cero y {@link #maxTabHeight} tambien: las cuentas
 * las hace el acomodador, y no corre hasta que alguien pregunta. Por eso
 * {@link #getTabRunCount} devuelve 2 en un panel de dos solapas aunque el campo diga cero -- fuerza
 * el calculo antes de contestar --. Esta medido, y es facil de confundir con un bug.
 *
 * <h2>Un panel de solapas no es opaco</h2>
 *
 * <p>Y su color de fondo es el de la sombra de las solapas, no un gris de panel. Los dos estan
 * medidos y los dos sorprenden; el motivo es que lo que se ve detras de las solapas es el borde del
 * contenido, no un fondo.
 *
 * <h2>Lo que queda dicho</h2>
 *
 * <p>El modo de una sola fila con botones de desplazamiento --{@code SCROLL_TAB_LAYOUT}-- necesita
 * botones y un viewport; aca todas las solapas se acomodan en corridas. Un panel puesto en ese modo
 * se ve como uno envuelto.
 *
 * <p>Las cuatro teclas protegidas quedan en nulo, como en el resto del paquete.
 */
public class BasicTabbedPaneUI extends TabbedPaneUI implements SwingConstants {

    protected JTabbedPane tabPane;

    protected Color highlight;
    protected Color lightHighlight;
    protected Color shadow;
    protected Color darkShadow;
    protected Color focus;

    protected int textIconGap;
    protected int tabRunOverlay;

    protected Insets tabInsets;
    protected Insets selectedTabPadInsets;
    protected Insets tabAreaInsets;
    protected Insets contentBorderInsets;

    /** Sin uso; ver la nota de la clase. */
    protected KeyStroke upKey;

    /** Sin uso; ver la nota de la clase. */
    protected KeyStroke downKey;

    /** Sin uso; ver la nota de la clase. */
    protected KeyStroke leftKey;

    /** Sin uso; ver la nota de la clase. */
    protected KeyStroke rightKey;

    /** Donde arranca cada corrida; ver la nota de la clase. */
    protected int[] tabRuns = new int[10];

    protected int runCount = 0;
    protected int selectedRun = -1;
    protected Rectangle[] rects = new Rectangle[0];
    protected int maxTabHeight;
    protected int maxTabWidth;

    protected ChangeListener tabChangeListener;
    protected PropertyChangeListener propertyChangeListener;
    protected MouseListener mouseListener;
    protected FocusListener focusListener;

    /** Un rectangulo de trabajo; se reusa para no crear uno por cada cuenta. */
    protected transient Rectangle calcRect = new Rectangle();

    private Component visibleComponent;
    private int rolloverTabIndex = -1;
    private boolean layoutCalculado;

    private static final ColorUIResource SOMBRA = new ColorUIResource(184, 207, 229);
    private static final ColorUIResource SOMBRA_OSCURA = new ColorUIResource(122, 138, 153);
    private static final ColorUIResource BRILLO = new ColorUIResource(238, 238, 238);
    private static final ColorUIResource BRILLO_CLARO = new ColorUIResource(255, 255, 255);
    private static final ColorUIResource FOCO = new ColorUIResource(99, 130, 191);
    private static final ColorUIResource FRENTE = new ColorUIResource(51, 51, 51);
    private static final FontUIResource FUENTE = new FontUIResource("Dialog", Font.BOLD, 12);

    public BasicTabbedPaneUI() {
    }

    /** Uno nuevo por panel: guarda las corridas y los rectangulos de las solapas. */
    public static ComponentUI createUI(JComponent c) {
        return new BasicTabbedPaneUI();
    }

    public void installUI(JComponent c) {
        this.tabPane = (JTabbedPane) c;
        calcRect = new Rectangle(0, 0, 0, 0);
        tabRuns = new int[10];
        installDefaults();
        installComponents();
        installListeners();
        installKeyboardActions();
    }

    public void uninstallUI(JComponent c) {
        uninstallKeyboardActions();
        uninstallListeners();
        uninstallComponents();
        uninstallDefaults();
        tabPane = null;
    }

    /** Colores, insets y fuente; los valores son los de {@code TabbedPane.*} en Metal. */
    protected void installDefaults() {
        Color fondo = tabPane.getBackground();
        if (fondo == null || fondo instanceof UIResource) {
            tabPane.setBackground(SOMBRA);
        }
        Color frente = tabPane.getForeground();
        if (frente == null || frente instanceof UIResource) {
            tabPane.setForeground(FRENTE);
        }
        Font fuente = tabPane.getFont();
        if (fuente == null || fuente instanceof UIResource) {
            tabPane.setFont(FUENTE);
        }
        highlight = BRILLO;
        lightHighlight = BRILLO_CLARO;
        shadow = SOMBRA;
        darkShadow = SOMBRA_OSCURA;
        focus = FOCO;

        textIconGap = 4;
        tabRunOverlay = 2;
        tabInsets = new InsetsUIResource(0, 9, 1, 9);
        selectedTabPadInsets = new InsetsUIResource(2, 2, 2, 1);
        tabAreaInsets = new Insets(2, 2, 0, 6);
        contentBorderInsets = new Insets(4, 2, 3, 3);

        LookAndFeel.installProperty(tabPane, "opaque", Boolean.FALSE);
    }

    /** No saca nada; ver {@link BasicPanelUI#uninstallDefaults}. */
    protected void uninstallDefaults() {
        highlight = null;
        lightHighlight = null;
        shadow = null;
        darkShadow = null;
        focus = null;
        tabInsets = null;
        selectedTabPadInsets = null;
        tabAreaInsets = null;
        contentBorderInsets = null;
    }

    /** Pone el acomodador de corridas. */
    protected void installComponents() {
        tabPane.setLayout(createLayoutManager());
    }

    protected void uninstallComponents() {
        tabPane.setLayout(null);
    }

    protected LayoutManager createLayoutManager() {
        return new TabbedPaneLayout(this);
    }

    protected void installListeners() {
        propertyChangeListener = createPropertyChangeListener();
        tabPane.addPropertyChangeListener(propertyChangeListener);
        tabChangeListener = createChangeListener();
        tabPane.addChangeListener(tabChangeListener);
        mouseListener = createMouseListener();
        tabPane.addMouseListener(mouseListener);
        focusListener = createFocusListener();
        tabPane.addFocusListener(focusListener);
    }

    protected void uninstallListeners() {
        tabPane.removePropertyChangeListener(propertyChangeListener);
        tabPane.removeChangeListener(tabChangeListener);
        tabPane.removeMouseListener(mouseListener);
        tabPane.removeFocusListener(focusListener);
        propertyChangeListener = null;
        tabChangeListener = null;
        mouseListener = null;
        focusListener = null;
    }

    /** Sin atajos propios; ver la nota de la clase. */
    protected void installKeyboardActions() {
    }

    protected void uninstallKeyboardActions() {
    }

    protected PropertyChangeListener createPropertyChangeListener() {
        return new Handler(this);
    }

    protected ChangeListener createChangeListener() {
        return new Handler(this);
    }

    protected MouseListener createMouseListener() {
        return new Handler(this);
    }

    protected FocusListener createFocusListener() {
        return new Handler(this);
    }

    protected FontMetrics getFontMetrics() {
        Font font = tabPane.getFont();
        return tabPane.getFontMetrics(font);
    }

    protected Icon getIconForTab(int tabIndex) {
        return (!tabPane.isEnabled() || !tabPane.isEnabledAt(tabIndex))
                ? tabPane.getDisabledIconAt(tabIndex) : tabPane.getIconAt(tabIndex);
    }

    /** La vista de HTML del titulo, si lo es; ver {@link BasicHTML}. */
    protected View getTextViewForTab(int tabIndex) {
        return null;
    }

    protected Insets getTabInsets(int tabPlacement, int tabIndex) {
        return tabInsets;
    }

    protected Insets getSelectedTabPadInsets(int tabPlacement) {
        rotateInsets(selectedTabPadInsets, calcRect2, tabPlacement);
        return new Insets(calcRect2.top, calcRect2.left, calcRect2.bottom, calcRect2.right);
    }

    private final Insets calcRect2 = new Insets(0, 0, 0, 0);

    protected Insets getTabAreaInsets(int tabPlacement) {
        rotateInsets(tabAreaInsets, calcRect2, tabPlacement);
        return new Insets(calcRect2.top, calcRect2.left, calcRect2.bottom, calcRect2.right);
    }

    protected Insets getContentBorderInsets(int tabPlacement) {
        rotateInsets(contentBorderInsets, calcRect2, tabPlacement);
        return new Insets(calcRect2.top, calcRect2.left, calcRect2.bottom, calcRect2.right);
    }

    /**
     * Gira unos insets segun de que lado van las solapas.
     *
     * <p>Los numeros estan escritos para solapas arriba; con las solapas a la izquierda, el "arriba"
     * de esos insets pasa a ser el "izquierda". Girarlos es mas barato -- y mucho menos propenso a
     * error -- que tener cuatro juegos de numeros.
     */
    protected static void rotateInsets(Insets topInsets, Insets targetInsets, int targetPlacement) {
        if (targetPlacement == LEFT) {
            targetInsets.top = topInsets.left;
            targetInsets.left = topInsets.top;
            targetInsets.bottom = topInsets.right;
            targetInsets.right = topInsets.bottom;
        } else if (targetPlacement == BOTTOM) {
            targetInsets.top = topInsets.bottom;
            targetInsets.left = topInsets.left;
            targetInsets.bottom = topInsets.top;
            targetInsets.right = topInsets.right;
        } else if (targetPlacement == RIGHT) {
            targetInsets.top = topInsets.left;
            targetInsets.left = topInsets.bottom;
            targetInsets.bottom = topInsets.right;
            targetInsets.right = topInsets.top;
        } else {
            targetInsets.top = topInsets.top;
            targetInsets.left = topInsets.left;
            targetInsets.bottom = topInsets.bottom;
            targetInsets.right = topInsets.right;
        }
    }

    protected int getTabRunOverlay(int tabPlacement) {
        return tabRunOverlay;
    }

    protected int getTabRunIndent(int tabPlacement, int run) {
        return 0;
    }

    /** Si la corrida se estira para llenar el ancho; la ultima no. */
    protected boolean shouldPadTabRun(int tabPlacement, int run) {
        return runCount > 1;
    }

    /** Si la corrida elegida se lleva al frente; ver la nota de la clase. */
    protected boolean shouldRotateTabRuns(int tabPlacement) {
        return true;
    }

    /** Alto de una solapa: el del texto mas sus insets mas dos. */
    protected int calculateTabHeight(int tabPlacement, int tabIndex, int fontHeight) {
        int height = 0;
        Component c = tabPane.getTabComponentAt(tabIndex);
        if (c != null) {
            height = c.getPreferredSize().height;
        } else {
            View v = getTextViewForTab(tabIndex);
            if (v != null) {
                height += (int) v.getPreferredSpan(View.Y_AXIS);
            } else {
                height += fontHeight;
            }
            Icon icon = getIconForTab(tabIndex);
            if (icon != null) {
                height = Math.max(height, icon.getIconHeight());
            }
        }
        Insets insets = getTabInsets(tabPlacement, tabIndex);
        height += insets.top + insets.bottom + 2;
        return height;
    }

    protected int calculateMaxTabHeight(int tabPlacement) {
        FontMetrics metrics = getFontMetrics();
        int tabCount = tabPane.getTabCount();
        int result = 0;
        int fontHeight = metrics.getHeight();
        for (int i = 0; i < tabCount; i++) {
            result = Math.max(calculateTabHeight(tabPlacement, i, fontHeight), result);
        }
        return result;
    }

    /** Ancho de una solapa: el del texto mas sus insets mas tres. */
    protected int calculateTabWidth(int tabPlacement, int tabIndex, FontMetrics metrics) {
        Insets insets = getTabInsets(tabPlacement, tabIndex);
        int width = insets.left + insets.right + 3;
        Component tabComponent = tabPane.getTabComponentAt(tabIndex);
        if (tabComponent != null) {
            width += tabComponent.getPreferredSize().width;
            return width;
        }
        Icon icon = getIconForTab(tabIndex);
        if (icon != null) {
            width += icon.getIconWidth() + textIconGap;
        }
        View v = getTextViewForTab(tabIndex);
        if (v != null) {
            width += (int) v.getPreferredSpan(View.X_AXIS);
        } else {
            String title = tabPane.getTitleAt(tabIndex);
            width += metrics.stringWidth(title);
        }
        return width;
    }

    protected int calculateMaxTabWidth(int tabPlacement) {
        FontMetrics metrics = getFontMetrics();
        int tabCount = tabPane.getTabCount();
        int result = 0;
        for (int i = 0; i < tabCount; i++) {
            result = Math.max(calculateTabWidth(tabPlacement, i, metrics), result);
        }
        return result;
    }

    /** Alto del area de solapas: las corridas menos el solape, mas los insets. */
    protected int calculateTabAreaHeight(int tabPlacement, int horizRunCount, int maxTabHeight) {
        Insets insets = getTabAreaInsets(tabPlacement);
        int overlay = getTabRunOverlay(tabPlacement);
        return (horizRunCount > 0)
                ? horizRunCount * (maxTabHeight - overlay) + overlay
                        + insets.top + insets.bottom
                : 0;
    }

    protected int calculateTabAreaWidth(int tabPlacement, int vertRunCount, int maxTabWidth) {
        Insets insets = getTabAreaInsets(tabPlacement);
        int overlay = getTabRunOverlay(tabPlacement);
        return (vertRunCount > 0)
                ? vertRunCount * (maxTabWidth - overlay) + overlay
                        + insets.left + insets.right
                : 0;
    }

    /** Agranda la tabla de corridas cuando no entran mas. */
    protected void expandTabRunsArray() {
        int[] mas = new int[tabRuns.length * 2];
        System.arraycopy(tabRuns, 0, mas, 0, tabRuns.length);
        tabRuns = mas;
    }

    /** Se asegura de que haya un rectangulo por solapa. */
    protected void assureRectsCreated(int tabCount) {
        if (rects == null || rects.length < tabCount) {
            Rectangle[] nuevos = new Rectangle[tabCount];
            int viejos = (rects == null) ? 0 : rects.length;
            for (int i = 0; i < viejos && i < tabCount; i++) {
                nuevos[i] = rects[i];
            }
            for (int i = viejos; i < tabCount; i++) {
                nuevos[i] = new Rectangle();
            }
            rects = nuevos;
        }
    }

    /** Cual solapa tiene el foco; la elegida. */
    protected int getFocusIndex() {
        return tabPane.getSelectedIndex();
    }

    /**
     * Un boton de desplazamiento del area de solapas.
     *
     * <p>Solo lo usa el modo de una sola fila, que esta biblioteca no acomoda; ver la nota de la
     * clase. El boton se arma igual, para que una subclase que lo quiera lo tenga.
     */
    protected javax.swing.JButton createScrollButton(int direction) {
        if (direction != SOUTH && direction != NORTH && direction != EAST
                && direction != WEST) {
            throw new IllegalArgumentException("Direction must be one of: "
                    + "SOUTH, NORTH, EAST or WEST");
        }
        return new BasicArrowButton(direction);
    }

    /** Fuerza el calculo si quedo viejo; ver la nota de la clase. */
    private void ensureCurrentLayout() {
        if (!layoutCalculado) {
            LayoutManager lm = tabPane.getLayout();
            if (lm instanceof TabbedPaneLayout) {
                ((TabbedPaneLayout) lm).calculateLayoutInfo();
            }
        }
    }

    public int getTabRunCount(JTabbedPane pane) {
        ensureCurrentLayout();
        return runCount;
    }

    /** El rectangulo de esa solapa; ver la nota de la clase sobre el calculo tardio. */
    public Rectangle getTabBounds(JTabbedPane pane, int i) {
        ensureCurrentLayout();
        return getTabBounds(i, new Rectangle());
    }

    protected Rectangle getTabBounds(int tabIndex, Rectangle dest) {
        if (rects == null || tabIndex < 0 || tabIndex >= rects.length
                || rects[tabIndex] == null) {
            dest.setBounds(0, 0, 0, 0);
            return dest;
        }
        dest.setBounds(rects[tabIndex]);
        return dest;
    }

    /** La corrida en la que esta esa solapa. */
    private int getRunForTab(int tabCount, int tabIndex) {
        for (int i = 0; i < runCount; i++) {
            int first = tabRuns[i];
            int last = lastTabInRun(tabCount, i);
            if (tabIndex >= first && tabIndex <= last) {
                return i;
            }
        }
        return 0;
    }

    /** La ultima solapa de esa corrida. */
    protected int lastTabInRun(int tabCount, int run) {
        if (runCount == 1) {
            return tabCount - 1;
        }
        int nextRun = (run == runCount - 1) ? 0 : run + 1;
        if (tabRuns[nextRun] == 0) {
            return tabCount - 1;
        }
        return tabRuns[nextRun] - 1;
    }

    protected int getNextTabRun(int baseRun) {
        return (baseRun + 1) % runCount;
    }

    protected int getPreviousTabRun(int baseRun) {
        return ((baseRun - 1) >= 0) ? (baseRun - 1) : (runCount - 1);
    }

    protected int getNextTabIndex(int base) {
        return (base + 1) % tabPane.getTabCount();
    }

    protected int getPreviousTabIndex(int base) {
        int i = base - 1;
        return (i < 0) ? (tabPane.getTabCount() - 1) : i;
    }

    protected int getNextTabIndexInRun(int tabCount, int base) {
        if (runCount < 2) {
            return getNextTabIndex(base);
        }
        int currentRun = getRunForTab(tabCount, base);
        int next = getNextTabIndex(base);
        if (next == tabRuns[getNextTabRun(currentRun)]) {
            return tabRuns[currentRun];
        }
        return next;
    }

    protected int getPreviousTabIndexInRun(int tabCount, int base) {
        if (runCount < 2) {
            return getPreviousTabIndex(base);
        }
        int currentRun = getRunForTab(tabCount, base);
        if (base == tabRuns[currentRun]) {
            int previousRun = getPreviousTabRun(currentRun);
            return lastTabInRun(tabCount, previousRun);
        }
        return getPreviousTabIndex(base);
    }

    /** Cuanto se corre esa corrida; el basico no corre ninguna. */
    protected int getTabRunOffset(int tabPlacement, int tabCount, int tabIndex,
            boolean forward) {
        return 0;
    }

    /** Cuanto se corre el texto de la solapa elegida; el basico no lo corre. */
    protected int getTabLabelShiftX(int tabPlacement, int tabIndex, boolean isSelected) {
        return 0;
    }

    protected int getTabLabelShiftY(int tabPlacement, int tabIndex, boolean isSelected) {
        return 0;
    }

    protected Component getVisibleComponent() {
        return visibleComponent;
    }

    /** Muestra el contenido de la solapa elegida y esconde el anterior. */
    protected void setVisibleComponent(Component component) {
        if (visibleComponent != null && visibleComponent != component
                && visibleComponent.getParent() == tabPane
                && visibleComponent.isVisible()) {
            visibleComponent.setVisible(false);
        }
        if (component != null && !component.isVisible()) {
            component.setVisible(true);
        }
        visibleComponent = component;
    }

    /** La solapa que tiene el mouse encima; un aspecto puede dibujarla distinto. */
    protected void setRolloverTab(int index) {
        rolloverTabIndex = index;
    }

    protected int getRolloverTab() {
        return rolloverTabIndex;
    }

    /** Elige la solapa siguiente, la anterior, o la de la corrida de al lado. */
    protected void navigateSelectedTab(int direction) {
        int tabCount = tabPane.getTabCount();
        if (tabCount <= 0) {
            return;
        }
        int current = tabPane.getSelectedIndex();
        if (direction == NORTH || direction == WEST) {
            selectPreviousTab(current);
        } else {
            selectNextTab(current);
        }
    }

    protected void selectNextTab(int current) {
        int tabIndex = getNextTabIndex(current);
        while (tabIndex != current && !tabPane.isEnabledAt(tabIndex)) {
            tabIndex = getNextTabIndex(tabIndex);
        }
        tabPane.setSelectedIndex(tabIndex);
    }

    protected void selectPreviousTab(int current) {
        int tabIndex = getPreviousTabIndex(current);
        while (tabIndex != current && !tabPane.isEnabledAt(tabIndex)) {
            tabIndex = getPreviousTabIndex(tabIndex);
        }
        tabPane.setSelectedIndex(tabIndex);
    }

    protected void selectNextTabInRun(int current) {
        int tabCount = tabPane.getTabCount();
        int tabIndex = getNextTabIndexInRun(tabCount, current);
        while (tabIndex != current && !tabPane.isEnabledAt(tabIndex)) {
            tabIndex = getNextTabIndexInRun(tabCount, tabIndex);
        }
        tabPane.setSelectedIndex(tabIndex);
    }

    protected void selectPreviousTabInRun(int current) {
        int tabCount = tabPane.getTabCount();
        int tabIndex = getPreviousTabIndexInRun(tabCount, current);
        while (tabIndex != current && !tabPane.isEnabledAt(tabIndex)) {
            tabIndex = getPreviousTabIndexInRun(tabCount, tabIndex);
        }
        tabPane.setSelectedIndex(tabIndex);
    }

    /** Elige la solapa mas parecida de la corrida de al lado. */
    protected void selectAdjacentRunTab(int tabPlacement, int tabIndex, int offset) {
        if (runCount < 2) {
            return;
        }
        int newIndex = tabIndex + offset;
        int tabCount = tabPane.getTabCount();
        if (newIndex < 0) {
            newIndex = tabCount - 1;
        } else if (newIndex >= tabCount) {
            newIndex = 0;
        }
        tabPane.setSelectedIndex(newIndex);
    }

    /**
     * Donde apoya el texto de esa solapa.
     *
     * <p>Se mide con la solapa, no con el panel: dos solapas de distinta altura tienen lineas de
     * base distintas, y quien pregunta quiere la de la que se ve.
     */
    protected int getBaseline(int tab) {
        if (tabPane.getTabComponentAt(tab) != null) {
            int offset = getBaselineOffset();
            if (offset != 0) {
                return -1;
            }
            Component c = tabPane.getTabComponentAt(tab);
            Dimension pref = c.getPreferredSize();
            Insets insets = getTabInsets(tabPane.getTabPlacement(), tab);
            int loc = getTabLabelShiftY(tabPane.getTabPlacement(), tab, false);
            return c.getBaseline(pref.width, pref.height) + insets.top + loc;
        }
        View view = getTextViewForTab(tab);
        if (view != null) {
            return -1;
        }
        FontMetrics metrics = getFontMetrics();
        int alto = calculateTabHeight(tabPane.getTabPlacement(), tab, metrics.getHeight());
        return (alto - metrics.getHeight()) / 2 + metrics.getAscent() + getBaselineOffset();
    }

    /**
     * Cuanto se corre la linea de base de una solapa.
     *
     * <p>Un pixel, y de signo distinto segun el lado y segun haya una solapa o varias. Es un ajuste
     * a ojo del JDK -- con una sola solapa el dibujo queda un pixel corrido respecto de con dos --,
     * y esta medido: sin el, la linea de base de un panel de dos solapas arriba da 14 en vez de 15.
     */
    protected int getBaselineOffset() {
        int tabPlacement = tabPane.getTabPlacement();
        boolean varias = tabPane.getTabCount() > 1;
        if (tabPlacement == TOP) {
            return varias ? 1 : -1;
        }
        if (tabPlacement == BOTTOM) {
            return varias ? -1 : 1;
        }
        return varias ? 1 : 0;
    }

    /**
     * La del panel: la de la primera solapa mas donde empieza el area.
     *
     * @throws NullPointerException si el componente es nulo
     * @throws IllegalArgumentException si el ancho o el alto son negativos
     */
    public int getBaseline(JComponent c, int width, int height) {
        super.getBaseline(c, width, height);
        if (tabPane.getTabCount() <= 0) {
            return -1;
        }
        int baseline = getBaseline(0);
        if (baseline < 0) {
            return -1;
        }
        Insets insets = tabPane.getInsets();
        Insets areaInsets = getTabAreaInsets(tabPane.getTabPlacement());
        return baseline + insets.top + areaInsets.top;
    }

    /**
     * {@code CONSTANT_ASCENT}: las solapas estan siempre arriba de todo.
     *
     * @throws NullPointerException si el componente es nulo
     */
    public Component.BaselineResizeBehavior getBaselineResizeBehavior(JComponent c) {
        super.getBaselineResizeBehavior(c);
        return Component.BaselineResizeBehavior.CONSTANT_ASCENT;
    }

    /** {@code null}; contesta el acomodador. */
    public Dimension getMinimumSize(JComponent c) {
        return null;
    }

    /** {@code null}; contesta el acomodador. */
    public Dimension getMaximumSize(JComponent c) {
        return null;
    }

    public void paint(Graphics g, JComponent c) {
        int selectedIndex = tabPane.getSelectedIndex();
        int tabPlacement = tabPane.getTabPlacement();
        ensureCurrentLayout();
        paintTabArea(g, tabPlacement, selectedIndex);
        paintContentBorder(g, tabPlacement, selectedIndex);
    }

    /** Todas las solapas, corrida por corrida, dejando la elegida para el final. */
    protected void paintTabArea(Graphics g, int tabPlacement, int selectedIndex) {
        int tabCount = tabPane.getTabCount();
        Rectangle iconRect = new Rectangle();
        Rectangle textRect = new Rectangle();
        for (int i = runCount - 1; i >= 0; i--) {
            int start = tabRuns[i];
            int next = tabRuns[(i == runCount - 1) ? 0 : i + 1];
            int end = (next != 0 ? next - 1 : tabCount - 1);
            for (int j = start; j <= end && j < tabCount; j++) {
                if (j != selectedIndex) {
                    paintTab(g, tabPlacement, rects, j, iconRect, textRect);
                }
            }
        }
        if (selectedIndex >= 0 && selectedIndex < tabCount) {
            paintTab(g, tabPlacement, rects, selectedIndex, iconRect, textRect);
        }
    }

    /** Una solapa: fondo, borde, icono, texto y marca de foco. */
    protected void paintTab(Graphics g, int tabPlacement, Rectangle[] rects, int tabIndex,
            Rectangle iconRect, Rectangle textRect) {
        if (rects == null || tabIndex >= rects.length || rects[tabIndex] == null) {
            return;
        }
        Rectangle tabRect = rects[tabIndex];
        int selectedIndex = tabPane.getSelectedIndex();
        boolean isSelected = selectedIndex == tabIndex;

        paintTabBackground(g, tabPlacement, tabIndex, tabRect.x, tabRect.y,
                tabRect.width, tabRect.height, isSelected);
        paintTabBorder(g, tabPlacement, tabIndex, tabRect.x, tabRect.y,
                tabRect.width, tabRect.height, isSelected);

        String title = tabPane.getTitleAt(tabIndex);
        Font font = tabPane.getFont();
        FontMetrics metrics = tabPane.getFontMetrics(font);
        Icon icon = getIconForTab(tabIndex);
        layoutLabel(tabPlacement, metrics, tabIndex, title, icon, tabRect, iconRect,
                textRect, isSelected);
        paintText(g, tabPlacement, font, metrics, tabIndex, title, textRect, isSelected);
        paintIcon(g, tabPlacement, tabIndex, icon, iconRect, isSelected);
        paintFocusIndicator(g, tabPlacement, rects, tabIndex, iconRect, textRect, isSelected);
    }

    /** Ubica el icono y el texto dentro de la solapa. */
    protected void layoutLabel(int tabPlacement, FontMetrics metrics, int tabIndex, String title,
            Icon icon, Rectangle tabRect, Rectangle iconRect, Rectangle textRect,
            boolean isSelected) {
        textRect.x = 0;
        textRect.y = 0;
        textRect.width = 0;
        textRect.height = 0;
        iconRect.x = 0;
        iconRect.y = 0;
        iconRect.width = 0;
        iconRect.height = 0;
        SwingUtilities.layoutCompoundLabel(tabPane, metrics, title, icon,
                SwingConstants.CENTER, SwingConstants.CENTER,
                SwingConstants.CENTER, SwingConstants.TRAILING,
                tabRect, iconRect, textRect, textIconGap);
        int xNudge = getTabLabelShiftX(tabPlacement, tabIndex, isSelected);
        int yNudge = getTabLabelShiftY(tabPlacement, tabIndex, isSelected);
        iconRect.x += xNudge;
        iconRect.y += yNudge;
        textRect.x += xNudge;
        textRect.y += yNudge;
    }

    protected void paintIcon(Graphics g, int tabPlacement, int tabIndex, Icon icon,
            Rectangle iconRect, boolean isSelected) {
        if (icon != null) {
            icon.paintIcon(tabPane, g, iconRect.x, iconRect.y);
        }
    }

    protected void paintText(Graphics g, int tabPlacement, Font font, FontMetrics metrics,
            int tabIndex, String title, Rectangle textRect, boolean isSelected) {
        g.setFont(font);
        View v = getTextViewForTab(tabIndex);
        if (v != null) {
            v.paint(g, textRect);
            return;
        }
        if (tabPane.isEnabled() && tabPane.isEnabledAt(tabIndex)) {
            Color fg = tabPane.getForegroundAt(tabIndex);
            g.setColor(fg);
        } else {
            g.setColor(darkShadow);
        }
        g.drawString(title, textRect.x, textRect.y + metrics.getAscent());
    }

    /** El fondo de una solapa: el color que tenga puesto, o el de la sombra. */
    protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex, int x, int y,
            int w, int h, boolean isSelected) {
        g.setColor(!isSelected || tabPane.getBackgroundAt(tabIndex) != null
                ? tabPane.getBackgroundAt(tabIndex) : lightHighlight);
        g.fillRect(x, y, w, h);
    }

    /** El borde de una solapa: dos lineas que la unen al contenido. */
    protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex, int x, int y,
            int w, int h, boolean isSelected) {
        g.setColor(lightHighlight);
        g.drawLine(x, y + h - 1, x, y + 2);
        g.drawLine(x, y + 2, x + 2, y);
        g.drawLine(x + 2, y, x + w - 3, y);
        g.setColor(shadow);
        g.drawLine(x + w - 2, y + 2, x + w - 2, y + h - 1);
        g.setColor(darkShadow);
        g.drawLine(x + w - 1, y + 2, x + w - 1, y + h - 1);
    }

    /** La marca de que la solapa tiene el foco: un rectangulo punteado. */
    protected void paintFocusIndicator(Graphics g, int tabPlacement, Rectangle[] rects,
            int tabIndex, Rectangle iconRect, Rectangle textRect, boolean isSelected) {
        if (!tabPane.hasFocus() || !isSelected) {
            return;
        }
        g.setColor(focus);
        g.drawRect(textRect.x - 1, textRect.y, textRect.width + 1, textRect.height - 1);
    }

    /** El marco alrededor del contenido, con el hueco de la solapa elegida. */
    protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
        int width = tabPane.getWidth();
        int height = tabPane.getHeight();
        Insets insets = tabPane.getInsets();
        Insets bordeInsets = getContentBorderInsets(tabPlacement);

        int x = insets.left;
        int y = insets.top;
        int w = width - insets.right - insets.left;
        int h = height - insets.top - insets.bottom;

        if (tabPlacement == TOP) {
            int alto = calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight);
            y += alto;
            h -= alto;
        } else if (tabPlacement == BOTTOM) {
            h -= calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight);
        } else if (tabPlacement == LEFT) {
            int ancho = calculateTabAreaWidth(tabPlacement, runCount, maxTabWidth);
            x += ancho;
            w -= ancho;
        } else {
            w -= calculateTabAreaWidth(tabPlacement, runCount, maxTabWidth);
        }
        paintContentBorderTopEdge(g, tabPlacement, selectedIndex, x, y, w, h);
        paintContentBorderLeftEdge(g, tabPlacement, selectedIndex, x, y, w, h);
        paintContentBorderBottomEdge(g, tabPlacement, selectedIndex, x, y, w, h);
        paintContentBorderRightEdge(g, tabPlacement, selectedIndex, x, y, w, h);
        // `bordeInsets` queda para las subclases que dibujen mas grueso.
        if (bordeInsets == null) {
            return;
        }
    }

    protected void paintContentBorderTopEdge(Graphics g, int tabPlacement, int selectedIndex,
            int x, int y, int w, int h) {
        g.setColor(lightHighlight);
        g.drawLine(x, y, x + w - 2, y);
    }

    protected void paintContentBorderLeftEdge(Graphics g, int tabPlacement, int selectedIndex,
            int x, int y, int w, int h) {
        g.setColor(lightHighlight);
        g.drawLine(x, y, x, y + h - 2);
    }

    protected void paintContentBorderBottomEdge(Graphics g, int tabPlacement, int selectedIndex,
            int x, int y, int w, int h) {
        g.setColor(darkShadow);
        g.drawLine(x, y + h - 1, x + w - 1, y + h - 1);
    }

    protected void paintContentBorderRightEdge(Graphics g, int tabPlacement, int selectedIndex,
            int x, int y, int w, int h) {
        g.setColor(darkShadow);
        g.drawLine(x + w - 1, y, x + w - 1, y + h - 1);
    }

    /** Que solapa cae en ese punto; -1 si ninguna. */
    public int tabForCoordinate(JTabbedPane pane, int x, int y) {
        ensureCurrentLayout();
        for (int i = 0; i < rects.length; i++) {
            if (rects[i] != null && rects[i].contains(x, y)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * El acomodador de corridas; ver la nota de la clase.
     *
     * <p>Estatico y con el UI como primer parametro, que es la firma que el JDK genera para una
     * clase interna; ver el hallazgo #518.
     */
    public static class TabbedPaneLayout implements LayoutManager {

        private final BasicTabbedPaneUI ui;

        public TabbedPaneLayout(BasicTabbedPaneUI ui) {
            this.ui = ui;
        }

        public void addLayoutComponent(String name, Component comp) {
        }

        public void removeLayoutComponent(Component comp) {
        }

        public Dimension preferredLayoutSize(Container parent) {
            return calculateSize(false);
        }

        public Dimension minimumLayoutSize(Container parent) {
            return calculateSize(true);
        }

        /** El del contenido mas el area de solapas. */
        protected Dimension calculateSize(boolean minimum) {
            JTabbedPane tabPane = ui.tabPane;
            int tabPlacement = tabPane.getTabPlacement();
            Insets insets = tabPane.getInsets();
            Insets contentInsets = ui.getContentBorderInsets(tabPlacement);
            int width = 0;
            int height = 0;
            for (int i = 0; i < tabPane.getTabCount(); i++) {
                Component component = tabPane.getComponentAt(i);
                if (component == null) {
                    continue;
                }
                Dimension size = minimum ? component.getMinimumSize()
                        : component.getPreferredSize();
                if (size != null) {
                    height = Math.max(height, size.height);
                    width = Math.max(width, size.width);
                }
            }
            width += contentInsets.left + contentInsets.right;
            height += contentInsets.top + contentInsets.bottom;
            if (tabPlacement == LEFT || tabPlacement == RIGHT) {
                width += preferredTabAreaWidth(tabPlacement, height - contentInsets.top
                        - contentInsets.bottom);
            } else {
                height += preferredTabAreaHeight(tabPlacement, width - contentInsets.left
                        - contentInsets.right);
            }
            return new Dimension(width + insets.left + insets.right,
                    height + insets.top + insets.bottom);
        }

        protected int preferredTabAreaHeight(int tabPlacement, int width) {
            JTabbedPane tabPane = ui.tabPane;
            FontMetrics metrics = ui.getFontMetrics();
            int tabCount = tabPane.getTabCount();
            int total = 0;
            if (tabCount > 0) {
                int rows = 1;
                int x = 0;
                int maxTabHeight = ui.calculateMaxTabHeight(tabPlacement);
                for (int i = 0; i < tabCount; i++) {
                    int tabWidth = ui.calculateTabWidth(tabPlacement, i, metrics);
                    if (x != 0 && x + tabWidth > width) {
                        rows++;
                        x = 0;
                    }
                    x += tabWidth;
                }
                total = ui.calculateTabAreaHeight(tabPlacement, rows, maxTabHeight);
            }
            return total;
        }

        protected int preferredTabAreaWidth(int tabPlacement, int height) {
            JTabbedPane tabPane = ui.tabPane;
            FontMetrics metrics = ui.getFontMetrics();
            int tabCount = tabPane.getTabCount();
            int total = 0;
            if (tabCount > 0) {
                int columns = 1;
                int y = 0;
                int fontHeight = metrics.getHeight();
                int maxTabWidth = ui.calculateMaxTabWidth(tabPlacement);
                for (int i = 0; i < tabCount; i++) {
                    int tabHeight = ui.calculateTabHeight(tabPlacement, i, fontHeight);
                    if (y != 0 && y + tabHeight > height) {
                        columns++;
                        y = 0;
                    }
                    y += tabHeight;
                }
                total = ui.calculateTabAreaWidth(tabPlacement, columns, maxTabWidth);
            }
            return total;
        }

        /** Rehace corridas, rectangulos y maximos. */
        public void calculateLayoutInfo() {
            JTabbedPane tabPane = ui.tabPane;
            int tabCount = tabPane.getTabCount();
            ui.assureRectsCreated(tabCount);
            calculateTabRects(tabPane.getTabPlacement(), tabCount);
            ui.layoutCalculado = true;
        }

        /** Reparte las solapas en corridas; ver la nota de la clase. */
        protected void calculateTabRects(int tabPlacement, int tabCount) {
            JTabbedPane tabPane = ui.tabPane;
            FontMetrics metrics = ui.getFontMetrics();
            Dimension size = tabPane.getSize();
            Insets insets = tabPane.getInsets();
            Insets tabAreaInsets = ui.getTabAreaInsets(tabPlacement);
            int fontHeight = metrics.getHeight();
            int selectedIndex = tabPane.getSelectedIndex();

            ui.maxTabHeight = ui.calculateMaxTabHeight(tabPlacement);
            ui.maxTabWidth = ui.calculateMaxTabWidth(tabPlacement);
            ui.runCount = 0;
            ui.selectedRun = -1;
            if (tabCount == 0) {
                return;
            }

            boolean vertical = (tabPlacement == LEFT || tabPlacement == RIGHT);
            int disponible = vertical
                    ? size.height - insets.top - insets.bottom
                            - tabAreaInsets.top - tabAreaInsets.bottom
                    : size.width - insets.left - insets.right
                            - tabAreaInsets.left - tabAreaInsets.right;
            int x = vertical ? insets.left + tabAreaInsets.left
                    : insets.left + tabAreaInsets.left;
            int y = vertical ? insets.top + tabAreaInsets.top
                    : insets.top + tabAreaInsets.top;
            int corrida = 0;
            ui.tabRuns[0] = 0;
            ui.runCount = 1;
            int pos = 0;

            for (int i = 0; i < tabCount; i++) {
                Rectangle rect = ui.rects[i];
                if (vertical) {
                    int alto = ui.calculateTabHeight(tabPlacement, i, fontHeight);
                    if (pos != 0 && pos + alto > disponible) {
                        corrida++;
                        if (corrida >= ui.tabRuns.length) {
                            ui.expandTabRunsArray();
                        }
                        ui.tabRuns[corrida] = i;
                        ui.runCount = corrida + 1;
                        pos = 0;
                    }
                    rect.x = x + corrida * (ui.maxTabWidth - ui.getTabRunOverlay(tabPlacement));
                    rect.y = y + pos;
                    rect.width = ui.maxTabWidth;
                    rect.height = alto;
                    pos += alto;
                } else {
                    int ancho = ui.calculateTabWidth(tabPlacement, i, metrics);
                    if (pos != 0 && pos + ancho > disponible) {
                        corrida++;
                        if (corrida >= ui.tabRuns.length) {
                            ui.expandTabRunsArray();
                        }
                        ui.tabRuns[corrida] = i;
                        ui.runCount = corrida + 1;
                        pos = 0;
                    }
                    rect.x = x + pos;
                    rect.y = y + corrida * (ui.maxTabHeight - ui.getTabRunOverlay(tabPlacement));
                    rect.width = ancho;
                    rect.height = ui.maxTabHeight;
                    pos += ancho;
                }
                if (i == selectedIndex) {
                    ui.selectedRun = corrida;
                }
            }
            if (ui.shouldRotateTabRuns(tabPlacement)) {
                rotateTabRuns(tabPlacement, ui.selectedRun);
            }
        }

        /** Lleva la corrida elegida al frente; ver la nota de la clase. */
        protected void rotateTabRuns(int tabPlacement, int selectedRun) {
            if (selectedRun < 1 || ui.runCount < 2) {
                return;
            }
            for (int i = 0; i < selectedRun; i++) {
                int primera = ui.tabRuns[0];
                for (int j = 1; j < ui.runCount; j++) {
                    ui.tabRuns[j - 1] = ui.tabRuns[j];
                }
                ui.tabRuns[ui.runCount - 1] = primera;
            }
        }

        /** Estira las solapas de una corrida para que llenen el ancho. */
        protected void padTabRun(int tabPlacement, int start, int end, int max) {
        }

        /** Agranda un poco la solapa elegida, para que se vea al frente. */
        protected void padSelectedTab(int tabPlacement, int selectedIndex) {
            if (selectedIndex < 0 || ui.rects == null || selectedIndex >= ui.rects.length) {
                return;
            }
            Rectangle selRect = ui.rects[selectedIndex];
            Insets padInsets = ui.getSelectedTabPadInsets(tabPlacement);
            selRect.x -= padInsets.left;
            selRect.width += (padInsets.left + padInsets.right);
            selRect.y -= padInsets.top;
            selRect.height += (padInsets.top + padInsets.bottom);
        }

        /** Reparte el sobrante entre las corridas para que queden parejas. */
        protected void normalizeTabRuns(int tabPlacement, int tabCount, int start, int max) {
        }

        public void layoutContainer(Container parent) {
            JTabbedPane tabPane = ui.tabPane;
            calculateLayoutInfo();
            int tabPlacement = tabPane.getTabPlacement();
            Insets insets = tabPane.getInsets();
            int selectedIndex = tabPane.getSelectedIndex();
            Component visible = (selectedIndex < 0) ? null
                    : tabPane.getComponentAt(selectedIndex);
            ui.setVisibleComponent(visible);
            if (visible == null) {
                return;
            }
            int cx = insets.left;
            int cy = insets.top;
            int cw = tabPane.getWidth() - insets.left - insets.right;
            int ch = tabPane.getHeight() - insets.top - insets.bottom;
            if (tabPlacement == LEFT || tabPlacement == RIGHT) {
                int ancho = ui.calculateTabAreaWidth(tabPlacement, ui.runCount, ui.maxTabWidth);
                if (tabPlacement == LEFT) {
                    cx += ancho;
                }
                cw -= ancho;
            } else {
                int alto = ui.calculateTabAreaHeight(tabPlacement, ui.runCount, ui.maxTabHeight);
                if (tabPlacement == TOP) {
                    cy += alto;
                }
                ch -= alto;
            }
            Insets contentInsets = ui.getContentBorderInsets(tabPlacement);
            visible.setBounds(cx + contentInsets.left, cy + contentInsets.top,
                    cw - contentInsets.left - contentInsets.right,
                    ch - contentInsets.top - contentInsets.bottom);
        }
    }

    /**
     * El que escucha el mouse, el foco, el modelo y las propiedades.
     *
     * <p>Estatico y con el UI como campo, por lo mismo que en todo el paquete.
     */
    private static class Handler extends MouseAdapter implements MouseListener, FocusListener,
            ChangeListener, PropertyChangeListener {

        private final BasicTabbedPaneUI ui;

        Handler(BasicTabbedPaneUI ui) {
            this.ui = ui;
        }

        public void mousePressed(MouseEvent e) {
            JTabbedPane tabPane = ui.tabPane;
            if (!tabPane.isEnabled()) {
                return;
            }
            int tabIndex = ui.tabForCoordinate(tabPane, e.getX(), e.getY());
            if (tabIndex >= 0 && tabPane.isEnabledAt(tabIndex)) {
                if (tabIndex != tabPane.getSelectedIndex()) {
                    tabPane.setSelectedIndex(tabIndex);
                } else if (tabPane.isRequestFocusEnabled()) {
                    tabPane.requestFocus();
                }
            }
        }

        public void focusGained(FocusEvent e) {
            ui.tabPane.repaint();
        }

        public void focusLost(FocusEvent e) {
            ui.tabPane.repaint();
        }

        public void stateChanged(ChangeEvent e) {
            JTabbedPane tabPane = ui.tabPane;
            if (tabPane == null) {
                return;
            }
            tabPane.revalidate();
            tabPane.repaint();
        }

        public void propertyChange(PropertyChangeEvent e) {
            JTabbedPane pane = ui.tabPane;
            if (pane == null) {
                return;
            }
            String name = e.getPropertyName();
            if ("tabPlacement".equals(name) || "font".equals(name)
                    || "indexForTabComponent".equals(name) || "tabLayoutPolicy".equals(name)) {
                ui.layoutCalculado = false;
                pane.revalidate();
                pane.repaint();
            }
        }
    }
}
