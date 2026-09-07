import java.awt.Color;
import java.awt.Dimension;
import java.io.File;

import javax.swing.Icon;
import javax.swing.JDesktopPane;
import javax.swing.JFileChooser;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JScrollBar;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.plaf.metal.MetalFileChooserUI;
import javax.swing.plaf.metal.MetalInternalFrameTitlePane;
import javax.swing.plaf.metal.MetalInternalFrameUI;
import javax.swing.plaf.metal.MetalScrollBarUI;
import javax.swing.plaf.metal.MetalSliderUI;
import javax.swing.plaf.metal.MetalTabbedPaneUI;

/**
 * La barra de desplazamiento, el deslizador, las solapas, la ventana interna y el selector.
 *
 * <p>Los tamanos que se comparan son los fijos -- que en estas clases son casi todos --; los que
 * dependen de la fuente, no.
 */
public class Metal5 {

    /** Una carpeta del repositorio que no cambia. */
    static final String CARPETA =
            "C:\\Users\\nicol\\Sources\\Larena\\KajiJVM\\KajiJDK\\repo\\KajiLibrary\\javax\\swing\\undo";

    static void linea(String s) {
        System.out.println("//" + s);
    }

    static String d(Dimension x) {
        return (x == null) ? "-" : x.width + "x" + x.height;
    }

    static String c(Color x) {
        return (x == null) ? "-" : x.getRed() + "," + x.getGreen() + "," + x.getBlue();
    }

    static String n(Object o) {
        if (o == null) {
            return "-";
        }
        String s = o.getClass().getName();
        return s.substring(s.lastIndexOf('.') + 1);
    }

    static String ic(Icon i) {
        return (i == null) ? "-" : i.getIconWidth() + "x" + i.getIconHeight();
    }

    static class Barra extends MetalScrollBarUI {
        String estado() {
            return "ancho=" + scrollBarWidth + " suelta=" + isFreeStanding
                    + " menos=" + n(decreaseButton) + " mas=" + n(increaseButton);
        }

        Dimension pulgarMinimo() {
            return getMinimumThumbSize();
        }

        Object menos(int o) {
            return createDecreaseButton(o);
        }

        Object escucha() {
            return createPropertyChangeListener();
        }
    }

    static class Desliza extends MetalSliderUI {
        String estado() {
            return "relleno=" + filledSlider + " buffer=" + TICK_BUFFER
                    + " propiedad='" + SLIDER_FILL + "'"
                    + " anchoPista=" + trackWidth + " marca=" + tickLength;
        }

        String colores() {
            return "pulgar=" + c(thumbColor) + " brillo=" + c(highlightColor)
                    + " oscuro=" + c(darkShadowColor);
        }

        String iconos() {
            return "acostado=" + ic(horizThumbIcon) + " parado=" + ic(vertThumbIcon);
        }

        Dimension pulgar() {
            return getThumbSize();
        }

        int anchoPista() {
            return getTrackWidth();
        }

        int voladizo() {
            return getThumbOverhang();
        }

        int largoMarca() {
            return getTickLength();
        }

        Object escucha(JSlider s) {
            return createPropertyChangeListener(s);
        }
    }

    static class Solapas extends MetalTabbedPaneUI {
        String colores() {
            return "elegida=" + c(selectColor) + " brillo=" + c(selectHighlight)
                    + " fondo del area=" + c(tabAreaBackground)
                    + " ancho minimo=" + minTabWidth;
        }

        int base() {
            return getBaselineOffset();
        }

        int pisada(int r) {
            return getTabRunOverlay(r);
        }

        int corridaX(int p, int i, boolean s) {
            return getTabLabelShiftX(p, i, s);
        }

        int corridaY(int p, int i, boolean s) {
            return getTabLabelShiftY(p, i, s);
        }

        boolean rotar(int p, int f) {
            return shouldRotateTabRuns(p, f);
        }

        boolean rellenar(int p, int i, int x, int y) {
            return shouldFillGap(p, i, x, y);
        }

        boolean acolchar(int p, int f) {
            return shouldPadTabRun(p, f);
        }

