package javax.swing.colorchooser;

import java.awt.Color;
import java.awt.Graphics;
import java.beans.PropertyChangeListener;
import javax.swing.Icon;
import javax.swing.JColorChooser;
import javax.swing.JPanel;

/**
 * A tab of the colour chooser: one way of picking the same colour.
 *
 * <p>Each panel --RGB, HSV, CMYK, the swatches-- presents the colour in a different way, but
 * **none of them holds the colour**: they all read and write the {@link ColorSelectionModel} of
 * the {@link JColorChooser} that hosts them. That is why the tabs stay in step without knowing
 * about each other, and why the panel's life cycle revolves around
 * {@link #installChooserPanel} and {@link #uninstallChooserPanel}: installing it is hooking it
 * to the model, uninstalling it is letting go.
 *
 * <p>A subclass implements five things: {@link #buildChooser} builds the interface once,
 * {@link #updateChooser} refreshes it every time the model changes, and
 * {@link #getDisplayName}, {@link #getSmallDisplayIcon} and {@link #getLargeDisplayIcon} say how
 * it is named on the tab.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>The life cycle and the access to the model are done. {@link #paint} delegates to the
 * superclass and paints nothing of its own --the JDK takes advantage of that point to refresh
 * the panel when the `LookAndFeel` changes, and here there is none-- and neither is the hook of
 * `enabled` to the chooser, which in the JDK is a {@link PropertyChangeListener} over a bound
 * property: this library's `JComponent` has no bound properties yet.
 */
public abstract class AbstractColorChooserPanel extends JPanel {

    /** The name of the property that says whether transparency can be picked. */
    public static final String TRANSPARENCY_ENABLED_PROPERTY = "TransparencyEnabled";

    /**
     * The listener that in the JDK follows the chooser's `enabled`. Here it is not registered --see
     * the class note-- but the field stays because it is where it would go.
     */
    private final PropertyChangeListener enabledListener = null;

    /** The chooser that hosts this panel, or `null` if it is not installed. */
    private JColorChooser chooser;

    /** Whether transparency can be picked. */
    private boolean transparencyEnabled = true;

    /** For the subclasses. */
    protected AbstractColorChooserPanel() {
        super();
    }

    /**
     * Refreshes the panel's interface with the colour in the model.
     *
     * <p>The chooser calls it every time the colour changes, whether it came from this panel or
     * from another.
     */
    public abstract void updateChooser();

    /**
     * Builds the panel's interface.
     *
     * <p>It is called only once, from {@link #installChooserPanel}.
     */
    protected abstract void buildChooser();

    /** The tab's name. */
    public abstract String getDisplayName();

    /**
     * The tab's mnemonic character, or 0 if it has none.
     *
     * <p>Zero by default: a mnemonic repeated between tabs is worse than none, and the base class
     * cannot know which ones are free.
     */
    public int getMnemonic() {
        return 0;
    }

    /**
     * Which letter of the name to underline as the mnemonic, or -1 for none.
     *
     * <p>It is an index and not a character because the name may repeat the letter, and only one
     * has to be underlined.
     */
    public int getDisplayedMnemonicIndex() {
        return -1;
    }

    /** The tab's small icon, or `null` if it has none. */
    public abstract Icon getSmallDisplayIcon();

    /** The tab's large icon, or `null` if it has none. */
    public abstract Icon getLargeDisplayIcon();

    /**
     * Hooks the panel to that chooser and builds its interface.
     *
     * <p>The chooser calls it; a subclass that overrides it has to call `super`, or the panel is
     * left without a model.
     */
    public void installChooserPanel(JColorChooser enclosingChooser) {
        if (this.chooser != null) {
            throw new RuntimeException("This chooser panel is already installed");
        }
        this.chooser = enclosingChooser;
        buildChooser();
        updateChooser();
    }

    /**
     * Lets the panel go from the chooser.
     *
     * <p>A subclass that overrides it has to call `super`, or the panel is left believing it is
     * still installed.
     */
    public void uninstallChooserPanel(JColorChooser enclosingChooser) {
        this.chooser = null;
    }

    /** The model of the chooser that hosts it, or `null` if it is not installed. */
    public ColorSelectionModel getColorSelectionModel() {
        return this.chooser == null ? null : this.chooser.getSelectionModel();
    }

    /** The colour in the model, or `null` if the panel is not installed. */
    protected Color getColorFromModel() {
        ColorSelectionModel model = getColorSelectionModel();
        return model == null ? null : model.getSelectedColor();
    }

    /**
     * Writes the colour into the model. Package access: it is how the panel answers the chooser.
     */
    void setSelectedColor(Color color) {
        ColorSelectionModel model = getColorSelectionModel();
        if (model != null) {
            model.setSelectedColor(color);
        }
    }

    /**
     * Turns the picking of transparency on or off in this panel.
     *
     * <p>It is kept; making it effective is the subclass's business, since it is the one that has
     * control of the alpha channel -- if it has one at all.
     */
    public void setColorTransparencySelectionEnabled(boolean b) {
        this.transparencyEnabled = b;
    }

    /** Whether this panel lets transparency be picked. By default, yes. */
    public boolean isColorTransparencySelectionEnabled() {
        return this.transparencyEnabled;
    }

    /** Paints the panel. See the class note: it adds nothing of its own. */
    public void paint(Graphics g) {
        super.paint(g);
    }
}
