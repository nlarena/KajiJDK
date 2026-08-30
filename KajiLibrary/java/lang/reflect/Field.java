package java.lang.reflect;

import java.util.Set;

// KajiLibrary's java.lang.reflect.Field — a reflective field. The VM allocates and populates Field
// objects natively (Class.getDeclaredFields), writing clazz/name/type/modifiers/slot directly.
//
// Reflective access is split VM/Java the way it factors cleanly: the VM offers three *raw* typed
// seams (a 4-byte read, an 8-byte read, a reference read, and their writes) that just move the slot;
// the type-checking, widening and boxing the reflection contract asks for is done here in Java, off
// the field's declared `type`. So `getInt` on a `byte` field widens, `get` boxes a primitive into
// its wrapper, and the wrong-type access raises `IllegalArgumentException` -- all in this file.
public final class Field extends AccessibleObject implements Member {

    private Class<?> clazz;
    private String name;
    private Class<?> type;
    private int modifiers;
    private int slot;

    // Only the VM constructs Field objects (populating the fields from native code).
    private Field() {
    }

    public Class<?> getDeclaringClass() {
        return this.clazz;
    }

    public String getName() {
        return this.name;
    }

    public Class<?> getType() {
        return this.type;
    }

    public Type getGenericType() {
        // KajiLibrary does not parse the `Signature` attribute for fields, so the generic type is
        // the erased one -- correct for every non-generic field.
        return this.type;
    }

    public int getModifiers() {
        return this.modifiers;
    }

    public boolean isSynthetic() {
        return (this.modifiers & 0x00001000) != 0;
    }

    public boolean isEnumConstant() {
        return (this.modifiers & 0x00004000) != 0;
    }

    public Set<AccessFlag> accessFlags() {
        return AccessFlag.maskToAccessFlags(this.modifiers, AccessFlag.Location.FIELD);
    }

    // ---- raw typed seams (the VM moves the slot; everything else is done here) ----

    private native int getInt0(Object obj);

    private native long getLong0(Object obj);

    private native Object getReference0(Object obj);

    private native void setInt0(Object obj, int value);

    private native void setLong0(Object obj, long value);

    private native void setReference0(Object obj, Object value);

    // ---- reads ----

    /** This field's value on {@code obj}, boxed if primitive. */
    public Object get(Object obj) {
        if (this.type == Integer.TYPE) {
            return Integer.valueOf(getInt0(obj));
        }
        if (this.type == Boolean.TYPE) {
            return Boolean.valueOf(getInt0(obj) != 0);
        }
        if (this.type == Byte.TYPE) {
            return Byte.valueOf((byte) getInt0(obj));
        }
        if (this.type == Character.TYPE) {
            return Character.valueOf((char) getInt0(obj));
        }
        if (this.type == Short.TYPE) {
            return Short.valueOf((short) getInt0(obj));
        }
        if (this.type == Long.TYPE) {
            return Long.valueOf(getLong0(obj));
        }
        if (this.type == Float.TYPE) {
            return Float.valueOf(Float.intBitsToFloat(getInt0(obj)));
        }
        if (this.type == Double.TYPE) {
            return Double.valueOf(Double.longBitsToDouble(getLong0(obj)));
        }
        return getReference0(obj);
    }

    public boolean getBoolean(Object obj) {
        if (this.type == Boolean.TYPE) {
            return getInt0(obj) != 0;
        }
        throw new IllegalArgumentException("field is not boolean");
    }

    public byte getByte(Object obj) {
        if (this.type == Byte.TYPE) {
            return (byte) getInt0(obj);
        }
        throw new IllegalArgumentException("field is not byte");
    }

    public char getChar(Object obj) {
        if (this.type == Character.TYPE) {
            return (char) getInt0(obj);
        }
        throw new IllegalArgumentException("field is not char");
    }

    public short getShort(Object obj) {
        if (this.type == Byte.TYPE || this.type == Short.TYPE) {
            return (short) getInt0(obj);
        }
        throw new IllegalArgumentException("field cannot be widened to short");
    }

