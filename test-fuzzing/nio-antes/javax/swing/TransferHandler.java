package javax.swing;

import java.awt.Point;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;

/**
 * Who knows how to copy, paste and drag a component's content.
 *
 * <h2>The idea</h2>
 *
 * <p>A component does not know how to copy itself: {@code TransferHandler} knows. That allows
 * the same component to be copied one way in one application and another way in another, and
 * copying and dragging to share the same code, because both end in a {@link Transferable}.
 *
 * <h2>What there is here</h2>
 *
 * <p>This VM has neither a system clipboard nor dragging between windows: there is no screen
 * nor window manager to negotiate with. The operations that need one --
 * {@link #exportToClipboard}, {@link #importData}, {@link #exportAsDrag} -- say so and do
 * nothing. What is complete is the <em>shape</em>: the types, the actions, the drop location
 * ({@link DropLocation}) and the query of whether something could be imported, which is what a
 * component asks in order to decide whether it marks the destination.
 */
public class TransferHandler implements Serializable {

    /** No operation. */
    public static final int NONE = 0;

    /** Copy: the source keeps what is its. */
    public static final int COPY = 1;

    /** Move: the source loses it. */
    public static final int MOVE = 2;

    /** Either of the two; whoever receives decides it. */
    public static final int COPY_OR_MOVE = 3;

    /** A link to what is in the source. */
    public static final int LINK = 1073741824;

    private String propertyName;

    private static final Action cutAction = new TransferAction("cut");
    private static final Action copyAction = new TransferAction("copy");
    private static final Action pasteAction = new TransferAction("paste");

    /**
     * A handler that transfers a single property of the component by its name.
     *
     * <p>It is the cheap way for a component to be copyable: instead of writing a handler, one
     * says "what is copied of this is its text property".
     */
    public TransferHandler(String property) {
        propertyName = property;
    }

    protected TransferHandler() {
        this(null);
    }

    /** The cut action, to put in a menu or a button. */
    public static Action getCutAction() {
        return cutAction;
    }

    public static Action getCopyAction() {
        return copyAction;
    }

    public static Action getPasteAction() {
        return pasteAction;
    }

