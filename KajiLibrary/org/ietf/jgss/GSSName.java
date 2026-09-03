package org.ietf.jgss;

/**
 * KajiLibrary's org.ietf.jgss.GSSName -- una identidad, en un formato o en varios.
 *
 * <p>Un nombre GSS-API no es una cadena: es una identidad que puede tener una representacion
 * distinta en cada mecanismo. {@code "juan@EJEMPLO.COM"} escrito por una persona es un nombre
 * <b>generico</b>; el mismo, resuelto para Kerberos, es otra cosa con otros bytes.
 *
 * <h2>Nombres genericos y nombres de mecanismo</h2>
 *
 * <p>{@link #isMN} distingue las dos formas y {@link #canonicalize} pasa de la primera a la segunda.
 * La diferencia importa al comparar: dos nombres genericos que se ven distintos pueden resolver a la
 * misma identidad --mayusculas, dominios equivalentes, alias-- y solo el mecanismo sabe si son la
 * misma.
 *
 * <p>De ahi salen los <b>dos</b> {@code equals}. El que recibe {@link GSSName} puede lanzar y es el
 * que compara de verdad, resolviendo lo que haga falta. El que recibe {@code Object} no puede lanzar
 * --lo hereda de {@code Object}-- y por eso, cuando la comparacion no se puede hacer, devuelve false
 * en vez de avisar. Poner nombres genericos en un {@code HashSet} es, por eso, una fuente silenciosa
 * de duplicados: usar {@code canonicalize} primero es lo unico que lo evita.
 *
 * <h2>Los tipos de nombre</h2>
 *
 * <p>Las constantes {@code NT_*} son OID que dicen <b>como leer</b> la cadena que se paso.
 * {@link #NT_USER_NAME} es un usuario local, {@link #NT_HOSTBASED_SERVICE} es {@code servicio@host}
 * --el mas usado en la practica-- y {@link #NT_EXPORT_NAME} es la forma binaria de
 * {@link #export()}, pensada para guardar y comparar sin volver a resolver nada.
 *
 * <p>{@link #NT_ANONYMOUS} es el unico que no nombra a nadie: es la identidad de quien se autentica
 * sin decir quien es, y {@link #isAnonymous} es la forma de no tratarla como un nombre comun por
 * accidente.
 */
public interface GSSName {

    /** {@code servicio@host}; el mas usado. */
    public static final Oid NT_HOSTBASED_SERVICE = Oid.literal("1.2.840.113554.1.2.1.4");

    /** Un usuario local. */
    public static final Oid NT_USER_NAME = Oid.literal("1.2.840.113554.1.2.1.1");

    /** Un identificador numerico de usuario, en bytes. */
    public static final Oid NT_MACHINE_UID_NAME = Oid.literal("1.2.840.113554.1.2.1.2");

    /** El mismo, escrito como texto. */
    public static final Oid NT_STRING_UID_NAME = Oid.literal("1.2.840.113554.1.2.1.3");

    /** Nadie. Ver la nota de la clase. */
    public static final Oid NT_ANONYMOUS = Oid.literal("1.3.6.1.5.6.3");

    /** La forma binaria de {@link #export}. */
    public static final Oid NT_EXPORT_NAME = Oid.literal("1.3.6.1.5.6.4");

    /**
     * Si nombran a la misma identidad.
     *
     * @throws GSSException si la comparacion no se pudo hacer
     */
    boolean equals(GSSName another) throws GSSException;

    /**
     * Idem, sin poder avisar.
     *
     * @return false si la comparacion no se pudo hacer; ver la nota de la clase
     */
    boolean equals(Object another);

    /** Coherente con el {@code equals} de {@code Object}. */
    int hashCode();

    /**
     * El mismo nombre, resuelto para ese mecanismo.
     *
     * @param mech el OID del mecanismo
     */
    GSSName canonicalize(Oid mech) throws GSSException;

    /**
     * La forma binaria, para guardar o comparar.
     *
     * @throws GSSException si el nombre no es de un mecanismo; hay que canonicalizarlo antes
     */
    byte[] export() throws GSSException;

    /** La forma legible. */
    String toString();

    /** El tipo de nombre de la cadena que devuelve {@link #toString}. */
    Oid getStringNameType() throws GSSException;

    /** Si es el nombre anonimo. Ver la nota de la clase. */
    boolean isAnonymous();

    /** Si ya esta resuelto para un mecanismo. */
    boolean isMN();
}
