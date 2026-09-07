package javax.swing.plaf.metal;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;
import java.util.Arrays;

import javax.swing.Icon;
import javax.swing.UIDefaults;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.UIResource;

/**
 * El tema Ocean: el Metal por omision desde Java 5.
 *
 * <h2>Que cambio, y por que</h2>
 *
 * <p>Steel se diseno para monitores de 256 colores y se nota: seis tonos de la paleta segura, todos
 * multiplos de {@code 0x33}. Ocean da por sentado color verdadero y elige lo que se ve mejor y no
 * lo que sobrevive a una pantalla pobre: celestes en vez de azul violaceo, gris {@code 238} en vez
 * de {@code 204}, y texto {@code (51,51,51)} en vez de negro puro, que a esa altura ya se sabia que
 * cansa menos.
 *
 * <p>Lo interesante es cuanto de eso entra en seis colores. Casi todo: Ocean redefine los seis y
 * hereda el resto de {@link MetalTheme}. Redefine ademas cinco derivados, y cada uno tiene su
 * motivo:
 *
 * <ul>
 *   <li>{@link #getBlack} pasa a {@code (51,51,51)}, y con eso se corre todo el texto de golpe.
 *   <li>{@link #getDesktopColor} pasa a blanco: el {@code primary2} de Ocean es un celeste claro y
 *       un escritorio de ese color no dejaria ver nada apoyado encima.
 *   <li>{@link #getInactiveControlTextColor} y {@link #getMenuDisabledForeground} vuelven al gris
 *       {@code 153}: derivarlos del {@code secondary2} de Ocean daria celeste, y un texto apagado
 *       tiene que verse apagado, no de otro color.
 *   <li>{@link #getControlTextColor} queda fijo en {@code (51,51,51)}.
 * </ul>
 *
 * <h2>Los degradados</h2>
 *
 * <p>Ocean es el primer tema que pinta botones con degradado, y lo hace con una lista de cinco
 * cosas: dos fracciones y tres colores. Las fracciones dicen donde estan los cortes -- el
 * {@code 0.3} es donde termina el primer tramo y el {@code 0.0} cuanto mide el tramo plano del
 * medio -- y los tres colores son el de arriba, el del medio y el de abajo. La lista es de
 * {@code Object} y en ese orden porque asi la lee {@code MetalUtils}, y el formato quedo publico
 * de hecho: un tema propio que quiera degradados tiene que armarla igual.
 *
 * <h2>Lo que queda dicho y no tapado</h2>
 *
 * <p>De las 67 entradas que el tema del JDK agrega, esta escribe 52. Las quince que faltan son
 * <strong>imagenes GIF</strong> que el JDK carga del jar -- los cuatro iconos de dialogo, los de
 * carpeta y archivo, los del arbol, las manijas --. No hay de donde sacarlas y una figura dibujada
 * a mano no seria la misma imagen; quedan sin poner, que es lo mismo que le pasa a un aspecto al
 * que le falta un recurso. Los cinco iconos de la barra de titulo si estan: esos el JDK tambien los
 * dibuja.
 */
public class OceanTheme extends DefaultMetalTheme {

    private static final ColorUIResource PRIMARIO_1 = new ColorUIResource(99, 130, 191);
    private static final ColorUIResource PRIMARIO_2 = new ColorUIResource(163, 184, 204);
    private static final ColorUIResource PRIMARIO_3 = new ColorUIResource(184, 207, 229);

    private static final ColorUIResource SECUNDARIO_1 = new ColorUIResource(122, 138, 153);
    private static final ColorUIResource SECUNDARIO_2 = new ColorUIResource(184, 207, 229);
    private static final ColorUIResource SECUNDARIO_3 = new ColorUIResource(238, 238, 238);

    private static final ColorUIResource NEGRO = new ColorUIResource(51, 51, 51);
    private static final ColorUIResource BLANCO = new ColorUIResource(255, 255, 255);
    private static final ColorUIResource GRIS_APAGADO = new ColorUIResource(153, 153, 153);

    public OceanTheme() {
    }

    public String getName() {
        return "Ocean";
    }

