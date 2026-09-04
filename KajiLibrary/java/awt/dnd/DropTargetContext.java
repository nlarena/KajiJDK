package java.awt.dnd;

import java.awt.Component;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * El canal por el que un destino le contesta a un arrastre en curso.
 *
 * <p>Los eventos que le llegan al destino son de sólo lectura: dicen qué está pasando. Este objeto es
 * el otro sentido — aceptar, rechazar, pedir los datos, dar por terminado el soltado. Los eventos
 * delegan todo acá, así que en la práctica se lo usa a través de ellos.
 *
 * <p>Casi todos sus métodos son **protegidos** a propósito: la manera normal de contestar es por el
 * evento, y exponerlos públicamente invitaría a contestar por fuera de la secuencia que el arrastre
 * espera.
 *
 * <p>El contexto lleva la cuenta de en qué punto de esa secuencia está, y de ahí salen las
 * {@link InvalidDnDOperationException}: pedir los datos sin haber aceptado, o darlo por terminado
 * dos veces.
 */
public final class DropTargetContext implements Serializable {

    private static final long serialVersionUID = -634158968993743371L;

    private final DropTarget dropTarget;
    private int targetActions = DnDConstants.ACTION_NONE;
    private transient Transferable transferable;
    private transient boolean dropAccepted;
    private transient boolean dropCompleted;

    /** Lo crea el destino; no se instancia desde afuera. */
    DropTargetContext(DropTarget dt) {
        this.dropTarget = dt;
    }

    /** El destino al que pertenece. */
    public DropTarget getDropTarget() {
        return this.dropTarget;
    }

    /** El componente sobre el que está pasando el arrastre. */
    public Component getComponent() {
        return this.dropTarget.getComponent();
    }

    /** Vuelve al estado inicial, entre un arrastre y el siguiente. */
    void reset() {
        this.transferable = null;
        this.dropAccepted = false;
        this.dropCompleted = false;
        this.targetActions = DnDConstants.ACTION_NONE;
    }

    /** Le pone los datos que el sistema de arrastre haya conseguido. */
    void setTransferable(Transferable t) {
        this.transferable = t;
    }

    /** Cambia qué acciones acepta el destino. */
    protected void setTargetActions(int actions) {
        this.targetActions = actions;
        this.dropTarget.doSetDefaultActions(actions);
    }

    /** Qué acciones acepta el destino. */
    protected int getTargetActions() {
        return this.targetActions;
    }

    /**
     * Da por terminado el soltado.
     *
     * <p>Es lo que le dice al origen si tiene que borrar el original. Llamarlo dos veces es un error
     * de secuencia y no una idempotencia.
     *
     * @throws InvalidDnDOperationException si ya se había llamado
     */
    public void dropComplete(boolean success) throws InvalidDnDOperationException {
        if (this.dropCompleted) {
            throw new InvalidDnDOperationException("drop has already been completed");
        }
        this.dropCompleted = true;
        this.transferable = null;
    }

    /** Acepta el arrastre con esa acción. */
    protected void acceptDrag(int dragOperation) {
        this.targetActions = dragOperation;
    }

    /** Rechaza el arrastre. */
    protected void rejectDrag() {
        this.targetActions = DnDConstants.ACTION_NONE;
    }

    /**
     * Acepta el soltado con esa acción.
     *
     * <p>Es lo que habilita a {@link #getTransferable} a entregar los datos.
     */
    protected void acceptDrop(int dropOperation) {
        this.targetActions = dropOperation;
        this.dropAccepted = true;
    }

    /** Rechaza el soltado. */
    protected void rejectDrop() {
        this.targetActions = DnDConstants.ACTION_NONE;
        this.dropAccepted = false;
    }

    /** En qué formatos puede entregar el origen. */
    protected DataFlavor[] getCurrentDataFlavors() {
        if (this.transferable == null) {
            return new DataFlavor[0];
        }
        return this.transferable.getTransferDataFlavors();
    }

    /** Lo mismo, como lista. */
    protected List<DataFlavor> getCurrentDataFlavorsAsList() {
        DataFlavor[] fs = this.getCurrentDataFlavors();
        List<DataFlavor> out = new ArrayList<DataFlavor>(fs.length);
        for (int i = 0; i < fs.length; i++) {
            out.add(fs[i]);
        }
        return out;
    }

    /** Si el origen puede entregar en ese formato. */
    protected boolean isDataFlavorSupported(DataFlavor df) {
        return this.getCurrentDataFlavorsAsList().contains(df);
    }

    /**
     * Los datos que se están arrastrando.
     *
     * @throws InvalidDnDOperationException si todavía no se aceptó el soltado, o si el arrastre ya
     *     terminó: en los dos casos no hay datos que entregar
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
     * Envuelve un transferible para que deje de servir cuando el arrastre termine.
     *
     * <p>Sin la envoltura, quien se guarde el transferible podría leerlo mucho después, cuando los
     * datos del origen ya no existen. El proxy es lo que hace que ese intento falle en vez de
     * devolver basura.
     */
    protected Transferable createTransferableProxy(Transferable t, boolean local) {
        return new TransferableProxy(t, local);
    }

    /** El transferible envuelto que deja de servir al terminar el arrastre. */
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
