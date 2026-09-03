package java.lang.classfile;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.invoke.TypeDescriptor.OfField;

// Los tipos que la JVM distingue en el bytecode. NO son los tipos del lenguaje: el conjunto de
// operaciones de la máquina no separa `boolean`, `byte`, `char` ni `short` de `int` —los carga y los
// opera como `int`— pero sí los separa en los arreglos (`baload` vs `saload`) y en los descriptores.
// De ahí que `asLoadable()` colapse los cuatro a `INT` y que `newarrayCode()` los distinga.
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

    // El código de `newarray` (JVMS §6.5, tabla de `atype`); 0 para los dos que no son primitivos.
    private final int newarrayCode;
    // Cuántas ranuras de variable local u operando ocupa: 2 para `long` y `double`, 0 para `void`.
    private final int slotSize;

    private TypeKind(int newarrayCode, int slotSize) {
        this.newarrayCode = newarrayCode;
        this.slotSize = slotSize;
    }

    /** El tipo nominal más específico que representa: `int`, `long`, …, `Object` para `REFERENCE`. */
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

    /** El `atype` de `newarray`. Tira `UnsupportedOperationException` en `REFERENCE` y `VOID`. */
    public int newarrayCode() {
        if (this.newarrayCode == 0) {
            throw new UnsupportedOperationException("newarray no aplica a " + name());
        }
        return this.newarrayCode;
    }

    /** Cuántas ranuras ocupa. */
    public int slotSize() {
        return this.slotSize;
    }

    /** El tipo con el que la JVM lo carga: los cuatro angostos se cargan como `int`. */
    public TypeKind asLoadable() {
        if (this == BOOLEAN || this == BYTE || this == CHAR || this == SHORT) {
            return INT;
        }
        return this;
    }

    /** El tipo del `atype` de un `newarray`. */
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
                throw new IllegalArgumentException("atype de newarray fuera de rango: " + newarrayCode);
        }
    }

    /** El tipo que describe este descriptor de campo. Sólo mira el primer carácter. */
    public static TypeKind fromDescriptor(CharSequence s) {
        if (s.length() == 0) {
            throw new IllegalArgumentException("descriptor vacío");
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
                throw new IllegalArgumentException("no es un descriptor de campo: " + s);
        }
    }

    /** El tipo de un descriptor nominal de campo. */
    public static TypeKind from(OfField<?> descriptor) {
        return fromDescriptor(descriptor.descriptorString());
    }
}
