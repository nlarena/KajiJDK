package jdk.swing.interop;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DndAccess;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetContext;
import java.awt.dnd.InvalidDnDOperationException;

/**
 * The target side of a drag, seen from another toolkit.
 *
 * <h2>The sequence</h2>
 *
 * <p>A drag passing over a component is a negotiation: the system says there is something above, the
 * target answers whether it takes it and with which action, and only once the user drops and the
 * target accepted are the data transferred. The methods here are that negotiation seen from the
 * receiving side.
 *
 * <p>The order matters: {@link #getTransferable} before having accepted throws, and
 * {@link #dropComplete} twice throws as well. That is not rigidity but the only way for the source
 * to know when it may delete what it dragged.
 *
 * <h2>{@link #isTransferableJVMLocal}</h2>
 *
 * <p>It tells dragging within the same virtual machine from dragging out of another application. In
 * the first case the data are the object itself; in the second they have to be serialized and passed
 * through the system, which is far more expensive and supports fewer formats.
 *
 * <h2>Where this library stands</h2>
 *
 * <p>{@link #reset} works. {@link #setDropTargetContext} has little to do: in the JDK it installs
 * this wrapper as the context's peer, so that accepting and rejecting from the context reach the
 * system. The {@link DropTargetContext} here has no peer -- it carries its own state -- so there is
 * nowhere to install it, and all that is left is remembering which context this is. The eleven
 * methods that do the work are abstract, and whoever uses the package implements them.
 *
 * @since 9
 */
public abstract class DropTargetContextWrapper {

    private DropTargetContext context;

    /** One. */
    public DropTargetContextWrapper() {
    }

    /**
     * Ties this wrapper to that context.
     *
     * @param dtc the context
     * @param dtcpw the wrapper serving it
     * @throws NullPointerException if the wrapper is {@code null}
     */
    public void setDropTargetContext(DropTargetContext dtc, DropTargetContextWrapper dtcpw) {
        if (dtcpw == null) {
            throw new NullPointerException("dtcpw");
        }
        this.context = dtc;
    }

    /**
     * Leaves the context ready for the next drag.
     *
     * @param dtc the context
     * @throws NullPointerException if the context is {@code null}
     */
    public void reset(DropTargetContext dtc) {
        DndAccess.reset(dtc);
        if (dtc == this.context) {
            this.context = null;
        }
    }

    /**
     * Changes which actions the target accepts.
     *
     * @param actions the actions
     */
    public abstract void setTargetActions(int actions);

    /**
     * Which actions the target accepts.
     *
     * @return the actions
     */
    public abstract int getTargetActions();

    /**
     * The target.
     *
     * @return the target
     */
    public abstract DropTarget getDropTarget();

    /**
     * Which formats the source can deliver in.
     *
     * @return the formats
     */
    public abstract DataFlavor[] getTransferDataFlavors();

    /**
     * The data being dragged.
     *
     * @return the data
     * @throws InvalidDnDOperationException if the drop has not been accepted yet
     */
    public abstract Transferable getTransferable() throws InvalidDnDOperationException;

    /**
     * Whether the data come from this same virtual machine.
     *
     * @return true if they come from here
     */
    public abstract boolean isTransferableJVMLocal();

    /**
     * Accepts the drag with that action.
     *
     * @param dragOperation the action
     */
    public abstract void acceptDrag(int dragOperation);

    /** Rejects the drag. */
    public abstract void rejectDrag();

    /**
     * Accepts the drop with that action.
     *
     * @param dropOperation the action
     */
    public abstract void acceptDrop(int dropOperation);

    /** Rejects the drop. */
    public abstract void rejectDrop();

    /**
     * Declares the drop finished.
     *
     * @param success whether the target kept the data
     */
    public abstract void dropComplete(boolean success);
}
