import java.awt.Dimension;
import java.awt.Rectangle;

import javax.swing.JTree;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.tree.AbstractLayoutCache;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;

/**
 * El arbol, contra el JDK.
 *
 * <p>Las dos manijas -- expandida y colapsada -- vienen de la tabla del aspecto y aca no hay
 * ninguna, asi que el ancho preferido y el ancho de cada banda no se comparan: dependen de iconos
 * que no estan y de metricas de fuente que esta biblioteca todavia no reproduce.
 *
 * <p>Lo que si se compara es la estructura: que tabla de posiciones se elige, cuanto se corre cada
 * nivel, que filas son hojas, donde arranca cada banda en x, y como contesta el UI a todo lo que no
 * necesita dibujar nada.
 */
public class Plaf14 {

    static void linea(String s) {
        System.out.println("//" + s);
    }

    static String corto(Object o) {
        if (o == null) {
            return "-";
        }
        String c = o.getClass().getName();
        return c.substring(c.lastIndexOf('.') + 1);
    }

    static class Arbol extends BasicTreeUI {

        String estado() {
            return "arbol=" + (tree != null)
                    + " modelo=" + (treeModel != null)
                    + " tabla=" + corto(treeState)
                    + " panel=" + (rendererPane != null)
                    + " dibujante=" + corto(currentCellRenderer)
                    + " creo dibujante=" + createdRenderer
                    + " editor=" + (cellEditor != null)
                    + " creo editor=" + createdCellEditor
                    + " modelo grande=" + largeModel
                    + " fila que edita=" + editingRow
                    + " camino que edita=" + editingPath
                    + " componente=" + (editingComponent != null);
        }

        String sangrias() {
            return "izq=" + leftChildIndent + " der=" + rightChildIndent
                    + " total=" + totalChildIndent + " profundidad=" + depthOffset
                    + " minimo=" + preferredMinSize
                    + " parar al completar=" + stopEditingInCompleteEditing;
        }

        int altoDeFila() {
            return getRowHeight();
        }

        int bufferH() {
            return getHorizontalLegBuffer();
        }

        int bufferV() {
            return getVerticalLegBuffer();
        }

        int equis(int fila, int profundidad) {
            return getRowX(fila, profundidad);
        }

        boolean raizVisible() {
            return isRootVisible();
        }

        boolean manijas() {
            return getShowsRootHandles();
        }

        boolean editable() {
            return isEditable();
        }

        boolean grande() {
            return isLargeModel();
        }

        boolean hoja(int fila) {
            return isLeaf(fila);
        }

        int filaElegida() {
            return getLeadSelectionRow();
        }

        Object color() {
            return getHashColor();
        }

        Object iconoExpandido() {
            return getExpandedIcon();
        }

        Object iconoColapsado() {
            return getCollapsedIcon();
        }

        TreeCellRenderer nuevoDibujante() {
            return createDefaultCellRenderer();
        }

        AbstractLayoutCache nuevaTabla() {
            return createLayoutCache();
        }

        AbstractLayoutCache.NodeDimensions nuevasMedidas() {
            return createNodeDimensions();
        }

        boolean pintaManija(TreePath p, int fila) {
            return shouldPaintExpandControl(p, fila, tree.isExpanded(p), true,
                    getModel().isLeaf(p.getLastPathComponent()));
        }

        void ponRaizVisible(boolean v) {
            setRootVisible(v);
        }

        void ponManijas(boolean v) {
            setShowsRootHandles(v);
        }

        void recalculaProfundidad() {
            updateDepthOffset();
        }

        int profundidad() {
            return depthOffset;
        }

        void ponGrande(boolean v) {
            setLargeModel(v);
        }

        TreePath ultimoHijo(TreePath p) {
            return getLastChildPath(p);
        }

        AbstractLayoutCache tablaInstalada() {
            return treeState;
        }

        Object editorInstalado() {
            return getCellEditor();
        }
    }

