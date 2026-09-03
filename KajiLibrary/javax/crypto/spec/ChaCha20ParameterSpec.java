package javax.crypto.spec;

import java.security.spec.AlgorithmParameterSpec;

/**
 * El nonce y el contador de bloque de ChaCha20.
 *
 * <p>El nonce son **exactamente doce bytes** y no hay margen: ChaCha20 arma su estado con un nonce
 * de 96 bits, asi que uno de otro largo no describe una configuracion posible.
 *
 * <p>El contador se guarda como `int` y se interpreta **sin signo**: el estado de ChaCha20 lo trata
 * como un entero de 32 bits sin signo, asi que un contador de `-1` es el bloque 4294967295 y no un
 * error. Por eso el constructor no lo valida.
 */
public final class ChaCha20ParameterSpec implements AlgorithmParameterSpec {

    /** El largo que el algoritmo exige. */
    private static final int NONCE_LEN = 12;

    private final byte[] nonce;
    private final int counter;

    /**
     * @throws NullPointerException si el nonce es nulo
     * @throws IllegalArgumentException si no mide doce bytes
     */
    public ChaCha20ParameterSpec(byte[] nonce, int counter) {
        if (nonce == null) {
            throw new NullPointerException("el nonce no puede ser nulo");
        }
        if (nonce.length != NONCE_LEN) {
            throw new IllegalArgumentException(
                    "el nonce de ChaCha20 son " + NONCE_LEN + " bytes, no " + nonce.length);
        }
        this.nonce = IvParameterSpec.copy(nonce, 0, NONCE_LEN);
        this.counter = counter;
    }

    /** Una copia del nonce. */
    public byte[] getNonce() {
        return IvParameterSpec.copy(this.nonce, 0, this.nonce.length);
    }

    /** El contador de bloque, sin signo. Ver la nota de la clase. */
    public int getCounter() {
        return this.counter;
    }
}
