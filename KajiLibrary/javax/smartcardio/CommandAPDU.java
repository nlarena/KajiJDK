package javax.smartcardio;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * KajiLibrary's javax.smartcardio.CommandAPDU -- una orden para la tarjeta.
 *
 * <p>Toda orden empieza con cuatro bytes: clase, instruccion y dos parametros. Despues puede llevar
 * datos y puede pedir una respuesta de cierto tamano, y de esas dos opciones salen los cuatro casos
 * de la norma ISO 7816-4:
 *
 * <table border="1">
 * <caption>Los cuatro casos</caption>
 * <tr><th>caso</th><th>datos (Nc)</th><th>respuesta (Ne)</th><th>largo</th></tr>
 * <tr><td>1</td><td>no</td><td>no</td><td>4</td></tr>
 * <tr><td>2</td><td>no</td><td>si</td><td>5 o 7</td></tr>
 * <tr><td>3</td><td>si</td><td>no</td><td>4 + 1 + Nc, o 4 + 3 + Nc</td></tr>
 * <tr><td>4</td><td>si</td><td>si</td><td>4 + 1 + Nc + 1, o 4 + 3 + Nc + 2</td></tr>
 * </table>
 *
 * <h2>Corto y extendido</h2>
 *
 * <p>Un byte no alcanza para decir 300, asi que la norma tiene dos codificaciones. En la corta el
 * largo va en un byte; en la extendida va en tres --un cero y despues dos bytes-- y la orden entera
 * cambia de forma.
 *
 * <p>La regla del cero es la que hay que tener presente: un byte de largo en cero <b>no</b> significa
 * cero. Como Le significa 256, y como marca del principio de un largo extendido significa que siguen
 * dos bytes mas. Un cero literal no se puede escribir, y por eso pedir Ne igual a cero es lo mismo
 * que no pedir respuesta.
 *
 * <p>Esta clase elige la codificacion sola: usa la corta mientras entre, y pasa a la extendida cuando
 * los datos pasan de 255 bytes o la respuesta pedida pasa de 256. Al construir desde bytes ya
 * armados, en cambio, {@link #getBytes} devuelve exactamente los que se le dieron, sin recodificar.
 */
public final class CommandAPDU implements Serializable {

    private static final long serialVersionUID = 398698301286670877L;

    /** El maximo que entra en la codificacion corta de los datos. */
    private static final int MAX_APDU_SIZE_SHORT = 255;

    /** La orden completa, tal como va al lector. */
    private byte[] apdu;

    /** Donde empiezan los datos dentro de {@link #apdu}. */
    private transient int dataOffset;

    /** Cuantos bytes de datos lleva. */
    private transient int nc;

    /** Cuantos bytes de respuesta pide. */
    private transient int ne;

    /**
     * Desde una orden ya armada. Los bytes se copian y se interpretan para sacar Nc y Ne.
     *
     * @throws NullPointerException si es null
     * @throws IllegalArgumentException si mide menos de cuatro bytes o no es ninguno de los cuatro
     *     casos
     */
    public CommandAPDU(byte[] apdu) {
        this.apdu = apdu.clone();
        parse();
    }

    /**
     * Desde un pedazo de un arreglo.
     *
     * @throws IndexOutOfBoundsException si el pedazo se sale del arreglo
     */
    public CommandAPDU(byte[] apdu, int apduOffset, int apduLength) {
        checkRange(apdu.length, apduOffset, apduLength);
        this.apdu = new byte[apduLength];
        System.arraycopy(apdu, apduOffset, this.apdu, 0, apduLength);
        parse();
    }

    /**
     * Desde lo que quede por leer de un buffer.
     *
     * <p>Consume el buffer: al volver, su posicion queda en el limite.
     */
    public CommandAPDU(ByteBuffer apdu) {
        this.apdu = new byte[apdu.remaining()];
        apdu.get(this.apdu);
        parse();
    }

    /** Caso 1: sin datos y sin respuesta. */
    public CommandAPDU(int cla, int ins, int p1, int p2) {
        this(cla, ins, p1, p2, null, 0, 0, 0);
    }

    /** Caso 2: pide {@code ne} bytes de respuesta. */
    public CommandAPDU(int cla, int ins, int p1, int p2, int ne) {
        this(cla, ins, p1, p2, null, 0, 0, ne);
    }

    /** Caso 3: manda datos y no espera respuesta. */
    public CommandAPDU(int cla, int ins, int p1, int p2, byte[] data) {
        this(cla, ins, p1, p2, data, 0, arrayLength(data), 0);
    }

    /** Caso 3, con los datos en un pedazo del arreglo. */
    public CommandAPDU(int cla, int ins, int p1, int p2, byte[] data, int dataOffset,
                       int dataLength) {
        this(cla, ins, p1, p2, data, dataOffset, dataLength, 0);
    }

    /** Caso 4: manda datos y pide respuesta. */
    public CommandAPDU(int cla, int ins, int p1, int p2, byte[] data, int ne) {
        this(cla, ins, p1, p2, data, 0, arrayLength(data), ne);
    }

    /**
     * Caso 4, con los datos en un pedazo del arreglo. Es el constructor al que van a parar todos.
     *
     * @throws IllegalArgumentException si {@code ne} es negativo o pasa de 65536, o si los datos
     *     pasan de 65535 bytes
     * @throws IndexOutOfBoundsException si el pedazo se sale del arreglo
     */
    public CommandAPDU(int cla, int ins, int p1, int p2, byte[] data, int dataOffset,
                       int dataLength, int ne) {
        if (dataLength < 0) {
            throw new IllegalArgumentException("dataLength must not be negative");
        }
        if (ne < 0) {
            throw new IllegalArgumentException("ne must not be negative");
        }
        if (ne > 65536) {
            throw new IllegalArgumentException("ne is too large");
        }
        if (dataLength > 65535) {
            throw new IllegalArgumentException("dataLength is too large");
        }
        if (data != null) {
            checkRange(data.length, dataOffset, dataLength);
        } else if (dataLength != 0) {
            throw new IllegalArgumentException("dataLength must be 0 if data is null");
        }
        this.nc = dataLength;
        this.ne = ne;
        build(cla, ins, p1, p2, data, dataOffset);
    }

    /** Arma los bytes eligiendo codificacion. Ver la nota de la clase. */
    private void build(int cla, int ins, int p1, int p2, byte[] data, int dataOffset) {
        boolean extended = this.nc > MAX_APDU_SIZE_SHORT || this.ne > 256;
        int length = 4;
        if (this.nc > 0) {
            length = length + (extended ? 3 : 1) + this.nc;
        }
        if (this.ne > 0) {
            if (this.nc > 0) {
                length = length + (extended ? 2 : 1);
            } else {
                length = length + (extended ? 3 : 1);
            }
        }
        this.apdu = new byte[length];
        this.apdu[0] = (byte) cla;
        this.apdu[1] = (byte) ins;
        this.apdu[2] = (byte) p1;
        this.apdu[3] = (byte) p2;
        int at = 4;
        if (this.nc > 0) {
            if (extended) {
                this.apdu[at] = 0;
                this.apdu[at + 1] = (byte) (this.nc >> 8);
                this.apdu[at + 2] = (byte) this.nc;
                at = at + 3;
            } else {
                this.apdu[at] = (byte) this.nc;
                at = at + 1;
            }
            this.dataOffset = at;
            System.arraycopy(data, dataOffset, this.apdu, at, this.nc);
            at = at + this.nc;
        } else {
            this.dataOffset = at;
        }
        if (this.ne > 0) {
            if (extended) {
                if (this.nc == 0) {
                    this.apdu[at] = 0;
                    at = at + 1;
                }
                // 65536 se escribe como dos ceros: el cero literal no hace falta, porque pedir cero
                // bytes es lo mismo que no pedir nada.
                this.apdu[at] = (byte) (this.ne >> 8);
                this.apdu[at + 1] = (byte) this.ne;
            } else {
                this.apdu[at] = (byte) this.ne;
            }
        }
    }

    /** Interpreta unos bytes ya armados para sacar Nc y Ne. Ver la nota de la clase. */
    private void parse() {
        if (this.apdu.length < 4) {
            throw new IllegalArgumentException("apdu must be at least 4 bytes long");
        }
        if (this.apdu.length == 4) {
            this.nc = 0;
            this.ne = 0;
            this.dataOffset = 4;
            return;
        }
        int first = this.apdu[4] & 0xFF;
        if (this.apdu.length == 5) {
            this.nc = 0;
            this.dataOffset = 5;
            this.ne = first == 0 ? 256 : first;
            return;
        }
        if (first != 0) {
            if (this.apdu.length == 4 + 1 + first) {
                this.nc = first;
                this.ne = 0;
                this.dataOffset = 5;
                return;
            }
            if (this.apdu.length == 4 + 2 + first) {
                this.nc = first;
                this.dataOffset = 5;
                int le = this.apdu[this.apdu.length - 1] & 0xFF;
                this.ne = le == 0 ? 256 : le;
                return;
            }
            throw new IllegalArgumentException("Invalid APDU: length=" + this.apdu.length
                + ", b1=" + first);
        }
        if (this.apdu.length < 7) {
            throw new IllegalArgumentException("Invalid APDU: length=" + this.apdu.length
                + ", b1=" + first);
        }
        int extended = ((this.apdu[5] & 0xFF) << 8) | (this.apdu[6] & 0xFF);
        if (this.apdu.length == 7) {
            this.nc = 0;
            this.dataOffset = 7;
            this.ne = extended == 0 ? 65536 : extended;
            return;
        }
        if (extended == 0) {
            throw new IllegalArgumentException("Invalid APDU: length=" + this.apdu.length
                + ", b1=" + first);
        }
        if (this.apdu.length == 7 + extended) {
            this.nc = extended;
            this.ne = 0;
            this.dataOffset = 7;
            return;
        }
        if (this.apdu.length == 9 + extended) {
            this.nc = extended;
            this.dataOffset = 7;
            int le = ((this.apdu[this.apdu.length - 2] & 0xFF) << 8)
                | (this.apdu[this.apdu.length - 1] & 0xFF);
            this.ne = le == 0 ? 65536 : le;
            return;
        }
        throw new IllegalArgumentException("Invalid APDU: length=" + this.apdu.length
            + ", b1=" + first);
    }

    /** El byte de clase. */
    public int getCLA() {
        return this.apdu[0] & 0xFF;
    }

    /** El de instruccion. */
    public int getINS() {
        return this.apdu[1] & 0xFF;
    }

    /** El primer parametro. */
    public int getP1() {
        return this.apdu[2] & 0xFF;
    }

    /** El segundo. */
    public int getP2() {
        return this.apdu[3] & 0xFF;
    }

    /** Cuantos bytes de datos lleva. */
    public int getNc() {
        return this.nc;
    }

    /** Los datos. Una copia; vacia si no lleva. */
    public byte[] getData() {
        byte[] data = new byte[this.nc];
        System.arraycopy(this.apdu, this.dataOffset, data, 0, this.nc);
        return data;
    }

    /** Cuantos bytes de respuesta pide; cero si no pide. */
    public int getNe() {
        return this.ne;
    }

    /** La orden completa. Una copia. */
    public byte[] getBytes() {
        return this.apdu.clone();
    }

    /** El tamano, Nc y Ne. */
    @Override
    public String toString() {
        // El JDK escribe "CommmandAPDU" con tres emes; se copia tal cual para que un programa que
        // compare esta salida siga viendo lo mismo.
        return "CommmandAPDU: " + this.apdu.length + " bytes, nc=" + this.nc + ", ne=" + this.ne;
    }

    /** Dos ordenes son iguales si tienen los mismos bytes. */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof CommandAPDU)) {
            return false;
        }
        return Arrays.equals(this.apdu, ((CommandAPDU) obj).apdu);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.apdu);
    }

    /** El largo del arreglo, o cero si es null. */
    private static int arrayLength(byte[] data) {
        return data == null ? 0 : data.length;
    }

    /** Que el pedazo entre en el arreglo. */
    private static void checkRange(int arrayLength, int offset, int length) {
        if ((offset < 0) || (length < 0) || (offset > arrayLength - length)) {
            throw new IllegalArgumentException("Offset or length invalid");
        }
    }

    /** Al leerse de un flujo hay que volver a interpretar los bytes: Nc y Ne no se serializan. */
    private void readObject(java.io.ObjectInputStream in)
            throws java.io.IOException, ClassNotFoundException {
        in.defaultReadObject();
        parse();
    }
}
