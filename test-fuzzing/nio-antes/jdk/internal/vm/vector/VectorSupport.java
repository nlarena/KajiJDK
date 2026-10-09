package jdk.internal.vm.vector;

/**
 * The internal base {@code jdk.incubator.vector} rests on.
 *
 * <p>It is not public API --{@code jdk.internal.*} is not exported-- and it exists for a very
 * concrete reason: the VM needs <strong>a common type</strong> to recognise in order to be able to
 * replace the vector operations by instructions of the machine. That recognition is the whole point
 * of the vector API: without it, each operation would be a call to a Java method and there would be
 * no gain at all.
 *
 * <p>{@link VectorPayload} keeps the payload --the array of lanes-- in a single field. That it is
 * an {@code Object} and not a concrete type is what allows the same class to serve for lanes of any
 * type, and what leaves the VM the freedom to replace that field by a vector register when it can.
 */
public class VectorSupport {

    /** For whoever instantiates it; the class only groups the ones inside. */
    public VectorSupport() {
    }

    /**
     * What carries the payload of a vector, a mask or a shuffle.
     *
     * <p>A single field, {@code final}, with the array of lanes inside.
     */
    public static class VectorPayload {

        private final Object payload;

        /**
         * With that payload.
         *
         * @param payload the array of lanes
         */
        public VectorPayload(Object payload) {
            this.payload = payload;
        }

        /**
         * The payload.
         *
         * <p>It is {@code protected} and {@code final}: only the hierarchy sees it, and nobody can
         * replace it by something else. The two conditions are needed for the VM to be able to
         * reason about it.
         *
         * @return the array of lanes
         */
        protected final Object getPayload() {
            return payload;
        }
    }

    /**
     * The base of {@code jdk.incubator.vector.Vector}.
     *
     * @param <E> the type of the lanes, boxed
     */
    public static class Vector<E> extends VectorPayload {

        /**
         * With that payload.
         *
         * @param payload the array of lanes
         */
        public Vector(Object payload) {
            super(payload);
        }
    }

    /**
     * The base of {@code jdk.incubator.vector.VectorMask}.
     *
     * @param <E> the type of the lanes, boxed
     */
    public static class VectorMask<E> extends VectorPayload {

        /**
         * With that payload.
         *
         * @param payload the array of flags
         */
        public VectorMask(Object payload) {
            super(payload);
        }
    }

    /**
     * The base of {@code jdk.incubator.vector.VectorShuffle}.
     *
     * @param <E> the type of the lanes, boxed
     */
    public static class VectorShuffle<E> extends VectorPayload {

        /**
         * With that payload.
         *
         * @param payload the array of indices
         */
        public VectorShuffle(Object payload) {
            super(payload);
        }
    }
}
