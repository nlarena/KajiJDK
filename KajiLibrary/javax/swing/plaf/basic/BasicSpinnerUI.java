package javax.swing.plaf.basic;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;
import javax.swing.JSpinner;
import javax.swing.LookAndFeel;
import javax.swing.SwingConstants;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.SpinnerUI;
import javax.swing.plaf.UIResource;

/**
 * El aspecto basico de un {@link JSpinner}: el editor y las dos flechitas.
 *
 * <h2>Tres componentes y un acomodador propio</h2>
 *
 * <p>Un spinner es el editor a la izquierda y dos botones apilados a la derecha: el de arriba sube y
 * el de abajo baja. Ningun acomodador de los que vienen hechos hace eso -- {@code BorderLayout}
 * pondria los botones uno al lado del otro y {@code GridLayout} les daria la mitad del ancho --,
 * asi que {@link #createLayout} devuelve uno escrito para esto.
 *
 * <p>El reparto es simple y tiene una sola decision: los botones se llevan lo que pidan de ancho, y
 * el editor todo el resto. Al reves --el editor primero-- las flechitas quedarian de un pixel en un
 * spinner angosto y no se podrian apretar.
 *
 * <h2>El editor no lo hace el aspecto</h2>
 *
 * <p>{@link #createEditor} devuelve el que ya tiene el spinner, no uno nuevo. Es la unica manera
 * correcta: el editor depende del <em>modelo</em> --numeros, fechas, una lista-- y de eso no sabe
 * nada el aspecto. Lo que el aspecto decide son los botones.
 *
 * <h2>Sin tamano preferido</h2>
 *
 * <p>{@link #getPreferredSize} devuelve {@code null}: contesta el acomodador, que es el unico que
 * sabe cuanto miden el editor y los botones. Medido.
 *
 * <h2>La linea de base es la del editor</h2>
 *
 * <p>Y se mueve con el alto, porque el editor va centrado: {@code CENTER_OFFSET}.
 */
public class BasicSpinnerUI extends SpinnerUI {

    protected JSpinner spinner;
    private PropertyChangeListener propertyChangeListener;

    private static final ColorUIResource FONDO = new ColorUIResource(255, 255, 255);
    private static final ColorUIResource FRENTE = new ColorUIResource(51, 51, 51);
    private static final FontUIResource FUENTE = new FontUIResource("Dialog", Font.BOLD, 12);

    public BasicSpinnerUI() {
    }

    /** Uno nuevo por spinner: guarda el componente y sus escuchas. */
    public static ComponentUI createUI(JComponent c) {
        return new BasicSpinnerUI();
    }

    public void installUI(JComponent c) {
        this.spinner = (JSpinner) c;
        installDefaults();
        installListeners();
        maybeAdd(createNextButton(), "Next");
        maybeAdd(createPreviousButton(), "Previous");
        maybeAdd(createEditor(), "Editor");
        updateEnabledState();
        installKeyboardActions();
    }

    public void uninstallUI(JComponent c) {
        uninstallDefaults();
        uninstallListeners();
        this.spinner = null;
        c.removeAll();
    }

    private void maybeAdd(Component c, String name) {
        if (c != null) {
            spinner.add(c, name);
        }
    }

    /** Colores, fuente, acomodador y opacidad. */
    protected void installDefaults() {
        spinner.setLayout(createLayout());
        java.awt.Color fondo = spinner.getBackground();
        if (fondo == null || fondo instanceof UIResource) {
            spinner.setBackground(FONDO);
        }
        java.awt.Color frente = spinner.getForeground();
        if (frente == null || frente instanceof UIResource) {
            spinner.setForeground(FRENTE);
        }
        Font fuente = spinner.getFont();
        if (fuente == null || fuente instanceof UIResource) {
            spinner.setFont(FUENTE);
        }
        LookAndFeel.installProperty(spinner, "opaque", Boolean.TRUE);
    }

    /** Saca el acomodador que puso este UI. */
    protected void uninstallDefaults() {
        spinner.setLayout(null);
    }

    protected void installListeners() {
        propertyChangeListener = createPropertyChangeListener();
        spinner.addPropertyChangeListener(propertyChangeListener);
    }

    protected void uninstallListeners() {
        spinner.removePropertyChangeListener(propertyChangeListener);
        propertyChangeListener = null;
    }

    /** Sin atajos propios: las flechas del teclado las atan los botones. */
    protected void installKeyboardActions() {
    }

    protected PropertyChangeListener createPropertyChangeListener() {
        return new Handler();
    }

    /** El acomodador propio; ver la nota de la clase. */
    protected LayoutManager createLayout() {
        return new Handler();
    }

    /** El editor que ya tiene el spinner; ver la nota de la clase. */
    protected JComponent createEditor() {
        return spinner.getEditor();
    }

    /** El boton de abajo. */
    protected Component createPreviousButton() {
        Component c = new BasicArrowButton(SwingConstants.SOUTH);
        installPreviousButtonListeners(c);
        return c;
    }

    /** El de arriba. */
    protected Component createNextButton() {
        Component c = new BasicArrowButton(SwingConstants.NORTH);
        installNextButtonListeners(c);
        return c;
    }

    /** Ata el boton al valor anterior del modelo. */
    protected void installPreviousButtonListeners(Component c) {
        instalarFlecha(c, false);
    }

    /** Ata el boton al valor siguiente. */
    protected void installNextButtonListeners(Component c) {
        instalarFlecha(c, true);
    }

