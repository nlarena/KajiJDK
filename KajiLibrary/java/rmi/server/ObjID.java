package java.rmi.server;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

/**
 * El identificador de un objeto remoto dentro de su VM.
 *
 * <h2>Los tres identificadores conocidos</h2>
 *
 * <p>{@link #REGISTRY_ID}, {@link #ACTIVATOR_ID} y {@link #DGC_ID} son fijos, y tienen que serlo:
 * un cliente que busca el registro no puede preguntarle a nadie cual es su identificador, porque
 * preguntar ya requiere el registro. Es el problema del arranque en frio, y se resuelve fijando de
 * antemano los tres identificadores que hacen falta antes de poder averiguar nada.
 *
 * <p>Todos los demas salen del constructor sin argumentos, que usa un {@link UID} para no repetirse.
 */
public final class ObjID implements Serializable {

    private static final long serialVersionUID = -6386392263968365220L;

    /** El registro RMI. */
    public static final int REGISTRY_ID = 0;

    /** El activador. */
    public static final int ACTIVATOR_ID = 1;

    /** El recolector distribuido. */
    public static final int DGC_ID = 2;

    private static final AtomicLong PROXIMO = new AtomicLong(0);

    private final long objNum;
    private final UID space;

    /** Uno nuevo, unico. */
    public ObjID() {
        this.objNum = PROXIMO.getAndIncrement();
        this.space = new UID();
    }

    /**
     * Uno de los conocidos.
     *
     * <p>El {@link UID} que lleva es el "conocido" —el de {@link UID#UID(short)} con cero— y no uno
     * nuevo: es lo que hace que dos VMs distintas construyan el mismo identificador.
     */
    public ObjID(int num) {
        this.objNum = num;
        this.space = new UID((short) 0);
    }

    private ObjID(long objNum, UID space) {
        this.objNum = objNum;
        this.space = space;
    }

    /** Lo escribe en el formato que espera {@link #read}. */
    public void write(ObjectOutput out) throws IOException {
        out.writeLong(this.objNum);
        this.space.write(out);
    }

    /** Lo lee del formato que escribe {@link #write}. */
    public static ObjID read(ObjectInput in) throws IOException {
        long num = in.readLong();
        UID space = UID.read(in);
        return new ObjID(num, space);
    }

    public int hashCode() {
        return (int) this.objNum;
    }

    public boolean equals(Object obj) {
        if (obj instanceof ObjID) {
            ObjID o = (ObjID) obj;
            return this.objNum == o.objNum && this.space.equals(o.space);
        }
        return false;
    }

    public String toString() {
        return "[" + this.space.toString() + ", " + String.valueOf(this.objNum) + "]";
    }
}
