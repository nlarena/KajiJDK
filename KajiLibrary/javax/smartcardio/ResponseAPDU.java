package javax.smartcardio;

import java.io.Serializable;
import java.util.Arrays;

/**
 * KajiLibrary's javax.smartcardio.ResponseAPDU -- lo que contesta la tarjeta.
 *
 * <p>Una respuesta son los datos --que pueden ser cero-- y siempre <b>dos bytes de estado</b> al
 * final, SW1 y SW2. Por eso una respuesta valida nunca mide menos de dos bytes.
 *
 * <p>{@code 9000} es "todo bien"; {@code 6A82} es "no existe ese archivo"; {@code 61xx} es "hay xx
 * bytes mas esperando". {@link #getSW} devuelve los dos juntos, que es la forma en que estan
 * tabulados en la norma.
 */
public final class ResponseAPDU implements Serializable {

    private static final long serialVersionUID = 6962744978375594225L;

    /** La respuesta completa, datos mas estado. */
    private final byte[] apdu;

    /**
     * Con esos bytes. El arreglo se copia.
     *
     * @throws NullPointerException si es null
     * @throws IllegalArgumentException si mide menos de dos bytes
     */
    public ResponseAPDU(byte[] apdu) {
        apdu = apdu.clone();
        if (apdu.length < 2) {
            throw new IllegalArgumentException("apdu must be at least 2 bytes long");
        }
        this.apdu = apdu;
    }

    /** Cuantos bytes de datos hay, sin contar el estado. */
    public int getNr() {
        return this.apdu.length - 2;
    }

    /** Los datos, sin el estado. Una copia. */
    public byte[] getData() {
        byte[] data = new byte[this.apdu.length - 2];
        System.arraycopy(this.apdu, 0, data, 0, data.length);
        return data;
    }

    /** El primer byte de estado. */
    public int getSW1() {
        return this.apdu[this.apdu.length - 2] & 0xFF;
    }

    /** El segundo. */
    public int getSW2() {
        return this.apdu[this.apdu.length - 1] & 0xFF;
    }

    /** Los dos juntos, SW1 arriba. Ver la nota de la clase. */
    public int getSW() {
        return (getSW1() << 8) | getSW2();
    }

    /** La respuesta completa. Una copia. */
    public byte[] getBytes() {
        return this.apdu.clone();
    }

    /** El tamano y el estado en hexadecimal. */
    @Override
    public String toString() {
        return "ResponseAPDU: " + this.apdu.length + " bytes, SW="
            + Integer.toHexString(getSW() | 0x10000).substring(1);
    }

    /** Dos respuestas son iguales si tienen los mismos bytes. */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ResponseAPDU)) {
            return false;
        }
        return Arrays.equals(this.apdu, ((ResponseAPDU) obj).apdu);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.apdu);
    }
}
