package jdk.incubator.vector;

/**
 * La mascara concreta de este paquete.
 *
 * <h2>Para que existe una clase que no hace nada</h2>
 *
 * <p>{@link VectorSpecies#maskType()} tiene que devolver la clase de las mascaras de esa especie, y
 * {@link VectorMask} es abstracta: no se la puede devolver ahi sin mentir. En el JDK la respuesta es
 * una clase concreta por cada combinacion de tipo y forma, todas privadas del paquete. Esta es su
 * equivalente: privada del paquete igual, y una sola, porque sin intrinsecos ninguna se distingue de
 * las otras.
 *
 * <p>Todos sus metodos tiran {@link UnsupportedOperationException}. No es un descuido: una mascara
 * sin la maquina abajo no puede contestar cuantas posiciones estan prendidas, y devolver cero seria
 * una respuesta falsa que compila y da resultados equivocados sin avisar.
 *
 * @param <E> el tipo de la posicion, en su version envuelta
 */
final class Mascara<E> extends VectorMask<E> {

    Mascara(Object payload) {
        super(payload);
    }

    @Override
    public VectorSpecies<E> vectorSpecies() {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    @Override
    public <F extends Object> VectorMask<F> cast(VectorSpecies<F> vectorSpecies) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    @Override
    public long toLong() {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    @Override
    public boolean[] toArray() {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    @Override
    public void intoArray(boolean[] flags, int i) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    @Override
    public boolean anyTrue() {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    @Override
    public boolean allTrue() {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    @Override
    public int trueCount() {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    @Override
    public int firstTrue() {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    @Override
    public int lastTrue() {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    @Override
    public VectorMask<E> and(VectorMask<E> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    @Override
    public VectorMask<E> or(VectorMask<E> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    @Override
    public VectorMask<E> xor(VectorMask<E> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    @Override
    public VectorMask<E> andNot(VectorMask<E> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    @Override
    public VectorMask<E> eq(VectorMask<E> vectorMask) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    @Override
    public VectorMask<E> not() {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    @Override
    public VectorMask<E> indexInRange(int i, int i2) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    @Override
    public VectorMask<E> indexInRange(long l, long l2) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    @Override
    public Vector<E> toVector() {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    @Override
    public boolean laneIsSet(int i) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    @Override
    public <F extends Object> VectorMask<F> check(Class<F> classArg) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    @Override
    public <F extends Object> VectorMask<F> check(VectorSpecies<F> vectorSpecies) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    @Override
    public VectorMask<E> compress() {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }
}
