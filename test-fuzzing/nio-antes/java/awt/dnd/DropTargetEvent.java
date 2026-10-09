package java.awt.dnd;

import java.util.EventObject;

/**
 * The base of the events that reach a drop target.
 *
 * <p>The only thing it brings is the context, and it is the only thing needed: the context is where
 * the destination answers through —accept, reject, ask for the data— and where it gets to know
 * which component the drag is passing over.
 */
public class DropTargetEvent extends EventObject {

    private static final long serialVersionUID = 2821229066521922993L;

    /** Where the answering is done. */
    protected DropTargetContext context;

    /**
     * With the context of the destination.
     *
     * @throws NullPointerException if the context is `null`
     */
    public DropTargetEvent(DropTargetContext dtc) {
        super(dtc.getDropTarget());
        this.context = dtc;
    }

    /** Where to answer the drag. */
    public DropTargetContext getDropTargetContext() {
        return this.context;
    }
}
