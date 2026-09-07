import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;

import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.border.Border;
import javax.swing.plaf.metal.MetalBorders;
import javax.swing.plaf.metal.MetalComboBoxButton;
import javax.swing.plaf.metal.MetalComboBoxEditor;
import javax.swing.plaf.metal.MetalComboBoxUI;
import javax.swing.plaf.metal.MetalIconFactory;
import javax.swing.plaf.metal.MetalSplitPaneUI;
import javax.swing.plaf.metal.MetalToolBarUI;
import javax.swing.plaf.metal.MetalTreeUI;

/**
 * El desplegable, la barra de herramientas, el panel dividido, el arbol y la fabrica de iconos.
 *
 * <p>De los iconos se compara el tamano, la clase y si son recursos del aspecto; los pixeles no,
 * que aca estan dibujados y en el JDK son otra figura. Del arbol se compara la zona sensible de la
 * manija, que es aritmetica pura.
 */
public class Metal4 {

    static void linea(String s) {
        System.out.println("//" + s);
    }

    static String n(Object o) {
        if (o == null) {
            return "-";
        }
        String s = o.getClass().getName();
        return s.substring(s.lastIndexOf('.') + 1);
    }

    static String ic(Icon i) {
        if (i == null) {
            return "-";
        }
        return i.getIconWidth() + "x" + i.getIconHeight() + " " + n(i)
                + (i instanceof javax.swing.plaf.UIResource ? " recurso" : "");
    }

    static String bo(Border b) {
        if (b == null) {
            return "-";
        }
        Insets i = b.getBorderInsets(new javax.swing.JLabel());
        return n(b) + " margenes=" + i.top + "," + i.left + "," + i.bottom + "," + i.right
                + " opaco=" + b.isBorderOpaque();
    }

    static class Combo extends MetalComboBoxUI {
        javax.swing.JButton flecha() {
            return createArrowButton();
        }

        Object popup() {
            return createPopup();
        }

        Object editor() {
            return createEditor();
        }

        LayoutManager layout() {
            return createLayoutManager();
        }
    }

    static class Barra extends MetalToolBarUI {
        Object contenedor() {
            return createContainerListener();
        }

        Object relieve() {
            return createRolloverListener();
        }

        Object arrastre() {
            return createDockingListener();
        }

        String campos() {
            return n(contListener) + "/" + n(rolloverListener);
        }

        Border conRelieve() {
            return createRolloverBorder();
        }

        Border sinRelieve() {
            return createNonRolloverBorder();
        }
    }

    static class Arbol extends MetalTreeUI {
        int buffer() {
            return getHorizontalLegBuffer();
        }

        boolean enManija(int fila, int nivel, int x, int y) {
            return isLocationInExpandControl(fila, nivel, x, y);
        }

        void estilo(Object o) {
            decodeLineStyle(o);
        }
    }

    static class Editor extends MetalComboBoxEditor {
        static String margenes() {
            return editorBorderInsets.top + "," + editorBorderInsets.left + ","
                    + editorBorderInsets.bottom + "," + editorBorderInsets.right;
        }
    }

    static void desplegable() {
        linea("--- el desplegable ---");
        JComboBox<String> cb = new JComboBox<String>(new String[] {"uno", "dos"});
        Combo u = new Combo();
        cb.setUI(u);
        linea("flecha=" + n(u.flecha()) + " popup=" + n(u.popup())
                + " editor=" + n(u.editor()) + " distribucion=" + n(u.layout()));
        linea("cada llamada da un UI nuevo="
                + (MetalComboBoxUI.createUI(cb) != MetalComboBoxUI.createUI(cb)));

        MetalComboBoxButton b = (MetalComboBoxButton) u.flecha();
        linea("el boton: icono=" + n(b.getComboIcon()) + " solo icono=" + b.isIconOnly()
                + " toma el foco=" + b.isFocusTraversable());
        linea("y sabe de que desplegable es="
                + (((Object) b.getComboBox()) == ((Object) cb)));

        // Al volverse editable, el boton pasa a ser solo la flecha.
        cb.setEditable(true);
        Combo ue = new Combo();
        cb.setUI(ue);
        MetalComboBoxButton be = (MetalComboBoxButton) ue.flecha();
        linea("editable: solo icono=" + be.isIconOnly());

        linea("margenes del editor=" + Editor.margenes());
        MetalComboBoxEditor ed = new MetalComboBoxEditor();
        linea("el editor tiene borde=" + (((JComponent) ed.getEditorComponent())
                .getBorder() != null));
        linea("el UIResource del editor es uno del aspecto="
                + (new MetalComboBoxEditor.UIResource() instanceof javax.swing.plaf.UIResource)
                + " y el comun no=" + !(ed instanceof javax.swing.plaf.UIResource));
    }

    static void herramientas() {
        linea("--- la barra de herramientas ---");
        JToolBar t = new JToolBar();
        Barra u = new Barra();
        t.setUI(u);
        linea("escucha de contenedor=" + n(u.contenedor())
                + " de relieve=" + n(u.relieve())
                + " de arrastre=" + n(u.arrastre()));
        linea("los campos quedan en: " + u.campos());
        linea("los dos bordes son de la misma clase="
                + (u.conRelieve().getClass() == u.sinRelieve().getClass()));
    }

    static void dividido() {
        linea("--- el panel dividido ---");
        JSplitPane p = new JSplitPane();
        MetalSplitPaneUI u = new MetalSplitPaneUI();
        p.setUI(u);
        linea("divisor=" + n(u.createDefaultDivider())
                + " tamano=" + p.getDividerSize());
        linea("el instalado es de la misma clase="
                + (u.getDivider().getClass() == u.createDefaultDivider().getClass()));
    }

