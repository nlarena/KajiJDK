package java.awt.dnd;

import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.List;

/**
 * Se soltó algo sobre el componente.
 *
 * <p>Es el único momento en que {@link #getTransferable} entrega datos de verdad, y aun así hay un
 * orden que respetar: **primero {@link #acceptDrop}, después los datos, y al final
 * {@link #dropComplete}**. Saltearse el primero da {@link InvalidDnDOperationException}, y olvidarse
 * del último deja al origen esperando para siempre sin saber si tiene que borrar el original.
 *
 * <p>{@link #isLocalTransfer} distingue un arrastre dentro de la misma máquina virtual de uno que
 * vino de otro programa. Importa porque en el primer caso los datos son el objeto mismo y en el
 * segundo pasaron por el portapapeles del sistema y se serializaron.
 */
public class DropTargetDropEvent extends DropTargetEvent {

    private static final long serialVersionUID = -1721911170440459322L;

    private final Point location;
    private final int actions;
    private final int dropAction;
    private final boolean isLocalTx;

    /**
     * Con el contexto, el punto y las acciones; transferencia entre programas.
     *
     * @throws NullPointerException si falta el contexto o el punto
     * @throws IllegalArgumentException si alguna acción no es válida
     */
    public DropTargetDropEvent(DropTargetContext dtc, Point cursorLocn, int dropAction,
            int srcActions) {
        this(dtc, cursorLocn, dropAction, srcActions, false);
    }

    /**
     * Como el anterior, diciendo si el arrastre viene de la misma máquina virtual.
     *
     * @throws NullPointerException si falta el contexto o el punto
     * @throws IllegalArgumentException si alguna acción no es válida
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

    /** Dónde se soltó, relativo al componente. */
    public Point getLocation() {
        return this.location;
    }

    /** En qué formatos se puede entregar. */
    public DataFlavor[] getCurrentDataFlavors() {
        return this.context.getCurrentDataFlavors();
    }

    /** Lo mismo, como lista. */
    public List<DataFlavor> getCurrentDataFlavorsAsList() {
        return this.context.getCurrentDataFlavorsAsList();
    }

    /** Si se puede entregar en ese formato. */
    public boolean isDataFlavorSupported(DataFlavor df) {
        return this.context.isDataFlavorSupported(df);
    }

    /** Todo lo que el origen acepta hacer. */
    public int getSourceActions() {
        return this.actions;
    }

    /** Qué acción eligió el usuario. */
    public int getDropAction() {
        return this.dropAction;
    }

    /**
     * Los datos.
     *
     * @throws InvalidDnDOperationException si no se llamó antes a {@link #acceptDrop}
     */
    public Transferable getTransferable() {
        return this.context.getTransferable();
    }

    /**
     * Acepta el soltado con esa acción.
     *
     * <p>Hay que llamarlo **antes** de pedir los datos.
     */
    public void acceptDrop(int dropAction) {
        this.context.acceptDrop(dropAction);
    }

    /** Rechaza el soltado. */
    public void rejectDrop() {
        this.context.rejectDrop();
    }

    /**
     * Avisa que se terminó de recibir, y si salió bien.
     *
     * <p>Es lo que le dice al origen si tiene que borrar el original. Olvidarlo deja el arrastre a
     * medio terminar del otro lado.
     */
    public void dropComplete(boolean success) {
        this.context.dropComplete(success);
    }

    /** Si el arrastre viene de esta misma máquina virtual. */
    public boolean isLocalTransfer() {
        return this.isLocalTx;
    }
}
