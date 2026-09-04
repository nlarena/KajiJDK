package javax.xml.crypto;

import java.security.Key;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;

/**
 * KajiLibrary's javax.xml.crypto.KeySelector -- decide con que clave se firma o se valida.
 *
 * <p>Recibe el {@link KeyInfo} del documento, para que se la usa, y el algoritmo, y devuelve la
 * clave. Es <b>la</b> decision de seguridad de toda la firma XML, y por eso el API la deja en manos
 * de quien usa la biblioteca en vez de resolverla sola.
 *
 * <h2>El KeyInfo no es una fuente de confianza</h2>
 *
 * <p>El argumento mas tentador es el {@code KeyInfo}: viene con la clave adentro, o con un
 * certificado, y usarlo hace que la firma valide. Y no prueba nada -- lo escribio quien firmo, asi
 * que una firma falsificada trae su propia clave y valida perfecto.
 *
 * <p>Un selector correcto usa el {@code KeyInfo} como <b>pista</b> --para elegir cual de las claves
 * que uno ya conoce corresponde-- y nunca como fuente. {@link #singletonKeySelector} es el caso
 * extremo y el mas seguro: siempre la misma clave, ignorando lo que el documento diga.
 *
 * <p>{@link Purpose} distingue los cuatro usos. Importa porque una clave puede servir para uno y no
 * para otro, y porque validar con una clave destinada a firmar es un error de configuracion que
 * conviene detectar.
 */
public abstract class KeySelector {

    /** Para las subclases. */
    protected KeySelector() {
    }

    /**
     * La clave para esa operacion.
     *
     * @param keyInfo lo que el documento dice; ver la nota de la clase
     * @param purpose para que se la quiere
     * @param method el algoritmo que la va a usar
     * @throws KeySelectorException si no se puede elegir ninguna
     */
    public abstract KeySelectorResult select(KeyInfo keyInfo, Purpose purpose,
                                             AlgorithmMethod method, XMLCryptoContext context)
        throws KeySelectorException;

    /**
     * Un selector que siempre devuelve esa clave.
     *
     * <p>Ignora el {@code KeyInfo} por completo, que es justamente lo que lo hace seguro: la clave la
     * elige quien valida y no el documento.
     *
     * @throws NullPointerException si la clave es null
     */
    public static KeySelector singletonKeySelector(Key key) {
        if (key == null) {
            throw new NullPointerException("key is null");
        }
        return new SingletonKeySelector(key);
    }

    /**
     * Para que se quiere la clave.
     *
     * <p>No es un enum porque la clase es de 2005 y su forma quedo fijada; son cuatro constantes con
     * constructor privado, que es el patron de enum a mano de esa epoca.
     */
    public static class Purpose {

        /** Para firmar. */
        public static final Purpose SIGN = new Purpose("sign");

        /** Para validar una firma. */
        public static final Purpose VERIFY = new Purpose("verify");

        /** Para cifrar. */
        public static final Purpose ENCRYPT = new Purpose("encrypt");

        /** Para descifrar. */
        public static final Purpose DECRYPT = new Purpose("decrypt");

        private final String name;

        private Purpose(String name) {
            this.name = name;
        }

        /** El nombre del proposito. */
        public String toString() {
            return this.name;
        }
    }

    /** El que devuelve {@link KeySelector#singletonKeySelector}. */
    private static final class SingletonKeySelector extends KeySelector {

        private final Key key;

        SingletonKeySelector(Key key) {
            this.key = key;
        }

        /** Siempre la misma, sin mirar nada. */
        public KeySelectorResult select(KeyInfo keyInfo, Purpose purpose, AlgorithmMethod method,
                                        XMLCryptoContext context) {
            return new SingletonResult(this.key);
        }
    }

    /** El resultado de {@link SingletonKeySelector}. */
    private static final class SingletonResult implements KeySelectorResult {

        private final Key key;

        SingletonResult(Key key) {
            this.key = key;
        }

        public Key getKey() {
            return this.key;
        }
    }
}
