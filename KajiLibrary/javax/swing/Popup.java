package javax.swing;

import java.awt.Component;
import java.awt.Container;

/**
 * A little window that appears on top of everything.
 *
 * <h2>Two ways of appearing</h2>
 *
 * <p>A drop-down may be drawn inside the window that opened it -- fast, but it cannot go outside
 * it -- or in a system window of its own, which can but costs more and flickers. Who decides is
 * {@link PopupFactory}, looking at whether what has to be shown fits.
 *
 * <p>This class hides that decision: whoever uses it calls {@link #show} and {@link #hide}
 * without knowing which of the two they got.
 *
 * <h2>It is not built by hand</h2>
 *
 * <p>The constructor is protected. They are made by {@link PopupFactory}, which is the one that
 * knows how to choose. A {@code Popup} built by hand would have no way of deciding and would
 * always end up in the wrong form.
 */
public class Popup {

    private Component owner;
    private Component contents;
    private int x;
    private int y;
    private java.awt.Window window;

    /** A little window with that content, at that point of the screen. */
    protected Popup(Component owner, Component contents, int x, int y) {
        if (contents == null) {
            throw new IllegalArgumentException("Contents must be non-null");
        }
        this.owner = owner;
        this.contents = contents;
        this.x = x;
        this.y = y;
    }

    /** A little window with nothing; the subclasses that build the content afterwards use it. */
    protected Popup() {
    }

    /**
     * It shows it.
     *
     * <p>With no screen there is nowhere to show it, so here only the state is noted. What is
     * missing is the addressee, not the logic.
     */
    public void show() {
        if (contents != null) {
            contents.setVisible(true);
        }
    }

    public void hide() {
        if (contents != null) {
            contents.setVisible(false);
        }
    }
}
