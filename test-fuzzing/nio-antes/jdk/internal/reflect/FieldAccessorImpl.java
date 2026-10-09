package jdk.internal.reflect;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * The {@link FieldAccessor} {@link ReflectionFactory#newFieldAccessor} manufactures.
 *
 * <p>The eighteen methods delegate one by one to {@link Field}, which already resolves them against
 * the native seams that move the slot. There is no conversion of its own: the widening (a
 * {@code getLong} over an {@code int} field) and the rejection (a {@code getBoolean} over one that
 * is not) are whatever {@code Field} decides, and they have to be the same because in the JDK that
 * class delegates to this accessor and here it is the other way round.
 *
 * <h2>The only behaviour that belongs to this object and not to {@code Field}: the read-only
 * mode</h2>
 *
 * <p>{@code Field.set} does not tell a {@code final} field from one that is not --the check of
 * access in the JDK lives on the side of the accessor, not of the field--, and that is why this is
 * the place where it has to be. The accessor is born read-only when the field is {@code final} and
 * whoever asked for it did <strong>not</strong> declare that they had already suppressed the
 * control of access ({@code override}); in that mode the nine writers throw {@link
 * IllegalAccessException}, which is what the interface declares and what the JDK answers.
 *
 * <p>That a bare {@code Field} does let one write a {@code final} in this VM is no contradiction
 * with this: it is that in this library {@code Field} never had the check, and bringing it in would
 * be changing {@code java.lang.reflect}. What this accessor promises is its own contract, and it
 * fulfils it.
 */
final class FieldAccessorImpl implements FieldAccessor {

    private final Field field;
    private final boolean readOnly;

    FieldAccessorImpl(Field field, boolean override) {
        this.field = field;
        this.readOnly = Modifier.isFinal(field.getModifiers()) && !override;
    }

    // ---- reads: pure forwarding ----

    public Object get(Object obj) throws IllegalArgumentException {
        return this.field.get(obj);
    }

    public boolean getBoolean(Object obj) throws IllegalArgumentException {
        return this.field.getBoolean(obj);
    }

    public byte getByte(Object obj) throws IllegalArgumentException {
        return this.field.getByte(obj);
    }

    public char getChar(Object obj) throws IllegalArgumentException {
        return this.field.getChar(obj);
    }

    public short getShort(Object obj) throws IllegalArgumentException {
        return this.field.getShort(obj);
    }

    public int getInt(Object obj) throws IllegalArgumentException {
        return this.field.getInt(obj);
    }

    public long getLong(Object obj) throws IllegalArgumentException {
        return this.field.getLong(obj);
    }

    public float getFloat(Object obj) throws IllegalArgumentException {
        return this.field.getFloat(obj);
    }

    public double getDouble(Object obj) throws IllegalArgumentException {
        return this.field.getDouble(obj);
    }

    // ---- writings: the check first, then the forwarding ----

    private void requireWritable() throws IllegalAccessException {
        if (this.readOnly) {
            throw new IllegalAccessException(
                    "Can not set final field " + this.field.getDeclaringClass().getName()
                            + "." + this.field.getName());
        }
    }

    public void set(Object obj, Object value) throws IllegalArgumentException, IllegalAccessException {
        this.requireWritable();
        this.field.set(obj, value);
    }

    public void setBoolean(Object obj, boolean value)
            throws IllegalArgumentException, IllegalAccessException {
        this.requireWritable();
        this.field.setBoolean(obj, value);
    }

    public void setByte(Object obj, byte value)
            throws IllegalArgumentException, IllegalAccessException {
        this.requireWritable();
        this.field.setByte(obj, value);
    }

    public void setChar(Object obj, char value)
            throws IllegalArgumentException, IllegalAccessException {
        this.requireWritable();
        this.field.setChar(obj, value);
    }

    public void setShort(Object obj, short value)
            throws IllegalArgumentException, IllegalAccessException {
        this.requireWritable();
        this.field.setShort(obj, value);
    }

    public void setInt(Object obj, int value)
            throws IllegalArgumentException, IllegalAccessException {
        this.requireWritable();
        this.field.setInt(obj, value);
    }

    public void setLong(Object obj, long value)
            throws IllegalArgumentException, IllegalAccessException {
        this.requireWritable();
        this.field.setLong(obj, value);
    }

    public void setFloat(Object obj, float value)
            throws IllegalArgumentException, IllegalAccessException {
        this.requireWritable();
        this.field.setFloat(obj, value);
    }

    public void setDouble(Object obj, double value)
            throws IllegalArgumentException, IllegalAccessException {
        this.requireWritable();
        this.field.setDouble(obj, value);
    }
}
