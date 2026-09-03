package javax.crypto.interfaces;

import javax.crypto.spec.DHParameterSpec;

/**
 * KajiLibrary's javax.crypto.interfaces.DHKey -- lo que comparten las dos mitades de una clave
 * Diffie-Hellman.
 *
 * <p>Un solo metodo, y lo que devuelve es la parte del acuerdo que <b>no es secreta y tiene que
 * coincidir</b>: el modulo primo y la base. Sin los mismos parametros de los dos lados no hay
 * secreto compartido, asi que esto se transmite en claro junto con la clave publica.
 *
 * <p>Que los parametros sean publicos no significa que den lo mismo. Elegir un modulo chico, o uno
 * que no sea primo seguro, deja el intercambio al alcance de un ataque de logaritmo discreto
 * precalculado -- que es exactamente lo que hizo caer a los primos de 1024 bits que se usaban de
 * memoria por todos lados.
 */
public interface DHKey {

    /** El modulo, la base y el largo del exponente privado. */
    DHParameterSpec getParams();
}
