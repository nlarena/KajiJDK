package jdk.swing.interop;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.AccesoDnD;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetContext;
import java.awt.dnd.InvalidDnDOperationException;

/**
 * El lado del destino de un arrastre, visto desde otro juego de herramientas graficas.
 *
 * <h2>La secuencia</h2>
 *
 * <p>Un arrastre que pasa por encima de un componente es una negociacion: el sistema avisa que hay
 * algo encima, el destino contesta si lo acepta y con que accion, y recien cuando el usuario suelta
 * y el destino acepto se transfieren los datos. Los metodos de aca son esa negociacion vista del
 * lado del que recibe.
 *
 * <p>El orden importa: {@link #getTransferable} antes de haber aceptado tira, y
 * {@link #dropComplete} dos veces tambien. No es rigidez sino la unica forma de que el origen sepa
 * cuando puede borrar lo que arrastro.
 *
 * <h2>{@link #isTransferableJVMLocal}</h2>
 *
 * <p>Distingue arrastrar dentro de la misma maquina virtual de arrastrar desde otra aplicacion. En
 * el primer caso los datos son el objeto mismo; en el segundo hay que serializarlos y pasarlos por
 * el sistema, que es mucho mas caro y soporta menos formatos.
 *
 * <h2>Estado en esta biblioteca</h2>
 *
 * <p>{@link #reset} funciona. {@link #setDropTargetContext} no tiene mucho que hacer: en el JDK
 * instala este envoltorio como par del contexto, para que aceptar y rechazar desde el contexto
 * lleguen hasta el sistema. El {@link DropTargetContext} de aca no tiene par --se lleva su propio
 * estado-- asi que no hay donde instalarlo, y lo unico que queda es recordar de que contexto se
 * trata. Los once metodos que hacen el trabajo son abstractos, y los implementa quien use el
 * paquete.
 *
 * @since 9
 */
public abstract class DropTargetContextWrapper {

    private DropTargetContext contexto;

    /** Uno. */
    public DropTargetContextWrapper() {
    }

    /**
     * Ata este envoltorio a ese contexto.
     *
     * @param dtc el contexto
     * @param dtcpw el envoltorio que lo atiende
     * @throws NullPointerException si el envoltorio es {@code null}
     */
    public void setDropTargetContext(DropTargetContext dtc, DropTargetContextWrapper dtcpw) {
        if (dtcpw == null) {
            throw new NullPointerException("dtcpw");
        }
        this.contexto = dtc;
    }

    /**
     * Deja el contexto listo para el arrastre siguiente.
     *
     * @param dtc el contexto
     * @throws NullPointerException si el contexto es {@code null}
     */
    public void reset(DropTargetContext dtc) {
        AccesoDnD.reiniciar(dtc);
        if (dtc == this.contexto) {
            this.contexto = null;
        }
    }

    /**
     * Cambia que acciones acepta el destino.
     *
     * @param actions las acciones
     */
    public abstract void setTargetActions(int actions);

    /**
     * Que acciones acepta el destino.
     *
     * @return las acciones
     */
    public abstract int getTargetActions();

    /**
     * El destino.
     *
     * @return el destino
     */
    public abstract DropTarget getDropTarget();

    /**
     * En que formatos puede entregar el origen.
     *
     * @return los formatos
     */
    public abstract DataFlavor[] getTransferDataFlavors();

    /**
     * Los datos que se estan arrastrando.
     *
     * @return los datos
     * @throws InvalidDnDOperationException si todavia no se acepto el soltado
     */
    public abstract Transferable getTransferable() throws InvalidDnDOperationException;

    /**
     * Si los datos vienen de esta misma maquina virtual.
     *
     * @return cierto si vienen de aca
     */
    public abstract boolean isTransferableJVMLocal();

    /**
     * Acepta el arrastre con esa accion.
     *
     * @param dragOperation la accion
     */
    public abstract void acceptDrag(int dragOperation);

    /** Rechaza el arrastre. */
    public abstract void rejectDrag();

    /**
     * Acepta el soltado con esa accion.
     *
     * @param dropOperation la accion
     */
    public abstract void acceptDrop(int dropOperation);

    /** Rechaza el soltado. */
    public abstract void rejectDrop();

    /**
     * Da por terminado el soltado.
     *
     * @param success si el destino se quedo con los datos
     */
    public abstract void dropComplete(boolean success);
}
