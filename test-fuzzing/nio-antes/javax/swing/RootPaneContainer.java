package javax.swing;

import java.awt.Component;
import java.awt.Container;

/**
 * It is implemented by whatever contains a {@link JRootPane}: frames, dialogs and applets.
 *
 * <h2>What having it is for</h2>
 *
 * <p>The six methods are shortcuts to the root pane's. They exist so that
 * {@code window.getContentPane()} can be written instead of
 * {@code window.getRootPane().getContentPane()}, and above all so that a method can receive
 * "something that has content" without knowing whether it is a frame, a dialog or an applet.
 *
 * <p>It is also what reminds one that components are not added to these containers directly;
 * see {@link JRootPane}'s note.
 */
public interface RootPaneContainer {

    /** The root pane. */
    JRootPane getRootPane();

    void setContentPane(Container contentPane);

    /** Where what the program adds goes. */
    Container getContentPane();

    void setLayeredPane(JLayeredPane layeredPane);

    JLayeredPane getLayeredPane();

    void setGlassPane(Component glassPane);

    /** The component at the very top; see {@link JRootPane}'s note. */
    Component getGlassPane();
}
