package jdk.incubator.vector;

import java.nio.ByteOrder;
import java.util.function.IntUnaryOperator;

/**
 * La especie concreta de este paquete: un tipo de posicion mas una forma.
 *
 * <h2>Que parte es real</h2>
 *
 * <p>Una especie es sobre todo una cuenta. Cuantas posiciones entran, cuantos bits ocupa cada una,
 * hasta donde llega un bucle que avanza de a un vector, cuantas partes hacen falta para pasar de una
 * especie a otra: todo eso sale de dos numeros y no necesita ninguna instruccion vectorial. Esta
 * implementado de verdad y comprobado contra el JDK 25, incluido {@link #partLimit} en las 1152
 * combinaciones de tipo y forma que hay.
 *
 * <p>Lo que no puede funcionar es lo que <strong>fabrica</strong>: {@link #zero()},
 * {@link #fromArray}, {@link #maskAll} y las demas devolverian un vector, y para eso si hacen falta
 * los intrinsecos. Esas tiran {@link UnsupportedOperationException}.
 *
 * <h2>Dos respuestas menos precisas que las del JDK</h2>
 *
 * <p>{@link #vectorType()} contesta {@code IntVector.class} donde el JDK contesta
 * {@code Int128Vector.class}, y {@link #maskType()} contesta {@link Mascara} donde el JDK tiene una
 * clase de mascara por cada combinacion. Las clases del JDK son privadas de su paquete --nadie las
 * puede nombrar desde afuera-- y existen porque cada una lleva su implementacion intrinseca. Sin
 * intrinsecos no hay nada que las distinga, asi que hay una sola. La respuesta sigue cumpliendo lo
 * que el tipo promete: es una clase de vector, y es una clase de mascara.
 *
 * @param <E> el tipo de la posicion, en su version envuelta
 */
final class Especie<E> implements VectorSpecies<E> {

    private final Class<?> etype;
    private final VectorShape shape;

    private Especie(final Class<?> etype, final VectorShape shape) {
        this.etype = etype;
        this.shape = shape;
    }

    /**
     * La especie de ese tipo y esa forma.
     *
     * @param <E> el tipo de la posicion, envuelto
     * @param etype el tipo de la posicion
     * @param shape la forma
     * @return la especie
     * @throws IllegalArgumentException si el tipo no puede ser posicion de un vector
     */
    static <E> VectorSpecies<E> de(final Class<E> etype, final VectorShape shape) {
        return new Especie<E>(validar(etype), shape);
    }

    private static Class<?> validar(final Class<?> t) {
        if (t != byte.class && t != short.class && t != int.class && t != long.class
                && t != float.class && t != double.class) {
            throw new IllegalArgumentException("Bad vector element type: " + t);
        }
        return t;
    }

