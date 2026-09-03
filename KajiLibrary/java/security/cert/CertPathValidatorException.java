package java.security.cert;

import java.io.Serializable;
import java.security.GeneralSecurityException;

// La validacion de un camino de certificacion fallo, y **donde** y **por que**.
//
// Es la excepcion mas informativa del paquete y con razon: cuando una cadena TLS no valida, saber
// que fallo no alcanza —hay que saber en que eslabon y si fue por fecha, por firma o por
// revocacion. Por eso lleva el camino, el indice del certificado que la provoco y una razon.
//
// La razon es una **interfaz** y no un enum cerrado, y esa es la decision de diseño que importa:
// `BasicReason` cubre lo que vale para cualquier PKI, `PKIXReason` agrega lo especifico de PKIX, y
// un validador de otro tipo puede aportar las suyas sin que haya que tocar esta clase. Un enum
// habria congelado la lista en el JDK 1.5.
//
// El indice es -1 cuando no se sabe cual fue el certificado culpable, y esa es la unica forma legal
// de decirlo: los indices reales son posiciones dentro del camino, y el constructor lo verifica.
public class CertPathValidatorException extends GeneralSecurityException {

    private static final long serialVersionUID = -3083180014971893139L;

    // La razon por la que fallo. Es `Serializable` porque viaja dentro de la excepcion, que
    // tambien lo es; no declara ningun metodo porque su unico proposito es ser una constante
    // identificable.
    public interface Reason extends Serializable {
    }

    // Las razones que valen para cualquier PKI, no solo PKIX.
    public enum BasicReason implements Reason {

        // Fallo, pero no se sabe por que. Es el default de todos los constructores que no reciben
        // razon: decir "no se" es correcto, inventar una razon no.
        UNSPECIFIED,

        // El certificado vencio.
        EXPIRED,

        // El certificado todavia no entro en vigencia.
        NOT_YET_VALID,

        // El emisor lo revoco.
        REVOKED,

        // No se pudo averiguar si esta revocado. Es **distinto** de "no esta revocado" y confundir
        // los dos es el error clasico: aceptar un certificado cuyo estado no se pudo consultar es
        // exactamente lo que un atacante que bloquea el OCSP quiere que pase.
        UNDETERMINED_REVOCATION_STATUS,

        // La firma no valida.
        INVALID_SIGNATURE,

        // El algoritmo esta prohibido por politica: no es que la firma este mal, es que ese
        // algoritmo ya no se acepta.
        ALGORITHM_CONSTRAINED
    }

    private final CertPath certPath;
    private final int index;
    private final Reason reason;

    public CertPathValidatorException() {
        this((String) null, null);
    }

    public CertPathValidatorException(String msg) {
        this(msg, null);
    }

    // Toma el mensaje de la causa, igual que el resto de la jerarquia.
    public CertPathValidatorException(Throwable cause) {
        this((cause == null ? null : cause.toString()), cause);
    }

    public CertPathValidatorException(String msg, Throwable cause) {
        this(msg, cause, null, -1);
    }

    public CertPathValidatorException(String msg, Throwable cause, CertPath certPath, int index) {
        this(msg, cause, certPath, index, BasicReason.UNSPECIFIED);
    }

    public CertPathValidatorException(String msg, Throwable cause, CertPath certPath, int index,
                                      Reason reason) {
        super(msg, cause);
        // Un indice sin camino no señala nada: si no hay camino el indice tiene que ser -1.
        if (certPath == null && index != -1) {
            throw new IllegalArgumentException();
        }
        if (index < -1 || (certPath != null && index >= certPath.getCertificates().size())) {
            throw new IndexOutOfBoundsException();
        }
        if (reason == null) {
            throw new NullPointerException("reason can't be null");
        }
        this.certPath = certPath;
        this.index = index;
        this.reason = reason;
    }

    // El camino que fallo, o null si no se dio.
    public CertPath getCertPath() {
        return this.certPath;
    }

    // La posicion del certificado culpable dentro del camino, o -1 si no se sabe.
    public int getIndex() {
        return this.index;
    }

    // Nunca null: en el peor caso es `BasicReason.UNSPECIFIED`.
    public Reason getReason() {
        return this.reason;
    }
}
