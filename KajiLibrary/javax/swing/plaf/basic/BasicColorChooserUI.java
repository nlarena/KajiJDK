package javax.swing.plaf.basic;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.LookAndFeel;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.colorchooser.ColorChooserComponentFactory;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.ColorChooserUI;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.UIResource;

/**
 * El aspecto basico de un selector de color.
 *
 * <h2>Dos mitades: las solapas y la muestra</h2>
 *
 * <p>Arriba, una solapa por cada forma de elegir un color -- muestrario, HSV, RGB --; abajo, la
 * muestra de como quedo. El selector no elige nada por su cuenta: cada panel de solapa escribe en el
 * mismo {@code ColorSelectionModel}, y la muestra lo escucha. Este UI solo los conecta.
 *
 * <h2>Sin paneles de solapa</h2>
 *
 * <p><strong>{@link #createDefaultChoosers} devuelve un arreglo vacio.</strong> Los cinco paneles
 * del JDK --muestrario, HSV, HSL, RGB, CMYK-- son componentes interactivos de verdad: deslizadores,
 * campos con formato y un diagrama de color que se pinta y se arrastra. Esta biblioteca no los trae
 * y {@code ColorChooserComponentFactory.getDefaultChooserPanels} lo dice tirando
 * {@code UnsupportedOperationException}.
 *
 * <p>Devolver ninguno es un subconjunto legal --un selector sin formas de elegir--; dejar que la
 * excepcion salga por {@link #installUI} haria que ni siquiera se pueda construir el componente, y
 * eso es peor. Lo que si esta es todo el andamiaje: la muestra, los escuchas, y el reemplazo de
 * paneles cuando el programa pone los suyos, que es el caso en el que un selector es util aca.
 */
public class BasicColorChooserUI extends ColorChooserUI {

    protected JColorChooser chooser;

    /** Los paneles que puso este UI; ver la nota de la clase. */
    protected AbstractColorChooserPanel[] defaultChoosers;

    /** El que redibuja la muestra cuando cambia el color elegido. */
    protected ChangeListener previewListener;

    protected PropertyChangeListener propertyChangeListener;

    private JTabbedPane tabbedPane;
    private JPanel singlePanel;
    private JPanel previewPanelHolder;
    private Component previewPanel;

    private static final ColorUIResource FONDO = new ColorUIResource(238, 238, 238);
    private static final ColorUIResource FRENTE = new ColorUIResource(51, 51, 51);
    private static final FontUIResource FUENTE = new FontUIResource("Dialog", Font.PLAIN, 12);

    public BasicColorChooserUI() {
    }

    /** Uno nuevo por selector: guarda el componente y los paneles que armo. */
    public static ComponentUI createUI(JComponent c) {
        return new BasicColorChooserUI();
    }

    public void installUI(JComponent c) {
        chooser = (JColorChooser) c;
        chooser.setLayout(new BorderLayout());
        defaultChoosers = createDefaultChoosers();
        chooser.setChooserPanels(defaultChoosers);
        installDefaults();
        installListeners();
        installPreviewPanel();
        chooser.applyComponentOrientation(c.getComponentOrientation());
    }

    public void uninstallUI(JComponent c) {
        chooser.remove(previewPanelHolder);
        uninstallListeners();
        uninstallDefaultChoosers();
        uninstallDefaults();
        chooser.setLayout(null);
        previewPanelHolder = null;
        previewPanel = null;
        defaultChoosers = null;
        chooser = null;
        tabbedPane = null;
    }

    /** Ver la nota de la clase: ninguno. */
    protected AbstractColorChooserPanel[] createDefaultChoosers() {
        return new AbstractColorChooserPanel[0];
    }

    /** Desconecta los paneles que este UI puso. */
    protected void uninstallDefaultChoosers() {
        if (defaultChoosers == null) {
            return;
        }
        for (int i = 0; i < defaultChoosers.length; i++) {
            chooser.removeChooserPanel(defaultChoosers[i]);
        }
    }

    /** Colores, fuente y opacidad; los valores son los de {@code ColorChooser.*} en Metal. */
    protected void installDefaults() {
        Color fondo = chooser.getBackground();
        if (fondo == null || fondo instanceof UIResource) {
            chooser.setBackground(FONDO);
        }
        Color frente = chooser.getForeground();
        if (frente == null || frente instanceof UIResource) {
            chooser.setForeground(FRENTE);
        }
        Font fuente = chooser.getFont();
        if (fuente == null || fuente instanceof UIResource) {
            chooser.setFont(FUENTE);
        }
        LookAndFeel.installProperty(chooser, "opaque", Boolean.TRUE);
    }

    /** No saca nada; ver {@link BasicPanelUI#uninstallDefaults}. */
    protected void uninstallDefaults() {
    }

