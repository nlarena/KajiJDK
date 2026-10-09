package jdk.internal.reflect;

import java.lang.reflect.Field;
import java.lang.reflect.Member;

/**
 * KajiLibrary's jdk.internal.reflect.ConstantPool -- reflective access to the constant pool of a
 * class that is already loaded.
 *
 * <p>It exists for one single thing: the annotations. The `RuntimeVisibleAnnotations` attribute
 * does not keep text but **indices into the constant pool** of the class that declares them, so
 * whoever wants to parse those raw bytes needs, besides the bytes, the pool to resolve them
 * against. This class is that second argument: it is the type the JDK names in {@code
 * VMSupport.encodeAnnotations(byte[], Class, ConstantPool, boolean, Class[])}.
 *
 * <p><strong>That method is still not in our {@link jdk.internal.vm.VMSupport}</strong>, and it is
 * not linked here precisely for that reason --a `@link` to a member that does not exist promises
 * something that is not there--. Having `ConstantPool` was necessary in order to be able even to
 * write that signature, but it is not enough: the body of the JDK also needs
 * `sun.reflect.annotation.AnnotationParser`, which is not in this library, and a pool **with
 * data**, which for what follows cannot exist. The complete reasons are in the header of
 * `VMSupport`.
 *
 * <h2>It is a surface, and the reason is on the other side of the border</h2>
 *
 * <p>In the JDK **the twenty methods are a one-line `native`**: each one passes the VM the private
 * field `constantPoolOop`, which is a pointer to the internal pool of HotSpot and which **the VM
 * writes** when manufacturing the object. There is no constructor that fills it: a
 * `new ConstantPool()` in the JDK also comes out with the field at `null` and all its methods fail.
 * The only legitimate road is for the VM to give you one, through
 * `JavaLangAccess.getConstantPool(Class)`.
 *
 * <p>This VM does not expose its constant pool to Java -- there is no
 * `JavaLangAccess.getConstantPool` nor anything equivalent. So **there is no way for an instance
 * with data to exist**, and the methods throw {@link UnsupportedOperationException} saying so. It
 * is the same decision as {@link jdk.internal.vm.ContinuationSupport#ensureSupported()} and as
 * {@code java.lang.StackWalker}: the member is there, with its signature and its return type
 * correct, and it cuts as soon as it is used instead of returning a zero or a `null` that the
 * caller would take for a datum.
 *
 * <p><strong>Here no method is `native`, and in the JDK the internal ones are.</strong> It is the
 * same justification that is already written in {@code VMSupport.getVMTemporaryDirectory()} and in
 * {@code Continuation.pin()}: in this VM a `native` method with no registered implementation **does
 * not throw an exception, it brings the process down**. A `native` faithful to the modifier would
 * kill the program that calls it; a Java method that throws leaves the caller with an error they
 * can catch and read. The `native`s of the JDK are all private, so the public surface does not
 * change.
 *
 * <p>The day the VM hands over its pool, what changes are the bodies and the field -- the signature
 * of each method is already the definitive one.
 */
public class ConstantPool {

    // The VM knows the name in HotSpot; here nobody writes it, and it is left as the mark of why
    // the methods cannot answer. It is declared all the same because it is what makes the class
    // have one single reason to fail instead of twenty.
    private Object constantPoolOop;

    public ConstantPool() {
    }

    /** The number of entries, that is the largest valid index. */
    public int getSize() {
        throw ConstantPool.noPool("getSize");
    }

    /** The class of the entry `index`, loading it if need be. */
    public Class<?> getClassAt(int index) {
        throw ConstantPool.noPool("getClassAt");
    }

    /** The same, but `null` if that class is not loaded yet. */
    public Class<?> getClassAtIfLoaded(int index) {
        throw ConstantPool.noPool("getClassAtIfLoaded");
    }

    /** The index of the class reference of a method or a field. */
    public int getClassRefIndexAt(int index) {
        throw ConstantPool.noPool("getClassRefIndexAt");
    }

    /**
     * The method of the entry `index`.
     *
     * <p>It returns `Member` and not `Method` because it may also be a constructor, and the static
     * initialisers come back as `Method`. It is of the JDK, not a generalisation of ours.
     */
    public Member getMethodAt(int index) {
        throw ConstantPool.noPool("getMethodAt");
    }

