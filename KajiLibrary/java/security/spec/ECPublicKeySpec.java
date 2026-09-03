package java.security.spec;

// Una clave publica EC en claro: el punto W = d*G, mas los parametros de dominio.
//
// A diferencia de `X509EncodedKeySpec`, aca la clave esta **abierta**: se ve el punto. Eso la hace
// util para construir una clave sin pasar por DER, y tambien es la razon por la que los parametros
// tienen que venir con ella: un punto sin curva no identifica ninguna clave.
public class ECPublicKeySpec implements KeySpec {

    private final ECPoint w;
    private final ECParameterSpec params;

    public ECPublicKeySpec(ECPoint w, ECParameterSpec params) {
        if (w == null) {
            throw new NullPointerException("w is null");
        }
        if (params == null) {
            throw new NullPointerException("params is null");
        }
        // El infinito es el neutro del grupo: como clave publica significaria d = 0, o sea ninguna
        // clave. Rechazarlo aca evita que llegue a un protocolo donde el resultado seria un secreto
        // compartido constante.
        if (w == ECPoint.POINT_INFINITY) {
            throw new IllegalArgumentException("w is ECPoint.POINT_INFINITY");
        }
        this.w = w;
        this.params = params;
    }

    // El punto publico.
    public ECPoint getW() {
        return this.w;
    }

    public ECParameterSpec getParams() {
        return this.params;
    }
}
