package javax.swing.plaf.metal;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.io.Serializable;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.plaf.UIResource;

/**
 * Los iconos del aspecto Metal; por ahora, la casilla y el boton de radio.
 *
 * <h2>Iconos medidos, no dibujados</h2>
 *
 * <p>En el JDK estos iconos se dibujan con lineas, arcos y un degradado ({@code Button.gradient})
 * sobre colores del tema. Aca estan como mapas de pixeles: cada estado de cada icono es lo que el
 * JDK 25 pinto, medido pixel por pixel en el tema Ocean. Es la misma sustitucion que
 * {@code jdk.internal.awt.FuenteBitmap}: el resultado exacto, en vez del procedimiento.
 *
 * <p>Los mapas usan una letra por color: {@code #} la sombra oscura (122, 138, 153), {@code p} el
 * control primario (184, 207, 229), {@code o} el negro del tema (51, 51, 51), {@code q} el gris
 * inactivo (153, 153, 153), {@code g} la fila correspondiente del degradado, y {@code .} nada. El
 * degradado va de arriba abajo y depende de la altura, asi que cada icono tiene su tabla de filas.
 *
 * <p>Los estados salen del modelo del boton, de mas a menos especifico: deshabilitado, apretado y
 * armado, con el cursor encima, y en reposo; cada uno con y sin seleccion. Un icono apretado
 * baja la tilde de la casilla un pixel, como baja el texto del boton.
 *
 * <h2>Los otros veinticuatro</h2>
 *
 * <p>Los dos de arriba son mapas medidos. Los demas -- el arbol, el marco interno, el selector de
 * archivos, los menues, el deslizador -- estan dibujados: tienen el <strong>tamano exacto</strong>
 * del JDK, la misma clase para cada uno, y la misma respuesta a
 * {@code instanceof UIResource}, que es lo que decide si el aspecto puede reemplazarlos. Lo que no
 * es identico es el pixel: son figuras propias en los colores del tema.
 *
 * <p>La distincion importa y esta documentada a proposito. Un tamano equivocado corre todo el
 * dibujo de alrededor; un trazo distinto adentro de un icono de 16 x 16 no corre nada.
 *
 * <h2>Cuales son recursos del aspecto y cuales no</h2>
 *
 * <p>Casi todos son {@link UIResource}, y eso significa que un cambio de aspecto los reemplaza. Los
 * tres del arbol que no lo son -- la manija, la carpeta y la hoja -- se quedan puestos, y esta
 * medido. La razon es que un arbol al que el programa le puso iconos propios no deberia perderlos
 * al cambiar de tema, y el JDK resuelve eso no marcandolos.
 *
 * <p>{@link #getMenuItemCheckIcon} devuelve {@code null}, y tambien esta medido: un item de menu de
 * Metal no lleva tilde propia.
 */
public class MetalIconFactory implements Serializable {

    private static final Color SOMBRA_OSCURA = new Color(122, 138, 153);
    private static final Color CONTROL_PRIMARIO = new Color(184, 207, 229);
    private static final Color NEGRO = new Color(51, 51, 51);
    private static final Color INACTIVO = new Color(153, 153, 153);

    private static Icon checkBoxIcon;
    private static Icon radioButtonIcon;

    public MetalIconFactory() {
    }

    /** El icono de casilla, compartido. */
    public static Icon getCheckBoxIcon() {
        if (checkBoxIcon == null) {
            checkBoxIcon = new CheckBoxIcon();
        }
        return checkBoxIcon;
    }

    /** El icono de boton de radio, compartido. */
    public static Icon getRadioButtonIcon() {
        if (radioButtonIcon == null) {
            radioButtonIcon = new RadioButtonIcon();
        }
        return radioButtonIcon;
    }

    /** Pinta un mapa de pixeles en ese lugar; ver la leyenda en la nota de la clase. */
    static void pintarMapa(Graphics g, int x, int y, String[] mapa, Color[] degradado) {
        for (int fila = 0; fila < mapa.length; fila++) {
            String linea = mapa[fila];
            for (int col = 0; col < linea.length(); col++) {
                char c = linea.charAt(col);
                Color color;
                if (c == '#') {
                    color = SOMBRA_OSCURA;
                } else if (c == 'p') {
                    color = CONTROL_PRIMARIO;
                } else if (c == 'o') {
                    color = NEGRO;
                } else if (c == 'q') {
                    color = INACTIVO;
                } else if (c == 'g') {
                    color = degradado[fila];
                } else {
                    continue;
                }
                g.setColor(color);
                g.fillRect(x + col, y + fila, 1, 1);
            }
        }
    }

