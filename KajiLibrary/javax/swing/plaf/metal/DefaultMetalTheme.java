package javax.swing.plaf.metal;

import java.awt.Font;

import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;

/**
 * El tema Steel: el Metal de siempre, azul y gris.
 *
 * <p>Es la clase mas corta que hace algo grande. Da los seis colores que {@link MetalTheme} pide
 * y seis tipografias, y con eso queda definido un aspecto grafico entero.
 *
 * <p>Los seis colores son tres tonos de un azul violaceo y tres grises, todos multiplos de
 * {@code 0x33}: {@code 102/153/204} y {@code 255}. Eso no es casualidad ni gusto -- es la paleta
 * segura de 216 colores de los monitores de 256 --. Metal nacio para verse igual en cualquier
 * maquina, y esos seis valores son los que sobreviven a una pantalla que no puede mostrar mas.
 *
 * <p>Las tipografias son cinco veces Dialog en 12 y una en 10. Lo unico que las distingue es la
 * negrita: <strong>lo que se toca va en negrita</strong> --botones, menues, titulo de ventana-- y
 * lo que se lee va normal --texto del sistema y del usuario--. La chica de 10 es para lo
 * secundario, como el acelerador de un item de menu.
 *
 * <p>El nombre es {@code "Steel"} y sale por
 * {@code MetalLookAndFeel.getCurrentTheme().getName()}.
 */
public class DefaultMetalTheme extends MetalTheme {

    private static final ColorUIResource PRIMARIO_1 = new ColorUIResource(102, 102, 153);
    private static final ColorUIResource PRIMARIO_2 = new ColorUIResource(153, 153, 204);
    private static final ColorUIResource PRIMARIO_3 = new ColorUIResource(204, 204, 255);

    private static final ColorUIResource SECUNDARIO_1 = new ColorUIResource(102, 102, 102);
    private static final ColorUIResource SECUNDARIO_2 = new ColorUIResource(153, 153, 153);
    private static final ColorUIResource SECUNDARIO_3 = new ColorUIResource(204, 204, 204);

    private static final FontUIResource CONTROL =
            new FontUIResource("Dialog", Font.BOLD, 12);
    private static final FontUIResource SISTEMA =
            new FontUIResource("Dialog", Font.PLAIN, 12);
    private static final FontUIResource USUARIO =
            new FontUIResource("Dialog", Font.PLAIN, 12);
    private static final FontUIResource MENU =
            new FontUIResource("Dialog", Font.BOLD, 12);
    private static final FontUIResource TITULO =
            new FontUIResource("Dialog", Font.BOLD, 12);
    private static final FontUIResource CHICA =
            new FontUIResource("Dialog", Font.PLAIN, 10);

    public DefaultMetalTheme() {
    }

    public String getName() {
        return "Steel";
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

    /** Dialog negrita 12: lo que se toca. */
    public FontUIResource getControlTextFont() {
        return CONTROL;
    }

    /** Dialog normal 12: lo que se lee. */
    public FontUIResource getSystemTextFont() {
        return SISTEMA;
    }

    public FontUIResource getUserTextFont() {
        return USUARIO;
    }

    public FontUIResource getMenuTextFont() {
        return MENU;
    }

    public FontUIResource getWindowTitleFont() {
        return TITULO;
    }

    /** Dialog normal 10: lo secundario. */
    public FontUIResource getSubTextFont() {
        return CHICA;
    }
}
