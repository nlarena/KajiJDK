package javax.swing.plaf.metal;

import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;

import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.tree.TreePath;

/**
 * El arbol de Metal.
 *
 * <h2>Tres estilos de linea, elegidos por el programa</h2>
 *
 * <p>Metal es el unico aspecto que deja elegir como se dibujan las lineas que unen las ramas, y no
 * por una propiedad del arbol sino por una <em>propiedad de cliente</em>:
 * {@code tree.putClientProperty("JTree.lineStyle", "Angled")}. Los tres valores son
 * {@code "Angled"} -- la escalera de siempre --, {@code "Horizontal"} -- una raya entre nodos de
 * primer nivel, sin verticales -- y {@code "None"}.
 *
 * <p>Que sea una propiedad de cliente y no una propiedad normal es lo que permite que exista sin
 * ensuciar la API de {@code JTree} con algo que solo un aspecto entiende. El precio es que no hay
 * como descubrirla mirando los metodos.
 *
 * <h2>La zona sensible de la manija es mas ancha que la manija</h2>
 *
 * <p>{@link #isLocationInExpandControl} acepta veinticinco columnas para un icono de dieciocho: el
 * ancho del icono mas dos veces {@link #getHorizontalLegBuffer}, que en Metal vale tres. Medido, y
 * es deliberado: la manija es chica y errarle por dos pixeles no deberia significar seleccionar el
 * nodo en vez de abrirlo.
 *
 * <p>Notar que este metodo tiene <em>cuatro</em> parametros y no coincide con el del basico: toma
 * la fila y su nivel ya calculados, en vez del camino. No lo redefine -- lo agrega --, asi que las
 * dos versiones conviven.
 */
public class MetalTreeUI extends BasicTreeUI {

    /** Los tres estilos; ver la nota de la clase. */
    private static final int ANGULADO = 1;
    private static final int HORIZONTAL = 2;
    private static final int NINGUNO = 3;

    private static final String CLAVE = "JTree.lineStyle";

    private int estilo = ANGULADO;

    public MetalTreeUI() {
    }

    public static ComponentUI createUI(JComponent x) {
        return new MetalTreeUI();
    }

    public void installUI(JComponent c) {
        super.installUI(c);
        decodeLineStyle(c.getClientProperty(CLAVE));
        // Las manijas: el basico las deja en nulo porque salen de la tabla del aspecto, y Metal
        // las dibuja. De su ancho -- dieciocho -- depende la zona sensible; ver la nota de la
        // clase.
        if (getExpandedIcon() == null) {
            setExpandedIcon(MetalIconFactory.getTreeControlIcon(false));
        }
        if (getCollapsedIcon() == null) {
            setCollapsedIcon(MetalIconFactory.getTreeControlIcon(true));
        }
    }

    public void uninstallUI(JComponent c) {
        super.uninstallUI(c);
    }

    /**
     * Traduce el valor de la propiedad de cliente.
     *
     * <p>Cualquier cosa que no sea uno de los tres textos deja el estilo en {@code "Angled"}, que
     * es el de omision. No tira: una propiedad de cliente la escribe cualquiera y romper el arbol
     * por un texto mal escrito seria peor que ignorarlo.
     */
    protected void decodeLineStyle(Object lineStyleFlag) {
        if ("Horizontal".equals(lineStyleFlag)) {
            estilo = HORIZONTAL;
        } else if ("None".equals(lineStyleFlag)) {
            estilo = NINGUNO;
        } else {
            estilo = ANGULADO;
        }
    }

    /** Tres; ver la nota de la clase. */
    protected int getHorizontalLegBuffer() {
        return 3;
    }

    /**
     * Si ese punto cae en la manija de esa fila.
     *
     * @param row la fila
     * @param rowLevel su nivel en el arbol
     * @param mouseX la coordenada horizontal
     * @param mouseY la vertical, que no se mira
     */
    protected boolean isLocationInExpandControl(int row, int rowLevel, int mouseX, int mouseY) {
        if (tree == null || isLeaf(row)) {
            return false;
        }
        int ancho = ((getExpandedIcon() != null) ? getExpandedIcon().getIconWidth() : 8)
                + 2 * getHorizontalLegBuffer();
        Insets i = tree.getInsets();
        int izquierda = ((i != null) ? i.left : 0)
                + (((rowLevel + depthOffset - 1) * totalChildIndent) + getLeftChildIndent())
                - ancho / 2;
        return mouseX >= izquierda && mouseX <= izquierda + ancho;
    }

    public void paint(Graphics g, JComponent c) {
        super.paint(g, c);
        if (estilo == HORIZONTAL) {
            paintHorizontalSeparators(g, c);
        }
    }

    /** La raya entre nodos de primer nivel del estilo {@code "Horizontal"}. */
    protected void paintHorizontalSeparators(Graphics g, JComponent c) {
        g.setColor(MetalLookAndFeel.getPrimaryControl());
        Rectangle clip = g.getClipBounds();
        if (clip == null) {
            return;
        }
        TreePath desde = getClosestPathForLocation(tree, 0, clip.y);
        TreePath hasta = getClosestPathForLocation(tree, 0, clip.y + clip.height);
        if (desde == null || hasta == null) {
            return;
        }
        int primera = getRowForPath(tree, desde);
        int ultima = getRowForPath(tree, hasta);
        for (int fila = primera; fila <= ultima; fila++) {
            TreePath p = getPathForRow(tree, fila);
            if (p != null && p.getPathCount() == 2) {
                Rectangle b = getPathBounds(tree, p);
                if (b != null) {
                    g.drawLine(clip.x, b.y, clip.x + clip.width, b.y);
                }
            }
        }
    }

    protected void paintVerticalPartOfLeg(Graphics g, Rectangle clipBounds, Insets insets,
            TreePath path) {
        if (estilo == ANGULADO) {
            super.paintVerticalPartOfLeg(g, clipBounds, insets, path);
        }
    }

    protected void paintHorizontalPartOfLeg(Graphics g, Rectangle clipBounds, Insets insets,
            Rectangle bounds, TreePath path, int row, boolean isExpanded,
            boolean hasBeenExpanded, boolean isLeaf) {
        if (estilo == ANGULADO) {
            super.paintHorizontalPartOfLeg(g, clipBounds, insets, bounds, path, row,
                    isExpanded, hasBeenExpanded, isLeaf);
        }
    }
}
