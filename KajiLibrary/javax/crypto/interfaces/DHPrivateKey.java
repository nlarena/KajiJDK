package javax.crypto.interfaces;

import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.spec.DHParameterSpec;

/**
 * KajiLibrary's javax.crypto.interfaces.DHPrivateKey -- la mitad privada de un Diffie-Hellman.
 *
 * <p>El valor es {@link #getX}, el exponente privado. Es el unico numero del intercambio que no sale
 * de la maquina: todo lo demas --el modulo, la base, la clave publica del otro-- viaja en claro y no
 * sirve de nada sin este.
 *
 * <p>Que la interfaz lo <b>exponga</b> vale la pena mirarlo. Una clave que vive en un token o en un
 * modulo de hardware no puede contestar {@code getX}, y por eso ese tipo de claves implementa
 * {@code PrivateKey} pero no esta interfaz. Pedir un {@code DHPrivateKey} es, de hecho, pedir que el
 * secreto este en memoria.
 *
 * <h2>Por que {@code getParams} tiene un default que devuelve null</h2>
 *
 * <p>{@link DHKey} declara {@code getParams()} devolviendo {@link DHParameterSpec}, y
 * {@code AsymmetricKey} trae otro con default devolviendo {@link AlgorithmParameterSpec}. Los dos
 * tienen la misma firma --el primero es una redefinicion covariante del segundo-- y Java exige que
 * una interfaz que hereda de las dos ramas <b>resuelva el empate</b> a mano.
 *
 * <p>Este default es esa resolucion, y devuelve null porque no tiene de donde sacar nada: una
 * interfaz no tiene estado. Es el mismo default que el JDK, y significa lo que dice --"esta clave no
 * publica sus parametros"-- asi que quien implemente de verdad tiene que redefinirlo.
 */
public interface DHPrivateKey extends DHKey, PrivateKey {

    /**
     * De 1998, cuando la serializacion cruzaba versiones a mano.
     *
     * <p>Es parte del API publico y no se puede cambiar: cambiarlo rompe la deserializacion de
     * cualquier clave guardada con la version anterior.
     */
    static final long serialVersionUID = 2211791113380396553L;

    /** El exponente privado. Ver la nota de la clase. */
    BigInteger getX();

    /** Resuelve el empate entre las dos ramas; ver la nota de la clase. */
    default DHParameterSpec getParams() {
        return null;
    }
}
