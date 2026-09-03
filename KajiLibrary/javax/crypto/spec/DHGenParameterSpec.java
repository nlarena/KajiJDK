package javax.crypto.spec;

import java.security.spec.AlgorithmParameterSpec;

/**
 * Lo que hace falta para **generar** parametros Diffie-Hellman: de cuantos bits el primo y de
 * cuantos el exponente.
 *
 * <p>Es la contraparte de {@link DHParameterSpec}: esta describe parametros que todavia no existen
 * y aquella los que ya se calcularon. De ahi que esta sean dos enteros y aquella dos numeros
 * enormes.
 */
public class DHGenParameterSpec implements AlgorithmParameterSpec {

    private final int primeSize;
    private final int exponentSize;

    /** El primo de `primeSize` bits y el exponente de `exponentSize`. */
    public DHGenParameterSpec(int primeSize, int exponentSize) {
        this.primeSize = primeSize;
        this.exponentSize = exponentSize;
    }

    /** El tamano del primo, en bits. */
    public int getPrimeSize() {
        return this.primeSize;
    }

    /** El tamano del exponente, en bits. */
    public int getExponentSize() {
        return this.exponentSize;
    }
}
