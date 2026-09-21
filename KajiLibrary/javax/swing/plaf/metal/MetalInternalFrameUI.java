package javax.swing.plaf.metal;

import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.event.MouseInputAdapter;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicInternalFrameUI;

/**
 * Metal's internal frame.
 *
 * <p>The only thing of its own is palette mode. {@link #setPalette} passes the notice on to the
 * title bar -- see {@link MetalInternalFrameTitlePane} -- and changes the window's border,
 * because a palette does not carry the thick four-pixel frame either.
 *
 * <p>The client property {@value #IS_PALETTE} does the same from outside: a program sets it and
 * the title bar's listener hears about it. It is the way of turning a window into a palette
 * without having the UI at hand.
 *
 * <p>{@link #IS_PALETTE} is {@code protected static} and <strong>not</strong> {@code final},
 * which is odd and is so in the JDK.
 */
public class MetalInternalFrameUI extends BasicInternalFrameUI {

    /** The client property that turns the window into a palette. */
    protected static String IS_PALETTE = "JInternalFrame.isPalette";

    public MetalInternalFrameUI(JInternalFrame b) {
        super(b);
    }

    public static ComponentUI createUI(JComponent c) {
        return new MetalInternalFrameUI((JInternalFrame) c);
    }

    public void installUI(JComponent c) {
        super.installUI(c);
        Object o = c instanceof JInternalFrame
                ? ((JInternalFrame) c).getClientProperty(IS_PALETTE) : null;
        setPalette(Boolean.TRUE.equals(o));
    }

    public void uninstallUI(JComponent c) {
        super.uninstallUI(c);
    }

    protected void installListeners() {
        super.installListeners();
    }

    protected void uninstallListeners() {
        super.uninstallListeners();
    }

    protected void installKeyboardActions() {
        super.installKeyboardActions();
    }

    protected void uninstallKeyboardActions() {
        super.uninstallKeyboardActions();
    }

    protected void uninstallComponents() {
        super.uninstallComponents();
    }

    protected JComponent createNorthPane(JInternalFrame w) {
        titlePane = new MetalInternalFrameTitlePane(w);
        return titlePane;
    }

    protected MouseInputAdapter createBorderListener(JInternalFrame w) {
        return super.createBorderListener(w);
    }

    /** It switches the window between normal and palette; see the class note. */
    public void setPalette(boolean isPalette) {
        if (titlePane instanceof MetalInternalFrameTitlePane) {
            ((MetalInternalFrameTitlePane) titlePane).setPalette(isPalette);
        }
        if (frame != null) {
            frame.setBorder(isPalette
                    ? MetalBorders.getPaletteBorder()
                    : MetalBorders.getInternalFrameBorder());
        }
    }
}