    public int getInt(Object obj) {
        if (this.type == Byte.TYPE || this.type == Short.TYPE
                || this.type == Character.TYPE || this.type == Integer.TYPE) {
            return getInt0(obj);
        }
        throw new IllegalArgumentException("field cannot be widened to int");
    }

    public long getLong(Object obj) {
        if (this.type == Long.TYPE) {
            return getLong0(obj);
        }
        if (this.type == Byte.TYPE || this.type == Short.TYPE
                || this.type == Character.TYPE || this.type == Integer.TYPE) {
            return (long) getInt0(obj);
        }
        throw new IllegalArgumentException("field cannot be widened to long");
    }

    public float getFloat(Object obj) {
        if (this.type == Float.TYPE) {
            return Float.intBitsToFloat(getInt0(obj));
        }
        if (this.type == Long.TYPE) {
            return (float) getLong0(obj);
        }
        if (this.type == Byte.TYPE || this.type == Short.TYPE
                || this.type == Character.TYPE || this.type == Integer.TYPE) {
            return (float) getInt0(obj);
        }
        throw new IllegalArgumentException("field cannot be widened to float");
    }

    public double getDouble(Object obj) {
        if (this.type == Double.TYPE) {
            return Double.longBitsToDouble(getLong0(obj));
        }
        if (this.type == Float.TYPE) {
            return (double) Float.intBitsToFloat(getInt0(obj));
        }
        if (this.type == Long.TYPE) {
            return (double) getLong0(obj);
        }
        if (this.type == Byte.TYPE || this.type == Short.TYPE
                || this.type == Character.TYPE || this.type == Integer.TYPE) {
            return (double) getInt0(obj);
        }
        throw new IllegalArgumentException("field cannot be widened to double");
    }

    // ---- writes ----

    /** Set this field on {@code obj}, unboxing {@code value} for a primitive field. */
    public void set(Object obj, Object value) {
        if (this.type == Integer.TYPE) {
            setInt0(obj, ((Integer) value).intValue());
        } else if (this.type == Boolean.TYPE) {
            setInt0(obj, ((Boolean) value).booleanValue() ? 1 : 0);
        } else if (this.type == Byte.TYPE) {
            setInt0(obj, ((Byte) value).byteValue());
        } else if (this.type == Character.TYPE) {
            setInt0(obj, ((Character) value).charValue());
        } else if (this.type == Short.TYPE) {
            setInt0(obj, ((Short) value).shortValue());
        } else if (this.type == Long.TYPE) {
            setLong0(obj, ((Long) value).longValue());
        } else if (this.type == Float.TYPE) {
            setInt0(obj, Float.floatToRawIntBits(((Float) value).floatValue()));
        } else if (this.type == Double.TYPE) {
            setLong0(obj, Double.doubleToRawLongBits(((Double) value).doubleValue()));
        } else {
            setReference0(obj, value);
        }
    }

    public void setBoolean(Object obj, boolean value) {
        if (this.type == Boolean.TYPE) {
            setInt0(obj, value ? 1 : 0);
        } else {
            throw new IllegalArgumentException("field is not boolean");
        }
    }

    public void setByte(Object obj, byte value) {
        if (this.type == Byte.TYPE) {
            setInt0(obj, value);
        } else if (this.type == Short.TYPE || this.type == Integer.TYPE) {
            setInt0(obj, value);
        } else if (this.type == Long.TYPE) {
            setLong0(obj, value);
        } else if (this.type == Float.TYPE) {
            setInt0(obj, Float.floatToRawIntBits((float) value));
        } else if (this.type == Double.TYPE) {
            setLong0(obj, Double.doubleToRawLongBits((double) value));
        } else {
            throw new IllegalArgumentException("cannot set byte into this field");
        }
    }

    public void setChar(Object obj, char value) {
        if (this.type == Character.TYPE || this.type == Integer.TYPE) {
            setInt0(obj, value);
        } else if (this.type == Long.TYPE) {
            setLong0(obj, value);
        } else if (this.type == Float.TYPE) {
            setInt0(obj, Float.floatToRawIntBits((float) value));
        } else if (this.type == Double.TYPE) {
            setLong0(obj, Double.doubleToRawLongBits((double) value));
        } else {
            throw new IllegalArgumentException("cannot set char into this field");
        }
    }