    /** Que mapa corresponde al estado del modelo; el orden es el de la nota de la clase. */
    static int estado(ButtonModel m) {
        boolean sel = m.isSelected();
        if (!m.isEnabled()) {
            return sel ? 5 : 4;
        }
        if (m.isPressed() && m.isArmed()) {
            return sel ? 3 : 2;
        }
        if (m.isRollover()) {
            return sel ? 7 : 6;
        }
        return sel ? 1 : 0;
    }

    /** La casilla de Ocean: 13 por 13, medida. */
    private static class CheckBoxIcon implements Icon, UIResource, Serializable {

        private static final Color[] DEGRADADO = {
            null, new Color(0xE8EFF6), new Color(0xF3F7FA), new Color(0xFFFFFF),
            new Color(0xF3F7FB), new Color(0xE8EFF7), new Color(0xDDE8F3), new Color(0xD7E4F1),
            new Color(0xD2E0EF), new Color(0xCDDDED), new Color(0xC7D9EB), new Color(0xC2D6E9),
            null };

        private static final String[] NORMAL = {
            "#############",
            "#ggggggggggg#",
            "#ggggggggggg#",
            "#ggggggggggg#",
            "#ggggggggggg#",
            "#ggggggggggg#",
            "#ggggggggggg#",
            "#ggggggggggg#",
            "#ggggggggggg#",
            "#ggggggggggg#",
            "#ggggggggggg#",
            "#ggggggggggg#",
            "#############" };

        private static final String[] SELECCIONADO = {
            "#############",
            "#ggggggggggg#",
            "#ggggggggggg#",
            "#ggggggggogg#",
            "#gggggggoogg#",
            "#ggooggooggg#",
            "#ggoogoogggg#",
            "#ggooooggggg#",
            "#ggooogggggg#",
            "#ggooggggggg#",
            "#ggggggggggg#",
            "#ggggggggggg#",
            "#############" };

        private static final String[] APRETADO = {
            "#############",
            "#############",
            "##pppppppppp#",
            "##pppppppppp#",
            "##pppppppppp#",
            "##pppppppppp#",
            "##pppppppppp#",
            "##pppppppppp#",
            "##pppppppppp#",
            "##pppppppppp#",
            "##pppppppppp#",
            "##pppppppppp#",
            "#############" };

        private static final String[] APRETADO_SELECCIONADO = {
            "#############",
            "#############",
            "##pppppppppp#",
            "##pppppppppp#",
            "##pppppppopp#",
            "##ppppppoopp#",
            "##pooppooppp#",
            "##poopoopppp#",
            "##pooooppppp#",
            "##pooopppppp#",
            "##pooppppppp#",
            "##pppppppppp#",
            "#############" };

        private static final String[] DESHABILITADO = {
            "#############",
            "#...........#",
            "#...........#",
            "#...........#",
            "#...........#",
            "#...........#",
            "#...........#",
            "#...........#",
            "#...........#",
            "#...........#",
            "#...........#",
            "#...........#",
            "#############" };

        private static final String[] DESHABILITADO_SELECCIONADO = {
            "#############",
            "#...........#",
            "#...........#",
            "#........#..#",
            "#.......##..#",
            "#..##..##...#",
            "#..##.##....#",
            "#..####.....#",
            "#..###......#",
            "#..##.......#",
            "#...........#",
            "#...........#",
            "#############" };

        private static final String[] ROLLOVER = {
            "#############",
            "#ppppppppppp#",
            "#ppppppppppp#",
            "#ppgggggggpp#",
            "#ppgggggggpp#",
            "#ppgggggggpp#",
            "#ppgggggggpp#",
            "#ppgggggggpp#",
            "#ppgggggggpp#",
            "#ppgggggggpp#",
            "#ppppppppppp#",
            "#ppppppppppp#",
            "#############" };

