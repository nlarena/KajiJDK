package java.lang.foreign;

import java.nio.ByteOrder;
import java.util.Optional;

// The implementations of `ValueLayout` and `AddressLayout`. They are package-private: nobody names
// them from outside, they are obtained through `ValueLayout`'s constants.
//
// **A layout is immutable**, and that governs the whole shape of the file: each `with*` builds a new
// one instead of mutating. It is no luxury -- `ValueLayout.JAVA_INT` is a constant shared by the
// whole program, and a `withName` that mutated would change its name for everybody.
//
// The base class gathers what does not depend on the carried type: size, alignment, name, order, and
// the comparison. The eight subtypes exist **only** to narrow the `with*` return type, which is what
// allows chaining them without casting and --more importantly-- what makes `MemorySegment.get`'s
// overload pick the one returning the right primitive.
abstract class ValueBase implements ValueLayout {

    // The letter the JDK prints each type with. They are copied because a layout's `toString` is
    // part of what people read when something does not add up, and saying something other than the
    // JDK would force a mental translation.
    static final char LETTER_BOOLEAN = 'z';
    static final char LETTER_BYTE = 'b';
    static final char LETTER_CHAR = 'c';
    static final char LETTER_SHORT = 's';
    static final char LETTER_INT = 'i';
    static final char LETTER_LONG = 'j';
    static final char LETTER_FLOAT = 'f';
    static final char LETTER_DOUBLE = 'd';
    static final char LETTER_ADDRESS = 'a';

    private final char letter;
    private final long size;
    private final long alignment;
    private final String name;
    private final ByteOrder order;

    ValueBase(char letter, long size, long alignment, String name, ByteOrder order) {
        this.letter = letter;
        this.size = size;
        this.alignment = alignment;
        this.name = name;
        this.order = order;
    }

    char letter() {
        return this.letter;
    }

    public long byteSize() {
        return this.size;
    }

    public long byteAlignment() {
        return this.alignment;
    }

    public Optional<String> name() {
        return Optional.ofNullable(this.name);
    }

    String rawName() {
        return this.name;
    }

    public ByteOrder order() {
        return this.order;
    }

    public long byteOffset(MemoryLayout.PathElement... elements) {
        return Layouts.offsetByPath(this, elements);
    }

    public MemoryLayout select(MemoryLayout.PathElement... elements) {
        return Layouts.selectByPath(this, elements);
    }

    public long scale(long offset, long index) {
        return Layouts.scaled(this, offset, index);
    }

    // Two layouts are equal if they describe **the same thing**, and the name is part of that: a
    // field called `x` and one called `y` of the same type are not interchangeable in a struct.
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ValueBase)) {
            return false;
        }
        ValueBase other = (ValueBase) obj;
        if (this.getClass() != other.getClass()) {
            return false;
        }
        boolean sameName = this.name == null ? other.name == null
                : this.name.equals(other.name);
        return this.letter == other.letter && this.size == other.size
                && this.alignment == other.alignment && this.order == other.order
                && sameName;
    }

    public int hashCode() {
        int h = this.letter;
        h = h * 31 + (int) this.size;
        h = h * 31 + (int) this.alignment;
        h = h * 31 + (this.order == ByteOrder.BIG_ENDIAN ? 1 : 0);
        h = h * 31 + (this.name == null ? 0 : this.name.hashCode());
        return h;
    }

    // The JDK's format, and the three parts all mean something:
    //   `1%`   the alignment, **only** when it is not the type's natural one;
    //   `I`    upper case if the order is big-endian, lower case if little;
    //   `(x)`  the name, if it has one.
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (this.alignment != this.size) {
            sb.append(this.alignment);
            sb.append('%');
        }
        char c = this.letter;
        if (this.order == ByteOrder.BIG_ENDIAN) {
            c = Character.toUpperCase(c);
        }
        sb.append(c);
        sb.append(this.size);
        this.appendName(sb);
        return sb.toString();
    }

    void appendName(StringBuilder sb) {
        if (this.name != null) {
            sb.append('(');
            sb.append(this.name);
            sb.append(')');
        }
    }
}

final class ValueBoolean extends ValueBase implements ValueLayout.OfBoolean {

    ValueBoolean(long alignment, String name, ByteOrder order) {
        super(ValueBase.LETTER_BOOLEAN, 1L, alignment, name, order);
    }

    public Class<?> carrier() {
        return Boolean.TYPE;
    }

    public ValueLayout.OfBoolean withName(String name) {
        return new ValueBoolean(this.byteAlignment(), Layouts.requireName(name), this.order());
    }

