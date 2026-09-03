package java.security.spec;

// Una clave privada de curva de Montgomery: el escalar, como bytes.
//
// Bytes y no `BigInteger` porque el escalar de X25519 no se usa tal cual: antes de multiplicar se le
// "podan" bits fijos —se limpian los tres de abajo y se fuerza el de arriba— para que siempre sea
// multiplo del cofactor y tenga largo constante. Eso es lo que neutraliza los ataques de subgrupo
// chico y lo que hace que la escalera corra en tiempo constante. Un entero no tiene donde guardar
// esa forma; un arreglo de bytes de largo fijo si.
public class XECPrivateKeySpec implements KeySpec {

    private final AlgorithmParameterSpec params;
    private final byte[] scalar;

    public XECPrivateKeySpec(AlgorithmParameterSpec params, byte[] scalar) {
        if (params == null) {
            throw new NullPointerException("params must not be null");
        }
        if (scalar == null) {
            throw new NullPointerException("scalar must not be null");
        }
        this.params = params;
        this.scalar = copiar(scalar);
    }

    private static byte[] copiar(byte[] b) {
        byte[] c = new byte[b.length];
        System.arraycopy(b, 0, c, 0, b.length);
        return c;
    }

    public AlgorithmParameterSpec getParams() {
        return this.params;
    }

    // Copia del escalar privado.
    public byte[] getScalar() {
        return copiar(this.scalar);
    }
}