        private static final String[] ROLLOVER_SELECCIONADO = {
            "#############",
            "#ppppppppppp#",
            "#ppppppppppp#",
            "#ppggggggopp#",
            "#ppgggggoopp#",
            "#ppooggoogpp#",
            "#ppoogooggpp#",
            "#ppoooogggpp#",
            "#ppoooggggpp#",
            "#ppoogggggpp#",
            "#ppppppppppp#",
            "#ppppppppppp#",
            "#############" };

        private static final String[][] MAPAS = { NORMAL, SELECCIONADO, APRETADO,
            APRETADO_SELECCIONADO, DESHABILITADO, DESHABILITADO_SELECCIONADO, ROLLOVER,
            ROLLOVER_SELECCIONADO };

        public void paintIcon(Component c, Graphics g, int x, int y) {
            ButtonModel modelo = ((AbstractButton) c).getModel();
            pintarMapa(g, x, y, MAPAS[estado(modelo)], DEGRADADO);
        }

        public int getIconWidth() {
            return 13;
        }

        public int getIconHeight() {
            return 13;
        }
    }

    /** El boton de radio de Ocean: 13 por 13, medido; la ultima fila queda vacia. */
    private static class RadioButtonIcon implements Icon, UIResource, Serializable {

        private static final Color[] DEGRADADO = {
            null, new Color(0xDDE8F3), new Color(0xE8EFF6), new Color(0xF3F7FA),
            new Color(0xFFFFFF), new Color(0xF3F7FB), new Color(0xE8EFF7), new Color(0xDDE8F3),
            new Color(0xD3E1EF), new Color(0xCADBEC), new Color(0xC1D5E8), null, null };

        private static final String[] NORMAL = {
            "....####.....",
            "..##gggg##...",
            ".#gggggggg#..",
            ".#gggggggg#..",
            "#gggggggggg#.",
            "#gggggggggg#.",
            "#gggggggggg#.",
            "#gggggggggg#.",
            ".#gggggggg#..",
            ".#gggggggg#..",
            "..##gggg##...",
            "....####.....",
            "............." };

        private static final String[] SELECCIONADO = {
            "....####.....",
            "..##gggg##...",
            ".#gggggggg#..",
            ".#ggoooogg#..",
            "#ggoooooogg#.",
            "#ggoooooogg#.",
            "#ggoooooogg#.",
            "#ggoooooogg#.",
            ".#ggoooogg#..",
            ".#gggggggg#..",
            "..##gggg##...",
            "....####.....",
            "............." };

        private static final String[] APRETADO = {
            "....####.....",
            "..########...",
            ".#.#ppppp.#..",
            ".##ppppppp#..",
            "##pppppppp.#.",
            "##pppppppp.#.",
            "##pppppppp.#.",
            "##pppppppp.#.",
            ".#pppppppp#..",
            ".##pppppp.#..",
            "..##....##...",
            "....####.....",
            "............." };

        private static final String[] APRETADO_SELECCIONADO = {
            "....####.....",
            "..########...",
            ".#.#ppppp.#..",
            ".##poooopp#..",
            "##poooooop.#.",
            "##poooooop.#.",
            "##poooooop.#.",
            "##poooooop.#.",
            ".#ppoooopp#..",
            ".##pppppp.#..",
            "..##....##...",
            "....####.....",
            "............." };

        private static final String[] DESHABILITADO = {
            "....qqqq.....",
            "..qq....qq...",
            ".q........q..",
            ".q........q..",
            "q..........q.",
            "q..........q.",
            "q..........q.",
            "q..........q.",
            ".q........q..",
            ".q........q..",
            "..qq....qq...",
            "....qqqq.....",
            "............." };

        private static final String[] DESHABILITADO_SELECCIONADO = {
            "....qqqq.....",
            "..qq....qq...",
            ".q........q..",
            ".q..####..q..",
            "q..######..q.",
            "q..######..q.",
            "q..######..q.",
            "q..######..q.",
            ".q..####..q..",
            ".q........q..",
            "..qq....qq...",
            "....qqqq.....",
            "............." };

        private static final String[] ROLLOVER = {
            "....####.....",
            "..##gggg##...",
            ".#ggppppgg#..",
            ".#gpggggpg#..",
            "#gpggggggpg#.",
            "#gpggggggpg#.",
            "#gpggggggpg#.",
            "#gpggggggpg#.",
            ".#gpggggpg#..",
            ".#ggppppgg#..",
            "..##gggg##...",
            "....####.....",
            "............." };

