package java.awt.dnd;

import java.awt.Component;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * The channel through which a destination answers a drag under way.
 *
 * <p>The events that reach the destination are read-only: they say what is happening. This object
 * is the other direction — accept, reject, ask for the data, declare the drop finished. The events
 * delegate everything here, so in practice it is used through them.
 *
 * <p>Almost all of its methods are **protected** on purpose: the normal way of answering is through
 * the event, and exposing them publicly would invite answering outside the sequence the drag
 * expects.
 *
 * <p>The context keeps count of where in that sequence it is, and out of that come the
 * {@link InvalidDnDOperationException}s: asking for the data without having accepted, or declaring
 * it finished twice.
 */
public final class DropTargetContext implements Serializable {

    private static final long serialVersionUID = -634158968993743371L;

    private final DropTarget dropTarget;
    private int targetActions = DnDConstants.ACTION_NONE;
    private transient Transferable transferable;
    private transient boolean dropAccepted;
    private transient boolean dropCompleted;

    /** It is created by the destination; it is not instantiated from outside. */
    DropTargetContext(DropTarget dt) {
        this.dropTarget = dt;
    }

    /** The destination it belongs to. */
    public DropTarget getDropTarget() {
        return this.dropTarget;
    }

    /** The component the drag is passing over. */
    public Component getComponent() {
        return this.dropTarget.getComponent();
    }

    /** Goes back to the initial state, between one drag and the next. */
    void reset() {
        this.transferable = null;
        this.dropAccepted = false;
        this.dropCompleted = false;
        this.targetActions = DnDConstants.ACTION_NONE;
    }

    /** Puts on it the data the drag system has got hold of. */
    void setTransferable(Transferable t) {
        this.transferable = t;
    }

    /** Changes which actions the destination accepts. */
    protected void setTargetActions(int actions) {
        this.targetActions = actions;
        this.dropTarget.doSetDefaultActions(actions);
    }

    /** Which actions the destination accepts. */
    protected int getTargetActions() {
        return this.targetActions;
    }

    /**
     * Declares the drop finished.
     *
     * <p>It is what tells the source whether it has to delete the original. Calling it twice is a
     * sequence error and not an idempotence.
     *
     * @throws InvalidDnDOperationException if it had been called already
     */
    public void dropComplete(boolean success) throws InvalidDnDOperationException {
        if (this.dropCompleted) {
            throw new InvalidDnDOperationException("drop has already been completed");
        }
        this.dropCompleted = true;
        this.transferable = null;
    }

    /** Accepts the drag with that action. */
    protected void acceptDrag(int dragOperation) {
        this.targetActions = dragOperation;
    }

    /** Rejects the drag. */
    protected void rejectDrag() {
        this.targetActions = DnDConstants.ACTION_NONE;
    }

    /**
     * Accepts the drop with that action.
     *
     * <p>It is what enables {@link #getTransferable} to deliver the data.
     */
    protected void acceptDrop(int dropOperation) {
        this.targetActions = dropOperation;
        this.dropAccepted = true;
    }

    /** Rejects the drop. */
    protected void rejectDrop() {
        this.targetActions = DnDConstants.ACTION_NONE;
        this.dropAccepted = false;
    }

    /** In which formats the source can deliver. */
    protected DataFlavor[] getCurrentDataFlavors() {
        if (this.transferable == null) {
            return new DataFlavor[0];
        }
        return this.transferable.getTransferDataFlavors();
    }

    /** The same, as a list. */
    protected List<DataFlavor> getCurrentDataFlavorsAsList() {
        DataFlavor[] fs = this.getCurrentDataFlavors();
        List<DataFlavor> out = new ArrayList<DataFlavor>(fs.length);
        for (int i = 0; i < fs.length; i++) {
            out.add(fs[i]);
        }
        return out;
    }

    /** Whether the source can deliver in that format. */
    protected boolean isDataFlavorSupported(DataFlavor df) {
        return this.getCurrentDataFlavorsAsList().contains(df);
    }

    /**
     * The data that are being dragged.
     *
     * @throws InvalidDnDOperationException if the drop has not been accepted yet, or if the drag is
     *     over already: in both cases there are no data to deliver
     */
    protected Transferable getTransferable() throws InvalidDnDOperationException {
        if (!this.dropAccepted) {
            throw new InvalidDnDOperationException("No drop current");
        }
        if (this.transferable == null) {
            throw new InvalidDnDOperationException("No data available");
        }
        return this.transferable;
    }

    /**
     * Wraps a transferable so that it stops serving when the drag finishes.
     *
     * <p>Without the wrapping, whoever kept the transferable could read it long afterwards, when
     * the data of the source no longer exist. The proxy is what makes that attempt fail instead of
     * returning rubbish.
     */
    protected Transferable createTransferableProxy(Transferable t, boolean local) {
        return new TransferableProxy(t, local);
    }

    /** The wrapped transferable that stops serving when the drag finishes. */
    private final class TransferableProxy implements Transferable {

        private final Transferable transferable;
        private final boolean isLocal;

        TransferableProxy(Transferable t, boolean local) {
            this.transferable = t;
            this.isLocal = local;
        }

        public DataFlavor[] getTransferDataFlavors() {
            return this.transferable.getTransferDataFlavors();
        }

        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return this.transferable.isDataFlavorSupported(flavor);
        }

        public Object getTransferData(DataFlavor df)
                throws java.awt.datatransfer.UnsupportedFlavorException, IOException {
            if (DropTargetContext.this.dropCompleted) {
                throw new InvalidDnDOperationException("drop has already been completed");
            }
            return this.transferable.getTransferData(df);
        }
    }
}
