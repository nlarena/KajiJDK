package javax.swing.plaf.metal;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.LayoutManager;
import java.awt.Rectangle;

import javax.swing.JComponent;
import javax.swing.SwingConstants;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicTabbedPaneUI;

/**
 * El panel de solapas de Metal.
 *
 * <h2>El hueco entre la solapa elegida y el contenido</h2>
 *
 * <p>La mitad de los metodos de esta clase existen por un solo detalle de dibujo: la solapa que
 * esta elegida no tiene borde del lado que da al contenido, asi que la solapa y el panel se leen
 * como una sola pieza. Eso deja un hueco donde el borde del contenido tendria que seguir, y
 * {@link #shouldFillGap} y {@link #getColorForGap} son los que deciden si se rellena y de que
 * color.
 *
 * <p>El color del hueco es el primario del tema y no el de la solapa: es el mismo que el borde que
 * se interrumpio.
 *
 * <h2>Los cuatro lados, uno por metodo</h2>
 *
 * <p>{@code paintTopTabBorder}, {@code paintLeftTabBorder}, {@code paintBottomTabBorder} y
 * {@code paintRightTabBorder} no son el mismo dibujo rotado. Con las solapas arriba, la elegida se
 * levanta un pixel; con las solapas a la izquierda, se corre. Metal las escribe por separado
 * porque el resultado no es simetrico.
 *
 * <h2>Los numeros</h2>
 *
 * <p>El ancho minimo de una solapa es cuarenta -- que es lo que evita que una solapa de una letra
 * quede como un boton cuadrado -- y el corrimiento de la etiqueta es cero en las dos direcciones,
 * elegida o no. Los dos, medidos; el basico corre la etiqueta y Metal no.
 */
public class MetalTabbedPaneUI extends BasicTabbedPaneUI {

    protected Color selectColor;
    protected Color selectHighlight;
    protected Color tabAreaBackground;

    /** Cuarenta; ver la nota de la clase. */
    protected int minTabWidth = 40;

    public MetalTabbedPaneUI() {
    }

    public static ComponentUI createUI(JComponent c) {
        return new MetalTabbedPaneUI();
    }

    protected void installDefaults() {
        super.installDefaults();
        selectColor = MetalLookAndFeel.colorDeLaTabla("TabbedPane.selected");
        selectHighlight = MetalLookAndFeel.colorDeLaTabla("TabbedPane.selectHighlight");
        if (selectHighlight == null) {
            selectHighlight = MetalLookAndFeel.getControlHighlight();
        }
        tabAreaBackground = MetalLookAndFeel.colorDeLaTabla("TabbedPane.tabAreaBackground");
    }

    protected LayoutManager createLayoutManager() {
        return new TabbedPaneLayout();
    }

    /**
     * La distribucion de Metal.
     *
     * <p>Es la del basico sin cambios; existe solo para que {@code getClass().getName()} diga
     * {@code MetalTabbedPaneUI$TabbedPaneLayout}, que es lo que dice el JDK y lo que una subclase
     * podria estar mirando.
     */
    public class TabbedPaneLayout extends BasicTabbedPaneUI.TabbedPaneLayout {

        public TabbedPaneLayout() {
        }
    }

    /** Cero: Metal no corre la etiqueta ni cuando la solapa esta elegida. */
    protected int getTabLabelShiftX(int tabPlacement, int tabIndex, boolean isSelected) {
        return 0;
    }

    protected int getTabLabelShiftY(int tabPlacement, int tabIndex, boolean isSelected) {
        return 0;
    }

    protected int getBaselineOffset() {
        return 0;
    }

    /** Cero: las filas de solapas de Metal no se pisan. */
    protected int getTabRunOverlay(int tabPlacement) {
        return 0;
    }

    /** No: la fila elegida se queda donde esta. */
    protected boolean shouldRotateTabRuns(int tabPlacement, int selectedRun) {
        return false;
    }

    protected boolean shouldPadTabRun(int tabPlacement, int run) {
        return false;
    }

    /** Si hay que tapar el hueco que deja la solapa elegida; ver la nota de la clase. */
    protected boolean shouldFillGap(int currentRun, int tabIndex, int x, int y) {
        return true;
    }

    /** El primario del tema: el mismo del borde que se interrumpio. */
    protected Color getColorForGap(int currentRun, int x, int y) {
        return MetalLookAndFeel.getPrimaryControl();
    }

    protected int calculateMaxTabHeight(int tabPlacement) {
        return super.calculateMaxTabHeight(tabPlacement);
    }

    public void update(Graphics g, JComponent c) {
        super.update(g, c);
    }

    public void paint(Graphics g, JComponent c) {
        super.paint(g, c);
    }

    protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex,
            int x, int y, int w, int h, boolean isSelected) {
        g.setColor(isSelected && selectColor != null
                ? selectColor : MetalLookAndFeel.getControl());
        g.fillRect(x, y, w, h);
    }

    protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex,
            int x, int y, int w, int h, boolean isSelected) {
        int fila = getRunForTab(tabPane.getTabCount(), tabIndex);
        // Con `if` y no con `switch`: las constantes de SwingConstants no las acepta el `case`
        // del compilador de esta casa.
        if (tabPlacement == SwingConstants.LEFT) {
            paintLeftTabBorder(tabIndex, g, x, y, w, h, fila, tabIndex, isSelected);
        } else if (tabPlacement == SwingConstants.BOTTOM) {
            paintBottomTabBorder(tabIndex, g, x, y, w, h, fila, tabIndex, isSelected);
        } else if (tabPlacement == SwingConstants.RIGHT) {
            paintRightTabBorder(tabIndex, g, x, y, w, h, fila, tabIndex, isSelected);
        } else {
            paintTopTabBorder(tabIndex, g, x, y, w, h, fila, tabIndex, isSelected);
        }
    }

    /** Arriba: la elegida no lleva la linea de abajo. */
    protected void paintTopTabBorder(int tabIndex, Graphics g, int x, int y, int w, int h,
            int btm, int rght, boolean isSelected) {
        g.setColor(isSelected ? selectHighlight : MetalLookAndFeel.getControlHighlight());
        g.drawLine(x, y + 2, x + w - 2, y + 2);
        g.setColor(MetalLookAndFeel.getControlDarkShadow());
        g.drawLine(x, y + 2, x, y + h - 1);
        g.drawLine(x + w - 1, y + 2, x + w - 1, y + h - 1);
        if (!isSelected) {
            g.drawLine(x, y + h - 1, x + w - 1, y + h - 1);
        }
    }

    protected void paintBottomTabBorder(int tabIndex, Graphics g, int x, int y, int w, int h,
            int btm, int rght, boolean isSelected) {
        g.setColor(MetalLookAndFeel.getControlDarkShadow());
        g.drawLine(x, y, x, y + h - 3);
        g.drawLine(x + w - 1, y, x + w - 1, y + h - 3);
        g.drawLine(x, y + h - 3, x + w - 1, y + h - 3);
        if (!isSelected) {
            g.setColor(MetalLookAndFeel.getControlHighlight());
            g.drawLine(x, y, x + w - 1, y);
        }
    }

    protected void paintLeftTabBorder(int tabIndex, Graphics g, int x, int y, int w, int h,
            int btm, int rght, boolean isSelected) {
        g.setColor(isSelected ? selectHighlight : MetalLookAndFeel.getControlHighlight());
        g.drawLine(x + 2, y, x + 2, y + h - 1);
        g.setColor(MetalLookAndFeel.getControlDarkShadow());
        g.drawLine(x + 2, y, x + w - 1, y);
        g.drawLine(x + 2, y + h - 1, x + w - 1, y + h - 1);
        if (!isSelected) {
            g.drawLine(x + w - 1, y, x + w - 1, y + h - 1);
        }
    }

    protected void paintRightTabBorder(int tabIndex, Graphics g, int x, int y, int w, int h,
            int btm, int rght, boolean isSelected) {
        g.setColor(MetalLookAndFeel.getControlDarkShadow());
        g.drawLine(x, y, x + w - 3, y);
        g.drawLine(x, y + h - 1, x + w - 3, y + h - 1);
        g.drawLine(x + w - 3, y, x + w - 3, y + h - 1);
        if (!isSelected) {
            g.setColor(MetalLookAndFeel.getControlHighlight());
            g.drawLine(x, y, x, y + h - 1);
        }
    }

    protected void paintContentBorderTopEdge(Graphics g, int tabPlacement, int selectedIndex,
            int x, int y, int w, int h) {
        super.paintContentBorderTopEdge(g, tabPlacement, selectedIndex, x, y, w, h);
    }

    protected void paintContentBorderBottomEdge(Graphics g, int tabPlacement, int selectedIndex,
            int x, int y, int w, int h) {
        super.paintContentBorderBottomEdge(g, tabPlacement, selectedIndex, x, y, w, h);
    }

    protected void paintContentBorderLeftEdge(Graphics g, int tabPlacement, int selectedIndex,
            int x, int y, int w, int h) {
        super.paintContentBorderLeftEdge(g, tabPlacement, selectedIndex, x, y, w, h);
    }

    protected void paintContentBorderRightEdge(Graphics g, int tabPlacement, int selectedIndex,
            int x, int y, int w, int h) {
        super.paintContentBorderRightEdge(g, tabPlacement, selectedIndex, x, y, w, h);
    }

    protected void paintFocusIndicator(Graphics g, int tabPlacement, Rectangle[] rects,
            int tabIndex, Rectangle iconRect, Rectangle textRect, boolean isSelected) {
    }

    /** El brillo de abajo de la solapa elegida, que la une con el contenido. */
    protected void paintHighlightBelowTab() {
    }
}