    /** Lo que el UI contesta recien instalado. */
    static void instalacion() {
        linea("--- BasicTreeUI: instalacion ---");
        JTree t = new JTree();
        Arbol u = new Arbol();
        linea("comparte instancia=" + (BasicTreeUI.createUI(t) == BasicTreeUI.createUI(t)));
        t.setUI(u);
        linea("estado: " + u.estado());
        linea("sangrias: " + u.sangrias());
        // Las dos manijas vienen de la tabla del aspecto; ver la nota de la clase.
        linea("hay color de linea=" + (u.color() != null));
        linea("arbol: opaco=" + t.isOpaque() + " fondo=" + t.getBackground()
                + " alto de fila=" + t.getRowHeight()
                + " raiz visible=" + t.isRootVisible()
                + " manijas=" + t.getShowsRootHandles());
        linea("del ui: alto de fila=" + u.altoDeFila()
                + " buffer horizontal=" + u.bufferH() + " vertical=" + u.bufferV());
        linea("preguntas: raiz visible=" + u.raizVisible() + " manijas=" + u.manijas()
                + " editable=" + u.editable() + " grande=" + u.grande()
                + " fila elegida=" + u.filaElegida());
        linea("creados: dibujante=" + corto(u.nuevoDibujante())
                + " tabla=" + corto(u.nuevaTabla())
                + " medidas=" + corto(u.nuevasMedidas()));
    }

    /** La sangria por nivel y el ajuste por raiz y manijas. */
    static void sangrias() {
        linea("--- BasicTreeUI: sangrias ---");
        JTree t = new JTree();
        Arbol u = new Arbol();
        t.setUI(u);
        for (int p = 0; p < 4; p++) {
            linea("equis(0," + p + ")=" + u.equis(0, p));
        }
        linea("cambiando la sangria izquierda a 12:");
        u.setLeftChildIndent(12);
        linea(" izq=" + u.getLeftChildIndent() + " der=" + u.getRightChildIndent()
                + " total=" + (u.getLeftChildIndent() + u.getRightChildIndent())
                + " equis(0,1)=" + u.equis(0, 1) + " equis(0,2)=" + u.equis(0, 2));
        u.setRightChildIndent(20);
        linea(" y la derecha a 20: equis(0,1)=" + u.equis(0, 1)
                + " equis(0,2)=" + u.equis(0, 2));

        // Las cuatro combinaciones de raiz visible y manijas.
        linea("ajuste de profundidad por combinacion:");
        for (int i = 0; i < 4; i++) {
            boolean visible = (i & 1) != 0;
            boolean conManijas = (i & 2) != 0;
            JTree otro = new JTree();
            otro.setRootVisible(visible);
            otro.setShowsRootHandles(conManijas);
            Arbol v = new Arbol();
            otro.setUI(v);
            v.recalculaProfundidad();
            linea(" raiz visible=" + visible + " manijas=" + conManijas
                    + " -> profundidad=" + v.profundidad()
                    + " equis(0,1)=" + v.equis(0, 1));
        }
    }

    /** Las filas: cuantas hay, cuales son hojas y donde arranca cada una. */
    static void filas() {
        linea("--- BasicTreeUI: filas ---");
        JTree t = new JTree();
        Arbol u = new Arbol();
        t.setUI(u);
        int filas = u.getRowCount(t);
        linea("cantidad de filas=" + filas);
        for (int f = 0; f < filas; f++) {
            TreePath p = u.getPathForRow(t, f);
            Rectangle b = u.getPathBounds(t, p);
            linea(" fila " + f + ": camino=" + p
                    + " vuelta=" + u.getRowForPath(t, p)
                    + " hoja=" + u.hoja(f)
                    + " profundidad=" + (p.getPathCount() - 1)
                    + " equis=" + (b == null ? "-" : "" + b.x)
                    + " manija=" + u.pintaManija(p, f));
        }
        linea("fila que no existe: camino=" + u.getPathForRow(t, 99)
                + " hoja=" + u.hoja(99));
        linea("camino que no esta: fila=" + u.getRowForPath(t, new TreePath("nada")));

        TreePath raiz = u.getPathForRow(t, 0);
        linea("ultimo hijo de la raiz=" + u.ultimoHijo(raiz));
        TreePath hoja = u.getPathForRow(t, 1);
        linea("ultimo hijo de la fila 1=" + u.ultimoHijo(hoja));

        // Abrir una rama agrega filas.
        t.expandRow(1);
        linea("tras abrir la fila 1: filas=" + u.getRowCount(t));
        for (int f = 0; f < u.getRowCount(t); f++) {
            TreePath p = u.getPathForRow(t, f);
            Rectangle b = u.getPathBounds(t, p);
            linea(" fila " + f + ": " + p.getLastPathComponent()
                    + " profundidad=" + (p.getPathCount() - 1)
                    + " equis=" + (b == null ? "-" : "" + b.x));
        }
        t.collapseRow(1);
        linea("y tras cerrarla: filas=" + u.getRowCount(t));
    }

