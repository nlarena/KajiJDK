package javax.swing.plaf.metal;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.LayoutManager;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Icon;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.plaf.basic.BasicInternalFrameTitlePane;

/**
 * An internal frame's title bar, in Metal.
 *
 * <h2>Two bars in one class</h2>
 *
 * <p>{@link #isPalette} decides which of the two is drawn. A normal bar is twenty-three pixels
 * high and carries three buttons; a <strong>palette</strong> one is eleven and carries a single
 * one, the close button, seven by seven. Measured.
 *
 * <p>A palette is a little tool window -- a drawing editor's colour one -- that has to be always
 * at hand and take up as little as possible. Halving its bar is what makes it fit; giving it a
 * single button is because a palette is neither maximized nor iconified, it is closed.
 *
 * <p>The change is on the fly: {@link #setPalette} rebuilds the buttons and assembles the bar
 * again. That is why {@link #createButtons} and {@link #addSubComponents} are methods and not
 * code in the constructor.
 */
public class MetalInternalFrameTitlePane extends BasicInternalFrameTitlePane {

    /** Eleven; see the class note. */
    protected int paletteTitleHeight = 11;

    protected Icon paletteCloseIcon;
    protected boolean isPalette;

    public MetalInternalFrameTitlePane(JInternalFrame f) {
        super(f);
        paletteCloseIcon = MetalIconFactory.getInternalFrameCloseIcon(7);
    }

    protected void installDefaults() {
        super.installDefaults();
        if (paletteCloseIcon == null) {
            paletteCloseIcon = MetalIconFactory.getInternalFrameCloseIcon(7);
        }
    }

    protected void uninstallDefaults() {
        super.uninstallDefaults();
    }

    public void addNotify() {
        super.addNotify();
    }

    protected void createButtons() {
        super.createButtons();
    }

    /** As a palette, only the close button; see the class note. */
    protected void addSubComponents() {
        if (!isPalette) {
            super.addSubComponents();
            return;
        }
        removeAll();
        if (closeButton != null) {
            closeButton.setIcon(paletteCloseIcon);
            add(closeButton);
        }
    }

    protected LayoutManager createLayout() {
        return new MetalTitlePaneLayout();
    }

    protected PropertyChangeListener createPropertyChangeListener() {
        return new MetalPropertyChangeHandler();
    }

    protected void assembleSystemMenu() {
        if (!isPalette) {
            super.assembleSystemMenu();
        }
    }

    protected void addSystemMenuItems(JMenu systemMenu) {
        super.addSystemMenuItems(systemMenu);
    }

    protected void showSystemMenu() {
        if (!isPalette) {
            super.showSystemMenu();
        }
    }

    /** It switches between the two bars and rebuilds the children. */
    public void setPalette(boolean b) {
        isPalette = b;
        addSubComponents();
        revalidate();
        repaint();
    }

    public void paintComponent(Graphics g) {
        if (isPalette) {
            paintPalette(g);
            return;
        }
        super.paintComponent(g);
    }

    /** The eleven-pixel bar: a fill and a line, with no title. */
    public void paintPalette(Graphics g) {
        Dimension s = getSize();
        g.setColor(MetalLookAndFeel.getPrimaryControlShadow());
        g.fillRect(0, 0, s.width, s.height);
        g.setColor(MetalLookAndFeel.getPrimaryControlDarkShadow());
        g.drawLine(0, s.height - 1, s.width, s.height - 1);
    }

    /** The bar's layout; as a palette, the height is another. */
    private class MetalTitlePaneLayout implements LayoutManager {

        public void addLayoutComponent(String name, java.awt.Component c) {
        }

        public void removeLayoutComponent(java.awt.Component c) {
        }

        public Dimension preferredLayoutSize(Container c) {
            Dimension d = basicLayoutManager().preferredLayoutSize(c);
            if (isPalette) {
                return new Dimension(d.width, paletteTitleHeight);
            }
            return d;
        }

        public Dimension minimumLayoutSize(Container c) {
            return preferredLayoutSize(c);
        }

        public void layoutContainer(Container c) {
            basicLayoutManager().layoutContainer(c);
        }
    }

    private LayoutManager basicLayout;

    /**
     * The basic one's, kept here and not inside the inner class.
     *
     * <p>For two reasons. One: this house's compiler does not yet accept
     * {@code MetalInternalFrameTitlePane.super.createLayout()}; see finding #400. The other, more
     * fundamental: {@code createLayout} is called by the superclass's constructor, so a field of
     * the inner class initialized there is built before this class finishes constructing itself.
     * Asking for it when it is needed avoids that.
     */
    private LayoutManager basicLayoutManager() {
        if (basicLayout == null) {
            basicLayout = super.createLayout();
        }
        return basicLayout;
    }

    /** The one that listens to the window's changes. */
    private class MetalPropertyChangeHandler implements PropertyChangeListener {

        public void propertyChange(PropertyChangeEvent e) {
            basicPropertyListener().propertyChange(e);
            if (MetalInternalFrameUI.IS_PALETTE.equals(e.getPropertyName())) {
                setPalette(Boolean.TRUE.equals(e.getNewValue()));
            }
        }
    }

    private PropertyChangeListener basicListener;

    /** The same as {@link #basicLayoutManager}. */
    private PropertyChangeListener basicPropertyListener() {
        if (basicListener == null) {
            basicListener = super.createPropertyChangeListener();
        }
        return basicListener;
    }
}
