package javax.crypto.interfaces;

import javax.crypto.SecretKey;

/**
 * KajiLibrary's javax.crypto.interfaces.PBEKey -- una clave derivada de una contrasena.
 *
 * <p>Expone las tres piezas de la derivacion, y las tres estan aca por un motivo distinto:
 *
 * <ul>
 *   <li>{@link #getPassword} -- lo unico que la persona recuerda. Devuelve {@code char[]} y no
 *       {@code String} <b>a proposito</b>: un arreglo se puede sobreescribir despues de usarlo, y una
 *       cadena queda en el pool de literales hasta que el recolector la levante, si es que la
 *       levanta. Cada llamada tiene que devolver una copia nueva, para que borrarla no rompa la
 *       clave;
 *   <li>{@link #getSalt} -- lo que hace que la misma contrasena de dos personas de claves distintas.
 *       Sin sal, una tabla precalculada rompe todas las cuentas de una sola pasada, y por eso la sal
 *       <b>no es secreta</b>: se guarda junto al resultado;
 *   <li>{@link #getIterationCount} -- cuantas veces se repite la funcion. Es lo unico que hace cara
 *       la derivacion, y es la unica defensa contra alguien que prueba contrasenas por fuerza bruta.
 *       Un numero de los noventa --mil vueltas-- hoy no protege nada.
 * </ul>
 *
 * <p>Que todo esto sea consultable es incomodo desde el punto de vista de la seguridad y es
 * necesario: sin la sal y las vueltas no se puede volver a derivar la misma clave, y sin poder
 * volver a derivarla no se puede descifrar nada.
 */
public interface PBEKey extends SecretKey {

    /**
     * De 2000. Es parte del API publico: cambiarlo rompe la deserializacion de claves ya guardadas.
     */
    static final long serialVersionUID = -1430015993304333921L;

    /** La contrasena, en una copia nueva. Ver la nota de la clase sobre por que no es un String. */
    char[] getPassword();

    /** La sal, o null si no tiene. No es secreta. */
    byte[] getSalt();

    /** Cuantas vueltas de derivacion. Ver la nota de la clase. */
    int getIterationCount();
}