    public ValueLayout.OfBoolean withoutName() {
        return new ValueBoolean(this.byteAlignment(), null, this.order());
    }

    public ValueLayout.OfBoolean withByteAlignment(long byteAlignment) {
        return new ValueBoolean(Layouts.requireAlignment(byteAlignment), this.rawName(),
                this.order());
    }

    public ValueLayout.OfBoolean withOrder(ByteOrder order) {
        return new ValueBoolean(this.byteAlignment(), this.rawName(),
                Layouts.requireOrder(order));
    }
}

final class ValueByte extends ValueBase implements ValueLayout.OfByte {

    ValueByte(long alignment, String name, ByteOrder order) {
        super(ValueBase.LETTER_BYTE, 1L, alignment, name, order);
    }

    public Class<?> carrier() {
        return Byte.TYPE;
    }

    public ValueLayout.OfByte withName(String name) {
        return new ValueByte(this.byteAlignment(), Layouts.requireName(name), this.order());
    }

    public ValueLayout.OfByte withoutName() {
        return new ValueByte(this.byteAlignment(), null, this.order());
    }

    public ValueLayout.OfByte withByteAlignment(long byteAlignment) {
        return new ValueByte(Layouts.requireAlignment(byteAlignment), this.rawName(),
                this.order());
    }

    public ValueLayout.OfByte withOrder(ByteOrder order) {
        return new ValueByte(this.byteAlignment(), this.rawName(), Layouts.requireOrder(order));
    }
}

final class ValueChar extends ValueBase implements ValueLayout.OfChar {

    ValueChar(long alignment, String name, ByteOrder order) {
        super(ValueBase.LETTER_CHAR, 2L, alignment, name, order);
    }

    public Class<?> carrier() {
        return Character.TYPE;
    }

    public ValueLayout.OfChar withName(String name) {
        return new ValueChar(this.byteAlignment(), Layouts.requireName(name), this.order());
    }

    public ValueLayout.OfChar withoutName() {
        return new ValueChar(this.byteAlignment(), null, this.order());
    }

    public ValueLayout.OfChar withByteAlignment(long byteAlignment) {
        return new ValueChar(Layouts.requireAlignment(byteAlignment), this.rawName(),
                this.order());
    }

    public ValueLayout.OfChar withOrder(ByteOrder order) {
        return new ValueChar(this.byteAlignment(), this.rawName(), Layouts.requireOrder(order));
    }
}

final class ValueShort extends ValueBase implements ValueLayout.OfShort {

    ValueShort(long alignment, String name, ByteOrder order) {
        super(ValueBase.LETTER_SHORT, 2L, alignment, name, order);
    }

    public Class<?> carrier() {
        return Short.TYPE;
    }

    public ValueLayout.OfShort withName(String name) {
        return new ValueShort(this.byteAlignment(), Layouts.requireName(name), this.order());
    }

    public ValueLayout.OfShort withoutName() {
        return new ValueShort(this.byteAlignment(), null, this.order());
    }

    public ValueLayout.OfShort withByteAlignment(long byteAlignment) {
        return new ValueShort(Layouts.requireAlignment(byteAlignment), this.rawName(),
                this.order());
    }

    public ValueLayout.OfShort withOrder(ByteOrder order) {
        return new ValueShort(this.byteAlignment(), this.rawName(), Layouts.requireOrder(order));
    }
}

final class ValueInt extends ValueBase implements ValueLayout.OfInt {

    ValueInt(long alignment, String name, ByteOrder order) {
        super(ValueBase.LETTER_INT, 4L, alignment, name, order);
    }

    public Class<?> carrier() {
        return Integer.TYPE;
    }

    public ValueLayout.OfInt withName(String name) {
        return new ValueInt(this.byteAlignment(), Layouts.requireName(name), this.order());
    }

    public ValueLayout.OfInt withoutName() {
        return new ValueInt(this.byteAlignment(), null, this.order());
    }

    public ValueLayout.OfInt withByteAlignment(long byteAlignment) {
        return new ValueInt(Layouts.requireAlignment(byteAlignment), this.rawName(),
                this.order());
    }

    public ValueLayout.OfInt withOrder(ByteOrder order) {
        return new ValueInt(this.byteAlignment(), this.rawName(), Layouts.requireOrder(order));
    }
}

final class ValueLong extends ValueBase implements ValueLayout.OfLong {

    ValueLong(long alignment, String name, ByteOrder order) {
        super(ValueBase.LETTER_LONG, 8L, alignment, name, order);
    }

    public Class<?> carrier() {
        return Long.TYPE;
    }

    public ValueLayout.OfLong withName(String name) {
        return new ValueLong(this.byteAlignment(), Layouts.requireName(name), this.order());
    }