    protected ColorUIResource getPrimary1() {
        return PRIMARIO_1;
    }

    protected ColorUIResource getPrimary2() {
        return PRIMARIO_2;
    }

    protected ColorUIResource getPrimary3() {
        return PRIMARIO_3;
    }

    protected ColorUIResource getSecondary1() {
        return SECUNDARIO_1;
    }

    protected ColorUIResource getSecondary2() {
        return SECUNDARIO_2;
    }

    protected ColorUIResource getSecondary3() {
        return SECUNDARIO_3;
    }

    /** Gris muy oscuro, no negro; ver la nota de la clase. */
    protected ColorUIResource getBlack() {
        return NEGRO;
    }

    /** Blanco: el celeste del tema no serviria de fondo de escritorio. */
    public ColorUIResource getDesktopColor() {
        return BLANCO;
    }

    public ColorUIResource getControlTextColor() {
        return NEGRO;
    }

    /** Gris, no celeste: lo apagado tiene que verse apagado. */
    public ColorUIResource getInactiveControlTextColor() {
        return GRIS_APAGADO;
    }

    public ColorUIResource getMenuDisabledForeground() {
        return GRIS_APAGADO;
    }

    /** El degradado de casi todos los controles. */
    private static java.util.List<Object> degradadoDeControl() {
        return Arrays.asList(new Object[] {
                Float.valueOf(0.3f), Float.valueOf(0.0f),
                new ColorUIResource(221, 232, 243),
                BLANCO,
                new ColorUIResource(184, 207, 229) });
    }

    /** El del deslizador, que tiene un tramo plano en el medio. */
    private static java.util.List<Object> degradadoDeDeslizador() {
        return Arrays.asList(new Object[] {
                Float.valueOf(0.3f), Float.valueOf(0.2f),
                new ColorUIResource(200, 221, 242),
                BLANCO,
                new ColorUIResource(184, 207, 229) });
    }

    /** El de la barra de menu, que va de blanco a gris de una sola vez. */
    private static java.util.List<Object> degradadoDeMenu() {
        return Arrays.asList(new Object[] {
                Float.valueOf(1.0f), Float.valueOf(0.0f),
                BLANCO,
                new ColorUIResource(218, 218, 218),
                new ColorUIResource(218, 218, 218) });
    }

