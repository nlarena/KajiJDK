package javax.smartcardio;

import java.io.Serializable;
import java.util.Arrays;

/**
 * KajiLibrary's javax.smartcardio.ATR -- lo primero que dice una tarjeta al encenderse.
 *
 * <p>ATR es <i>Answer To Reset</i>: la tarjeta responde con una tira de bytes que declara a que
 * velocidad habla, que protocolos entiende y quien es. Es lo unico que se puede leer sin haber
 * negociado nada.
 *
 * <h2>Los bytes historicos</h2>
 *
 * <p>{@link #getHistoricalBytes} devuelve la parte del final, que es la que identifica al producto
 * --el sistema operativo de la tarjeta, el emisor--. Llegar hasta ellos no es cortar por una posicion
 * fija: hay que <b>caminar</b> el ATR, porque los bytes de interfaz que van antes son opcionales y
 * cada uno anuncia al siguiente.
 *
 * <p>El byte T0 dice cuantos bytes historicos hay (los cuatro bits de abajo) y cuales de TA1, TB1,
 * TC1 y TD1 estan presentes (los cuatro de arriba). Si TD1 esta, sus cuatro bits de arriba anuncian a
 * TA2, TB2, TC2 y TD2, y asi hasta que un TD no anuncie al siguiente. Un ATR truncado devuelve un
 * arreglo vacio en vez de reventar: lo que se leyo de una tarjeta que se saco a mitad de camino no es
 * un error de programacion.
 */
public final class ATR implements Serializable {

    private static final long serialVersionUID = 6695383790847736493L;

    /** El ATR completo, tal cual vino. */
    private final byte[] atr;

    /** Donde empiezan los bytes historicos, o -1 si el ATR esta truncado. */
    private transient int startHistorical;

    /** Cuantos bytes historicos hay. */
    private transient int nHistorical;

    /**
     * Con esos bytes. El arreglo se copia.
     *
     * @throws NullPointerException si es null
     */
    public ATR(byte[] atr) {
        this.atr = atr.clone();
        parse();
    }

    /** Camina los bytes de interfaz hasta dar con los historicos. Ver la nota de la clase. */
    private void parse() {
        this.startHistorical = -1;
        this.nHistorical = 0;
        if (this.atr.length < 2) {
            return;
        }
        int t0 = this.atr[1] & 0xFF;
        int count = t0 & 0x0F;
        int present = t0 >> 4;
        int at = 2;
        while (true) {
            int i = 0;
            while (i < 3) {
                if ((present & (1 << i)) != 0) {
                    at = at + 1;
                }
                i = i + 1;
            }
            if ((present & 8) == 0) {
                break;
            }
            if (at >= this.atr.length) {
                return;
            }
            present = (this.atr[at] & 0xFF) >> 4;
            at = at + 1;
        }
        if (at + count > this.atr.length) {
            return;
        }
        this.startHistorical = at;
        this.nHistorical = count;
    }

    /** El ATR completo. Una copia. */
    public byte[] getBytes() {
        return this.atr.clone();
    }

    /**
     * Los bytes historicos. Ver la nota de la clase.
     *
     * @return un arreglo vacio si el ATR esta truncado
     */
    public byte[] getHistoricalBytes() {
        if (this.startHistorical < 0) {
            return new byte[0];
        }
        byte[] result = new byte[this.nHistorical];
        System.arraycopy(this.atr, this.startHistorical, result, 0, this.nHistorical);
        return result;
    }

    /** Cuantos bytes tiene. */
    @Override
    public String toString() {
        return "ATR: " + this.atr.length + " bytes";
    }

    /** Dos ATR son iguales si tienen los mismos bytes. */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ATR)) {
            return false;
        }
        return Arrays.equals(this.atr, ((ATR) obj).atr);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.atr);
    }

    /** Al leerse de un flujo hay que volver a caminar el ATR: la posicion no se serializa. */
    private void readObject(java.io.ObjectInputStream in)
            throws java.io.IOException, ClassNotFoundException {
        in.defaultReadObject();
        parse();
    }
}
