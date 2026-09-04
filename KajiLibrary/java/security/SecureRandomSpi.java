package java.security;

import java.io.Serializable;

/**
 * KajiLibrary's java.security.SecureRandomSpi -- lo que tiene que saber hacer un generador
 * criptografico para que {@link SecureRandom} lo pueda usar.
 *
 * <p>Es un contrato, no una implementacion: tres metodos abstractos y tres con un default. Los tres
 * abstractos son el minimo que cualquier generador sabe hacer -- aceptar semilla, entregar bytes,
 * fabricar semilla -- y los tres con default son de la extension de 2017 para DRBGs, que un
 * generador viejo no tiene por que conocer.
 *
 * <h2>La diferencia entre nextBytes y generateSeed</h2>
 *
 * <p>Los dos devuelven bytes y no son lo mismo, y confundirlos es el error clasico:
 *
 * <ul>
 *   <li>{@code engineNextBytes} entrega la <b>salida</b> del generador. Puede ser todo lo rapida
 *       que quiera y sale de expandir el estado interno.
 *   <li>{@code engineGenerateSeed} entrega <b>entropia</b>, para sembrar a otro generador. Puede
 *       ser lenta, puede bloquear, y no debe salir de expandir nada: sembrar un generador con la
 *       salida de otro no agrega entropia, solo la reparte.
 * </ul>
 *
 * <h2>Los defaults son los seguros</h2>
 *
 * <p>{@code engineReseed} y {@code engineNextBytes(byte[], SecureRandomParameters)} lanzan
 * {@code UnsupportedOperationException}, y {@code engineGetParameters} devuelve null. Es a
 * proposito: un generador que no es un DRBG no tiene estado que resembrar ni parametros que
 * contestar, y decir que si -- devolviendo bytes sin resembrar de verdad -- dejaria al llamador
 * creyendo que refresco el estado cuando no paso nada.
 */
public abstract class SecureRandomSpi implements Serializable {

    private static final long serialVersionUID = -2991854161009191830L;

    private final SecureRandomParameters params;

    /** Un generador sin parametros: el caso de cualquier generador que no sea un DRBG. */
    public SecureRandomSpi() {
        this.params = null;
    }

    /**
     * Un generador con parametros, que es lo que declara un DRBG.
     *
     * @throws IllegalArgumentException si los parametros son null
     */
    protected SecureRandomSpi(SecureRandomParameters params) {
        if (params == null) {
            throw new IllegalArgumentException("params cannot be null");
        }
        this.params = params;
    }

    /**
     * Agrega esa semilla al estado. <b>Agrega</b>: nunca reemplaza, asi que llamarlo no puede dejar
     * al generador mas predecible de lo que ya era.
     */
    protected abstract void engineSetSeed(byte[] seed);

    /** Llena el arreglo con la salida del generador. */
    protected abstract void engineNextBytes(byte[] bytes);

    /**
     * Idem, con parametros por llamada. El default no la soporta.
     *
     * @throws UnsupportedOperationException si este generador no es un DRBG
     */
    protected void engineNextBytes(byte[] bytes, SecureRandomParameters params) {
        throw new UnsupportedOperationException();
    }

    /** Devuelve `numBytes` bytes de <b>entropia</b>. Ver la nota de la clase. */
    protected abstract byte[] engineGenerateSeed(int numBytes);

    /**
     * Resiembra el estado interno.
     *
     * @throws UnsupportedOperationException si este generador no tiene estado que resembrar
     */
    protected void engineReseed(SecureRandomParameters params) {
        throw new UnsupportedOperationException();
    }

    /** Los parametros con los que se creo, o null si no tiene. */
    protected SecureRandomParameters engineGetParameters() {
        return this.params;
    }

    @Override
    public String toString() {
        return this.params == null ? super.toString() : this.params.toString();
    }
}
