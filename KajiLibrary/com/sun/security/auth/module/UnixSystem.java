package com.sun.security.auth.module;

/**
 * Quien es el usuario del proceso, segun Unix: nombre, uid, gid y grupos.
 *
 * <h2>Por que no se puede escribir en Java</h2>
 *
 * <p>Porque los identificadores numericos vienen de {@code getuid}, {@code getgid} y
 * {@code getgroups}, que son llamadas al sistema. No hay propiedad ni variable de entorno que los
 * tenga: {@code user.name} da el nombre, y el nombre no determina el uid — un mismo nombre puede
 * tener uid distinto en dos maquinas, y un uid puede no tener nombre.
 *
 * <h2>Por que el constructor falla en vez de contestar algo</h2>
 *
 * <p>Porque lo que devuelve esta clase se usa para decidir permisos, y ahi un valor inventado es
 * peor que ningun valor. {@code getUid()} devolviendo {@code 0} no significa "no se": significa
 * <strong>root</strong>. Un programa que consulte esta clase para saber si esta corriendo como
 * administrador recibiria un si.
 *
 * <p>Ese es exactamente el caso que la casa evita: un miembro que falta es un subconjunto legal y
 * no compila del otro lado; uno que miente compila y revienta despues. Aca revienta con permisos de
 * mas, asi que fallar de entrada es la unica opcion defendible.
 *
 * <p>El dia que la VM tenga como hacer las tres llamadas, lo unico que cambia es el cuerpo del
 * constructor: los cuatro campos ya estan declarados como en el JDK, {@code protected}, para que una
 * subclase pueda llenarlos por su cuenta si consigue los datos de otro lado.
 *
 * @since 1.4
 */
public class UnixSystem {

    /** El nombre del usuario. */
    protected String username;

    /** El identificador numerico del usuario. */
    protected long uid;

    /** El identificador numerico del grupo principal. */
    protected long gid;

    /** Los identificadores de todos los grupos a los que pertenece. */
    protected long[] groups;

    /**
     * Consulta al sistema operativo quien es el usuario del proceso.
     *
     * @throws UnsupportedOperationException siempre, en esta biblioteca: los datos vienen de
     *     {@code getuid}, {@code getgid} y {@code getgroups}, y esta VM no tiene como llamarlas
     */
    public UnixSystem() {
        throw new UnsupportedOperationException(
                "los datos de UnixSystem vienen de getuid/getgid/getgroups, que esta VM no puede "
                + "llamar; devolver un uid inventado en una decision de permisos seria peor que "
                + "fallar");
    }

    /**
     * El nombre del usuario.
     *
     * @return el nombre
     */
    public String getUsername() {
        return username;
    }

    /**
     * El identificador numerico del usuario.
     *
     * @return el uid
     */
    public long getUid() {
        return uid;
    }

    /**
     * El identificador numerico del grupo principal.
     *
     * @return el gid
     */
    public long getGid() {
        return gid;
    }

    /**
     * Los grupos a los que pertenece.
     *
     * @return los identificadores; es el arreglo interno, como en el JDK
     */
    public long[] getGroups() {
        return groups;
    }
}