    protected void installListeners() {
        propertyChangeListener = createPropertyChangeListener();
        chooser.addPropertyChangeListener(propertyChangeListener);
        previewListener = new Handler(this);
        chooser.getSelectionModel().addChangeListener(previewListener);
    }

    protected void uninstallListeners() {
        chooser.removePropertyChangeListener(propertyChangeListener);
        chooser.getSelectionModel().removeChangeListener(previewListener);
        propertyChangeListener = null;
        previewListener = null;
    }

    protected PropertyChangeListener createPropertyChangeListener() {
        return new Handler(this);
    }

    /**
     * Pone la muestra abajo de todo.
     *
     * <p>Si el programa no puso una propia, la que arma la fabrica. Y si la fabrica no puede --lo
     * mismo que con los paneles de solapa--, ninguna: la mitad de abajo queda vacia y el selector
     * sigue andando.
     */
    protected void installPreviewPanel() {
        if (previewPanelHolder == null) {
            previewPanelHolder = new JPanel(new BorderLayout());
            previewPanelHolder.setName("ColorChooser.previewPanelHolder");
        }
        previewPanelHolder.removeAll();
        Component previa = chooser.getPreviewPanel();
        if (previa == null) {
            try {
                previa = ColorChooserComponentFactory.getPreviewPanel();
            } catch (UnsupportedOperationException e) {
                // Ver la nota del metodo: sin muestra, el selector igual funciona.
                previa = null;
            }
        }
        previewPanel = previa;
        if (previewPanel != null) {
            previewPanelHolder.add(previewPanel, BorderLayout.CENTER);
        }
        chooser.add(previewPanelHolder, BorderLayout.SOUTH);
    }

    protected void uninstallPreviewPanel() {
        if (previewPanelHolder != null) {
            previewPanelHolder.removeAll();
        }
        previewPanel = null;
    }

    /**
     * Rearma la parte de arriba segun cuantos paneles haya.
     *
     * <p>Con uno solo va suelto; con dos o mas, cada uno en su solapa. Poner una sola solapa se
     * veria como un marco de mas alrededor de nada.
     */
    private void rearmarPaneles() {
        AbstractColorChooserPanel[] panels = chooser.getChooserPanels();
        if (tabbedPane != null) {
            chooser.remove(tabbedPane);
            tabbedPane = null;
        }
        if (singlePanel != null) {
            chooser.remove(singlePanel);
            singlePanel = null;
        }
        if (panels == null || panels.length == 0) {
            return;
        }
        if (panels.length == 1) {
            singlePanel = new JPanel(new BorderLayout());
            singlePanel.add(panels[0], BorderLayout.CENTER);
            chooser.add(singlePanel, BorderLayout.CENTER);
        } else {
            tabbedPane = new JTabbedPane();
            tabbedPane.setName("ColorChooser.tabPane");
            for (int i = 0; i < panels.length; i++) {
                tabbedPane.addTab(panels[i].getDisplayName(), panels[i]);
            }
            chooser.add(tabbedPane, BorderLayout.CENTER);
        }
    }

    /**
     * El que reacciona al color elegido y a los cambios del selector.
     *
     * <p>Estatico y con el UI como campo, por lo mismo que en todo el paquete; ver el hallazgo #518.
     */
    private static class Handler implements PropertyChangeListener, ChangeListener {

        private final BasicColorChooserUI ui;

        Handler(BasicColorChooserUI ui) {
            this.ui = ui;
        }

        public void stateChanged(ChangeEvent e) {
            if (ui.previewPanel != null) {
                ui.previewPanel.repaint();
            }
        }

        public void propertyChange(PropertyChangeEvent e) {
            String nombre = e.getPropertyName();
            if (JColorChooser.CHOOSER_PANELS_PROPERTY.equals(nombre)) {
                ui.rearmarPaneles();
                ui.chooser.revalidate();
            } else if (JColorChooser.PREVIEW_PANEL_PROPERTY.equals(nombre)) {
                ui.installPreviewPanel();
                ui.chooser.revalidate();
            } else if (JColorChooser.SELECTION_MODEL_PROPERTY.equals(nombre)) {
                Object viejo = e.getOldValue();
                Object nuevo = e.getNewValue();
                if (viejo instanceof javax.swing.colorchooser.ColorSelectionModel) {
                    ((javax.swing.colorchooser.ColorSelectionModel) viejo)
                            .removeChangeListener(ui.previewListener);
                }
                if (nuevo instanceof javax.swing.colorchooser.ColorSelectionModel) {
                    ((javax.swing.colorchooser.ColorSelectionModel) nuevo)
                            .addChangeListener(ui.previewListener);
                }
            }
        }
    }
}