    /** Los cincuenta y dos valores propios de Ocean; ver la nota de la clase. */
    public void addCustomEntriesToTable(UIDefaults table) {
        if (table == null) {
            return;
        }
        ColorUIResource azul = PRIMARIO_1;
        ColorUIResource celesteClaro = new ColorUIResource(200, 221, 242);
        ColorUIResource fondoDeSuelta = new ColorUIResource(210, 233, 255);
        ColorUIResource gris204 = new ColorUIResource(204, 204, 204);
        ColorUIResource gris218 = new ColorUIResource(218, 218, 218);
        Object bordeDeFoco = new BorderUIResource.LineBorderUIResource(azul);

        Object[] pares = {
            "Button.gradient", degradadoDeControl(),
            "Button.rollover", Boolean.TRUE,
            "Button.rolloverIconType", "ocean",
            "Button.toolBarBorderBackground", GRIS_APAGADO,
            "Button.disabledToolBarBorderBackground", gris204,

            "CheckBox.gradient", degradadoDeControl(),
            "CheckBox.rollover", Boolean.TRUE,
            "CheckBoxMenuItem.gradient", degradadoDeControl(),
            "RadioButton.gradient", degradadoDeControl(),
            "RadioButton.rollover", Boolean.TRUE,
            "RadioButtonMenuItem.gradient", degradadoDeControl(),
            "ToggleButton.gradient", degradadoDeControl(),
            "ScrollBar.gradient", degradadoDeControl(),

            "InternalFrame.activeTitleGradient", degradadoDeControl(),
            "InternalFrame.closeIcon", new IconoDeTitulo(16, IconoDeTitulo.CERRAR),
            "InternalFrame.iconifyIcon", new IconoDeTitulo(16, IconoDeTitulo.ACHICAR),
            "InternalFrame.maximizeIcon", new IconoDeTitulo(16, IconoDeTitulo.AGRANDAR),
            "InternalFrame.minimizeIcon", new IconoDeTitulo(16, IconoDeTitulo.RESTAURAR),
            "InternalFrame.paletteCloseIcon", new IconoDeTitulo(7, IconoDeTitulo.CERRAR),

            "Label.disabledForeground", GRIS_APAGADO,

            "List.focusCellHighlightBorder", bordeDeFoco,
            "List.dropLineColor", azul,
            "List.dropCellBackground", fondoDeSuelta,

            "Menu.opaque", Boolean.FALSE,
            "MenuBar.gradient", degradadoDeMenu(),
            "MenuBar.borderColor", gris204,

            "Slider.altTrackColor", new ColorUIResource(210, 226, 239),
            "Slider.gradient", degradadoDeDeslizador(),
            "Slider.focusGradient", degradadoDeDeslizador(),

            "SplitPane.oneTouchButtonsOpaque", Boolean.FALSE,
            "SplitPane.dividerFocusColor", celesteClaro,

            "TabbedPane.borderHightlightColor", azul,
            "TabbedPane.contentAreaColor", celesteClaro,
            "TabbedPane.contentBorderInsets", new Insets(4, 2, 3, 3),
            "TabbedPane.selected", celesteClaro,
            "TabbedPane.tabAreaBackground", gris218,
            "TabbedPane.tabAreaInsets", new Insets(2, 2, 0, 6),
            "TabbedPane.unselectedBackground", SECUNDARIO_3,

            "Table.focusCellHighlightBorder", bordeDeFoco,
            "Table.gridColor", SECUNDARIO_1,
            "Table.dropLineColor", azul,
            "Table.dropLineShortColor", NEGRO,
            "Table.dropCellBackground", fondoDeSuelta,
            "TableHeader.focusCellBackground", celesteClaro,

            "ToolBar.borderColor", gris204,
            "ToolBar.isRollover", Boolean.TRUE,

            "Tree.dropLineColor", azul,
            "Tree.dropCellBackground", fondoDeSuelta,
            "Tree.selectionBorderColor", azul,
        };
        table.putDefaults(pares);
    }

    /**
     * Los botones de la barra de titulo de un marco interno.
     *
     * <p>El JDK los dibuja en vez de cargarlos, y por eso estos si estan. Son cuadrados de
     * dieciseis -- salvo el de cerrar de una paleta, que es de siete -- y cada uno dibuja su figura
     * en el color del texto del tema.
     */
    private static class IconoDeTitulo implements Icon, UIResource {

        static final int CERRAR = 0;
        static final int ACHICAR = 1;
        static final int AGRANDAR = 2;
        static final int RESTAURAR = 3;

        private final int lado;
        private final int cual;

        IconoDeTitulo(int lado, int cual) {
            this.lado = lado;
            this.cual = cual;
        }

        public int getIconWidth() {
            return lado;
        }

        public int getIconHeight() {
            return lado;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            Color antes = g.getColor();
            g.setColor(NEGRO);
            int m = lado / 4;
            int f = lado - 1 - m;
            switch (cual) {
                case CERRAR:
                    g.drawLine(x + m, y + m, x + f, y + f);
                    g.drawLine(x + m + 1, y + m, x + f, y + f - 1);
                    g.drawLine(x + f, y + m, x + m, y + f);
                    g.drawLine(x + f - 1, y + m, x + m, y + f - 1);
                    break;
                case ACHICAR:
                    // Una raya abajo, que es lo que queda de una ventana achicada.
                    g.fillRect(x + m, y + f - 1, f - m + 1, 2);
                    break;
                case AGRANDAR:
                    g.drawRect(x + m, y + m, f - m, f - m);
                    g.drawLine(x + m, y + m + 1, x + f, y + m + 1);
                    break;
                case RESTAURAR:
                    // Dos marcos corridos: la ventana vuelve a su tamano anterior.
                    g.drawRect(x + m, y + m + 2, f - m - 2, f - m - 2);
                    g.drawRect(x + m + 2, y + m, f - m - 2, f - m - 2);
                    break;
                default:
                    break;
            }
            g.setColor(antes);
        }
    }
}
