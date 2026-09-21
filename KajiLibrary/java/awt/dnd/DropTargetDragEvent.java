package java.awt.dnd;

import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.List;

/**
 * A drag is passing over the component, without having been dropped yet.
 *
 * <p>It brings where the pointer is and which formats the source offers, which is all that is
 * needed to decide whether the component can receive it. With that one answers {@link #acceptDrag}
 * or {@link #rejectDrag}, and out of that answer comes the cursor the user sees.
 *
 * <p><strong>{@link #getTransferable} is there but does not give the real data</strong> during the
 * drag: the specification allows using it only for asking about the formats, and asking for the
 * contents before dropping is exactly what {@link InvalidDnDOperationException} exists to signal.
 */
public class DropTargetDragEvent extends DropTargetEvent {

    private static final long serialVersionUID = -8422265619058953682L;

    private final Point location;
    private final int actions;
    private final int dropAction;

    /**
     * With the context, the point and the actions.
     *
     * @throws NullPointerException if the context or the point is missing
     * @throws IllegalArgumentException if the point falls outside the component or one of the
     *     actions is not one of those of {@link DnDConstants}
     */
    public DropTargetDragEvent(DropTargetContext dtc, Point cursorLocn, int dropAction,
            int srcActions) {
        super(dtc);
        if (cursorLocn == null) {
            throw new NullPointerException("cursorLocn");
        }
        if (dropAction != DnDConstants.ACTION_NONE && dropAction != DnDConstants.ACTION_COPY
                && dropAction != DnDConstants.ACTION_MOVE
                && dropAction != DnDConstants.ACTION_LINK) {
            throw new IllegalArgumentException("dropAction" + dropAction);
        }
        if ((srcActions & ~(DnDConstants.ACTION_COPY_OR_MOVE | DnDConstants.ACTION_LINK)) != 0) {
            throw new IllegalArgumentException("srcActions");
        }
        this.location = cursorLocn;
        this.actions = srcActions;
        this.dropAction = dropAction;
    }

    /** Where the pointer is, relative to the component. */
    public Point getLocation() {
        return this.location;
    }

    /** In which formats the source can deliver. */
    public DataFlavor[] getCurrentDataFlavors() {
        return this.context.getCurrentDataFlavors();
    }

    /** The same, as a list. */
    public List<DataFlavor> getCurrentDataFlavorsAsList() {
        return this.context.getCurrentDataFlavorsAsList();
    }

    /** Whether the source can deliver in that format. */
    public boolean isDataFlavorSupported(DataFlavor df) {
        return this.context.isDataFlavorSupported(df);
    }

    /** Everything the source accepts doing. */
    public int getSourceActions() {
        return this.actions;
    }

    /** Which action the user proposes, according to the keys they have held down. */
    public int getDropAction() {
        return this.dropAction;
    }

    /**
     * The transferable object, **only for asking it for the formats**.
     *
     * <p>Asking it for the data during the drag is not allowed and gives
     * {@link InvalidDnDOperationException}: there is nothing to deliver yet.
     */
    public Transferable getTransferable() {
        return this.context.getTransferable();
    }

    /**
     * Accepts the drag with that action.
     *
     * <p>It has to be called in every {@code dragEnter} and {@code dragOver}, or the user sees the
     * "not here" cursor even though the component does accept.
     */
    public void acceptDrag(int dragOperation) {
        this.context.acceptDrag(dragOperation);
    }

    /** Rejects the drag. */
    public void rejectDrag() {
        this.context.rejectDrag();
    }
}
