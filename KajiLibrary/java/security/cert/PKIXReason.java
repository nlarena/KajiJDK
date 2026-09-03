package java.security.cert;

// Las razones de falla propias de PKIX (RFC 5280), las que no tienen sentido fuera de el.
//
// Complementan a `BasicReason`, que cubre lo que vale para cualquier PKI. Que sean dos enums
// distintos implementando la misma interfaz es justamente el punto del diseño de `Reason`: la lista
// se extiende sin tocar la excepcion.
public enum PKIXReason implements CertPathValidatorException.Reason {

    // El emisor de un certificado no coincide con el sujeto del siguiente: la cadena esta cortada.
    NAME_CHAINING,

    // El certificado se uso para algo que su extension KeyUsage no permite. Un certificado de
    // servidor firmando otros certificados cae aca.
    INVALID_KEY_USAGE,

    // La politica del certificado no es aceptable segun los parametros de validacion.
    INVALID_POLICY,

    // La cadena no termina en ninguna ancla de confianza conocida: el "no se quien firmo esto".
    NO_TRUST_ANCHOR,

    // Hay una extension marcada critica que el validador no entiende. Rechazar es obligatorio y no
    // opcional: "critica" quiere decir exactamente "si no entendes esto, no aceptes".
    UNRECOGNIZED_CRIT_EXT,

    // Un certificado de la cadena firmo a otro sin ser una CA.
    NOT_CA_CERT,

    // La cadena es mas larga de lo que permite la restriccion de largo de alguna CA.
    PATH_TOO_LONG,

    // Un nombre viola las restricciones de nombres de alguna CA de la cadena.
    INVALID_NAME
}