    static void arbol() {
        linea("--- el arbol ---");
        JTree t = new JTree();
        Arbol u = new Arbol();
        t.setUI(u);
        linea("buffer horizontal=" + u.buffer());
        linea("sangrias: izq=" + u.getLeftChildIndent()
                + " der=" + u.getRightChildIndent());

        // La zona sensible de la manija, nivel por nivel.
        for (int nivel = 0; nivel <= 2; nivel++) {
            int desde = 1000;
            int hasta = -1000;
            for (int x = -60; x <= 60; x++) {
                if (u.enManija(1, nivel, x, 0)) {
                    if (x < desde) {
                        desde = x;
                    }
                    if (x > hasta) {
                        hasta = x;
                    }
                }
            }
            linea(" nivel " + nivel + " acepta de " + desde + " a " + hasta
                    + " (ancho " + (hasta - desde + 1) + ")");
        }
        linea("sobre una hoja nunca=" + u.enManija(3, 2, 20, 0));

        // Los tres estilos de linea no rompen nada, y uno mal escrito tampoco.
        u.estilo("Horizontal");
        u.estilo("None");
        u.estilo("Angled");
        u.estilo("cualquier cosa");
        u.estilo(null);
        linea("los estilos de linea no rompen; filas=" + u.getRowCount(t));

        JTree otro = new JTree();
        otro.putClientProperty("JTree.lineStyle", "Horizontal");
        Arbol uo = new Arbol();
        otro.setUI(uo);
        linea("instalado con el estilo puesto: filas=" + uo.getRowCount(otro));
        linea("cada llamada da un UI nuevo="
                + (MetalTreeUI.createUI(t) != MetalTreeUI.createUI(t)));
    }

    static void iconos() {
        linea("--- la fabrica de iconos ---");
        linea("LIGHT=" + MetalIconFactory.LIGHT + " DARK=" + MetalIconFactory.DARK);
        linea("casilla de menu=" + ic(MetalIconFactory.getCheckBoxMenuItemIcon()));
        linea("opcion de menu=" + ic(MetalIconFactory.getRadioButtonMenuItemIcon()));
        linea("tilde de item=" + ic(MetalIconFactory.getMenuItemCheckIcon()));
        linea("flecha de item=" + ic(MetalIconFactory.getMenuItemArrowIcon()));
        linea("flecha de menu=" + ic(MetalIconFactory.getMenuArrowIcon()));
        linea("manija cerrada=" + ic(MetalIconFactory.getTreeControlIcon(true)));
        linea("manija abierta mide igual="
                + (MetalIconFactory.getTreeControlIcon(false).getIconWidth()
                        == MetalIconFactory.getTreeControlIcon(true).getIconWidth()));
        linea("carpeta=" + ic(MetalIconFactory.getTreeFolderIcon()));
        linea("hoja=" + ic(MetalIconFactory.getTreeLeafIcon()));
        linea("computadora=" + ic(MetalIconFactory.getTreeComputerIcon()));
        linea("disco=" + ic(MetalIconFactory.getTreeHardDriveIcon()));
        linea("diskette=" + ic(MetalIconFactory.getTreeFloppyDriveIcon()));
        linea("carpeta nueva=" + ic(MetalIconFactory.getFileChooserNewFolderIcon()));
        linea("subir=" + ic(MetalIconFactory.getFileChooserUpFolderIcon()));
        linea("casa=" + ic(MetalIconFactory.getFileChooserHomeFolderIcon()));
        linea("detalle=" + ic(MetalIconFactory.getFileChooserDetailViewIcon()));
        linea("lista=" + ic(MetalIconFactory.getFileChooserListViewIcon()));
        linea("pulgar acostado=" + ic(MetalIconFactory.getHorizontalSliderThumbIcon()));
        linea("pulgar parado=" + ic(MetalIconFactory.getVerticalSliderThumbIcon()));
        linea("menu del marco=" + ic(MetalIconFactory.getInternalFrameDefaultMenuIcon()));
        for (int lado : new int[] {16, 8, 20}) {
            linea(" de lado " + lado
                    + ": cerrar=" + ic(MetalIconFactory.getInternalFrameCloseIcon(lado))
                    + " agrandar=" + ic(MetalIconFactory.getInternalFrameMaximizeIcon(lado)));
            linea("            restaurar="
                    + ic(MetalIconFactory.getInternalFrameAltMaximizeIcon(lado))
                    + " achicar=" + ic(MetalIconFactory.getInternalFrameMinimizeIcon(lado)));
        }
        linea("los del arbol se comparten="
                + (MetalIconFactory.getTreeFolderIcon()
                        == MetalIconFactory.getTreeFolderIcon()));
        linea("y los del marco no="
                + (MetalIconFactory.getInternalFrameCloseIcon(16)
                        != MetalIconFactory.getInternalFrameCloseIcon(16)));
    }

    static void bordes() {
        linea("--- los bordes ---");
        linea("texto=" + bo(MetalBorders.getTextBorder()));
        linea("campo=" + bo(MetalBorders.getTextFieldBorder()));
        linea("icono de escritorio=" + bo(MetalBorders.getDesktopIconBorder()));
        linea("boton=" + bo(MetalBorders.getButtonBorder()));
        linea("conmutador=" + bo(MetalBorders.getToggleButtonBorder()));
        linea("se comparten=" + (MetalBorders.getTextBorder()
                == MetalBorders.getTextBorder()));
    }

    public static int run() {
        desplegable();
        herramientas();
        dividido();
        arbol();
        iconos();
        bordes();
        return 0;
    }
}