    public ValueLayout.OfLong withoutName() {
        return new ValueLong(this.byteAlignment(), null, this.order());
    }

    public ValueLayout.OfLong withByteAlignment(long byteAlignment) {
        return new ValueLong(Layouts.requireAlignment(byteAlignment), this.rawName(),
                this.order());
    }

    public ValueLayout.OfLong withOrder(ByteOrder order) {
        return new ValueLong(this.byteAlignment(), this.rawName(), Layouts.requireOrder(order));
    }
}

final class ValueFloat extends ValueBase implements ValueLayout.OfFloat {

    ValueFloat(long alignment, String name, ByteOrder order) {
        super(ValueBase.LETTER_FLOAT, 4L, alignment, name, order);
    }

    public Class<?> carrier() {
        return Float.TYPE;
    }

    public ValueLayout.OfFloat withName(String name) {
        return new ValueFloat(this.byteAlignment(), Layouts.requireName(name), this.order());
    }

    public ValueLayout.OfFloat withoutName() {
        return new ValueFloat(this.byteAlignment(), null, this.order());
    }

    public ValueLayout.OfFloat withByteAlignment(long byteAlignment) {
        return new ValueFloat(Layouts.requireAlignment(byteAlignment), this.rawName(),
                this.order());
    }

    public ValueLayout.OfFloat withOrder(ByteOrder order) {
        return new ValueFloat(this.byteAlignment(), this.rawName(), Layouts.requireOrder(order));
    }
}

final class ValueDouble extends ValueBase implements ValueLayout.OfDouble {

    ValueDouble(long alignment, String name, ByteOrder order) {
        super(ValueBase.LETTER_DOUBLE, 8L, alignment, name, order);
    }

    public Class<?> carrier() {
        return Double.TYPE;
    }

    public ValueLayout.OfDouble withName(String name) {
        return new ValueDouble(this.byteAlignment(), Layouts.requireName(name), this.order());
    }

    public ValueLayout.OfDouble withoutName() {
        return new ValueDouble(this.byteAlignment(), null, this.order());
    }

    public ValueLayout.OfDouble withByteAlignment(long byteAlignment) {
        return new ValueDouble(Layouts.requireAlignment(byteAlignment), this.rawName(),
                this.order());
    }

    public ValueLayout.OfDouble withOrder(ByteOrder order) {
        return new ValueDouble(this.byteAlignment(), this.rawName(),
                Layouts.requireOrder(order));
    }
}

// The address. It is an eight-byte value **plus** the layout it points at, which is optional: in C
// there are pointers to `void`, and forcing a target would mean inventing one.
final class ValueAddress extends ValueBase implements AddressLayout {

    private final MemoryLayout target;

    ValueAddress(long alignment, String name, ByteOrder order, MemoryLayout target) {
        super(ValueBase.LETTER_ADDRESS, 8L, alignment, name, order);
        this.target = target;
    }

    public Class<?> carrier() {
        return MemorySegment.class;
    }

    public Optional<MemoryLayout> targetLayout() {
        return Optional.ofNullable(this.target);
    }

    public AddressLayout withTargetLayout(MemoryLayout layout) {
        if (layout == null) {
            throw new IllegalArgumentException("the target cannot be null");
        }
        return new ValueAddress(this.byteAlignment(), this.rawName(), this.order(), layout);
    }

    public AddressLayout withoutTargetLayout() {
        return new ValueAddress(this.byteAlignment(), this.rawName(), this.order(), null);
    }

    public AddressLayout withName(String name) {
        return new ValueAddress(this.byteAlignment(), Layouts.requireName(name), this.order(),
                this.target);
    }

    public AddressLayout withoutName() {
        return new ValueAddress(this.byteAlignment(), null, this.order(), this.target);
    }

    public AddressLayout withByteAlignment(long byteAlignment) {
        return new ValueAddress(Layouts.requireAlignment(byteAlignment), this.rawName(),
                this.order(), this.target);
    }

    public AddressLayout withOrder(ByteOrder order) {
        return new ValueAddress(this.byteAlignment(), this.rawName(), Layouts.requireOrder(order),
                this.target);
    }

    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        ValueAddress other = (ValueAddress) obj;
        return this.target == null ? other.target == null : this.target.equals(other.target);
    }

    public int hashCode() {
        return super.hashCode() * 31 + (this.target == null ? 0 : this.target.hashCode());
    }

    // `a8:i4` -- the target goes after a colon, which is how the JDK prints it.
    public String toString() {
        String base = super.toString();
        if (this.target == null) {
            return base;
        }
        return base + ":" + this.target.toString();
    }
}
