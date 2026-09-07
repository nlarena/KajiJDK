package jdk.internal.vm.vector;

/**
 * La base interna sobre la que se apoya {@code jdk.incubator.vector}.
 *
 * <p>No es API publica --{@code jdk.internal.*} no se exporta-- y existe por una razon muy concreta:
 * la VM necesita <strong>un tipo comun</strong> al que reconocer para poder reemplazar las
 * operaciones de vector por instrucciones de la maquina. Ese reconocimiento es todo el punto del
 * API de vectores: sin el, cada operacion seria una llamada a un metodo Java y no habria ninguna
 * ganancia.
 *
 * <p>{@link VectorPayload} guarda la carga util --el arreglo de carriles-- en un solo campo. Que
 * sea un {@code Object} y no un tipo concreto es lo que permite que la misma clase sirva para
 * carriles de cualquier tipo, y lo que le deja a la VM la libertad de reemplazar ese campo por un
 * registro vectorial cuando puede.
 */
public class VectorSupport {

    /** Para quien la instancie; la clase solo agrupa a las de adentro. */
    public VectorSupport() {
    }

    /**
     * Lo que lleva la carga util de un vector, una mascara o una permutacion.
     *
     * <p>Un solo campo, {@code final}, con el arreglo de carriles adentro.
     */
    public static class VectorPayload {

        private final Object payload;

        /**
         * Con esa carga util.
         *
         * @param payload el arreglo de carriles
         */
        public VectorPayload(Object payload) {
            this.payload = payload;
        }

        /**
         * La carga util.
         *
         * <p>Es {@code protected} y {@code final}: solo la jerarquia la ve, y nadie la puede
         * reemplazar por otra cosa. Las dos condiciones hacen falta para que la VM pueda razonar
         * sobre ella.
         *
         * @return el arreglo de carriles
         */
        protected final Object getPayload() {
            return payload;
        }
    }

    /**
     * La base de {@code jdk.incubator.vector.Vector}.
     *
     * @param <E> el tipo de los carriles, encajonado
     */
    public static class Vector<E> extends VectorPayload {

        /**
         * Con esa carga util.
         *
         * @param payload el arreglo de carriles
         */
        public Vector(Object payload) {
            super(payload);
        }
    }

    /**
     * La base de {@code jdk.incubator.vector.VectorMask}.
     *
     * @param <E> el tipo de los carriles, encajonado
     */
    public static class VectorMask<E> extends VectorPayload {

        /**
         * Con esa carga util.
         *
         * @param payload el arreglo de banderas
         */
        public VectorMask(Object payload) {
            super(payload);
        }
    }

    /**
     * La base de {@code jdk.incubator.vector.VectorShuffle}.
     *
     * @param <E> el tipo de los carriles, encajonado
     */
    public static class VectorShuffle<E> extends VectorPayload {

        /**
         * Con esa carga util.
         *
         * @param payload el arreglo de indices
         */
        public VectorShuffle(Object payload) {
            super(payload);
        }
    }
}
