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
 * The basic look and feel of a {@link JSpinner}: the editor and the two little arrows.
 *
 * <h2>Three components and a layout of its own</h2>
 *
 * <p>A spinner is the editor on the left and two buttons stacked on the right: the top one goes
 * up and the bottom one goes down. None of the ready-made layouts does that --
 * {@code BorderLayout} would put the buttons side by side and {@code GridLayout} would give
 * them half the width --, so {@link #createLayout} returns one written for this.
 *
 * <p>The sharing out is simple and has a single decision: the buttons take whatever width they
 * ask for, and the editor all the rest. The other way round -- the editor first -- the little
 * arrows would be one pixel wide in a narrow spinner and could not be pressed.
 *
 * <h2>The editor is not made by the look and feel</h2>
 *
 * <p>{@link #createEditor} returns the one the spinner already has, not a new one. It is the
 * only correct way: the editor depends on the <em>model</em> -- numbers, dates, a list -- and
 * the look and feel knows nothing about that. What the look and feel decides are the buttons.
 *
 * <h2>No preferred size</h2>
 *
 * <p>{@link #getPreferredSize} returns {@code null}: the layout answers, which is the only one
 * that knows how much the editor and the buttons measure. Measured.
 *
 * <h2>The baseline is the editor's</h2>
 *
 * <p>And it moves with the height, because the editor goes centred: {@code CENTER_OFFSET}.
 */
public class BasicSpinnerUI extends SpinnerUI {

    protected JSpinner spinner;
    private PropertyChangeListener propertyChangeListener;

    private static final ColorUIResource BACKGROUND = new ColorUIResource(255, 255, 255);
    private static final ColorUIResource FOREGROUND = new ColorUIResource(51, 51, 51);
    private static final FontUIResource FONT = new FontUIResource("Dialog", Font.BOLD, 12);

    public BasicSpinnerUI() {
    }

    /** A new one per spinner: it keeps the component and its listeners. */
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

    /** Colours, typeface, layout and opacity. */
    protected void installDefaults() {
        spinner.setLayout(createLayout());
        java.awt.Color background = spinner.getBackground();
        if (background == null || background instanceof UIResource) {
            spinner.setBackground(BACKGROUND);
        }
        java.awt.Color foreground = spinner.getForeground();
        if (foreground == null || foreground instanceof UIResource) {
            spinner.setForeground(FOREGROUND);
        }
        Font font = spinner.getFont();
        if (font == null || font instanceof UIResource) {
            spinner.setFont(FONT);
        }
        LookAndFeel.installProperty(spinner, "opaque", Boolean.TRUE);
    }

    /** It removes the layout this look and feel set. */
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

    /** With no shortcuts of its own: the keyboard arrows are tied by the buttons. */
    protected void installKeyboardActions() {
    }

    protected PropertyChangeListener createPropertyChangeListener() {
        return new Handler();
    }

    /** The layout of its own; see the class note. */
    protected LayoutManager createLayout() {
        return new Handler();
    }

    /** The editor the spinner already has; see the class note. */
    protected JComponent createEditor() {
        return spinner.getEditor();
    }

    /** The bottom button. */
    protected Component createPreviousButton() {
        Component c = new BasicArrowButton(SwingConstants.SOUTH);
        installPreviousButtonListeners(c);
        return c;
    }

    /** The top one. */
    protected Component createNextButton() {
        Component c = new BasicArrowButton(SwingConstants.NORTH);
        installNextButtonListeners(c);
        return c;
    }

    /** It ties the button to the model's previous value. */
    protected void installPreviousButtonListeners(Component c) {
        installArrow(c, false);
    }

    /** It ties the button to the next value. */
    protected void installNextButtonListeners(Component c) {
        installArrow(c, true);
    }

    private void installArrow(Component c, final boolean next) {
        if (!(c instanceof javax.swing.AbstractButton)) {
            return;
        }
        ((javax.swing.AbstractButton) c).addActionListener(
                new java.awt.event.ActionListener() {
                    public void actionPerformed(java.awt.event.ActionEvent e) {
                        if (spinner == null || !spinner.isEnabled()) {
                            return;
                        }
                        Object value = next ? spinner.getNextValue()
                                : spinner.getPreviousValue();
                        if (value != null) {
                            spinner.setValue(value);
                        }
                    }
                });
    }

    /**
     * It swaps the editor for another one.
     *
     * <p>The spinner calls it when the program gives it a new editor. It takes the old one out of
     * the container and puts the new one in its place; the order matters, because the layout
     * identifies the three components by what they take up and not by a name.
     */
    protected void replaceEditor(JComponent oldEditor, JComponent newEditor) {
        spinner.remove(oldEditor);
        spinner.add(newEditor, "Editor");
    }

    private void updateEnabledState() {
        boolean on = spinner.isEnabled();
        for (int i = 0; i < spinner.getComponentCount(); i++) {
            Component c = spinner.getComponent(i);
            if (c instanceof BasicArrowButton) {
                c.setEnabled(on);
            }
        }
    }

    /** {@code null}; see the class note. */
    public Dimension getPreferredSize(JComponent c) {
        return null;
    }

    /**
     * The editor's.
     *
     * @throws NullPointerException if the component is null
     * @throws IllegalArgumentException if the width or the height are negative
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
     * {@code CENTER_OFFSET}: the editor goes centred.
     *
     * @throws NullPointerException if the component is null
     */
    public Component.BaselineResizeBehavior getBaselineResizeBehavior(JComponent c) {
        super.getBaselineResizeBehavior(c);
        return Component.BaselineResizeBehavior.CENTER_OFFSET;
    }

    /**
     * The layout and the property listener, in one object.
     *
     * <p>The name is the JDK's even though the class is private, because it shows through
     * {@code getClass()}: see {@code JTable}'s note on the same thing.
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

        private Dimension preferred(Component c) {
            return (c == null) ? new Dimension(0, 0) : c.getPreferredSize();
        }

        public Dimension preferredLayoutSize(Container parent) {
            Dimension nextD = preferred(nextButton);
            Dimension previousD = preferred(previousButton);
            Dimension editorD = preferred(editor);
            // The editor and the buttons share the height: the taller of the two sides wins.
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
            Dimension nextD = preferred(nextButton);
            Dimension previousD = preferred(previousButton);
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