        Object distribucion() {
            return createLayoutManager();
        }

        Color hueco(int p, int x, int y) {
            return getColorForGap(p, x, y);
        }
    }

    static class Marco extends MetalInternalFrameUI {
        Marco(JInternalFrame f) {
            super(f);
        }

        Object norte(JInternalFrame f) {
            return createNorthPane(f);
        }

        static String propiedad() {
            return IS_PALETTE;
        }
    }

    static class Titulo extends MetalInternalFrameTitlePane {
        Titulo(JInternalFrame f) {
            super(f);
        }

        int altoDePaleta() {
            return paletteTitleHeight;
        }

        boolean esPaleta() {
            return isPalette;
        }

        Icon cierre() {
            return paletteCloseIcon;
        }
    }

    static class Selector extends MetalFileChooserUI {
        Selector(JFileChooser c) {
            super(c);
        }

        Object modeloDeCarpetas(JFileChooser c) {
            return createDirectoryComboBoxModel(c);
        }

        Object modeloDeFiltros() {
            return createFilterComboBoxModel();
        }

        Object dibujanteDeFiltros() {
            return createFilterComboBoxRenderer();
        }

        Object acciones() {
            return getActionMap();
        }

        Object panelDeBotones() {
            return getButtonPanel();
        }

        Object panelDeAbajo() {
            return getBottomPanel();
        }
    }

    static void barra() {
        linea("--- la barra de desplazamiento ---");
        linea("propiedad=" + MetalScrollBarUI.FREE_STANDING_PROP);
        JScrollBar b = new JScrollBar();
        Barra u = new Barra();
        b.setUI(u);
        linea(u.estado());
        linea("pulgar minimo=" + d(u.pulgarMinimo())
                + " preferido parada=" + d(u.getPreferredSize(b)));
        linea("fondo=" + c(b.getBackground()) + " frente=" + c(b.getForeground()));
        JScrollBar h = new JScrollBar(JScrollBar.HORIZONTAL);
        Barra uh = new Barra();
        h.setUI(uh);
        linea("preferido acostada=" + d(uh.getPreferredSize(h)));
        linea("el boton de menos es=" + n(u.menos(SwingConstants.NORTH))
                + " y el escucha=" + n(u.escucha()));
        linea("cada llamada da un UI nuevo="
                + (MetalScrollBarUI.createUI(b) != MetalScrollBarUI.createUI(b)));

        // Una barra marcada como pegada le pasa la marca a sus dos botones.
        JScrollBar pegada = new JScrollBar();
        pegada.putClientProperty(MetalScrollBarUI.FREE_STANDING_PROP, Boolean.FALSE);
        Barra up = new Barra();
        pegada.setUI(up);
        linea("una barra marcada como pegada: " + up.estado());
    }

    static void deslizador() {
        linea("--- el deslizador ---");
        JSlider s = new JSlider();
        Desliza u = new Desliza();
        s.setUI(u);
        linea(u.estado());
        linea(u.colores());
        linea(u.iconos());
        linea("pulgar acostado=" + d(u.pulgar())
                + " ancho de pista=" + u.anchoPista()
                + " voladizo=" + u.voladizo()
                + " largo de marca=" + u.largoMarca());
        linea("el largo de marca no es el campo="
                + (u.largoMarca() != 6) + " y vale " + u.largoMarca());
        linea("escucha=" + n(u.escucha(s)));
        JSlider v = new JSlider(JSlider.VERTICAL);
        Desliza uv = new Desliza();
        v.setUI(uv);
        linea("pulgar parado=" + d(uv.pulgar()));
        linea("cada llamada da un UI nuevo="
                + (MetalSliderUI.createUI(s) != MetalSliderUI.createUI(s)));
    }

