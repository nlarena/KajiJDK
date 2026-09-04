package java.rmi.server;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Un identificador unico dentro de esta VM y su arranque.
 *
 * <h2>Como se consigue la unicidad sin coordinacion</h2>
 *
 * <p>Con tres numeros: el momento en que arranco la VM, un discriminante y un contador. La
 * combinacion no se repite <strong>en esta maquina</strong> — que es todo lo que promete, y por eso
 * un {@link ObjID} le agrega el suyo para ser unico entre maquinas.
 *
 * <p>Que el contador sea atomico no es adorno: dos hilos exportando objetos a la vez pedirian el
 * mismo numero, y dos objetos remotos con el mismo identificador es exactamente la clase de bug que
 * aparece en produccion y no en las pruebas.
 */
public final class UID implements Serializable {

    private static final long serialVersionUID = 1086053664494604050L;

    private static final AtomicInteger PROXIMO = new AtomicInteger(0);
    private static final long ARRANQUE = System.currentTimeMillis();

    private final int unique;
    private final long time;
    private final short count;

    /** Uno nuevo, unico en esta VM. */
    public UID() {
        this.unique = 0;
        this.time = ARRANQUE;
        this.count = (short) PROXIMO.getAndIncrement();
    }

    /**
     * Uno "conocido": el mismo numero da siempre el mismo identificador.
     *
     * <p>Sirve para los objetos que tienen que ser encontrables sin haberlos anunciado — el
     * registro, el recolector distribuido. Ver {@link ObjID}.
     */
    public UID(short num) {
        this.unique = 0;
        this.time = 0;
        this.count = num;
    }

    private UID(int unique, long time, short count) {
        this.unique = unique;
        this.time = time;
        this.count = count;
    }

    public int hashCode() {
        return (int) this.time + (int) this.count;
    }

    public boolean equals(Object obj) {
        if (obj instanceof UID) {
            UID o = (UID) obj;
            return this.unique == o.unique && this.count == o.count && this.time == o.time;
        }
        return false;
    }

    public String toString() {
        return Integer.toString(this.unique, 16) + ":"
                + Long.toString(this.time, 16) + ":"
                + Integer.toString(this.count, 16);
    }

    /** Lo escribe en el formato que espera {@link #read}. */
    public void write(DataOutput out) throws IOException {
        out.writeInt(this.unique);
        out.writeLong(this.time);
        out.writeShort(this.count);
    }

    /** Lo lee del formato que escribe {@link #write}. */
    public static UID read(DataInput in) throws IOException {
        int unique = in.readInt();
        long time = in.readLong();
        short count = in.readShort();
        return new UID(unique, time, count);
    }
}
