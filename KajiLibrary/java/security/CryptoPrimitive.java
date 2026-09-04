package java.security;

// Las primitivas criptograficas que una restriccion de algoritmos puede nombrar.
//
// Es un enum y no un conjunto de strings porque de esto se hacen conjuntos —`AlgorithmConstraints`
// recibe un `Set<CryptoPrimitive>`— y un algoritmo suele valer para mas de una: RSA sirve para
// firmar y para encriptar, y una politica que quiera prohibirlo solo para firmar tiene que poder
// decirlo.
public enum CryptoPrimitive {

    // Hash sin clave.
    MESSAGE_DIGEST,

    // Generacion de numeros seudoaleatorios seguros.
    SECURE_RANDOM,

    // Cifrado simetrico por bloques.
    BLOCK_CIPHER,

    // Cifrado simetrico de flujo.
    STREAM_CIPHER,

    // Codigo de autenticacion de mensaje.
    MAC,

    // Envoltura de una clave con otra.
    KEY_WRAP,

    // Cifrado con clave publica.
    PUBLIC_KEY_ENCRYPTION,

    // Firma digital.
    SIGNATURE,

    // Encapsulamiento de clave.
    KEY_ENCAPSULATION,

    // Acuerdo de clave.
    KEY_AGREEMENT
}
