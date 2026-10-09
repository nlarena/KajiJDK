package javax.swing.plaf.metal;

import java.awt.Dimension;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicDesktopIconUI;

/**
 * A minimized internal frame's icon, in Metal.
 *
 * <p>Metal does not draw it as an icon: it draws it as a <strong>tiny window</strong>, with its
 * title bar and its restore button. That is why the preferred width is fixed -- 160 pixels --
 * and does not depend on the title: if it did, a desktop with several minimized windows would
 * have buttons of different lengths and would not read as a row.
 *
 * <p>The three sizes are the same. A desktop icon does not stretch.
 */
public class MetalDesktopIconUI extends BasicDesktopIconUI {

    /** The fixed width; see the class note. */
    private static final int WIDTH = 160;

    public MetalDesktopIconUI() {
    }

    public static ComponentUI createUI(JComponent c) {
        return new MetalDesktopIconUI();
    }

    /** The basic one's; Metal changes none, and the icon stays opaque. Measured. */
    protected void installDefaults() {
        super.installDefaults();
    }

    protected void installComponents() {
        super.installComponents();
    }

    protected void uninstallComponents() {
        super.uninstallComponents();
    }

    protected void installListeners() {
        super.installListeners();
    }

    protected void uninstallListeners() {
        super.uninstallListeners();
    }

    public Dimension getPreferredSize(JComponent c) {
        return new Dimension(WIDTH, height());
    }

    public Dimension getMinimumSize(JComponent c) {
        return getPreferredSize(c);
    }

    public Dimension getMaximumSize(JComponent c) {
        return getPreferredSize(c);
    }

    /** The one of the title bar it carries inside. */
    private int height() {
        if (iconPane != null) {
            return iconPane.getPreferredSize().height;
        }
        return 0;
    }
}