    /**
     * El tamano en bits de una posicion de ese tipo.
     *
     * @param t el tipo
     * @return 8, 16, 32 o 64
     */
    static int bitsDe(final Class<?> t) {
        if (t == byte.class) {
            return 8;
        }
        if (t == short.class) {
            return 16;
        }
        if (t == int.class || t == float.class) {
            return 32;
        }
        if (t == long.class || t == double.class) {
            return 64;
        }
        throw new IllegalArgumentException("Bad vector element type: " + t);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<E> elementType() {
        return (Class<E>) etype;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends Vector<E>> vectorType() {
        final Class<?> c;
        if (etype == byte.class) {
            c = ByteVector.class;
        } else if (etype == short.class) {
            c = ShortVector.class;
        } else if (etype == int.class) {
            c = IntVector.class;
        } else if (etype == long.class) {
            c = LongVector.class;
        } else if (etype == float.class) {
            c = FloatVector.class;
        } else {
            c = DoubleVector.class;
        }
        return (Class<? extends Vector<E>>) c;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends VectorMask<E>> maskType() {
        return (Class<? extends VectorMask<E>>) (Class<?>) Mascara.class;
    }

    @Override
    public int elementSize() {
        return bitsDe(etype);
    }

    @Override
    public VectorShape vectorShape() {
        return shape;
    }

    @Override
    public int length() {
        return vectorBitSize() / elementSize();
    }

    @Override
    public int vectorBitSize() {
        return shape.vectorBitSize();
    }

    @Override
    public int vectorByteSize() {
        return vectorBitSize() / 8;
    }

    /**
     * El multiplo de {@link #length()} mas grande que no pasa de ese numero.
     *
     * <p>Es lo que va en la condicion de un bucle que avanza de a un vector entero, para saber
     * donde parar y pasar al remanente. Se hace con bits y no con una division porque la cantidad de
     * posiciones siempre es una potencia de dos; ademas asi redondea hacia abajo tambien con
     * negativos, que es lo que hace el JDK.
     *
     * @param length el largo
     * @return el multiplo
     */
    @Override
    public int loopBound(final int length) {
        return length & ~(length() - 1);
    }

    @Override
    public long loopBound(final long length) {
        return length & ~((long) length() - 1);
    }

    @Override
    public VectorMask<E> indexInRange(final int offset, final int limit) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    @Override
    public VectorMask<E> indexInRange(final long offset, final long limit) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <F> VectorSpecies<F> check(final Class<F> elementType) {
        if (elementType != etype) {
            throw new ClassCastException(this + ": required " + elementType.getName()
                    + " but found " + etype.getName());
        }
        return (VectorSpecies<F>) this;
    }

    /**
     * Cuantas partes hacen falta para ir de esta especie a la otra.
     *
     * <p>Cuando el destino no entra en el origen el resultado es negativo, y ahi el numero dice
     * cuantas veces hay que <strong>expandir</strong> en lugar de partir. Cero quiere decir que no
     * hay que hacer ninguna de las dos cosas.
     *
     * <p>La cuenta es entre logaritmos porque todo lo que interviene es potencia de dos: el tamano
     * del vector y el de la posicion. Con {@code lanewise} se cuenta ademas el cambio de tamano de
     * la posicion, que es lo que distingue reinterpretar de convertir.
     *
     * @param toSpecies la especie de destino
     * @param lanewise si se convierte posicion por posicion
     * @return la cantidad de partes, negativa si hay que expandir
     */
    @Override
    public int partLimit(final VectorSpecies<?> toSpecies, final boolean lanewise) {
        int origen = log2(shape.vectorBitSize());
        final int destino = log2(toSpecies.vectorShape().vectorBitSize());
        if (lanewise) {
            origen += log2(toSpecies.elementSize()) - log2(elementSize());
        }
        final int d = origen - destino;
        if (d == 0) {
            return 0;
        }
        return d > 0 ? 1 << d : -(1 << -d);
    }

    private static int log2(final int n) {
        return Integer.numberOfTrailingZeros(n);
    }

    @Override
    public <F> VectorSpecies<F> withLanes(final Class<F> elementType) {
        return Especie.de(elementType, shape);
    }

    @Override
    public VectorSpecies<E> withShape(final VectorShape newShape) {
        return new Especie<E>(etype, newShape);
    }

    @Override
    public Vector<E> zero() {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    @Override
    public Vector<E> fromArray(final Object a, final int offset) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    @Override
    public Vector<E> fromMemorySegment(final java.lang.foreign.MemorySegment ms, final long offset,
            final ByteOrder bo) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    @Override
    public VectorMask<E> loadMask(final boolean[] bits, final int offset) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    @Override
    public VectorMask<E> maskAll(final boolean bit) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    @Override
    public Vector<E> broadcast(final long e) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    /**
     * Comprueba que ese valor entre en una posicion de esta especie.
     *
     * <p>La prueba es ir y volver: se lo convierte al tipo de la posicion y se lo trae de vuelta. Si
     * no da lo mismo, no entraba. Sirve igual para los enteros --donde se pierden bits altos-- y
     * para {@code float}, donde lo que se pierde son cifras significativas.
     *
     * @param e el valor
     * @return el mismo valor
     * @throws IllegalArgumentException si no entra
     */
    @Override
    public long checkValue(final long e) {
        final long vuelta;
        final String comoQueda;
        if (etype == byte.class) {
            vuelta = (byte) e;
            comoQueda = String.valueOf((byte) e);
        } else if (etype == short.class) {
            vuelta = (short) e;
            comoQueda = String.valueOf((short) e);
        } else if (etype == int.class) {
            vuelta = (int) e;
            comoQueda = String.valueOf((int) e);
        } else if (etype == float.class) {
            vuelta = (long) (float) e;
            comoQueda = String.valueOf((float) e);
        } else if (etype == double.class) {
            vuelta = (long) (double) e;
            comoQueda = String.valueOf((double) e);
        } else {
            return e;
        }
        if (vuelta != e) {
            throw new IllegalArgumentException("Vector creation failed: value " + e
                    + " cannot be represented in ETYPE " + etype.getName()
                    + "; result of cast is " + comoQueda);
        }
        return e;
    }

    @Override
    public VectorShuffle<E> shuffleFromValues(final int... sourceIndexes) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    @Override
    public VectorShuffle<E> shuffleFromArray(final int[] sourceIndexes, final int offset) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    @Override
    public VectorShuffle<E> shuffleFromOp(final IntUnaryOperator fn) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    @Override
    public VectorShuffle<E> iotaShuffle(final int start, final int step, final boolean wrap) {
        throw new UnsupportedOperationException(Msg.NO_HAY);
    }

    @Override
    public String toString() {
        return "Species[" + etype.getName() + ", " + length() + ", " + shape + "]";
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Especie)) {
            return false;
        }
        final Especie<?> o = (Especie<?>) obj;
        return etype == o.etype && shape == o.shape;
    }

    @Override
    public int hashCode() {
        return etype.hashCode() * 31 + shape.hashCode();
    }
}