    static void solapas() {
        linea("--- las solapas ---");
        JTabbedPane t = new JTabbedPane();
        t.addTab("uno", new JLabel("a"));
        t.addTab("dos", new JLabel("b"));
        t.setSize(300, 200);
        Solapas u = new Solapas();
        t.setUI(u);
        linea(u.colores());
        linea("linea de base=" + u.base() + " distribucion=" + n(u.distribucion()));
        linea("pisada de fila=" + u.pisada(0));
        linea("corrida de la etiqueta: elegida=" + u.corridaX(SwingConstants.TOP, 0, true)
                + "," + u.corridaY(SwingConstants.TOP, 0, true)
                + " sin elegir=" + u.corridaX(SwingConstants.TOP, 0, false)
                + "," + u.corridaY(SwingConstants.TOP, 0, false));
        linea("rotar filas=" + u.rotar(SwingConstants.TOP, 2)
                + " acolchar=" + u.acolchar(SwingConstants.TOP, 1)
                + " rellenar el hueco=" + u.rellenar(SwingConstants.TOP, 0, 0, 2));
        linea("color del hueco=" + c(u.hueco(SwingConstants.TOP, 0, 0)));
        linea("cada llamada da un UI nuevo="
                + (MetalTabbedPaneUI.createUI(t) != MetalTabbedPaneUI.createUI(t)));
    }

    static void marco() {
        linea("--- la ventana interna ---");
        linea("propiedad=" + Marco.propiedad());
        JDesktopPane d = new JDesktopPane();
        JInternalFrame f = new JInternalFrame("titulo", true, true, true, true);
        d.add(f);
        Marco u = new Marco(f);
        f.setUI(u);
        linea("barra de titulo=" + n(u.getNorthPane())
                + " y una nueva tambien=" + n(u.norte(f)));
        linea("hijos del marco=" + f.getComponentCount() + " borde=" + n(f.getBorder()));
        u.setPalette(true);
        linea("como paleta: borde=" + n(f.getBorder()));
        u.setPalette(false);
        linea("y de vuelta: borde=" + n(f.getBorder()));
        linea("cada llamada da un UI nuevo="
                + (MetalInternalFrameUI.createUI(f) != MetalInternalFrameUI.createUI(f)));

        linea("--- la barra de titulo ---");
        JInternalFrame f2 = new JInternalFrame("t2", true, true, true, true);
        Titulo t = new Titulo(f2);
        linea("alto de paleta=" + t.altoDePaleta()
                + " es paleta=" + t.esPaleta()
                + " icono de cierre=" + ic(t.cierre()));
        int normales = t.getComponentCount();
        t.setPalette(true);
        linea("como paleta: es paleta=" + t.esPaleta()
                + " tiene menos hijos=" + (t.getComponentCount() < normales)
                + " y el alto preferido es el de paleta="
                + (t.getPreferredSize().height == t.altoDePaleta()));
    }

    static void selector() {
        linea("--- el selector de archivos ---");
        JFileChooser fc = new JFileChooser(new File(CARPETA));
        Selector u = new Selector(fc);
        u.installUI(fc);
        Dimension pref = u.getPreferredSize(fc);
        linea("preferido=" + d(pref));
        linea("el minimo es el preferido=" + u.getMinimumSize(fc).equals(pref));
        linea("maximo=" + d(u.getMaximumSize(fc)));
        linea("no es el mismo objeto cada vez="
                + (u.getPreferredSize(fc) != u.getPreferredSize(fc)));
        linea("modelo de carpetas=" + n(u.modeloDeCarpetas(fc))
                + " de filtros=" + n(u.modeloDeFiltros())
                + " dibujante=" + n(u.dibujanteDeFiltros()));
        linea("mapa de acciones=" + n(u.acciones())
                + " panel de botones=" + n(u.panelDeBotones())
                + " de abajo=" + n(u.panelDeAbajo()));
        linea("nombre de archivo='" + u.getFileName()
                + "' de carpeta=" + u.getDirectoryName());
        u.setFileName("x.txt");
        u.setDirectoryName("C:/tmp");
        linea("tras ponerlos: archivo='" + u.getFileName()
                + "' carpeta=" + u.getDirectoryName());
        linea("cada llamada da un UI nuevo="
                + (MetalFileChooserUI.createUI(fc) != MetalFileChooserUI.createUI(fc)));
    }

    public static int run() {
        barra();
        deslizador();
        solapas();
        marco();
        selector();
        return 0;
    }
}
