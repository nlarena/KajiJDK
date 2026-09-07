package javax.swing.plaf.metal;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.ComboBoxEditor;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.ComboPopup;

/**
 * El desplegable de Metal.
 *
 * <p>La diferencia grande con el basico esta en {@link #createArrowButton}: no devuelve una
 * flechita sino un {@link MetalComboBoxButton}, que es <em>todo el desplegable</em>. Ver la nota
 * de esa clase; de ahi sale que la distribucion de Metal sea propia, porque el boton ocupa el
 * ancho entero cuando el desplegable no es editable y solo la punta cuando si lo es.
 *
 * <p>{@link #layoutComboBox} es publico y toma la distribucion como parametro, que es una firma
 * rara y esta asi en el JDK: existe para que una subclase pueda cambiar donde va cada pieza sin
 * escribir un {@code LayoutManager} entero.
 */
public class MetalComboBoxUI extends BasicComboBoxUI {

    /**
     * La distribucion y el escucha del basico, que las dos clases de abajo envuelven.
     *
     * <p>Estan aca y no adentro de cada clase interna porque el compilador de esta casa todavia no
     * acepta {@code MetalComboBoxUI.super.createLayoutManager()}; ver el hallazgo #400.
     */
    private LayoutManager layoutDelBasico;
    private PropertyChangeListener escuchaDelBasico;

    public MetalComboBoxUI() {
    }

    private LayoutManager layoutDelBasico() {
        if (layoutDelBasico == null) {
            layoutDelBasico = super.createLayoutManager();
        }
        return layoutDelBasico;
    }

    private PropertyChangeListener escuchaDelBasico() {
        if (escuchaDelBasico == null) {
            escuchaDelBasico = super.createPropertyChangeListener();
        }
        return escuchaDelBasico;
    }

    public static ComponentUI createUI(JComponent c) {
        return new MetalComboBoxUI();
    }

    /**
     * El desplegable entero, o la flechita sola.
     *
     * <p>Con Ocean es <strong>siempre</strong> la flechita, sea o no editable, y esta medido: Ocean
     * dibuja el desplegable como un campo con una flecha al lado, no como un boton unico. Con Steel
     * depende de si es editable. Ver la nota de {@link MetalComboBoxButton}.
     */
    protected JButton createArrowButton() {
        boolean soloIcono = MetalLookAndFeel.usandoOcean()
                || ((comboBox != null) && comboBox.isEditable());
        MetalComboBoxButton b = new MetalComboBoxButton(comboBox, new MetalComboBoxIcon(),
                soloIcono, currentValuePane, listBox);
        b.setMargin(new Insets(0, 1, 1, 3));
        return b;
    }

    protected ComboBoxEditor createEditor() {
        return new MetalComboBoxEditor.UIResource();
    }

    protected ComboPopup createPopup() {
        return super.createPopup();
    }

    protected LayoutManager createLayoutManager() {
        return new MetalComboBoxLayoutManager();
    }

    public PropertyChangeListener createPropertyChangeListener() {
        return new MetalPropertyChangeListener();
    }

    /** Al volverse editable, el boton pasa a ser solo la flecha. */
    protected void editablePropertyChanged(PropertyChangeEvent e) {
        if (arrowButton instanceof MetalComboBoxButton) {
            MetalComboBoxButton b = (MetalComboBoxButton) arrowButton;
            b.setIconOnly(comboBox.isEditable());
            comboBox.repaint();
        }
    }

    public void configureEditor() {
        super.configureEditor();
    }

    public void unconfigureEditor() {
        super.unconfigureEditor();
    }

    protected void removeListeners() {
    }

    public Dimension getMinimumSize(JComponent c) {
        return super.getMinimumSize(c);
    }

    public int getBaseline(JComponent c, int width, int height) {
        return super.getBaseline(c, width, height);
    }

    public void paint(Graphics g, JComponent c) {
        super.paint(g, c);
    }

    /** El fondo de la celda del valor actual. */
    public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
        super.paintCurrentValueBackground(g, bounds, hasFocus);
    }

    public void paintCurrentValue(Graphics g, Rectangle bounds, boolean hasFocus) {
        super.paintCurrentValue(g, bounds, hasFocus);
    }

    /** Ver la nota de la clase sobre esta firma. */
    public void layoutComboBox(Container parent, MetalComboBoxLayoutManager manager) {
        if (comboBox == null) {
            return;
        }
        if (!comboBox.isEditable() && arrowButton != null) {
            // El boton es todo el desplegable.
            Insets i = comboBox.getInsets();
            arrowButton.setBounds(i.left, i.top,
                    comboBox.getWidth() - i.left - i.right,
                    comboBox.getHeight() - i.top - i.bottom);
            return;
        }
        manager.superLayout(parent);
    }

    /**
     * La distribucion de Metal, que delega en {@link #layoutComboBox}.
     *
     * <p>Envuelve a la del basico en vez de heredarla: lo unico que necesita de ella es
     * {@code layoutContainer} para el caso editable, y envolver deja los otros tres metodos de
     * {@code LayoutManager} pasando derecho sin una cadena de herencia de por medio.
     */
    public class MetalComboBoxLayoutManager implements LayoutManager {

        public MetalComboBoxLayoutManager() {
        }

        public void layoutContainer(Container parent) {
            layoutComboBox(parent, this);
        }

        /** La del basico, para cuando el desplegable si es editable. */
        public void superLayout(Container parent) {
            layoutDelBasico().layoutContainer(parent);
        }

        public void addLayoutComponent(String name, java.awt.Component comp) {
        }

        public void removeLayoutComponent(java.awt.Component comp) {
        }

        public Dimension preferredLayoutSize(Container parent) {
            return layoutDelBasico().preferredLayoutSize(parent);
        }

        public Dimension minimumLayoutSize(Container parent) {
            return layoutDelBasico().minimumLayoutSize(parent);
        }
    }

    /** El que se entera de que el desplegable paso a ser editable. */
    public class MetalPropertyChangeListener implements PropertyChangeListener {

        public MetalPropertyChangeListener() {
        }

        public void propertyChange(PropertyChangeEvent e) {
            escuchaDelBasico().propertyChange(e);
            if ("editable".equals(e.getPropertyName())) {
                editablePropertyChanged(e);
            }
        }
    }
}
