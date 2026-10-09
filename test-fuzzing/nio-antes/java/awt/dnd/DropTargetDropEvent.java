package java.awt.dnd;

import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.List;

/**
 * Something was dropped over the component.
 *
 * <p>It is the only moment when {@link #getTransferable} delivers real data, and even then there is
 * an order to respect: **first {@link #acceptDrop}, then the data, and at the end {@link
 * #dropComplete}**. Skipping the first gives {@link InvalidDnDOperationException}, and forgetting
 * the last leaves the source waiting forever without knowing whether it has to delete the original.
 *
 * <p>{@link #isLocalTransfer} tells a drag inside the same virtual machine apart from one that came
 * from another program. It matters because in the first case the data are the object itself and in
 * the second they went through the system clipboard and were serialised.
 */
public class DropTargetDropEvent extends DropTargetEvent {

    private static final long serialVersionUID = -1721911170440459322L;

    private final Point location;
    private final int actions;
    private final int dropAction;
    private final boolean isLocalTx;

    /**
     * With the context, the point and the actions; transfer between programs.
     *
     * @throws NullPointerException if the context or the point is missing
     * @throws IllegalArgumentException if one of the actions is not valid
     */
    public DropTargetDropEvent(DropTargetContext dtc, Point cursorLocn, int dropAction,
            int srcActions) {
        this(dtc, cursorLocn, dropAction, srcActions, false);
    }

    /**
     * Like the previous one, saying whether the drag comes from the same virtual machine.
     *
     * @throws NullPointerException if the context or the point is missing
     * @throws IllegalArgumentException if one of the actions is not valid
     */
    public DropTargetDropEvent(DropTargetContext dtc, Point cursorLocn, int dropAction,
            int srcActions, boolean isLocal) {
        super(dtc);
        if (cursorLocn == null) {
            throw new NullPointerException("cursorLocn");
        }
        if (dropAction != DnDConstants.ACTION_NONE && dropAction != DnDConstants.ACTION_COPY
                && dropAction != DnDConstants.ACTION_MOVE
                && dropAction != DnDConstants.ACTION_LINK) {
            throw new IllegalArgumentException("dropAction = " + dropAction);
        }
        if ((srcActions & ~(DnDConstants.ACTION_COPY_OR_MOVE | DnDConstants.ACTION_LINK)) != 0) {
            throw new IllegalArgumentException("srcActions = " + srcActions);
        }
        this.location = cursorLocn;
        this.actions = srcActions;
        this.dropAction = dropAction;
        this.isLocalTx = isLocal;
    }

    /** Where it was dropped, relative to the component. */
    public Point getLocation() {
        return this.location;
    }

    /** In which formats it can be delivered. */
    public DataFlavor[] getCurrentDataFlavors() {
        return this.context.getCurrentDataFlavors();
    }

    /** The same, as a list. */
    public List<DataFlavor> getCurrentDataFlavorsAsList() {
        return this.context.getCurrentDataFlavorsAsList();
    }

    /** Whether it can be delivered in that format. */
    public boolean isDataFlavorSupported(DataFlavor df) {
        return this.context.isDataFlavorSupported(df);
    }

    /** Everything the source accepts doing. */
    public int getSourceActions() {
        return this.actions;
    }

    /** Which action the user chose. */
    public int getDropAction() {
        return this.dropAction;
    }

    /**
     * The data.
     *
     * @throws InvalidDnDOperationException if {@link #acceptDrop} was not called first
     */
    public Transferable getTransferable() {
        return this.context.getTransferable();
    }

    /**
     * Accepts the drop with that action.
     *
     * <p>It has to be called **before** asking for the data.
     */
    public void acceptDrop(int dropAction) {
        this.context.acceptDrop(dropAction);
    }

    /** Rejects the drop. */
    public void rejectDrop() {
        this.context.rejectDrop();
    }

    /**
     * Tells that the receiving is finished, and whether it went well.
     *
     * <p>It is what tells the source whether it has to delete the original. Forgetting it leaves
     * the drag half finished on the other side.
     */
    public void dropComplete(boolean success) {
        this.context.dropComplete(success);
    }

    /** Whether the drag comes from this same virtual machine. */
    public boolean isLocalTransfer() {
        return this.isLocalTx;
    }
}
