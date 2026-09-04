package java.security;

// Fallo en una operacion de firma.
//
// Ojo con el matiz, porque es la fuente clasica de agujeros: esta excepcion significa que la
// operacion **no se pudo llevar a cabo** —estado equivocado, datos corruptos, proveedor roto— y
// **no** significa "la firma no valida". Una firma que no valida es un `verify()` que devuelve
// `false`, sin excepcion. El codigo que trata a las dos cosas igual, o que atrapa esto y sigue de
// largo, termina aceptando firmas invalidas.
public class SignatureException extends GeneralSecurityException {

    public SignatureException() {
        super();
    }

    public SignatureException(String message) {
        super(message);
    }

    public SignatureException(String message, Throwable cause) {
        super(message, cause);
    }

    public SignatureException(Throwable cause) {
        super(cause);
    }
}
