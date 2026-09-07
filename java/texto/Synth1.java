import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JRootPane;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.JToolTip;
import javax.swing.JTree;
import javax.swing.JViewport;
import javax.swing.plaf.synth.SynthButtonUI;
import javax.swing.plaf.synth.SynthCheckBoxUI;
import javax.swing.plaf.synth.SynthColorChooserUI;
import javax.swing.plaf.synth.SynthComboBoxUI;
import javax.swing.plaf.synth.SynthConstants;
import javax.swing.plaf.synth.SynthContext;
import javax.swing.plaf.synth.SynthDesktopPaneUI;
import javax.swing.plaf.synth.SynthInternalFrameUI;
import javax.swing.plaf.synth.SynthLabelUI;
import javax.swing.plaf.synth.SynthListUI;
import javax.swing.plaf.synth.SynthLookAndFeel;
import javax.swing.plaf.synth.SynthMenuBarUI;
import javax.swing.plaf.synth.SynthOptionPaneUI;
import javax.swing.plaf.synth.SynthPanelUI;
import javax.swing.plaf.synth.SynthPopupMenuUI;
import javax.swing.plaf.synth.SynthProgressBarUI;
import javax.swing.plaf.synth.SynthRadioButtonUI;
import javax.swing.plaf.synth.SynthRootPaneUI;
import javax.swing.plaf.synth.SynthScrollBarUI;
import javax.swing.plaf.synth.SynthScrollPaneUI;
import javax.swing.plaf.synth.SynthSeparatorUI;
import javax.swing.plaf.synth.SynthSliderUI;
import javax.swing.plaf.synth.SynthSpinnerUI;
import javax.swing.plaf.synth.SynthSplitPaneUI;
import javax.swing.plaf.synth.SynthTabbedPaneUI;
import javax.swing.plaf.synth.SynthTableHeaderUI;
import javax.swing.plaf.synth.SynthTableUI;
import javax.swing.plaf.synth.SynthTextAreaUI;
import javax.swing.plaf.synth.SynthTextFieldUI;
import javax.swing.plaf.synth.SynthToggleButtonUI;
import javax.swing.plaf.synth.SynthToolBarUI;
import javax.swing.plaf.synth.SynthToolTipUI;
import javax.swing.plaf.synth.SynthTreeUI;
import javax.swing.plaf.synth.SynthUI;
import javax.swing.plaf.synth.SynthViewportUI;

/**
 * El aspecto Synth, contra el JDK.
 *
 * <p>Lo que se compara es el contexto que cada una de las treinta y tres interfaces graficas
 * contesta -- su region, su estado y que el estilo sea nulo -- y que instalar cualquiera de ellas
 * sin haber cargado un archivo de estilos reviente igual que en el JDK.
 *
 * <p>Que reviente <strong>es</strong> el comportamiento correcto: Synth no tiene un aspecto por
 * omision, y ese es justamente el punto del paquete. Sin estilos no hay nada que dibujar.
 */
public class Synth1 {

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

    /** El contexto de un UI recien construido, sin instalar. */
    static void ctx(String rot, Object ui, JComponent c) {
        try {
            SynthContext x = ((SynthUI) ui).getContext(c);
            linea(rot + ": region=" + x.getRegion()
                    + " estado=" + x.getComponentState()
                    + " estilo=" + n(x.getStyle())
                    + " es el mismo componente=" + (x.getComponent() == c));
        } catch (Throwable e) {
            linea(rot + ": ROMPE " + e.getClass().getName());
        }
    }

    static void contextos() {
        linea("--- el contexto de cada region ---");
        ctx("etiqueta", new SynthLabelUI(), new JLabel("x"));
        ctx("panel", new SynthPanelUI(), new JPanel());
        ctx("separador", new SynthSeparatorUI(), new JSeparator());
        ctx("ventanilla", new SynthViewportUI(), new JViewport());
        ctx("globo", new SynthToolTipUI(), new JToolTip());
        ctx("panel raiz", new SynthRootPaneUI(), new JRootPane());
        ctx("escritorio", new SynthDesktopPaneUI(), new JDesktopPane());
        ctx("menu contextual", new SynthPopupMenuUI(), new JPopupMenu());
        ctx("boton", new SynthButtonUI(), new JButton("x"));
        ctx("conmutador", new SynthToggleButtonUI(), new JToggleButton("x"));
        ctx("opcion", new SynthRadioButtonUI(), new JRadioButton("x"));
        ctx("casilla", new SynthCheckBoxUI(), new JCheckBox("x"));
        ctx("barra de menu", new SynthMenuBarUI(), new JMenuBar());
        ctx("campo", new SynthTextFieldUI(), new JTextField());
        ctx("area", new SynthTextAreaUI(), new JTextArea());
        ctx("lista", new SynthListUI(), new JList<String>());
        ctx("tabla", new SynthTableUI(), new JTable());
        ctx("encabezado", new SynthTableHeaderUI(), new Encabezado());
        ctx("arbol", new SynthTreeUI(), new JTree());
        ctx("barra", new SynthScrollBarUI(), new JScrollBar());
        ctx("panel de desplazamiento", new SynthScrollPaneUI(), new JScrollPane());
        JSlider s = new JSlider();
        ctx("deslizador", SynthSliderUI.createUI(s), s);
        ctx("selector numerico", new SynthSpinnerUI(), new JSpinner());
        ctx("panel dividido", new SynthSplitPaneUI(), new JSplitPane());
        ctx("solapas", new SynthTabbedPaneUI(), new JTabbedPane());
        ctx("herramientas", new SynthToolBarUI(), new JToolBar());
        ctx("progreso", new SynthProgressBarUI(), new JProgressBar());
        ctx("dialogo", new SynthOptionPaneUI(), new JOptionPane());
        ctx("selector de color", new SynthColorChooserUI(), new JColorChooser());
        ctx("desplegable", new SynthComboBoxUI(), new JComboBox<String>());
        JInternalFrame f = new JInternalFrame();
        ctx("ventana interna", SynthInternalFrameUI.createUI(f), f);
    }

