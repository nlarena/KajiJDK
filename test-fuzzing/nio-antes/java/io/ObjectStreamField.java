package java.io;

// KajiLibrary's java.io.ObjectStreamField -- the description of **one** serializable field.
//
// It is a value, not an operation: a name, a type, and whether it goes shared. It is worth having
// on its own -- whoever declares a `serialPersistentFields` is describing their class's shape, and
// that description is right whether it is serialized afterwards or not -- and it is also the
// currency the two streams understand each other in: the one writing takes each field's order and
// type code from here, and the one reading builds one per field coming from the other side, with
// **the stream's signature** and not with a local type's.
//
// **The order is part of the format, not a convenience.** `compareTo` puts the primitives before
// the references, and within each group sorts by name. The separation exists because the stream
// first writes every primitive value --of known size, one right after another-- and then the
// references, which each carry a structure of their own. Mixing them would force two ways of
// decoding to be interleaved in the same block.
public class ObjectStreamField implements Comparable<Object> {

    private final String name;
    private final Class<?> type;
    private final boolean unshared;

    // The type's JVM descriptor: `I`, `Ljava/lang/String;`, `[[D`. It is worked out once because
    // `toString` and `getTypeString` ask for it and building it walks the type's name.
    private final String signature;

    private int offset = 0;

    /** A shared field (`unshared` false), which is the normal thing. */
    public ObjectStreamField(String name, Class<?> type) {
        this(name, type, false);
    }

    /**
     * A field of the given type.
     *
     * <p>`unshared` set to true asks for the value to be written and read **without** going through
     * the stream's table of shared references. It serves when the object has to be exclusive to
     * this field: with the table, two fields that pointed at the same object go on sharing it after
     * deserializing, and a class depending on having one of its own breaks silently.
     *
     * @throws NullPointerException if `name` or `type` is `null` -- a field with no name or no type
     *     describes nothing
     */
    public ObjectStreamField(String name, Class<?> type, boolean unshared) {
        if (name == null || type == null) {
            throw new NullPointerException();
        }
        this.name = name;
        this.type = type;
        this.unshared = unshared;
        this.signature = descriptor(type);
    }

    /**
     * The field as it came from a stream: a name and **the stream's signature**, not a local
     * type's.
     *
     * <p>The signature is taken raw instead of derived from a `Class` because the stream may name a
     * type that does not exist on this side -- and that signature is precisely what has to be
     * compared against the local field in order to decide whether they are the same field. Deriving
     * it from a local type would force the class to be resolved before being able to compare, which
     * is the reverse of how reading goes.
     *
     * <p>`type` is left at {@code Object.class} for the reference fields, as in the JDK: the real
     * type may not be loaded, and `Object` is the only certain thing that can be asserted without
     * loading it.
     */
    ObjectStreamField(String name, String signature) {
        this.name = name;
        this.signature = signature;
        this.unshared = false;
        this.type = typeFromSignature(signature);
    }

    private static Class<?> typeFromSignature(String signature) {
        char c = signature.charAt(0);
        if (c == 'I') {
            return Integer.TYPE;
        }
        if (c == 'J') {
            return Long.TYPE;
        }
        if (c == 'D') {
            return Double.TYPE;
        }
        if (c == 'F') {
            return Float.TYPE;
        }
        if (c == 'B') {
            return Byte.TYPE;
        }
        if (c == 'S') {
            return Short.TYPE;
        }
        if (c == 'C') {
            return Character.TYPE;
        }
        if (c == 'Z') {
            return Boolean.TYPE;
        }
        return Object.class;
    }

    public String getName() {
        return this.name;
    }

    public Class<?> getType() {
        return this.type;
    }

    /**
     * The type's letter: `B C D F I J S Z` for the primitives, `[` for arrays, `L` for the rest.
     *
     * <p>It is the descriptor's first letter, and that is why it comes from there instead of
     * repeating the table: two copies of the same correspondence end up disagreeing.
     */
    public char getTypeCode() {
        return this.signature.charAt(0);
    }

    /**
     * The whole descriptor, or `null` if the field is primitive.
     *
     * <p>`null` and not the lone letter, which is what one would expect: for a primitive the type
     * code already says everything there is to know, and returning something here would give the
     * caller two sources for the same datum. The contract uses the absence to say "primitive".
     */
    public String getTypeString() {
        return this.isPrimitive() ? null : this.signature;
    }

    /** Where this field falls inside the stream's data block. */
    public int getOffset() {
        return this.offset;
    }

    /**
     * It sets the offset.
     *
     * <p>It is `protected` because it is decided by whoever builds the block --the class's
     * descriptor-- and not by whoever describes the field: a hand-written `serialPersistentFields`
     * that started moving offsets would put the format out of joint for every other field.
     */
    protected void setOffset(int offset) {
        this.offset = offset;
    }

    public boolean isPrimitive() {
        char c = this.signature.charAt(0);
        return c != 'L' && c != '[';
    }

    public boolean isUnshared() {
        return this.unshared;
    }

    /**
     * Primitives before references; within the same category, by name.
     *
     * <p>It receives `Object` and not `ObjectStreamField` because that is how the JDK declares it
     * --the class is `Comparable<Object>`-- and narrowing it here would stop a raw `Comparable`
     * from compiling.
     */
    public int compareTo(Object obj) {
        ObjectStreamField other = (ObjectStreamField) obj;
        boolean mine = this.isPrimitive();
        if (mine != other.isPrimitive()) {
            return mine ? -1 : 1;
        }
        return this.name.compareTo(other.name);
    }

    /** The descriptor and the name, which is how a field reads in a class dump. */
    public String toString() {
        return this.signature + " " + this.name;
    }

    // A type's JVM descriptor. The arrays are asked of `Class.getName()`, which already returns the
    // bracketed form (`[[D`) and only needs its dots turned into slashes.
    private static String descriptor(Class<?> t) {
        if (t == Integer.TYPE) {
            return "I";
        }
        if (t == Long.TYPE) {
            return "J";
        }
        if (t == Double.TYPE) {
            return "D";
        }
        if (t == Float.TYPE) {
            return "F";
        }
        if (t == Byte.TYPE) {
            return "B";
        }
        if (t == Short.TYPE) {
            return "S";
        }
        if (t == Character.TYPE) {
            return "C";
        }
        if (t == Boolean.TYPE) {
            return "Z";
        }
        if (t == Void.TYPE) {
            return "V";
        }
        String n = t.getName();
        if (n.charAt(0) == '[') {
            return n.replace('.', '/');
        }
        return "L" + n.replace('.', '/') + ";";
    }
}
