package java.security.spec;

// Una clave privada Edwards: la curva por nombre, mas los bytes de la semilla.
//
// Son **bytes** y no un `BigInteger`, y la diferencia importa: en EdDSA la clave privada no es el
// escalar sino una semilla de la que se derivan por hash tanto el escalar como el valor con el que
// se genera el nonce de cada firma. Es lo que hace a Ed25519 deterministico y lo que lo salva del
// desastre que hunde a DSA y ECDSA cuando el nonce se repite. Tratar estos bytes como un entero
// perderia esa distincion.
public final class EdECPrivateKeySpec implements KeySpec {

    private final NamedParameterSpec params;
    private final byte[] bytes;

    public EdECPrivateKeySpec(NamedParameterSpec params, byte[] bytes) {
        if (params == null) {
            throw new NullPointerException("params must not be null");
        }
        if (bytes == null) {
            throw new NullPointerException("bytes must not be null");
        }
        this.params = params;
        this.bytes = copiar(bytes);
    }

    private static byte[] copiar(byte[] b) {
        byte[] c = new byte[b.length];
        System.arraycopy(b, 0, c, 0, b.length);
        return c;
    }

    public NamedParameterSpec getParams() {
        return this.params;
    }

    // Copia de la semilla. La copia es obligatoria en los dos sentidos: es material secreto y el que
    // lo entrega no puede quedar expuesto a que el receptor le cambie el arreglo por debajo.
    public byte[] getBytes() {
        return copiar(this.bytes);
    }
}