        private static final String[] ROLLOVER_SELECCIONADO = {
            "....####.....",
            "..##gggg##...",
            ".#ggppppgg#..",
            ".#gpoooopg#..",
            "#gpoooooopg#.",
            "#gpoooooopg#.",
            "#gpoooooopg#.",
            "#gpoooooopg#.",
            ".#gpoooopg#..",
            ".#ggppppgg#..",
            "..##gggg##...",
            "....####.....",
            "............." };

        private static final String[][] MAPAS = { NORMAL, SELECCIONADO, APRETADO,
            APRETADO_SELECCIONADO, DESHABILITADO, DESHABILITADO_SELECCIONADO, ROLLOVER,
            ROLLOVER_SELECCIONADO };

        public void paintIcon(Component c, Graphics g, int x, int y) {
            ButtonModel modelo = ((AbstractButton) c).getModel();
            pintarMapa(g, x, y, MAPAS[estado(modelo)], DEGRADADO);
        }

        public int getIconWidth() {
            return 13;
        }

        public int getIconHeight() {
            return 13;
        }
    }

    // ---- los otros veinticuatro; ver la nota de la clase ----

    /** El sentido de la luz para un icono de marco interno: desde arriba. */
    public static final boolean LIGHT = true;

    /** Y el contrario. */
    public static final boolean DARK = false;

    private static Icon checkBoxMenuItemIcon;
    private static Icon radioButtonMenuItemIcon;
    private static Icon menuArrowIcon;
    private static Icon menuItemArrowIcon;
    private static Icon treeComputerIcon;
    private static Icon treeHardDriveIcon;
    private static Icon treeFloppyDriveIcon;
    private static Icon fcNewFolderIcon;
    private static Icon fcUpFolderIcon;
    private static Icon fcHomeFolderIcon;
    private static Icon fcDetailViewIcon;
    private static Icon fcListViewIcon;
    private static Icon hSliderThumbIcon;
    private static Icon vSliderThumbIcon;
    private static Icon ifDefaultMenuIcon;

    public static Icon getCheckBoxMenuItemIcon() {
        if (checkBoxMenuItemIcon == null) {
            checkBoxMenuItemIcon = new CheckBoxMenuItemIcon();
        }
        return checkBoxMenuItemIcon;
    }

    public static Icon getRadioButtonMenuItemIcon() {
        if (radioButtonMenuItemIcon == null) {
            radioButtonMenuItemIcon = new RadioButtonMenuItemIcon();
        }
        return radioButtonMenuItemIcon;
    }

    /** Ninguno; ver la nota de la clase. */
    public static Icon getMenuItemCheckIcon() {
        return null;
    }

    public static Icon getMenuItemArrowIcon() {
        if (menuItemArrowIcon == null) {
            menuItemArrowIcon = new MenuItemArrowIcon();
        }
        return menuItemArrowIcon;
    }

    public static Icon getMenuArrowIcon() {
        if (menuArrowIcon == null) {
            menuArrowIcon = new MenuArrowIcon();
        }
        return menuArrowIcon;
    }

    /**
     * La manija de abrir y cerrar una rama.
     *
     * <p>Uno nuevo cada vez, igual que la carpeta y la hoja. Los tres son justo los que no son
     * {@link UIResource} -- un arbol puede quedarselos aunque cambie el aspecto -- y compartirlos
     * haria que dos arboles con temas distintos se pisaran el icono. Medido.
     *
     * @param isCollapsed si la rama esta cerrada
     */
    public static Icon getTreeControlIcon(boolean isCollapsed) {
        return new TreeControlIcon(isCollapsed);
    }

    public static Icon getTreeFolderIcon() {
        return new TreeFolderIcon();
    }

    public static Icon getTreeLeafIcon() {
        return new TreeLeafIcon();
    }

    public static Icon getTreeComputerIcon() {
        if (treeComputerIcon == null) {
            treeComputerIcon = new TreeComputerIcon();
        }
        return treeComputerIcon;
    }