    /**
     * Whether any of those types can be imported into that component.
     *
     * @deprecated it is {@link #canImport(TransferSupport)}, which also knows where it is going to
     *     fall.
     */
    @Deprecated
    public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
        return false;
    }

    /** Whether what is being dragged can be dropped there. */
    public boolean canImport(TransferSupport support) {
        return false;
    }

    /** Which operations the source admits; none with no handler of its own. */
    public int getSourceActions(JComponent c) {
        return NONE;
    }

    /**
     * What has to be transferred from that component.
     *
     * <p>{@code null} if there is nothing; with the property by name, that property's value.
     */
    protected Transferable createTransferable(JComponent c) {
        return null;
    }

    /** The exporting finished; with {@code MOVE} it is where the source erases what is its. */
    protected void exportDone(JComponent source, Transferable data, int action) {
    }

    /** There is no system clipboard on this VM; see the class note. */
    public void exportToClipboard(JComponent comp, Clipboard clip, int action) {
        throw new UnsupportedOperationException("this VM has no system clipboard");
    }

    /** There is no dragging on this VM; see the class note. */
    public void exportAsDrag(JComponent comp, java.awt.event.InputEvent e, int action) {
        throw new UnsupportedOperationException("this VM has no drag and drop");
    }

    /** @deprecated it is {@link #importData(TransferSupport)}. */
    @Deprecated
    public boolean importData(JComponent comp, Transferable t) {
        return false;
    }

    /** Nothing to import with neither clipboard nor dragging; see the class note. */
    public boolean importData(TransferSupport support) {
        return false;
    }

    /** The image that goes with the cursor while dragging; none. */
    public void setDragImage(java.awt.Image img) {
    }

    public java.awt.Image getDragImage() {
        return null;
    }

    public void setDragImageOffset(Point p) {
    }

    public Point getDragImageOffset() {
        return new Point(0, 0);
    }

    /**
     * The image that is dragged under the pointer.
     *
     * <p>Null -- which is the usual thing -- lets the system show its dragging cursor. A subclass
     * returns an icon when it wants what is being moved to be seen.
     */
    public Icon getVisualRepresentation(java.awt.datatransfer.Transferable t) {
        return null;
    }

    /**
     * Where what is being dragged is going to fall.
     *
     * <p>Each component defines its own subclass with what matters to it -- a row, a text
     * index --; this one only carries the point.
     */
    public static class DropLocation {

        private final Point dropPoint;

        protected DropLocation(Point dropPoint) {
            if (dropPoint == null) {
                throw new IllegalArgumentException("Location cannot be null");
            }
            this.dropPoint = new Point(dropPoint);
        }

        public final Point getDropPoint() {
            return new Point(dropPoint);
        }

        public String toString() {
            return getClass().getName() + "[dropPoint=" + dropPoint + "]";
        }
    }

    /**
     * A transfer's context: who receives it, what it brings and where it falls.
     *
     * <p>It is passed to {@link #canImport(TransferSupport)} and to
     * {@link #importData(TransferSupport)} instead of three loose arguments, so that adding
     * information later does not change the signature.
     */
    public static final class TransferSupport {

        private boolean isDrop;
        private java.awt.Component component;
        private DropLocation dropLocation;
        private int dropAction = -1;
        private boolean showDropLocationIsSet;
        private boolean showDropLocation;
        private int sourceSupportedActions;
        private Transferable transferable;

        /** A paste context -- not a dragging one -- over that component. */
        public TransferSupport(java.awt.Component component, Transferable transferable) {
            if (component == null || transferable == null) {
                throw new NullPointerException("component and transferable must be non-null");
            }
            this.component = component;
            this.transferable = transferable;
            this.isDrop = false;
        }

        /** Whether it comes from a drag; if not, from pasting. */
        public boolean isDrop() {
            return isDrop;
        }

        public java.awt.Component getComponent() {
            return component;
        }

        /** Where it falls; it only makes sense in a drag. */
        public DropLocation getDropLocation() {
            assureIsDrop();
            return dropLocation;
        }

        /** Whether the component has to mark the destination while dragging. */
        public void setShowDropLocation(boolean showDropLocation) {
            assureIsDrop();
            this.showDropLocationIsSet = true;
            this.showDropLocation = showDropLocation;
        }

        /** It chooses between copying and moving when the source admits both. */
        public void setDropAction(int dropAction) {
            assureIsDrop();
            this.dropAction = dropAction;
        }

        public int getDropAction() {
            return dropAction == -1 ? getUserDropAction() : dropAction;
        }

        /** The one the user asked for with the keyboard modifiers. */
        public int getUserDropAction() {
            assureIsDrop();
            return NONE;
        }

        public int getSourceDropActions() {
            assureIsDrop();
            return sourceSupportedActions;
        }

        public DataFlavor[] getDataFlavors() {
            return transferable.getTransferDataFlavors();
        }

        public boolean isDataFlavorSupported(DataFlavor df) {
            return transferable.isDataFlavorSupported(df);
        }

        public Transferable getTransferable() {
            return transferable;
        }

        private void assureIsDrop() {
            if (!isDrop) {
                throw new IllegalStateException("Not a drop");
            }
        }
    }

    /** The three menu actions; with no clipboard, they do nothing. */
    static class TransferAction extends AbstractAction implements Serializable {

        TransferAction(String name) {
            super(name);
        }

        public void actionPerformed(ActionEvent e) {
        }
    }

    /**
     * It is implemented by whatever has a {@link TransferHandler} without being a
     * {@link JComponent}.
     *
     * <p>They are the windows: {@code JDialog}, {@code JFrame} and {@code JWindow} may have one
     * and do not inherit from {@code JComponent}. Without this interface, the code that looks a
     * component's handler up would have to ask for each window class.
     *
     * <p>It is not public: it is a detail of how Swing finds the handler, not something a program
     * should implement.
     */
    interface HasGetTransferHandler {

        /** The handler, or null. */
        TransferHandler getTransferHandler();
    }
}
