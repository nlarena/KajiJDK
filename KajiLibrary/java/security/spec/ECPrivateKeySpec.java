package java.security.spec;

import java.math.BigInteger;

// Una clave privada EC en claro: el escalar s, mas los parametros de dominio.
//
// Es el tipo mas peligroso de todo el paquete y no por lo que hace sino por lo que contiene: el
// `BigInteger` es la clave privada entera, en memoria, sin proteccion. No hay forma de borrarlo
// —`BigInteger` es inmutable y no expone su arreglo— y por eso el API real prefiere que una clave
// privada viva detras de una `PrivateKey` opaca. Esta spec existe para el momento en que hay que
// construir una clave desde sus numeros, no para andar guardandola asi.
public class ECPrivateKeySpec implements KeySpec {

    private final BigInteger s;
    private final ECParameterSpec params;

    public ECPrivateKeySpec(BigInteger s, ECParameterSpec params) {
        if (s == null) {
            throw new NullPointerException("s is null");
        }
        if (params == null) {
            throw new NullPointerException("params is null");
        }
        this.s = s;
        this.params = params;
    }

    // El escalar privado.
    public BigInteger getS() {
        return this.s;
    }

    public ECParameterSpec getParams() {
        return this.params;
    }
}
