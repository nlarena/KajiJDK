package java.awt.dnd;

import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.List;

/**
 * Un arrastre está pasando por encima del componente, sin haberse soltado todavía.
 *
 * <p>Trae dónde está el puntero y qué formatos ofrece el origen, que es todo lo que hace falta para
 * decidir si el componente puede recibirlo. Con eso se contesta {@link #acceptDrag} o
 * {@link #rejectDrag}, y de esa respuesta sale el cursor que ve el usuario.
 *
 * <p><strong>{@link #getTransferable} está pero no da los datos de verdad</strong> durante el
 * arrastre: la especificación permite usarlo sólo para preguntar por los formatos, y pedir el
 * contenido antes de soltar es justamente lo que {@link InvalidDnDOperationException} existe para
 * señalar.
 */
public class DropTargetDragEvent extends DropTargetEvent {

    private static final long serialVersionUID = -8422265619058953682L;

    private final Point location;
    private final int actions;
    private final int dropAction;

    /**
     * Con el contexto, el punto y las acciones.
     *
     * @throws NullPointerException si falta el contexto o el punto
     * @throws IllegalArgumentException si el punto cae fuera del componente o alguna acción no es
     *     una de las de {@link DnDConstants}
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

    /** Dónde está el puntero, relativo al componente. */
    public Point getLocation() {
        return this.location;
    }

    /** En qué formatos puede entregar el origen. */
    public DataFlavor[] getCurrentDataFlavors() {
        return this.context.getCurrentDataFlavors();
    }

    /** Lo mismo, como lista. */
    public List<DataFlavor> getCurrentDataFlavorsAsList() {
        return this.context.getCurrentDataFlavorsAsList();
    }

    /** Si el origen puede entregar en ese formato. */
    public boolean isDataFlavorSupported(DataFlavor df) {
        return this.context.isDataFlavorSupported(df);
    }

    /** Todo lo que el origen acepta hacer. */
    public int getSourceActions() {
        return this.actions;
    }

    /** Qué acción propone el usuario, según las teclas que tenga apretadas. */
    public int getDropAction() {
        return this.dropAction;
    }

    /**
     * El objeto transferible, **sólo para preguntarle los formatos**.
     *
     * <p>Pedirle los datos durante el arrastre no está permitido y da
     * {@link InvalidDnDOperationException}: todavía no hay nada que entregar.
     */
    public Transferable getTransferable() {
        return this.context.getTransferable();
    }

    /**
     * Acepta el arrastre con esa acción.
     *
     * <p>Hay que llamarlo en cada {@code dragEnter} y {@code dragOver}, o el usuario ve el cursor de
     * "acá no se puede" aunque el componente sí acepte.
     */
    public void acceptDrag(int dragOperation) {
        this.context.acceptDrag(dragOperation);
    }

    /** Rechaza el arrastre. */
    public void rejectDrag() {
        this.context.rejectDrag();
    }
}
