package javax.swing;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.colorchooser.ColorSelectionModel;
import javax.swing.colorchooser.DefaultColorSelectionModel;

/**
 * The colour chooser: a pane with several ways of choosing a colour and a single answer.
 *
 * <p>The piece that orders it all is the {@link ColorSelectionModel}: the tabs -- RGB, HSV,
 * CMYK, the swatches -- are different {@link AbstractColorChooserPanel}s that **share the same
 * model**. That is why moving a slider in RGB updates what is seen in HSV without either of the
 * two knowing about the other: both listen to the model.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>The part that is state is there: the constructors, the model, the colour, the panels and
 * the preview panel. What is missing is what needs windows or a `LookAndFeel`: `showDialog` and
 * `createDialog` -- which open a modal dialog -- and `getUI`/`setUI`/`updateUI`.
 *
 * <p>Nor are the property change events fired when the model or the panels are replaced: this
 * library's `JComponent` does not have bound property support yet, and manufacturing it here
 * just for this class would be worse.
 */
public class JColorChooser extends JComponent {


    /** The name of the selection model's bound property. */
    public static final String SELECTION_MODEL_PROPERTY = "selectionModel";

    /** The name of the preview panel's bound property. */
    public static final String PREVIEW_PANEL_PROPERTY = "previewPanel";

    /** The name of the panel array's bound property. */
    public static final String CHOOSER_PANELS_PROPERTY = "chooserPanels";

    private ColorSelectionModel selectionModel;
    private JComponent previewPanel;
    private AbstractColorChooserPanel[] chooserPanels = new AbstractColorChooserPanel[0];
    private boolean dragEnabled;

    /** A chooser with white chosen. */
    public JColorChooser() {
        this(Color.white);
    }

    /**
     * A chooser with that colour chosen.
     *
     * @param initialColor the initial colour
     */
    public JColorChooser(Color initialColor) {
        this(new DefaultColorSelectionModel(initialColor));
    }

    /**
     * A chooser over that model.
     *
     * @param model the model the panels share
     */
    public JColorChooser(ColorSelectionModel model) {
        super();
        this.selectionModel = model;
    }

    /** The key the `LookAndFeel` looks the look and feel up with: {@code "ColorChooserUI"}. */
    public String getUIClassID() {
        return "ColorChooserUI";
    }

    /** The chosen colour. */
    public Color getColor() {
        return this.selectionModel.getSelectedColor();
    }

    /**
     * It chooses that colour.
     *
     * @throws NullPointerException if it is null
     */
    public void setColor(Color color) {
        this.selectionModel.setSelectedColor(color);
    }

    /**
     * It chooses the colour with those three components.
     *
     * @throws IllegalArgumentException if one of them goes outside 0..255
     */
    public void setColor(int r, int g, int b) {
        setColor(new Color(r, g, b));
    }

    /**
     * It chooses the colour packed in an integer, in the format 0xRRGGBB.
     *
     * <p>The top eight bits are ignored: the chooser does not choose transparency this way.
     */
    public void setColor(int c) {
        setColor(new Color(c & 0xFFFFFF));
    }

    /**
     * It switches the dragging of the colour out of the chooser on or off.
     *
     * <p>It is kept, but there is no dragging: that is handled by the installed look and feel,
     * which there is none of here.
     */
    public void setDragEnabled(boolean b) {
        this.dragEnabled = b;
    }

    /** Whether the colour can be dragged outside. */
    public boolean getDragEnabled() {
        return this.dragEnabled;
    }

    /**
     * It fixes the panel that shows the chosen colour, or `null` for there to be none.
     *
     * <p>The JDK tells `null` from an empty component: `null` asks for the usual panel, and a
     * `JPanel` with nothing inside is how one asks for there to be no preview. The same holds
     * here.
     */
    public void setPreviewPanel(JComponent preview) {
        this.previewPanel = preview;
    }

    /** The preview panel, or `null` if it is the usual one. */
    public JComponent getPreviewPanel() {
        return this.previewPanel;
    }

    /** It adds a chooser panel at the end of those that are already there. */
    public void addChooserPanel(AbstractColorChooserPanel panel) {
        AbstractColorChooserPanel[] added =
                new AbstractColorChooserPanel[this.chooserPanels.length + 1];
        System.arraycopy(this.chooserPanels, 0, added, 0, this.chooserPanels.length);
        added[this.chooserPanels.length] = panel;
        setChooserPanels(added);
    }

    /**
     * It removes a chooser panel.
     *
     * @return the panel that was removed
     * @throws IllegalArgumentException if that panel was not there
     */
    public AbstractColorChooserPanel removeChooserPanel(AbstractColorChooserPanel panel) {
        int where = -1;
        for (int i = 0; i < this.chooserPanels.length; i++) {
            if (this.chooserPanels[i] == panel) {
                where = i;
                break;
            }
        }
        if (where < 0) {
            throw new IllegalArgumentException("chooser panel not in this chooser");
        }
        List<AbstractColorChooserPanel> left = new ArrayList<AbstractColorChooserPanel>();
        for (int i = 0; i < this.chooserPanels.length; i++) {
            if (i != where) {
                left.add(this.chooserPanels[i]);
            }
        }
        AbstractColorChooserPanel[] added = new AbstractColorChooserPanel[left.size()];
        for (int i = 0; i < added.length; i++) {
            added[i] = left.get(i);
        }
        setChooserPanels(added);
        panel.uninstallChooserPanel(this);
        return panel;
    }

    /** It replaces the set of chooser panels. */
    public void setChooserPanels(AbstractColorChooserPanel[] panels) {
        this.chooserPanels = panels;
    }