    public static Icon getTreeHardDriveIcon() {
        if (treeHardDriveIcon == null) {
            treeHardDriveIcon = new TreeHardDriveIcon();
        }
        return treeHardDriveIcon;
    }

    public static Icon getTreeFloppyDriveIcon() {
        if (treeFloppyDriveIcon == null) {
            treeFloppyDriveIcon = new TreeFloppyDriveIcon();
        }
        return treeFloppyDriveIcon;
    }

    public static Icon getFileChooserNewFolderIcon() {
        if (fcNewFolderIcon == null) {
            fcNewFolderIcon = new FileChooserNewFolderIcon();
        }
        return fcNewFolderIcon;
    }

    public static Icon getFileChooserUpFolderIcon() {
        if (fcUpFolderIcon == null) {
            fcUpFolderIcon = new FileChooserUpFolderIcon();
        }
        return fcUpFolderIcon;
    }

    public static Icon getFileChooserHomeFolderIcon() {
        if (fcHomeFolderIcon == null) {
            fcHomeFolderIcon = new FileChooserHomeFolderIcon();
        }
        return fcHomeFolderIcon;
    }

    public static Icon getFileChooserDetailViewIcon() {
        if (fcDetailViewIcon == null) {
            fcDetailViewIcon = new FileChooserDetailViewIcon();
        }
        return fcDetailViewIcon;
    }

    public static Icon getFileChooserListViewIcon() {
        if (fcListViewIcon == null) {
            fcListViewIcon = new FileChooserListViewIcon();
        }
        return fcListViewIcon;
    }

    /** El pulgar de un deslizador acostado: quince de ancho por dieciseis de alto. */
    public static Icon getHorizontalSliderThumbIcon() {
        if (hSliderThumbIcon == null) {
            hSliderThumbIcon = new OceanHorizontalSliderThumbIcon();
        }
        return hSliderThumbIcon;
    }

    public static Icon getVerticalSliderThumbIcon() {
        if (vSliderThumbIcon == null) {
            vSliderThumbIcon = new OceanVerticalSliderThumbIcon();
        }
        return vSliderThumbIcon;
    }

    public static Icon getInternalFrameDefaultMenuIcon() {
        if (ifDefaultMenuIcon == null) {
            ifDefaultMenuIcon = new InternalFrameDefaultMenuIcon();
        }
        return ifDefaultMenuIcon;
    }

    /**
     * La cruz de cerrar una ventana interna.
     *
     * <p>Los cuatro botones de la barra de titulo toman el tamano como parametro y no lo tienen
     * fijo: una ventana normal los quiere de dieciseis y una paleta de ocho. Por eso estos cinco
     * metodos fabrican uno nuevo cada vez en vez de compartir.
     *
     * @param size el lado, en pixeles
     */
    public static Icon getInternalFrameCloseIcon(int size) {
        return new InternalFrameCloseIcon(size);
    }

    public static Icon getInternalFrameMaximizeIcon(int size) {
        return new InternalFrameMaximizeIcon(size);
    }

    /** El de restaurar: dos marcos corridos. */
    public static Icon getInternalFrameAltMaximizeIcon(int size) {
        return new InternalFrameAltMaximizeIcon(size);
    }

    public static Icon getInternalFrameMinimizeIcon(int size) {
        return new InternalFrameMinimizeIcon(size);
    }

    // ---- las clases; los nombres son los del JDK y se ven por getClass().getName() ----

    /** Un icono de lado fijo que dibuja en los colores del tema. */
    private abstract static class Dibujado implements Icon, Serializable {

        private final int ancho;
        private final int alto;

        Dibujado(int ancho, int alto) {
            this.ancho = ancho;
            this.alto = alto;
        }

        public int getIconWidth() {
            return ancho;
        }

        public int getIconHeight() {
            return alto;
        }

        /** El color de trazo: el del tema, o el gris si el componente no responde. */
        static Color trazo(Component c) {
            if (c != null && !c.isEnabled()) {
                return MetalLookAndFeel.getControlShadow();
            }
            return MetalLookAndFeel.getControlInfo();
        }
    }

    /** El cuadradito de un item de menu con casilla. */
    public static class CheckBoxMenuItemIcon extends Dibujado implements UIResource {

