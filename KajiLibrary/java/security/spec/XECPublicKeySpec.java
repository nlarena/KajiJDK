package java.security.spec;

import java.math.BigInteger;

// Una clave publica de curva de Montgomery (X25519, X448): la coordenada u.
//
// Solo u, sin v. No es una compresion como la de Edwards: la escalera de Montgomery que usa X25519
// nunca necesita la otra coordenada, asi que directamente no se transmite. Una clave publica X25519
// son 32 bytes y no hay bit de signo que agregar.
//
// Los parametros son un `AlgorithmParameterSpec` y no un `NamedParameterSpec` como en Edwards. La
// diferencia es del API real y hay que respetarla: aca entra tanto un `NamedParameterSpec` como
// otra cosa.
public class XECPublicKeySpec implements KeySpec {

    private final AlgorithmParameterSpec params;
    private final BigInteger u;

    public XECPublicKeySpec(AlgorithmParameterSpec params, BigInteger u) {
        if (params == null) {
            throw new NullPointerException("params must not be null");
        }
        if (u == null) {
            throw new NullPointerException("u must not be null");
        }
        this.params = params;
        this.u = u;
    }

    public AlgorithmParameterSpec getParams() {
        return this.params;
    }

    // La coordenada u del punto publico.
    public BigInteger getU() {
        return this.u;
    }
}
