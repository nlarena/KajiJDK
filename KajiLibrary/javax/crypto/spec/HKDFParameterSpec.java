package javax.crypto.spec;

import java.security.spec.AlgorithmParameterSpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.crypto.SecretKey;

/**
 * Los parametros de HKDF (RFC 5869), la derivacion de claves en dos pasos.
 *
 * <p>Los dos pasos son lo que explica la forma de esta interfaz, que si no parece caprichosa:
 *
 * <ul>
 * <li><strong>Extraer</strong> toma material de entrada --que puede no ser uniforme: un secreto
 *     Diffie-Hellman, una contrasena estirada-- y una sal, y produce una clave pseudoaleatoria
 *     (`PRK`) que si lo es.</li>
 * <li><strong>Expandir</strong> toma esa `PRK`, un texto de contexto y un largo, y produce la
 *     clave final. Se puede expandir varias veces la misma `PRK` con contextos distintos para
 *     obtener claves independientes.</li>
 * </ul>
 *
 * <p>De ahi las tres implementaciones: {@link Extract} hace solo el primer paso, {@link Expand}
 * solo el segundo --cuando uno ya tiene la `PRK`-- y {@link ExtractThenExpand} los dos de una.
 * Son las tres formas legitimas de usar HKDF y no hay una cuarta, por eso son `final` y no hay
 * constructor publico de la interfaz.
 *
 * <p><strong>El material se acumula.</strong> {@link Builder#addIKM} y {@link Builder#addSalt} se
 * pueden llamar varias veces, y lo que se pasa se **concatena** en orden en vez de reemplazar. No
 * es una comodidad: permite armar la entrada a partir de pedazos que llegan por separado sin tener
 * que juntarlos en un arreglo antes, que es justamente lo que uno quiere evitar con material
 * secreto.
 */
public interface HKDFParameterSpec extends AlgorithmParameterSpec {

    /** Un constructor para las formas que empiezan por extraer. */
    public static Builder ofExtract() {
        return new Builder();
    }

    /**
     * La forma que **solo expande**, a partir de una `PRK` que uno ya tiene.
     *
     * @param prk la clave pseudoaleatoria; no puede ser nula
     * @param info el contexto, o nulo para ninguno
     * @param length cuantos bytes se quieren, mayor que cero
     * @throws NullPointerException si `prk` es nula
     * @throws IllegalArgumentException si `length` no es positivo
     */
    public static Expand expandOnly(SecretKey prk, byte[] info, int length) {
        if (prk == null) {
            throw new NullPointerException("la PRK no puede ser nula");
        }
        if (length <= 0) {
            throw new IllegalArgumentException("el largo tiene que ser positivo");
        }
        return new Expand(prk, info, length);
    }

    /**
     * El constructor de las formas que extraen.
     *
     * <p>Es mutable y se usa una vez: se le agregan entradas y se termina con {@link #extractOnly}
     * o {@link #thenExpand}. Los dos devuelven un objeto ya congelado, asi que seguir usando el
     * constructor despues no cambia lo que se devolvio.
     */
    public static final class Builder {

        private final List<SecretKey> ikms = new ArrayList<SecretKey>();
        private final List<SecretKey> salts = new ArrayList<SecretKey>();

        Builder() {
        }

        /** Solo el primer paso: produce la `PRK` y ahi termina. */
        public Extract extractOnly() {
            return new Extract(this.ikms, this.salts);
        }

        /**
         * Los dos pasos: extrae y despues expande con ese contexto a ese largo.
         *
         * @throws IllegalArgumentException si `length` no es positivo
         */
        public ExtractThenExpand thenExpand(byte[] info, int length) {
            if (length <= 0) {
                throw new IllegalArgumentException("el largo tiene que ser positivo");
            }
            return new ExtractThenExpand(this.ikms, this.salts, info, length);
        }

        /**
         * Agrega material de entrada. Ver la nota de la interfaz sobre la concatenacion.
         *
         * @throws NullPointerException si la clave es nula
         */
        public Builder addIKM(SecretKey ikm) {
            if (ikm == null) {
                throw new NullPointerException("el material de entrada no puede ser nulo");
            }
            this.ikms.add(ikm);
            return this;
        }

