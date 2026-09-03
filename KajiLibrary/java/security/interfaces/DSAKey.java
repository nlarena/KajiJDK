package java.security.interfaces;

// Lo que toda clave DSA tiene, sea publica o privada: sus parametros de dominio.
//
// Esta interfaz **no** extiende `Key`, y es a proposito: una clave DSA en un HSM puede querer
// exponer sus parametros sin comprometerse con `getEncoded()`. Las que si son claves lo dicen
// heredando ademas de `PublicKey` o `PrivateKey`.
public interface DSAKey {

    // Los parametros de esta clave.
    DSAParams getParams();
}
