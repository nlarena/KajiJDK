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
 * Metal's combo box.
 *
 * <p>The big difference from the basic one is in {@link #createArrowButton}: it does not return
 * a little arrow but a {@link MetalComboBoxButton}, which is <em>the whole combo box</em>. See
 * that class's note; from there it follows that Metal's layout is its own, because the button
 * takes up the whole width when the combo box is not editable and only the tip when it is.
 *
 * <p>{@link #layoutComboBox} is public and takes the layout as a parameter, which is an odd
 * signature and is so in the JDK: it exists so that a subclass can change where each piece goes
 * without writing a whole {@code LayoutManager}.
 */
public class MetalComboBoxUI extends BasicComboBoxUI {

    /**
     * The basic one's layout and listener, which the two classes below wrap.
     *
     * <p>They are here and not inside each inner class because this house's compiler does not yet
     * accept {@code MetalComboBoxUI.super.createLayoutManager()}; see finding #400.
     */
    private LayoutManager basicComboLayout;
    private PropertyChangeListener basicListener;

    public MetalComboBoxUI() {
    }

    private LayoutManager basicComboLayout() {
        if (basicComboLayout == null) {
            basicComboLayout = super.createLayoutManager();
        }
        return basicComboLayout;
    }

    private PropertyChangeListener basicListener() {
        if (basicListener == null) {
            basicListener = super.createPropertyChangeListener();
        }
        return basicListener;
    }

    public static ComponentUI createUI(JComponent c) {
        return new MetalComboBoxUI();
    }

    /**
     * The whole combo box, or the little arrow alone.
     *
     * <p>With Ocean it is <strong>always</strong> the little arrow, editable or not, and it is
     * measured: Ocean draws the combo box as a field with an arrow beside it, not as a single
     * button. With Steel it depends on whether it is editable. See
     * {@link MetalComboBoxButton}'s note.
     */
    protected JButton createArrowButton() {
        boolean iconOnly = MetalLookAndFeel.usingOcean()
                || ((comboBox != null) && comboBox.isEditable());
        MetalComboBoxButton b = new MetalComboBoxButton(comboBox, new MetalComboBoxIcon(),
                iconOnly, currentValuePane, listBox);
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

    /** On becoming editable, the button turns into the arrow alone. */
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

    /** The background of the current value's cell. */
    public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
        super.paintCurrentValueBackground(g, bounds, hasFocus);
    }

    public void paintCurrentValue(Graphics g, Rectangle bounds, boolean hasFocus) {
        super.paintCurrentValue(g, bounds, hasFocus);
    }

    /** See the class note about this signature. */
    public void layoutComboBox(Container parent, MetalComboBoxLayoutManager manager) {
        if (comboBox == null) {
            return;
        }
        if (!comboBox.isEditable() && arrowButton != null) {
            // The button is the whole combo box.
            Insets i = comboBox.getInsets();
            arrowButton.setBounds(i.left, i.top,
                    comboBox.getWidth() - i.left - i.right,
                    comboBox.getHeight() - i.top - i.bottom);
            return;
        }
        manager.superLayout(parent);
    }

    /**
     * Metal's layout, which delegates to {@link #layoutComboBox}.
     *
     * <p>It wraps the basic one instead of inheriting it: the only thing it needs from it is
     * {@code layoutContainer} for the editable case, and wrapping leaves the other three
     * {@code LayoutManager} methods passing straight through without a chain of inheritance in
     * between.
     */
    public class MetalComboBoxLayoutManager implements LayoutManager {

        public MetalComboBoxLayoutManager() {
        }

        public void layoutContainer(Container parent) {
            layoutComboBox(parent, this);
        }

        /** The basic one's, for when the combo box is editable. */
        public void superLayout(Container parent) {
            basicComboLayout().layoutContainer(parent);
        }

        public void addLayoutComponent(String name, java.awt.Component comp) {
        }

        public void removeLayoutComponent(java.awt.Component comp) {
        }

        public Dimension preferredLayoutSize(Container parent) {
            return basicComboLayout().preferredLayoutSize(parent);
        }

        public Dimension minimumLayoutSize(Container parent) {
            return basicComboLayout().minimumLayoutSize(parent);
        }
    }

    /** The one that hears that the combo box has become editable. */
    public class MetalPropertyChangeListener implements PropertyChangeListener {

        public MetalPropertyChangeListener() {
        }

        public void propertyChange(PropertyChangeEvent e) {
            basicListener().propertyChange(e);
            if ("editable".equals(e.getPropertyName())) {
                editablePropertyChanged(e);
            }
        }
    }
}