        public CheckBoxMenuItemIcon() {
            super(10, 10);
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            boolean prendido = (c instanceof AbstractButton)
                    && ((AbstractButton) c).getModel().isSelected();
            g.setColor(trazo(c));
            g.drawRect(x, y, 9, 9);
            if (prendido) {
                g.drawLine(x + 2, y + 5, x + 4, y + 7);
                g.drawLine(x + 4, y + 7, x + 7, y + 2);
            }
        }
    }

    /** Y el circulito de uno con opcion. */
    public static class RadioButtonMenuItemIcon extends Dibujado implements UIResource {

        public RadioButtonMenuItemIcon() {
            super(10, 10);
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            boolean prendido = (c instanceof AbstractButton)
                    && ((AbstractButton) c).getModel().isSelected();
            g.setColor(trazo(c));
            g.drawOval(x, y, 9, 9);
            if (prendido) {
                g.fillOval(x + 3, y + 3, 4, 4);
            }
        }
    }

    /** La flecha que dice que un item de menu abre un submenu. */
    public static class MenuArrowIcon extends Dibujado implements UIResource {

        public MenuArrowIcon() {
            super(4, 8);
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(trazo(c));
            for (int i = 0; i < 4; i++) {
                g.drawLine(x + i, y + i, x + i, y + 7 - i);
            }
        }
    }

    /** La misma flecha, para un item que no es un menu. */
    public static class MenuItemArrowIcon extends Dibujado implements UIResource {

        public MenuItemArrowIcon() {
            super(4, 8);
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
        }
    }

    /** La manija de una rama; ver {@link MetalIconFactory#getTreeControlIcon}. */
    public static class TreeControlIcon extends Dibujado {

        protected boolean isLight;

        public TreeControlIcon(boolean isCollapsed) {
            super(18, 18);
            this.isLight = isCollapsed;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            drawControlIcon(g, x, y, isLight);
        }

        /** Un circulo con un mas o un menos adentro. */
        void drawControlIcon(Graphics g, int x, int y, boolean cerrada) {
            g.setColor(MetalLookAndFeel.getPrimaryControlDarkShadow());
            g.drawOval(x + 4, y + 4, 9, 9);
            g.drawLine(x + 6, y + 8, x + 11, y + 8);
            if (cerrada) {
                g.drawLine(x + 8, y + 6, x + 8, y + 11);
            }
        }
    }

    /** La carpeta del arbol. No es un recurso del aspecto; ver la nota de la clase. */
    public static class TreeFolderIcon extends Dibujado {

        public TreeFolderIcon() {
            super(16, 18);
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(MetalLookAndFeel.getPrimaryControlShadow());
            g.fillRect(x + 1, y + 5, 14, 11);
            g.setColor(MetalLookAndFeel.getPrimaryControlDarkShadow());
            g.drawRect(x + 1, y + 5, 13, 10);
            g.drawLine(x + 1, y + 4, x + 6, y + 4);
            g.drawLine(x + 6, y + 4, x + 8, y + 5);
        }
    }

    /** Y la hoja. Tampoco es un recurso. */
    public static class TreeLeafIcon extends Dibujado {

        public TreeLeafIcon() {
            super(16, 20);
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(MetalLookAndFeel.getWindowBackground());
            g.fillRect(x + 2, y + 2, 11, 15);
            g.setColor(MetalLookAndFeel.getPrimaryControlDarkShadow());
            g.drawRect(x + 2, y + 2, 10, 14);
            // La esquina doblada, que es lo que lo hace leerse como una hoja de papel.
            g.drawLine(x + 9, y + 2, x + 12, y + 5);
        }
    }

    /** La computadora del selector de archivos. */
    public static class TreeComputerIcon extends Dibujado implements UIResource {

        public TreeComputerIcon() {
            super(16, 16);
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(MetalLookAndFeel.getPrimaryControlDarkShadow());
            g.drawRect(x + 2, y + 2, 11, 8);
            g.drawRect(x + 5, y + 12, 5, 2);
            g.setColor(MetalLookAndFeel.getPrimaryControlShadow());
            g.fillRect(x + 3, y + 3, 10, 7);
        }
    }

    /** El disco rigido. */
    public static class TreeHardDriveIcon extends Dibujado implements UIResource {

