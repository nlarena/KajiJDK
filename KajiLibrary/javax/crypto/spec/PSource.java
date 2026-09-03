package javax.crypto.spec;

/**
 * De donde sale la etiqueta de entrada (`P`) de OAEP.
 *
 * <p>Es una clase y no una enumeracion porque el estandar deja la puerta abierta a otras fuentes,
 * pero en la practica hay una sola: {@link PSpecified}, "los bytes que le doy". Que la jerarquia
 * exista con un solo miembro es del diseno de PKCS#1, no de esta biblioteca.
 *
 * <p>El constructor es `protected`: la clase base no representa ninguna fuente por si sola, solo el
 * concepto.
 */
public class PSource {

    private final String pSrcName;

    /**
     * @throws NullPointerException si el nombre es nulo
     */
    protected PSource(String pSrcName) {
        if (pSrcName == null) {
            throw new NullPointerException("el nombre de la fuente no puede ser nulo");
        }
        this.pSrcName = pSrcName;
    }

    /** El nombre del algoritmo de la fuente. */
    public String getAlgorithm() {
        return this.pSrcName;
    }

    /**
     * La fuente que son unos bytes dados.
     *
     * <p>{@link #DEFAULT} es la etiqueta **vacia**, que es lo que casi todo el mundo usa: OAEP con
     * etiqueta vacia es lo que hacen TLS y la mayoria de los protocolos. Tenerla como constante
     * evita fabricar un arreglo de cero bytes en cada llamada.
     */
    public static final class PSpecified extends PSource {

        /** La etiqueta vacia. */
        public static final PSpecified DEFAULT = new PSpecified(new byte[0]);

        private final byte[] p;

        /**
         * @throws NullPointerException si el arreglo es nulo
         */
        public PSpecified(byte[] p) {
            super("PSpecified");
            if (p == null) {
                throw new NullPointerException("la etiqueta no puede ser nula");
            }
            this.p = IvParameterSpec.copy(p, 0, p.length);
        }

        /** Una copia de la etiqueta. */
        public byte[] getValue() {
            return IvParameterSpec.copy(this.p, 0, this.p.length);
        }
    }
}