        /**
         * Agrega material de entrada en crudo.
         *
         * <p>Se envuelve en un {@link SecretKeySpec} con algoritmo `"Generic"`, que es el nombre
         * que el JDK usa para material que no pertenece a ningun algoritmo en particular. Un
         * arreglo **vacio** se ignora en vez de agregarse: concatenar cero bytes no cambia nada, y
         * guardarlo solo haria que la lista tuviera un elemento que no aporta.
         *
         * @throws NullPointerException si el arreglo es nulo
         */
        public Builder addIKM(byte[] ikm) {
            if (ikm == null) {
                throw new NullPointerException("el material de entrada no puede ser nulo");
            }
            if (ikm.length != 0) {
                this.ikms.add(new SecretKeySpec(ikm, "Generic"));
            }
            return this;
        }

        /**
         * Agrega sal.
         *
         * @throws NullPointerException si la clave es nula
         */
        public Builder addSalt(SecretKey salt) {
            if (salt == null) {
                throw new NullPointerException("la sal no puede ser nula");
            }
            this.salts.add(salt);
            return this;
        }

        /**
         * Agrega sal en crudo. Vale la misma nota que {@link #addIKM(byte[])}.
         *
         * @throws NullPointerException si el arreglo es nulo
         */
        public Builder addSalt(byte[] salt) {
            if (salt == null) {
                throw new NullPointerException("la sal no puede ser nula");
            }
            if (salt.length != 0) {
                this.salts.add(new SecretKeySpec(salt, "Generic"));
            }
            return this;
        }
    }

    /** Solo el primer paso: de material de entrada y sal a una `PRK`. */
    public static final class Extract implements HKDFParameterSpec {

        private final List<SecretKey> ikms;
        private final List<SecretKey> salts;

        Extract(List<SecretKey> ikms, List<SecretKey> salts) {
            this.ikms = Collections.unmodifiableList(new ArrayList<SecretKey>(ikms));
            this.salts = Collections.unmodifiableList(new ArrayList<SecretKey>(salts));
        }

        /** El material de entrada, en orden y de solo lectura. */
        public List<SecretKey> ikms() {
            return this.ikms;
        }

        /** La sal, en orden y de solo lectura. */
        public List<SecretKey> salts() {
            return this.salts;
        }
    }

    /** Solo el segundo paso: de una `PRK` y un contexto a la clave final. */
    public static final class Expand implements HKDFParameterSpec {

        private final SecretKey prk;
        private final byte[] info;
        private final int length;

        Expand(SecretKey prk, byte[] info, int length) {
            this.prk = prk;
            this.info = info == null ? null : IvParameterSpec.copy(info, 0, info.length);
            this.length = length;
        }

        /** La clave pseudoaleatoria de la que se expande. */
        public SecretKey prk() {
            return this.prk;
        }

        /** Una copia del contexto, o nulo si no hay. */
        public byte[] info() {
            return this.info == null ? null
                    : IvParameterSpec.copy(this.info, 0, this.info.length);
        }

        /** Cuantos bytes se quieren. */
        public int length() {
            return this.length;
        }
    }

    /** Los dos pasos de una. */
    public static final class ExtractThenExpand implements HKDFParameterSpec {

        private final List<SecretKey> ikms;
        private final List<SecretKey> salts;
        private final byte[] info;
        private final int length;

        ExtractThenExpand(List<SecretKey> ikms, List<SecretKey> salts, byte[] info, int length) {
            this.ikms = Collections.unmodifiableList(new ArrayList<SecretKey>(ikms));
            this.salts = Collections.unmodifiableList(new ArrayList<SecretKey>(salts));
            this.info = info == null ? null : IvParameterSpec.copy(info, 0, info.length);
            this.length = length;
        }

        /** El material de entrada, en orden y de solo lectura. */
        public List<SecretKey> ikms() {
            return this.ikms;
        }

        /** La sal, en orden y de solo lectura. */
        public List<SecretKey> salts() {
            return this.salts;
        }

        /** Una copia del contexto, o nulo si no hay. */
        public byte[] info() {
            return this.info == null ? null
                    : IvParameterSpec.copy(this.info, 0, this.info.length);
        }

        /** Cuantos bytes se quieren. */
        public int length() {
            return this.length;
        }
    }
}