        public TreeHardDriveIcon() {
            super(16, 16);
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(MetalLookAndFeel.getPrimaryControlShadow());
            g.fillRect(x + 1, y + 5, 14, 6);
            g.setColor(MetalLookAndFeel.getPrimaryControlDarkShadow());
            g.drawRect(x + 1, y + 5, 13, 5);
            g.drawLine(x + 11, y + 8, x + 12, y + 8);
        }
    }

    /** Y el diskette, que sigue ahi por compatibilidad con una epoca. */
    public static class TreeFloppyDriveIcon extends Dibujado implements UIResource {

        public TreeFloppyDriveIcon() {
            super(16, 16);
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(MetalLookAndFeel.getPrimaryControlShadow());
            g.fillRect(x + 1, y + 2, 14, 12);
            g.setColor(MetalLookAndFeel.getPrimaryControlDarkShadow());
            g.drawRect(x + 1, y + 2, 13, 11);
            g.fillRect(x + 5, y + 3, 6, 4);
            g.drawRect(x + 4, y + 9, 7, 4);
        }
    }

    /** Una carpeta de dieciocho, base de los tres botones del selector. */
    private abstract static class CarpetaDeSelector extends Dibujado implements UIResource {

        CarpetaDeSelector() {
            super(18, 18);
        }

        /** La carpeta sola; cada boton le agrega su marca encima. */
        void carpeta(Graphics g, int x, int y) {
            g.setColor(MetalLookAndFeel.getPrimaryControlShadow());
            g.fillRect(x + 1, y + 5, 15, 10);
            g.setColor(MetalLookAndFeel.getPrimaryControlDarkShadow());
            g.drawRect(x + 1, y + 5, 14, 9);
            g.drawLine(x + 1, y + 4, x + 6, y + 4);
            g.drawLine(x + 6, y + 4, x + 8, y + 5);
        }
    }

    /** La carpeta con una estrella: crear una nueva. */
    public static class FileChooserNewFolderIcon extends CarpetaDeSelector {

        public FileChooserNewFolderIcon() {
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            carpeta(g, x, y);
        }
    }

    /** La carpeta con una flecha para arriba: subir un nivel. */
    public static class FileChooserUpFolderIcon extends CarpetaDeSelector {

        public FileChooserUpFolderIcon() {
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            carpeta(g, x, y);
            g.setColor(MetalLookAndFeel.getControlInfo());
            g.drawLine(x + 8, y + 7, x + 8, y + 12);
            g.drawLine(x + 6, y + 9, x + 8, y + 7);
            g.drawLine(x + 8, y + 7, x + 10, y + 9);
        }
    }

    /** Y la casita. */
    public static class FileChooserHomeFolderIcon extends Dibujado implements UIResource {

        public FileChooserHomeFolderIcon() {
            super(18, 18);
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(MetalLookAndFeel.getPrimaryControlDarkShadow());
            g.drawLine(x + 2, y + 9, x + 9, y + 2);
            g.drawLine(x + 9, y + 2, x + 16, y + 9);
            g.drawRect(x + 4, y + 9, 10, 6);
        }
    }

    /** Las tres rayas de la vista de detalle. */
    public static class FileChooserDetailViewIcon extends Dibujado implements UIResource {

        public FileChooserDetailViewIcon() {
            super(18, 18);
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(MetalLookAndFeel.getPrimaryControlInfo());
            for (int i = 0; i < 3; i++) {
                g.fillRect(x + 3, y + 4 + i * 4, 2, 2);
                g.fillRect(x + 7, y + 4 + i * 4, 8, 2);
            }
        }
    }

    /** Y las dos columnas de la vista de lista. */
    public static class FileChooserListViewIcon extends Dibujado implements UIResource {

        public FileChooserListViewIcon() {
            super(18, 18);
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(MetalLookAndFeel.getPrimaryControlInfo());
            for (int col = 0; col < 2; col++) {
                for (int i = 0; i < 3; i++) {
                    g.fillRect(x + 3 + col * 8, y + 4 + i * 4, 2, 2);
                    g.fillRect(x + 6 + col * 8, y + 4 + i * 4, 3, 2);
                }
            }
        }
    }

