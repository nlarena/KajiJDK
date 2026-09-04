package java.lang;

import java.io.Serializable;

// KajiLibrary's java.lang.Number — the abstract superclass of the numeric wrappers
// (Integer, Long, Short, Byte, Float, Double). Each subclass holds a primitive value and
// exposes it as any numeric type; the two narrowing views default to the int one.
//
// `Serializable` is declared HERE and not on each wrapper, which is what the JDK does and is
// why `Integer implements Serializable` holds without Integer saying so. It is not decoration:
// `Serializable.class.isAssignableFrom(Integer.class)` answers from this clause, and a library
// that omits it makes every `instanceof Serializable` on a boxed number quietly false. Nothing
// in the API census catches that — an implements clause is not a member.
public abstract class Number implements Serializable {

    public abstract int intValue();

    public abstract long longValue();

    public abstract float floatValue();

    public abstract double doubleValue();

    public byte byteValue() {
        return (byte) intValue();
    }

    public short shortValue() {
        return (short) intValue();
    }
}