    public void setShort(Object obj, short value) {
        if (this.type == Short.TYPE || this.type == Integer.TYPE) {
            setInt0(obj, value);
        } else if (this.type == Long.TYPE) {
            setLong0(obj, value);
        } else if (this.type == Float.TYPE) {
            setInt0(obj, Float.floatToRawIntBits((float) value));
        } else if (this.type == Double.TYPE) {
            setLong0(obj, Double.doubleToRawLongBits((double) value));
        } else {
            throw new IllegalArgumentException("cannot set short into this field");
        }
    }

    public void setInt(Object obj, int value) {
        if (this.type == Integer.TYPE) {
            setInt0(obj, value);
        } else if (this.type == Long.TYPE) {
            setLong0(obj, value);
        } else if (this.type == Float.TYPE) {
            setInt0(obj, Float.floatToRawIntBits((float) value));
        } else if (this.type == Double.TYPE) {
            setLong0(obj, Double.doubleToRawLongBits((double) value));
        } else {
            throw new IllegalArgumentException("cannot set int into this field");
        }
    }

    public void setLong(Object obj, long value) {
        if (this.type == Long.TYPE) {
            setLong0(obj, value);
        } else if (this.type == Float.TYPE) {
            setInt0(obj, Float.floatToRawIntBits((float) value));
        } else if (this.type == Double.TYPE) {
            setLong0(obj, Double.doubleToRawLongBits((double) value));
        } else {
            throw new IllegalArgumentException("cannot set long into this field");
        }
    }

    public void setFloat(Object obj, float value) {
        if (this.type == Float.TYPE) {
            setInt0(obj, Float.floatToRawIntBits(value));
        } else if (this.type == Double.TYPE) {
            setLong0(obj, Double.doubleToRawLongBits((double) value));
        } else {
            throw new IllegalArgumentException("cannot set float into this field");
        }
    }

    public void setDouble(Object obj, double value) {
        if (this.type == Double.TYPE) {
            setLong0(obj, Double.doubleToRawLongBits(value));
        } else {
            throw new IllegalArgumentException("cannot set double into this field");
        }
    }

    // ---- identity, printing, misc ----

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Field)) {
            return false;
        }
        Field that = (Field) other;
        return this.clazz == that.clazz && this.name.equals(that.name) && this.type == that.type;
    }

    public int hashCode() {
        return this.clazz.getName().hashCode() ^ this.name.hashCode();
    }

    public String toString() {
        String mods = Modifier.toString(this.modifiers);
        StringBuilder sb = new StringBuilder();
        if (!mods.isEmpty()) {
            sb.append(mods).append(' ');
        }
        sb.append(this.type.getTypeName()).append(' ');
        sb.append(this.clazz.getTypeName()).append('.').append(this.name);
        return sb.toString();
    }

    public String toGenericString() {
        return this.toString();
    }

    // The JDK re-declares this override so a Field's own accessibility flag is what a reflective
    // access checks; KajiJDK does not enforce access, so it defers to the inherited flag.
    public void setAccessible(boolean flag) {
        super.setAccessible(flag);
    }

    // ---- annotations ----
    //
    // A KajiLibrary subset: field-level RUNTIME annotation reflection is not wired (Class-level is;
    // see Class.getAnnotation). A field carrying no runtime annotation -- the common case -- gets the
    // right empty answer; one that does would need a field-attribute native like the class one.

    public java.lang.annotation.Annotation[] getDeclaredAnnotations() {
        return new java.lang.annotation.Annotation[0];
    }

    public <T extends java.lang.annotation.Annotation> T getAnnotation(Class<T> annotationClass) {
        if (annotationClass == null) {
            throw new NullPointerException();
        }
        return null;
    }

    public <T extends java.lang.annotation.Annotation> T[] getAnnotationsByType(Class<T> annotationClass) {
        return (T[]) Array.newInstance(annotationClass, 0);
    }

    /** This field's type as an {@link AnnotatedType} (carrying no type annotations here). */
    public AnnotatedType getAnnotatedType() {
        return new AnnotatedTypeImpl(this.type);
    }
}