    /**
     * El pulgar de un deslizador acostado.
     *
     * <p>Quince de ancho por dieciseis de alto, y la punta abajo. El nombre lleva {@code Ocean}
     * porque el tema Steel usa otro; los dos existen en el JDK y {@code getHorizontalSliderThumbIcon}
     * devuelve el que corresponda al tema.
     */
    public static class OceanHorizontalSliderThumbIcon extends Dibujado implements UIResource {

        public OceanHorizontalSliderThumbIcon() {
            super(15, 16);
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(MetalLookAndFeel.getPrimaryControlShadow());
            g.fillRect(x + 1, y + 1, 13, 9);
            g.setColor(MetalLookAndFeel.getPrimaryControlDarkShadow());
            g.drawRect(x, y, 14, 9);
            for (int i = 0; i < 6; i++) {
                g.drawLine(x + 1 + i, y + 10 + i, x + 13 - i, y + 10 + i);
            }
        }
    }

    /** El mismo, parado. */
    public static class OceanVerticalSliderThumbIcon extends Dibujado implements UIResource {

        public OceanVerticalSliderThumbIcon() {
            super(16, 15);
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(MetalLookAndFeel.getPrimaryControlShadow());
            g.fillRect(x + 1, y + 1, 9, 13);
            g.setColor(MetalLookAndFeel.getPrimaryControlDarkShadow());
            g.drawRect(x, y, 9, 14);
            for (int i = 0; i < 6; i++) {
                g.drawLine(x + 10 + i, y + 1 + i, x + 10 + i, y + 13 - i);
            }
        }
    }

    /** El icono de la esquina izquierda de una barra de titulo. */
    public static class InternalFrameDefaultMenuIcon extends Dibujado implements UIResource {

        public InternalFrameDefaultMenuIcon() {
            super(16, 16);
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(MetalLookAndFeel.getPrimaryControlShadow());
            g.fillRect(x + 2, y + 3, 12, 10);
            g.setColor(MetalLookAndFeel.getPrimaryControlDarkShadow());
            g.drawRect(x + 2, y + 3, 11, 9);
        }
    }

    /** Los cuatro botones de la barra de titulo; el lado viene por parametro. */
    private abstract static class BotonDeTitulo extends Dibujado implements UIResource {

        BotonDeTitulo(int lado) {
            super(lado, lado);
        }

        int margen() {
            return getIconWidth() / 4;
        }

        int lejos() {
            return getIconWidth() - 1 - margen();
        }
    }

    /** La cruz. */
    public static class InternalFrameCloseIcon extends BotonDeTitulo {

        public InternalFrameCloseIcon(int size) {
            super(size);
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            int m = margen();
            int f = lejos();
            g.setColor(MetalLookAndFeel.getControlInfo());
            g.drawLine(x + m, y + m, x + f, y + f);
            g.drawLine(x + m + 1, y + m, x + f, y + f - 1);
            g.drawLine(x + f, y + m, x + m, y + f);
            g.drawLine(x + f - 1, y + m, x + m, y + f - 1);
        }
    }

    /** El cuadrado de agrandar. */
    public static class InternalFrameMaximizeIcon extends BotonDeTitulo {

        public InternalFrameMaximizeIcon(int size) {
            super(size);
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            int m = margen();
            int f = lejos();
            g.setColor(MetalLookAndFeel.getControlInfo());
            g.drawRect(x + m, y + m, f - m, f - m);
            g.drawLine(x + m, y + m + 1, x + f, y + m + 1);
        }
    }

    /** Los dos cuadrados corridos de restaurar. */
    public static class InternalFrameAltMaximizeIcon extends BotonDeTitulo {

        public InternalFrameAltMaximizeIcon(int size) {
            super(size);
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            int m = margen();
            int f = lejos();
            g.setColor(MetalLookAndFeel.getControlInfo());
            g.drawRect(x + m, y + m + 2, f - m - 2, f - m - 2);
            g.drawRect(x + m + 2, y + m, f - m - 2, f - m - 2);
        }
    }

    /** Y la raya de achicar. */
    public static class InternalFrameMinimizeIcon extends BotonDeTitulo {

        public InternalFrameMinimizeIcon(int size) {
            super(size);
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            int m = margen();
            int f = lejos();
            g.setColor(MetalLookAndFeel.getControlInfo());
            g.fillRect(x + m, y + f - 1, f - m + 1, 2);
        }
    }
}