    /** The chooser panels. */
    public AbstractColorChooserPanel[] getChooserPanels() {
        return this.chooserPanels;
    }

    /** The model the panels share. */
    public ColorSelectionModel getSelectionModel() {
        return this.selectionModel;
    }

    /**
     * It replaces the model.
     *
     * <p>The installed panels go on listening to the **old model** until they are reinstalled; it
     * is like that in the JDK too.
     */
    public void setSelectionModel(ColorSelectionModel newModel) {
        this.selectionModel = newModel;
    }

    // -- the dialog ----------------------------------------------------------------------------

    /**
     * It opens a modal dialog for choosing a colour.
     *
     * @return the chosen colour, or null if the user cancelled
     * @throws java.awt.HeadlessException if there is no screen
     */
    public static Color showDialog(java.awt.Component component, String title,
            Color initialColor) throws java.awt.HeadlessException {
        return showDialog(component, title, initialColor, true);
    }

    /**
     * The same, being able to hide the transparency panel.
     *
     * <p><strong>Here the dialog does not block</strong>, just as in {@link JOptionPane} and for
     * the same reason: this library does not hand out window events. Everything is built, it is
     * shown and null is returned, which is what corresponds to a dialog closed without choosing.
     *
     * @return the chosen colour, or null if the user cancelled
     * @throws java.awt.HeadlessException if there is no screen
     */
    public static Color showDialog(java.awt.Component component, String title, Color initialColor,
            boolean colorTransparencySelectionEnabled) throws java.awt.HeadlessException {
        final JColorChooser pane = new JColorChooser(initialColor != null ? initialColor
                : Color.white);
        ColorTracker ok = new ColorTracker(pane);
        JDialog dialog = createDialog(component, title, true, pane, ok, null);
        dialog.setVisible(true);
        dialog.dispose();
        return ok.getColor();
    }

    /**
     * It builds the dialog that contains that chooser, with its three buttons.
     *
     * <p>The two listeners are OK's and Cancel's; either of the two may be null. The Reset button
     * gives the colour back the one it had on opening, and it needs no listener because it closes
     * nothing.
     *
     * @throws java.awt.HeadlessException if there is no screen
     */
    public static JDialog createDialog(java.awt.Component c, String title, boolean modal,
            JColorChooser chooserPane, java.awt.event.ActionListener okListener,
            java.awt.event.ActionListener cancelListener) throws java.awt.HeadlessException {
        java.awt.Window owner = (c == null) ? null : SwingUtilities.getWindowAncestor(c);
        JDialog dialog;
        if (owner instanceof java.awt.Dialog) {
            dialog = new JDialog((java.awt.Dialog) owner, title, modal);
        } else if (owner instanceof java.awt.Frame) {
            dialog = new JDialog((java.awt.Frame) owner, title, modal);
        } else {
            dialog = new JDialog((java.awt.Frame) null, title, modal);
        }
        java.awt.Container content = dialog.getContentPane();
        content.setLayout(new java.awt.BorderLayout());
        content.add(chooserPane, java.awt.BorderLayout.CENTER);

        JPanel buttons = new JPanel();
        JButton accept = new JButton(UIManager.getString("ColorChooser.okText") != null
                ? UIManager.getString("ColorChooser.okText") : "OK");
        JButton cancel = new JButton(UIManager.getString("ColorChooser.cancelText") != null
                ? UIManager.getString("ColorChooser.cancelText") : "Cancel");
        JButton reset = new JButton(UIManager.getString("ColorChooser.resetText") != null
                ? UIManager.getString("ColorChooser.resetText") : "Reset");
        if (okListener != null) {
            accept.addActionListener(okListener);
        }
        accept.addActionListener(new CloseDialog(dialog));
        if (cancelListener != null) {
            cancel.addActionListener(cancelListener);
        }
        cancel.addActionListener(new CloseDialog(dialog));
        reset.addActionListener(new Reset(chooserPane, chooserPane.getColor()));
        buttons.add(accept);
        buttons.add(cancel);
        buttons.add(reset);
        content.add(buttons, java.awt.BorderLayout.SOUTH);
        dialog.pack();
        return dialog;
    }

    /** The installed look and feel. */
    public javax.swing.plaf.ColorChooserUI getUI() {
        return (javax.swing.plaf.ColorChooserUI) ui;
    }

    /** It installs that look and feel. */
    public void setUI(javax.swing.plaf.ColorChooserUI ui) {
        super.setUI(ui);
    }

    /** It keeps the colour on accepting; null if it was never accepted. */
    private static class ColorTracker implements java.awt.event.ActionListener {

        private final JColorChooser chooser;
        private Color color;

        ColorTracker(JColorChooser c) {
            chooser = c;
        }

        public void actionPerformed(java.awt.event.ActionEvent e) {
            color = chooser.getColor();
        }

        Color getColor() {
            return color;
        }
    }

    /** It closes the dialog; it is what the two buttons that finish do. */
    private static class CloseDialog implements java.awt.event.ActionListener {

        private final JDialog dialog;

        CloseDialog(JDialog d) {
            dialog = d;
        }

        public void actionPerformed(java.awt.event.ActionEvent e) {
            dialog.setVisible(false);
        }
    }

    /** It gives the colour back the one it had on opening. */
    private static class Reset implements java.awt.event.ActionListener {

        private final JColorChooser chooser;
        private final Color original;

        Reset(JColorChooser c, Color original) {
            this.chooser = c;
            this.original = original;
        }

        public void actionPerformed(java.awt.event.ActionEvent e) {
            chooser.setColor(original);
        }
    }
}
