package java.nio.file.attribute;

// Que hace una entrada de ACL cuando coincide: permite, niega, o solo deja rastro.
public enum AclEntryType {

    /** Da acceso. */
    ALLOW,

    /** Niega acceso. */
    DENY,

    /** No cambia el acceso: registra el intento en la auditoria. */
    AUDIT,

    /** No cambia el acceso: dispara una alarma. */
    ALARM
}
