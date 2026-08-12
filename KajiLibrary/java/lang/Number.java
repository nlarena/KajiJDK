package java.lang;

// KajiLibrary's java.lang.Number — the abstract superclass of the numeric wrappers
// (Integer, Long, Short, Byte, Float, Double). Each subclass holds a primitive value and
// exposes it as any numeric type; the two narrowing views default to the int one.
public abstract class Number {

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
