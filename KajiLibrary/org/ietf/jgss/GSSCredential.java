package org.ietf.jgss;

/**
 * KajiLibrary's org.ietf.jgss.GSSCredential -- con que se prueba una identidad.
 *
 * <p>Una credencial junta un {@link GSSName} con el material que permite demostrarlo --un ticket de
 * Kerberos, una clave-- y con un vencimiento. Un objeto de estos puede tener elementos de
 * <b>varios</b> mecanismos a la vez, que es para lo que sirve {@link #add}: la misma credencial
 * responde por la misma identidad en Kerberos y en lo que venga despues.
 *
 * <h2>Iniciar y aceptar son cosas distintas</h2>
 *
 * <p>{@link #INITIATE_ONLY} sirve para conectarse a otros, {@link #ACCEPT_ONLY} para recibir
 * conexiones, y las dos tienen vencimientos separados --de ahi
 * {@link #getRemainingInitLifetime} y {@link #getRemainingAcceptLifetime}--. Un servidor normalmente
 * quiere solo la segunda: pedir tambien la primera le da al proceso la capacidad de <b>hacerse
 * pasar</b> por el servicio contra terceros, que es exactamente lo que no se quiere si alguien lo
 * compromete.
 *
 * <h2>Hay que llamar a dispose</h2>
 *
 * <p>{@link #dispose} borra el material secreto. No alcanza con soltar la referencia: hasta que el
 * recolector la levante, las claves siguen en memoria, y un volcado del proceso las contiene. Es la
 * misma razon por la que las contrasenas van en {@code char[]}.
 */
public interface GSSCredential extends Cloneable {

    /** Sirve para las dos cosas. */
    public static final int INITIATE_AND_ACCEPT = 0;

    /** Solo para conectarse a otros. */
    public static final int INITIATE_ONLY = 1;

    /** Solo para recibir. Ver la nota de la clase. */
    public static final int ACCEPT_ONLY = 2;

    /** El vencimiento por omision del mecanismo. */
    public static final int DEFAULT_LIFETIME = 0;

    /** No vence. */
    public static final int INDEFINITE_LIFETIME = Integer.MAX_VALUE;

    /** Borra el material secreto. Ver la nota de la clase. */
    void dispose() throws GSSException;

    /** La identidad de la que responde. */
    GSSName getName() throws GSSException;

    /** La misma, en la forma de ese mecanismo. */
    GSSName getName(Oid mech) throws GSSException;

    /**
     * Cuantos segundos le quedan.
     *
     * <p>De la que menos tenga entre todos sus elementos: la credencial vale mientras valgan todos.
     */
    int getRemainingLifetime() throws GSSException;

    /** Cuantos segundos le quedan para iniciar con ese mecanismo. */
    int getRemainingInitLifetime(Oid mech) throws GSSException;

    /** Cuantos para aceptar. */
    int getRemainingAcceptLifetime(Oid mech) throws GSSException;

    /** Para que sirve: una de las tres constantes de arriba. */
    int getUsage() throws GSSException;

    /** Para que sirve con ese mecanismo. */
    int getUsage(Oid mech) throws GSSException;

    /** Los mecanismos que tiene adentro. */
    Oid[] getMechs() throws GSSException;

    /**
     * Le agrega un elemento de otro mecanismo.
     *
     * @param name la identidad; null para la por omision
     * @param initLifetime segundos para iniciar
     * @param acceptLifetime segundos para aceptar
     * @param mech el mecanismo
     * @param usage una de las tres constantes de arriba
     * @throws GSSException con {@link GSSException#DUPLICATE_ELEMENT} si ya tenia uno de ese
     *     mecanismo
     */
    void add(GSSName name, int initLifetime, int acceptLifetime, Oid mech, int usage)
        throws GSSException;

    /** Por identidad y por lo que contiene. */
    boolean equals(Object another);

    /** Coherente con {@link #equals}. */
    int hashCode();
}
