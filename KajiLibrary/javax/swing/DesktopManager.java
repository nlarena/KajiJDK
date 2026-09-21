package javax.swing;

/**
 * Who decides how a desktop's internal frames behave.
 *
 * <h2>Why it exists</h2>
 *
 * <p>A {@link JInternalFrame} does not close, enlarge or move on its own: it asks the desktop's
 * manager to do it. That way the desktop can impose its policy -- a maximized frame that covers
 * the others, icons lined up in a row, dragging with an outline instead of live -- without each
 * frame knowing anything about it.
 *
 * <h2>The three rounds of methods</h2>
 *
 * <p>The first eight are changes of state the frame asks for. The six in the middle are the
 * three stages -- begin, go on, end -- of dragging and of resizing; they are separate because
 * the outline mode only draws during the middle and moves only at the end. The last,
 * {@link #setBoundsForFrame}, is the one that finally moves something.
 */
public interface DesktopManager {

    /** The frame was added to the desktop and has to be shown. */
    void openFrame(JInternalFrame f);

    /** It removes the frame from the desktop. */
    void closeFrame(JInternalFrame f);

    /** It enlarges the frame to the whole desktop. */
    void maximizeFrame(JInternalFrame f);

    /** It gives the frame back its previous size. */
    void minimizeFrame(JInternalFrame f);

    /** It replaces the frame with its icon. */
    void iconifyFrame(JInternalFrame f);

    /** It gives the frame back in place of its icon. */
    void deiconifyFrame(JInternalFrame f);

    /** The frame became the active one. */
    void activateFrame(JInternalFrame f);

    /** The frame stopped being the active one. */
    void deactivateFrame(JInternalFrame f);

    /** It begins a drag; see the interface note. */
    void beginDraggingFrame(JComponent f);

    /** The drag is going by that position. */
    void dragFrame(JComponent f, int newX, int newY);

    /** It ends the drag. */
    void endDraggingFrame(JComponent f);

    /** It begins resizing from that edge. */
    void beginResizingFrame(JComponent f, int direction);

    /** The resizing is going by that rectangle. */
    void resizeFrame(JComponent f, int newX, int newY, int newWidth, int newHeight);

    /** It ends the resizing. */
    void endResizingFrame(JComponent f);

    /** It moves and resizes the frame; it is the only one that really changes anything. */
    void setBoundsForFrame(JComponent f, int newX, int newY, int newWidth, int newHeight);
}
