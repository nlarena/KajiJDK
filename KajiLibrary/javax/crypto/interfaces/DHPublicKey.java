package javax.crypto.interfaces;

import java.math.BigInteger;
import java.security.PublicKey;
import javax.crypto.spec.DHParameterSpec;

/**
 * KajiLibrary's javax.crypto.interfaces.DHPublicKey -- la mitad publica de un Diffie-Hellman.
 *
 * <p>El valor es {@link #getY}, que es la base elevada al exponente privado, modulo el primo. Se
 * manda en claro: quien lo intercepte no puede sacar el exponente de vuelta sin resolver un
 * logaritmo discreto, y ese es todo el truco.
 *
 * <p>Lo que este numero <b>no</b> hace es probar quien es el otro. Un Diffie-Hellman crudo no
 * autentica a nadie: dos partes terminan con el mismo secreto, y si alguien se puso en el medio, con
 * dos secretos distintos y las dos partes contentas. Por eso en la practica siempre va firmado por
 * algo -- un certificado, una clave conocida-- y usarlo pelado es el error clasico.
 *
 * <p>Su {@code getParams} resuelve el mismo empate que en {@link DHPrivateKey}; ver la nota de alla.
 */
public interface DHPublicKey extends DHKey, PublicKey {

    /**
     * De 1998. Es parte del API publico: cambiarlo rompe la deserializacion de claves ya guardadas.
     */
    static final long serialVersionUID = -6628103563352519193L;

    /** La base elevada al exponente privado, modulo el primo. Ver la nota de la clase. */
    BigInteger getY();

    /** Resuelve el empate entre las dos ramas; ver {@link DHPrivateKey#getParams}. */
    default DHParameterSpec getParams() {
        return null;
    }
}