    /** The same, but `null` if the class that declares it is not loaded. */
    public Member getMethodAtIfLoaded(int index) {
        throw ConstantPool.noPool("getMethodAtIfLoaded");
    }

    /** The field of the entry `index`. */
    public Field getFieldAt(int index) {
        throw ConstantPool.noPool("getFieldAt");
    }

    /** The same, but `null` if the class that declares it is not loaded. */
    public Field getFieldAtIfLoaded(int index) {
        throw ConstantPool.noPool("getFieldAtIfLoaded");
    }

    /** Class name, member name and descriptor, in that order and loading nothing. */
    public String[] getMemberRefInfoAt(int index) {
        throw ConstantPool.noPool("getMemberRefInfoAt");
    }

    /** The index of the `NameAndType` entry of a method, a field or an `invokedynamic`. */
    public int getNameAndTypeRefIndexAt(int index) {
        throw ConstantPool.noPool("getNameAndTypeRefIndexAt");
    }

    /** The name and the descriptor of a `NameAndType` entry, in that order. */
    public String[] getNameAndTypeRefInfoAt(int index) {
        throw ConstantPool.noPool("getNameAndTypeRefInfoAt");
    }

    /** The `int` constant of the entry `index`. */
    public int getIntAt(int index) {
        throw ConstantPool.noPool("getIntAt");
    }

    /** The `long` constant. */
    public long getLongAt(int index) {
        throw ConstantPool.noPool("getLongAt");
    }

    /** The `float` constant. */
    public float getFloatAt(int index) {
        throw ConstantPool.noPool("getFloatAt");
    }

    /** The `double` constant. */
    public double getDoubleAt(int index) {
        throw ConstantPool.noPool("getDoubleAt");
    }

    /** The `String` constant -- the `CONSTANT_String` entry, already resolved. */
    public String getStringAt(int index) {
        throw ConstantPool.noPool("getStringAt");
    }

    /** The raw text of a `CONSTANT_Utf8` entry. */
    public String getUTF8At(int index) {
        throw ConstantPool.noPool("getUTF8At");
    }

    /** What kind of entry the `index` one is. */
    public Tag getTagAt(int index) {
        throw ConstantPool.noPool("getTagAt");
    }

    // One single place where the reason is said, so that the twenty methods give the same reason
    // and not twenty variants of the same text.
    private static UnsupportedOperationException noPool(String method) {
        return new UnsupportedOperationException(
                "ConstantPool." + method + ": this VM does not expose the constant pool to Java");
    }

    /**
     * What kind of entry one of the pool is.
     *
     * <p>The codes are those of the specification of the `.class` format (table 4.4-A), and that is
     * why the enumeration carries them inside instead of depending on the order of the constants.
     * {@link #INVALID} with code 0 is of the JDK: it covers the entries the VM marks as unusable,
     * which in the specification have no code of their own.
     */
    public static enum Tag {
        /** `CONSTANT_Utf8`. */
        UTF8(1),
        /** `CONSTANT_Integer`. */
        INTEGER(3),
        /** `CONSTANT_Float`. */
        FLOAT(4),
        /** `CONSTANT_Long`. */
        LONG(5),
        /** `CONSTANT_Double`. */
        DOUBLE(6),
        /** `CONSTANT_Class`. */
        CLASS(7),
        /** `CONSTANT_String`. */
        STRING(8),
        /** `CONSTANT_Fieldref`. */
        FIELDREF(9),
        /** `CONSTANT_Methodref`. */
        METHODREF(10),
        /** `CONSTANT_InterfaceMethodref`. */
        INTERFACEMETHODREF(11),
        /** `CONSTANT_NameAndType`. */
        NAMEANDTYPE(12),
        /** `CONSTANT_MethodHandle`. */
        METHODHANDLE(15),
        /** `CONSTANT_MethodType`. */
        METHODTYPE(16),
        /** `CONSTANT_InvokeDynamic`. */
        INVOKEDYNAMIC(18),
        /** None of the above. */
        INVALID(0);

        private final int code;

        private Tag(int code) {
            this.code = code;
        }

        // From the byte of the specification to the constant. Private, just as in the JDK: the
        // numeric code is a detail of the format and not part of the contract of the enumeration.
        private static Tag fromCode(byte v) {
            for (Tag t : Tag.values()) {
                if (t.code == v) {
                    return t;
                }
            }
            throw new IllegalArgumentException("unknown tag code " + v);
        }
    }
}