    /** El estado que Synth le ve a un componente. */
    static void estados() {
        linea("--- los estados ---");
        linea("ENABLED=" + SynthConstants.ENABLED
                + " DISABLED=" + SynthConstants.DISABLED
                + " PRESSED=" + SynthConstants.PRESSED
                + " MOUSE_OVER=" + SynthConstants.MOUSE_OVER
                + " SELECTED=" + SynthConstants.SELECTED
                + " FOCUSED=" + SynthConstants.FOCUSED
                + " DEFAULT=" + SynthConstants.DEFAULT);

        JLabel l = new JLabel("x");
        SynthLabelUI u = new SynthLabelUI();
        linea("una etiqueta encendida=" + u.getContext(l).getComponentState());
        l.setEnabled(false);
        linea("y apagada=" + u.getContext(l).getComponentState());

        // Un boton le suma lo que diga su modelo.
        JButton b = new JButton("x");
        SynthButtonUI ub = new SynthButtonUI();
        linea("un boton quieto=" + ub.getContext(b).getComponentState());
        b.getModel().setSelected(true);
        linea("elegido=" + ub.getContext(b).getComponentState());
        b.getModel().setRollover(true);
        linea("y con el cursor encima=" + ub.getContext(b).getComponentState());
        b.getModel().setSelected(false);
        b.getModel().setRollover(false);
        b.getModel().setArmed(true);
        b.getModel().setPressed(true);
        linea("apretado=" + ub.getContext(b).getComponentState());
        b.setEnabled(false);
        linea("y apagado=" + ub.getContext(b).getComponentState());
    }

    /** Sin estilos no hay aspecto; ver la nota de la clase. */
    static void sinEstilos() {
        linea("--- sin archivo de estilos ---");
        linea("hay fabrica de estilos=" + (SynthLookAndFeel.getStyleFactory() != null));
        JLabel l = new JLabel("x");
        try {
            l.setUI(new SynthLabelUI());
            linea("instalar una etiqueta: ok");
        } catch (Throwable e) {
            linea("instalar una etiqueta: " + e.getClass().getName());
        }
        JPanel p = new JPanel();
        try {
            p.setUI(new SynthPanelUI());
            linea("instalar un panel: ok");
        } catch (Throwable e) {
            linea("instalar un panel: " + e.getClass().getName());
        }
    }

    /** La regla del nombre; ver {@link SynthLookAndFeel#createUI}. */
    static void fabrica() {
        linea("--- la regla del nombre ---");
        linea("de un JLabel sale=" + n(SynthLookAndFeel.createUI(new JLabel())));
        linea("de un JPanel sale=" + n(SynthLookAndFeel.createUI(new JPanel())));
        linea("de un JTree sale=" + n(SynthLookAndFeel.createUI(new JTree())));
        linea("de un JScrollBar sale=" + n(SynthLookAndFeel.createUI(new JScrollBar())));
        linea("la region de un JLabel=" + SynthLookAndFeel.getRegion(new JLabel()));
        linea("la de un JToolBar=" + SynthLookAndFeel.getRegion(new JToolBar()));
        linea("cada llamada da uno nuevo="
                + (SynthLabelUI.createUI(new JLabel()) != SynthLabelUI.createUI(new JLabel())));
    }

    /** El encabezado hay que heredarlo: su constructor no esta a mano. */
    static class Encabezado extends javax.swing.table.JTableHeader {
    }

    public static int run() {
        contextos();
        estados();
        sinEstilos();
        fabrica();
        return 0;
    }
}
