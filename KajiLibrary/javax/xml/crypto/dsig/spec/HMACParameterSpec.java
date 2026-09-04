package javax.xml.crypto.dsig.spec;

/**
 * KajiLibrary's javax.xml.crypto.dsig.spec.HMACParameterSpec -- cuantos bits del HMAC se conservan.
 *
 * <p>Un solo numero: el largo, en bits, al que se <b>trunca</b> la salida del HMAC.
 *
 * <p>Truncar esta permitido por la especificacion y es una mala idea casi siempre. Un HMAC-SHA1
 * truncado a 80 bits es lo que la especificacion de XML-DSig nombra como ejemplo, y hoy eso esta al
 * alcance de la fuerza bruta; la recomendacion actual es no truncar por debajo de la mitad de la
 * salida, y en la practica no truncar.
 *
 * <p>La clase no valida el valor: acepta cualquier entero. Es la implementacion la que rechaza los
 * que no puede, y por eso un largo absurdo se descubre al firmar y no al construir esto.
 */
public final class HMACParameterSpec implements SignatureMethodParameterSpec {

    /** El largo en bits. */
    private final int outputLength;

    /**
     * @param outputLength a cuantos bits truncar; ver la nota de la clase
     */
    public HMACParameterSpec(int outputLength) {
        this.outputLength = outputLength;
    }

    /** El largo en bits. */
    public int getOutputLength() {
        return this.outputLength;
    }
}
