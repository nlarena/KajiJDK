package java.net;

// Las tres familias de protocolo que define la plataforma.
//
// Es un enum y no constantes sueltas porque el conjunto es cerrado del lado de la plataforma: hay
// exactamente estas tres, y un `switch` sobre ellas puede ser exhaustivo. Que igual implemente
// `ProtocolFamily` es lo que deja la puerta abierta a familias de terceros sin abrir este enum.
//
// Nada de esto pide una pila de red: son nombres.
public enum StandardProtocolFamily implements ProtocolFamily {

    /** IPv4. */
    INET,

    /** IPv6. */
    INET6,

    /** Sockets de dominio Unix. */
    UNIX;
}
