package java.lang.classfile;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.invoke.TypeDescriptor.OfField;

// The types the JVM tells apart in the bytecode. They are NOT the language's types: the machine's set
// of operations does not separate `boolean`, `byte`, `char` or `short` from `int` --it loads them and
// operates on them as `int`-- but it does separate them in arrays (`baload` vs `saload`) and in
// descriptors. Hence `asLoadable()` collapsing all four to `INT` and `newarrayCode()` telling them
// apart.
public enum TypeKind {

    BOOLEAN(4, 1),
    BYTE(8, 1),
    CHAR(5, 1),
    SHORT(9, 1),
    INT(10, 1),
    LONG(11, 2),
    FLOAT(6, 1),
    DOUBLE(7, 2),
    REFERENCE(0, 1),
    VOID(0, 0);

    // The `newarray` code (JVMS §6.5, `atype` table); 0 for the two that are not primitive.
    private final int newarrayCode;
    // How many local variable or operand slots it takes: 2 for `long` and `double`, 0 for `void`.
    private final int slotSize;

    private TypeKind(int newarrayCode, int slotSize) {
        this.newarrayCode = newarrayCode;
        this.slotSize = slotSize;
    }

    /** The most specific nominal type it stands for: `int`, `long`, ..., `Object` for `REFERENCE`. */
    public ClassDesc upperBound() {
        switch (this) {
            case BOOLEAN: return ConstantDescs.CD_boolean;
            case BYTE: return ConstantDescs.CD_byte;
            case CHAR: return ConstantDescs.CD_char;
            case SHORT: return ConstantDescs.CD_short;
            case INT: return ConstantDescs.CD_int;
            case LONG: return ConstantDescs.CD_long;
            case FLOAT: return ConstantDescs.CD_float;
            case DOUBLE: return ConstantDescs.CD_double;
            case VOID: return ConstantDescs.CD_void;
            default: return ConstantDescs.CD_Object;
        }
    }

    /** `newarray`'s `atype`. It throws `UnsupportedOperationException` on `REFERENCE` and `VOID`. */
    public int newarrayCode() {
        if (this.newarrayCode == 0) {
            throw new UnsupportedOperationException("newarray no aplica a " + name());
        }
        return this.newarrayCode;
    }

    /** How many slots it takes. */
    public int slotSize() {
        return this.slotSize;
    }

    /** The type the JVM loads it as: the four narrow ones are loaded as `int`. */
    public TypeKind asLoadable() {
        if (this == BOOLEAN || this == BYTE || this == CHAR || this == SHORT) {
            return INT;
        }
        return this;
    }

    /** The type of a `newarray`'s `atype`. */
    public static TypeKind fromNewarrayCode(int newarrayCode) {
        switch (newarrayCode) {
            case 4: return BOOLEAN;
            case 5: return CHAR;
            case 6: return FLOAT;
            case 7: return DOUBLE;
            case 8: return BYTE;
            case 9: return SHORT;
            case 10: return INT;
            case 11: return LONG;
            default:
                throw new IllegalArgumentException("newarray atype out of range: " + newarrayCode);
        }
    }

    /** The type this field descriptor describes. It only looks at the first character. */
    public static TypeKind fromDescriptor(CharSequence s) {
        if (s.length() == 0) {
            throw new IllegalArgumentException("empty descriptor");
        }
        switch (s.charAt(0)) {
            case 'Z': return BOOLEAN;
            case 'B': return BYTE;
            case 'C': return CHAR;
            case 'S': return SHORT;
            case 'I': return INT;
            case 'J': return LONG;
            case 'F': return FLOAT;
            case 'D': return DOUBLE;
            case 'V': return VOID;
            case 'L':
            case '[': return REFERENCE;
            default:
                throw new IllegalArgumentException("not a field descriptor: " + s);
        }
    }

    /** The type of a nominal field descriptor. */
    public static TypeKind from(OfField<?> descriptor) {
        return fromDescriptor(descriptor.descriptorString());
    }
}
