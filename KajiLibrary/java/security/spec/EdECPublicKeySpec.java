package java.security.spec;

// Una clave publica Edwards: la curva por nombre, mas el punto.
//
// Los parametros son un `NamedParameterSpec` y no un `ECParameterSpec` a proposito: en las curvas de
// Edwards no se ofrece describir la curva a mano. Ed25519 y Ed448 son las unicas dos, sus parametros
// son fijos, y no dar forma de escribir otros es lo que evita que alguien firme sobre una curva que
// eligio mal.
public final class EdECPublicKeySpec implements KeySpec {

    private final NamedParameterSpec params;
    private final EdECPoint point;

    public EdECPublicKeySpec(NamedParameterSpec params, EdECPoint point) {
        if (params == null) {
            throw new NullPointerException("params must not be null");
        }
        if (point == null) {
            throw new NullPointerException("point must not be null");
        }
        this.params = params;
        this.point = point;
    }

    public NamedParameterSpec getParams() {
        return this.params;
    }

    public EdECPoint getPoint() {
        return this.point;
    }
}
