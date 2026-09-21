package javax.swing;

import java.awt.FlowLayout;
import java.awt.LayoutManager;

/**
 * A generic container: the brick any arrangement in Swing is built with.
 *
 * <p>It has no behaviour of its own -- it draws nothing by itself, it answers nothing -- and
 * that is its usefulness: a place to put other components with an arrangement. Almost any Swing
 * screen is a tree of panels.
 *
 * <p>Its default arrangement is {@link FlowLayout}, not `null`: a newly created panel already
 * knows how to lay out whatever is put into it.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>The four constructors and the UI identifier are there. What is missing hangs from the
 * `LookAndFeel` -- `getUI`, `setUI`, `updateUI` -- and from accessibility, which this library
 * does not have.
 *
 * <p>The `isDoubleBuffered` parameter is kept but not applied: double buffering is a decision of
 * the painting, and there is no painting here.
 */
public class JPanel extends JComponent {


    /** Whether double buffering would be asked for when painting. See the class note. */
    private boolean doubleBuffered;

    /**
     * A panel with that arrangement and that buffering mode.
     *
     * @param layout the arrangement, or `null` for none
     * @param isDoubleBuffered whether double buffering would be asked for
     */
    public JPanel(LayoutManager layout, boolean isDoubleBuffered) {
        super();
        this.doubleBuffered = isDoubleBuffered;
        setLayout(layout);
    }

    /**
     * A panel with {@link FlowLayout} and that buffering mode.
     *
     * @param isDoubleBuffered whether double buffering would be asked for
     */
    public JPanel(boolean isDoubleBuffered) {
        this(new FlowLayout(), isDoubleBuffered);
    }

    /**
     * A panel with that arrangement, with double buffering.
     *
     * @param layout the arrangement, or `null` for none
     */
    public JPanel(LayoutManager layout) {
        this(layout, true);
    }

    /** A panel with {@link FlowLayout} and double buffering. */
    public JPanel() {
        this(new FlowLayout(), true);
    }

    /** The installed look and feel. */
    public javax.swing.plaf.PanelUI getUI() {
        return (javax.swing.plaf.PanelUI) ui;
    }

    /** It installs that look and feel. */
    public void setUI(javax.swing.plaf.PanelUI ui) {
        super.setUI(ui);
    }

    /** The key the `LookAndFeel` looks a panel's look and feel up with: {@code "PanelUI"}. */
    public String getUIClassID() {
        return "PanelUI";
    }

    /** Whether double buffering would be asked for when painting. See the class note. */
    public boolean isDoubleBuffered() {
        return this.doubleBuffered;
    }
}