    /** Lo mas cercano a un punto, y el orden vertical de las bandas. */
    static void ubicacion() {
        linea("--- BasicTreeUI: ubicacion ---");
        JTree t = new JTree();
        Arbol u = new Arbol();
        t.setUI(u);
        Rectangle a = u.getPathBounds(t, u.getPathForRow(t, 0));
        Rectangle b = u.getPathBounds(t, u.getPathForRow(t, 1));
        linea("la fila 0 arranca en y=" + a.y
                + " y la 1 esta mas abajo=" + (b.y > a.y)
                + " pegadas=" + (b.y == a.y + a.height));
        linea("mas cercano a (0,0)=" + u.getClosestPathForLocation(t, 0, 0));
        linea("mas cercano a (0,-100)=" + u.getClosestPathForLocation(t, 0, -100));
        linea("mas cercano a (0,10000)=" + u.getClosestPathForLocation(t, 0, 10000));
        // La coordenada no se imprime: el alto de fila depende de la fuente.
        linea("mas cercano a la derecha de la fila 1="
                + u.getClosestPathForLocation(t, 500, b.y));
        linea("banda de un camino que no esta="
                + u.getPathBounds(t, new TreePath("nada")));
        linea("banda de null=" + u.getPathBounds(t, null));
    }

    /** Los tres tamanos. */
    static void tamanos() {
        linea("--- BasicTreeUI: tamanos ---");
        JTree t = new JTree();
        Arbol u = new Arbol();
        t.setUI(u);
        Dimension pref = u.getPreferredSize(t);
        Dimension min = u.getMinimumSize(t);
        Dimension max = u.getMaximumSize(t);
        linea("minimo=" + min);
        linea("el maximo es el preferido=" + max.equals(pref));
        linea("el preferido tiene alto positivo=" + (pref.height > 0)
                + " y ancho positivo=" + (pref.width > 0));
        linea("el preferido sin comprobar es el mismo="
                + u.getPreferredSize(t, false).equals(pref));
        linea("minimo propio antes=" + u.getPreferredMinSize());
        u.setPreferredMinSize(new Dimension(1000, 1000));
        linea("tras pedir 1000x1000: minimo propio=" + u.getPreferredMinSize()
                + " preferido=" + u.getPreferredSize(t));
        u.setPreferredMinSize(null);
        linea("y al sacarlo vuelve=" + u.getPreferredSize(t).equals(pref));
        linea("comportamiento de la linea de base=" + u.getBaselineResizeBehavior(t));
        linea("la linea de base cae dentro de la primera fila="
                + (u.getBaseline(t, 200, 100) >= 0
                        && u.getBaseline(t, 200, 100) <= u.getPathBounds(t,
                                u.getPathForRow(t, 0)).height));
    }

    /** El modelo grande solo se puede con filas de alto fijo. */
    static void modeloGrande() {
        linea("--- BasicTreeUI: modelo grande ---");
        JTree t = new JTree();
        Arbol u = new Arbol();
        t.setUI(u);
        linea("con alto de fila " + t.getRowHeight() + ": grande=" + u.grande()
                + " tabla=" + corto(u.nuevaTabla()));
        u.ponGrande(true);
        linea("pedirlo con alto cero no alcanza: grande=" + u.grande());
        t.setRowHeight(18);
        t.setLargeModel(true);
        linea("con alto 18 y el arbol pidiendolo: grande=" + u.grande()
                + " tabla=" + corto(u.nuevaTabla()));
        linea("y la tabla instalada paso a ser " + corto(u.tablaInstalada()));
        t.setLargeModel(false);
        linea("al sacarlo: grande=" + u.grande() + " tabla=" + corto(u.nuevaTabla()));
    }

    /** Editar: no hay editor mientras el arbol no sea editable. */
    static void edicion() {
        linea("--- BasicTreeUI: edicion ---");
        JTree t = new JTree();
        Arbol u = new Arbol();
        t.setUI(u);
        linea("editando=" + u.isEditing(t) + " camino=" + u.getEditingPath(t)
                + " editor=" + corto(u.editorInstalado()));
        linea("parar la edicion sin editar=" + u.stopEditing(t));
        u.cancelEditing(t);
        linea("cancelar sin editar no rompe");
        t.setEditable(true);
        linea("con el arbol editable: editor=" + (u.editorInstalado() != null)
                + " editando=" + u.isEditing(t));
        t.setEditable(false);
        linea("y al sacarlo: editor=" + corto(u.editorInstalado()));
    }

    public static int run() {
        instalacion();
        sangrias();
        filas();
        ubicacion();
        tamanos();
        modeloGrande();
        edicion();
        return 0;
    }
}
