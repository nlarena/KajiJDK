package javax.swing.plaf.basic;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.beans.PropertyVetoException;

import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.LookAndFeel;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.MouseInputListener;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.DesktopIconUI;

/**
 * The basic look and feel of an internal frame turned into an icon.
 *
 * <h2>The icon is a title bar with no frame</h2>
 *
 * <p>{@link #iconPane} <em>is</em> a {@link BasicInternalFrameTitlePane}. It does not look like
 * one: it is one. That explains why a desktop icon shows the title and the restore and close
 * buttons, and why it measures what it measures -- whatever that bar measures --.
 *
 * <p>The practical consequence is that this class cannot be written before the title bar, and
 * that is why it was left for the end of the package.
 *
 * <h2>A double click restores</h2>
 *
 * <p>{@link #deiconize} is the operation, and the mouse listener fires it with two clicks. A
 * single click chooses the frame without restoring it, which is what allows dragging the icon
 * to another place on the desktop.
 *
 * <h2>What is left said</h2>
 *
 * <p>Dragging the icon around the desktop needs the desktop manager and a mouse; the listener
 * is there and does not drag.
 */
public class BasicDesktopIconUI extends DesktopIconUI {

    protected JInternalFrame.JDesktopIcon desktopIcon;
    protected JInternalFrame frame;

    /** The title bar that is seen as an icon; see the class note. */
    protected JComponent iconPane;

    private MouseInputListener mouseInputListener;

    public BasicDesktopIconUI() {
    }

    /** A new one per icon: it keeps the bar it shows. */
    public static ComponentUI createUI(JComponent c) {
        return new BasicDesktopIconUI();
    }

    public void installUI(JComponent c) {
        desktopIcon = (JInternalFrame.JDesktopIcon) c;
        frame = desktopIcon.getInternalFrame();
        installDefaults();
        installComponents();
        installListeners();
        javax.swing.JLayeredPane.putLayer(desktopIcon, javax.swing.JLayeredPane.PALETTE_LAYER);
    }

    public void uninstallUI(JComponent c) {
        uninstallComponents();
        uninstallListeners();
        uninstallDefaults();
        frame = null;
        desktopIcon = null;
    }

    /** Colours, layout and opacity. */
    protected void installDefaults() {
        desktopIcon.setLayout(new BorderLayout());
        LookAndFeel.installProperty(desktopIcon, "opaque", Boolean.TRUE);
    }

    /** It removes nothing; see {@link BasicPanelUI#uninstallDefaults}. */
    protected void uninstallDefaults() {
        desktopIcon.setLayout(null);
    }

    /** It builds the title bar and puts it in the centre; see the class note. */
    protected void installComponents() {
        iconPane = new BasicInternalFrameTitlePane(frame);
        desktopIcon.setLayout(new BorderLayout());
        desktopIcon.add(iconPane, BorderLayout.CENTER);
    }

    protected void uninstallComponents() {
        if (iconPane != null) {
            desktopIcon.remove(iconPane);
        }
        desktopIcon.setLayout(null);
        iconPane = null;
    }

    protected void installListeners() {
        mouseInputListener = createMouseInputListener();
        desktopIcon.addMouseListener(mouseInputListener);
        desktopIcon.addMouseMotionListener(mouseInputListener);
    }

    protected void uninstallListeners() {
        desktopIcon.removeMouseListener(mouseInputListener);
        desktopIcon.removeMouseMotionListener(mouseInputListener);
        mouseInputListener = null;
    }

    protected MouseInputListener createMouseInputListener() {
        return new Handler(this);
    }

    /** It gives the frame back its size; see the class note. */
    public void deiconize() {
        try {
            frame.setIcon(false);
        } catch (PropertyVetoException ex) {
            // Somebody said no. It is a valid answer, not an error.
        }
    }

    /** The title bar's plus the margins. */
    public Dimension getPreferredSize(JComponent c) {
        return desktopIcon.getLayout().preferredLayoutSize(desktopIcon);
    }

    /** The same with the minimum. */
    public Dimension getMinimumSize(JComponent c) {
        return desktopIcon.getLayout().minimumLayoutSize(desktopIcon);
    }

    /** No cap. */
    public Dimension getMaximumSize(JComponent c) {
        return new Dimension(Short.MAX_VALUE, Short.MAX_VALUE);
    }

    /** Four pixels of air all around. */
    public Insets getInsets(JComponent c) {
        JInternalFrame f = desktopIcon.getInternalFrame();
        javax.swing.border.Border border = f.getBorder();
        if (border != null) {
            return border.getBorderInsets(f);
        }
        return new Insets(0, 0, 0, 0);
    }

    /** Two clicks restore the frame; see the class note. */
    private static class Handler extends MouseInputAdapter implements MouseInputListener {

        private final BasicDesktopIconUI ui;

        Handler(BasicDesktopIconUI ui) {
            this.ui = ui;
        }

        public void mousePressed(MouseEvent e) {
            if (e.getClickCount() > 1 && ui.frame.isIconifiable()) {
                ui.deiconize();
                return;
            }
            try {
                ui.frame.setSelected(true);
            } catch (PropertyVetoException ex) {
                // Somebody said no.
            }
        }
    }
}
