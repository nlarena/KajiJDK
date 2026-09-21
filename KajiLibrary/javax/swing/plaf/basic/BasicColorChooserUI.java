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
 * The basic look and feel of a colour chooser.
 *
 * <h2>Two halves: the tabs and the preview</h2>
 *
 * <p>Above, one tab for each way of choosing a colour -- swatches, HSV, RGB --; below, the
 * preview of how it came out. The chooser chooses nothing on its own: each tab panel writes
 * into the same {@code ColorSelectionModel}, and the preview listens to it. This look and feel
 * only connects them.
 *
 * <h2>With no tab panels</h2>
 *
 * <p><strong>{@link #createDefaultChoosers} returns an empty array.</strong> The JDK's five
 * panels -- swatches, HSV, HSL, RGB, CMYK -- are real interactive components: sliders,
 * formatted fields and a colour diagram that is painted and dragged. This library does not
 * bring them and {@code ColorChooserComponentFactory.getDefaultChooserPanels} says so by
 * throwing {@code UnsupportedOperationException}.
 *
 * <p>Returning none is a legal subset -- a chooser with no ways of choosing --; letting the
 * exception come out through {@link #installUI} would make it impossible even to build the
 * component, and that is worse. What is there is the whole scaffolding: the preview, the
 * listeners, and the replacing of panels when the program puts its own in, which is the case in
 * which a chooser is useful here.
 */
public class BasicColorChooserUI extends ColorChooserUI {

    protected JColorChooser chooser;

    /** The panels this look and feel put in; see the class note. */
    protected AbstractColorChooserPanel[] defaultChoosers;

    /** The one that redraws the preview when the chosen colour changes. */
    protected ChangeListener previewListener;

    protected PropertyChangeListener propertyChangeListener;

    private JTabbedPane tabbedPane;
    private JPanel singlePanel;
    private JPanel previewPanelHolder;
    private Component previewPanel;

    private static final ColorUIResource BACKGROUND = new ColorUIResource(238, 238, 238);
    private static final ColorUIResource FOREGROUND = new ColorUIResource(51, 51, 51);
    private static final FontUIResource FONT = new FontUIResource("Dialog", Font.PLAIN, 12);

    public BasicColorChooserUI() {
    }

    /** A new one per chooser: it keeps the component and the panels it built. */
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

    /** See the class note: none. */
    protected AbstractColorChooserPanel[] createDefaultChoosers() {
        return new AbstractColorChooserPanel[0];
    }

    /** It disconnects the panels this look and feel put in. */
    protected void uninstallDefaultChoosers() {
        if (defaultChoosers == null) {
            return;
        }
        for (int i = 0; i < defaultChoosers.length; i++) {
            chooser.removeChooserPanel(defaultChoosers[i]);
        }
    }

    /** Colours, typeface and opacity; the values are those of {@code ColorChooser.*} in Metal. */
    protected void installDefaults() {
        Color background = chooser.getBackground();
        if (background == null || background instanceof UIResource) {
            chooser.setBackground(BACKGROUND);
        }
        Color foreground = chooser.getForeground();
        if (foreground == null || foreground instanceof UIResource) {
            chooser.setForeground(FOREGROUND);
        }
        Font font = chooser.getFont();
        if (font == null || font instanceof UIResource) {
            chooser.setFont(FONT);
        }
        LookAndFeel.installProperty(chooser, "opaque", Boolean.TRUE);
    }

    /** It removes nothing; see {@link BasicPanelUI#uninstallDefaults}. */
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
     * It puts the preview at the very bottom.
     *
     * <p>If the program did not put one of its own, the one the factory builds. And if the
     * factory cannot -- the same as with the tab panels --, none: the bottom half is left empty
     * and the chooser goes on working.
     */
    protected void installPreviewPanel() {
        if (previewPanelHolder == null) {
            previewPanelHolder = new JPanel(new BorderLayout());
            previewPanelHolder.setName("ColorChooser.previewPanelHolder");
        }
        previewPanelHolder.removeAll();
        Component previous = chooser.getPreviewPanel();
        if (previous == null) {
            try {
                previous = ColorChooserComponentFactory.getPreviewPanel();
            } catch (UnsupportedOperationException e) {
                // See the method's note: with no preview, the chooser works all the same.
                previous = null;
            }
        }
        previewPanel = previous;
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
     * It rebuilds the top part according to how many panels there are.
     *
     * <p>With only one it goes loose; with two or more, each in its tab. Putting a single tab
     * would look like an extra frame around nothing.
     */
    private void rebuildPanels() {
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
     * The one that reacts to the chosen colour and to the chooser's changes.
     *
     * <p>Static and with the look and feel as a field, for the same reason as everywhere in the
     * package; see finding #518.
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
            String name = e.getPropertyName();
            if (JColorChooser.CHOOSER_PANELS_PROPERTY.equals(name)) {
                ui.rebuildPanels();
                ui.chooser.revalidate();
            } else if (JColorChooser.PREVIEW_PANEL_PROPERTY.equals(name)) {
                ui.installPreviewPanel();
                ui.chooser.revalidate();
            } else if (JColorChooser.SELECTION_MODEL_PROPERTY.equals(name)) {
                Object old = e.getOldValue();
                Object newValue = e.getNewValue();
                if (old instanceof javax.swing.colorchooser.ColorSelectionModel) {
                    ((javax.swing.colorchooser.ColorSelectionModel) old)
                            .removeChangeListener(ui.previewListener);
                }
                if (newValue instanceof javax.swing.colorchooser.ColorSelectionModel) {
                    ((javax.swing.colorchooser.ColorSelectionModel) newValue)
                            .addChangeListener(ui.previewListener);
                }
            }
        }
    }
}
