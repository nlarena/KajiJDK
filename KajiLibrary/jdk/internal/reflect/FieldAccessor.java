package jdk.internal.reflect;

/**
 * KajiLibrary's jdk.internal.reflect.FieldAccessor -- the contract of "read and write this field".
 *
 * <h2>Eighteen methods that are nine types by two directions</h2>
 *
 * <p>There is no {@code get(Object, Class)} because the type of the field is not an argument: it is
 * a property of the accessor, fixed when it is manufactured. What varies is the type the
 * <em>caller</em> wants the value in, and that is chosen in the name of the method -- which is what
 * allows {@code getInt} to return a real {@code int} and not an {@code Integer} that has to be
 * unpacked. Reflection with no boxing is the whole reason why this interface is not two methods.
 *
 * <p>In this library the eighteen have an exact mirror in {@link java.lang.reflect.Field}, which
 * already resolves them against the native seams {@code getInt0}/{@code getLong0}/
 * {@code getReference0} that move the slot. The accessor
 * {@link ReflectionFactory#newFieldAccessor} returns delegates to that mirror: it does not
 * reimplement the access to the field, it names it.
 *
 * <p>As in {@link MethodAccessor}, the interface is a pure declaration and that is why it cannot
 * lie: it has no bodies.
 */
public interface FieldAccessor {

    /** The value of the field in {@code obj}, boxed if the field is primitive. */
    Object get(Object obj) throws IllegalArgumentException;

    boolean getBoolean(Object obj) throws IllegalArgumentException;

    byte getByte(Object obj) throws IllegalArgumentException;

    char getChar(Object obj) throws IllegalArgumentException;

    short getShort(Object obj) throws IllegalArgumentException;

    int getInt(Object obj) throws IllegalArgumentException;

    long getLong(Object obj) throws IllegalArgumentException;

    float getFloat(Object obj) throws IllegalArgumentException;

    double getDouble(Object obj) throws IllegalArgumentException;

    /** It writes {@code value} into the field of {@code obj}, unpacking it if the field is
     * primitive. */
    void set(Object obj, Object value) throws IllegalArgumentException, IllegalAccessException;

    void setBoolean(Object obj, boolean value) throws IllegalArgumentException, IllegalAccessException;

    void setByte(Object obj, byte value) throws IllegalArgumentException, IllegalAccessException;

    void setChar(Object obj, char value) throws IllegalArgumentException, IllegalAccessException;

    void setShort(Object obj, short value) throws IllegalArgumentException, IllegalAccessException;

    void setInt(Object obj, int value) throws IllegalArgumentException, IllegalAccessException;

    void setLong(Object obj, long value) throws IllegalArgumentException, IllegalAccessException;

    void setFloat(Object obj, float value) throws IllegalArgumentException, IllegalAccessException;

    void setDouble(Object obj, double value) throws IllegalArgumentException, IllegalAccessException;
}
