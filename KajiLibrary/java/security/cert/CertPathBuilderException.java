package java.security.cert;

import java.security.GeneralSecurityException;

// No se pudo construir un camino de certificacion.
//
// Que sea la misma excepcion para "no existe camino" y para "el constructor se rompio" es del API y
// vale saberlo: la ausencia de camino no es un error del programa, es un resultado. Distinguir uno
// del otro obliga a mirar la causa.
public class CertPathBuilderException extends GeneralSecurityException {

    private static final long serialVersionUID = 5316471420178794402L;

    public CertPathBuilderException() {
        super();
    }

    public CertPathBuilderException(String msg) {
        super(msg);
    }

    public CertPathBuilderException(Throwable cause) {
        super(cause);
    }

    public CertPathBuilderException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