    private void instalarFlecha(Component c, final boolean siguiente) {
        if (!(c instanceof javax.swing.AbstractButton)) {
            return;
        }
        ((javax.swing.AbstractButton) c).addActionListener(
                new java.awt.event.ActionListener() {
                    public void actionPerformed(java.awt.event.ActionEvent e) {
                        if (spinner == null || !spinner.isEnabled()) {
                            return;
                        }
                        Object valor = siguiente ? spinner.getNextValue()
                                : spinner.getPreviousValue();
                        if (valor != null) {
                            spinner.setValue(valor);
                        }
                    }
                });
    }

    /**
     * Cambia el editor por otro.
     *
     * <p>Lo llama el spinner cuando el programa le pone un editor nuevo. Saca el viejo del
     * contenedor y pone el nuevo en su lugar; el orden importa, porque el acomodador identifica a
     * los tres componentes por lo que ocupan y no por un nombre.
     */
    protected void replaceEditor(JComponent oldEditor, JComponent newEditor) {
        spinner.remove(oldEditor);
        spinner.add(newEditor, "Editor");
    }

    private void updateEnabledState() {
        boolean prendido = spinner.isEnabled();
        for (int i = 0; i < spinner.getComponentCount(); i++) {
            Component c = spinner.getComponent(i);
            if (c instanceof BasicArrowButton) {
                c.setEnabled(prendido);
            }
        }
    }

    /** {@code null}; ver la nota de la clase. */
    public Dimension getPreferredSize(JComponent c) {
        return null;
    }

    /**
     * La del editor.
     *
     * @throws NullPointerException si el componente es nulo
     * @throws IllegalArgumentException si el ancho o el alto son negativos
     */
    public int getBaseline(JComponent c, int width, int height) {
        super.getBaseline(c, width, height);
        JComponent editor = spinner.getEditor();
        if (editor == null) {
            return -1;
        }
        Insets insets = spinner.getInsets();
        width = width - insets.left - insets.right;
        height = height - insets.top - insets.bottom;
        if (width < 0 || height < 0) {
            return -1;
        }
        int baseline = editor.getBaseline(width, height);
        return (baseline < 0) ? -1 : baseline + insets.top;
    }

    /**
     * {@code CENTER_OFFSET}: el editor va centrado.
     *
     * @throws NullPointerException si el componente es nulo
     */
    public Component.BaselineResizeBehavior getBaselineResizeBehavior(JComponent c) {
        super.getBaselineResizeBehavior(c);
        return Component.BaselineResizeBehavior.CENTER_OFFSET;
    }

    /**
     * El acomodador y el escucha de propiedades, en un objeto.
     *
     * <p>El nombre es el del JDK aunque la clase sea privada, porque se ve por {@code getClass()}:
     * ver la nota de {@code JTable} sobre lo mismo.
     */
    private class Handler implements LayoutManager, PropertyChangeListener {

        private Component nextButton;
        private Component previousButton;
        private Component editor;

        public void addLayoutComponent(String name, Component c) {
            if ("Next".equals(name)) {
                nextButton = c;
            } else if ("Previous".equals(name)) {
                previousButton = c;
            } else if ("Editor".equals(name)) {
                editor = c;
            }
        }

        public void removeLayoutComponent(Component c) {
            if (c == nextButton) {
                nextButton = null;
            } else if (c == previousButton) {
                previousButton = null;
            } else if (c == editor) {
                editor = null;
            }
        }

        private Dimension preferida(Component c) {
            return (c == null) ? new Dimension(0, 0) : c.getPreferredSize();
        }

        public Dimension preferredLayoutSize(Container parent) {
            Dimension nextD = preferida(nextButton);
            Dimension previousD = preferida(previousButton);
            Dimension editorD = preferida(editor);
            // El editor y los botones comparten el alto: se lleva el mas alto de los dos lados.
            editorD.height = ((editorD.height + 1) / 2) * 2;
            Dimension size = new Dimension(editorD.width, editorD.height);
            size.width += Math.max(nextD.width, previousD.width);
            Insets insets = parent.getInsets();
            size.width += insets.left + insets.right;
            size.height += insets.top + insets.bottom;
            return size;
        }

        public Dimension minimumLayoutSize(Container parent) {
            return preferredLayoutSize(parent);
        }

        public void layoutContainer(Container parent) {
            Insets insets = parent.getInsets();
            int availWidth = parent.getWidth() - (insets.left + insets.right);
            int availHeight = parent.getHeight() - (insets.top + insets.bottom);
            Dimension nextD = preferida(nextButton);
            Dimension previousD = preferida(previousButton);
            int nextWidth = Math.max(nextD.width, previousD.width);
            int editorWidth = availWidth - nextWidth;
            int editorX = insets.left;
            int buttonsX = editorX + editorWidth;
            int nextY = insets.top;
            int nextHeight = (availHeight + 1) / 2;
            int previousY = insets.top + nextHeight;
            int previousHeight = availHeight - nextHeight;
            if (editor != null) {
                editor.setBounds(editorX, insets.top, editorWidth, availHeight);
            }
            if (nextButton != null) {
                nextButton.setBounds(buttonsX, nextY, nextWidth, nextHeight);
            }
            if (previousButton != null) {
                previousButton.setBounds(buttonsX, previousY, nextWidth, previousHeight);
            }
        }

        public void propertyChange(PropertyChangeEvent e) {
            String propertyName = e.getPropertyName();
            if ("editor".equals(propertyName)) {
                JComponent oldEditor = (JComponent) e.getOldValue();
                JComponent newEditor = (JComponent) e.getNewValue();
                replaceEditor(oldEditor, newEditor);
                updateEnabledState();
            } else if ("enabled".equals(propertyName)) {
                updateEnabledState();
            }
        }
    }
}
