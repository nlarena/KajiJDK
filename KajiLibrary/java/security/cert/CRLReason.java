package java.security.cert;

// Por que se revoco un certificado, segun el codigo `CRLReason` de RFC 5280.
//
// El orden **es** el contrato: los valores se codifican en la CRL como su numero ordinal, asi que
// mover uno de lugar cambia el significado de las CRLs ya emitidas. Por eso `UNUSED` esta en el
// medio y no se saca: es el codigo 7, que se reservo y nunca se llego a usar.
//
// La distincion que mas importa en la practica es `KEY_COMPROMISE` contra el resto: solo esa dice
// que la clave privada se filtro, y por lo tanto solo esa invalida hacia atras las firmas hechas
// antes de la revocacion. Con `SUPERSEDED` o `CESSATION_OF_OPERATION`, lo firmado antes sigue
// valiendo.
public enum CRLReason {

    // Se revoco sin decir por que.
    UNSPECIFIED,

    // La clave privada del sujeto se filtro. La unica razon que invalida hacia atras.
    KEY_COMPROMISE,

    // La clave privada de la CA se filtro: cae todo lo que esa CA emitio.
    CA_COMPROMISE,

    // Cambio algo del sujeto —nombre, organizacion— sin sospecha sobre la clave.
    AFFILIATION_CHANGED,

    // Hay un certificado nuevo que lo reemplaza.
    SUPERSEDED,

    // El sujeto dejo de operar.
    CESSATION_OF_OPERATION,

    // Suspension temporal: puede volver a valer. La unica razon reversible.
    CERTIFICATE_HOLD,

    // El codigo 7, reservado y nunca usado. Esta solo para que los ordinales siguientes caigan
    // donde el RFC dice que caen.
    UNUSED,

    // Se saca de la CRL: solo aparece en CRLs delta, para levantar un CERTIFICATE_HOLD.
    REMOVE_FROM_CRL,

    // Se le quito un privilegio al sujeto.
    PRIVILEGE_WITHDRAWN,

    // La clave de una autoridad de atributos se filtro.
    AA_COMPROMISE
}
