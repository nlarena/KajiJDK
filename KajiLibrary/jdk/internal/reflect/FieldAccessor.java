package jdk.internal.reflect;

/**
 * KajiLibrary's jdk.internal.reflect.FieldAccessor — el contrato de "leer y escribir este campo".
 *
 * <h2>Diez y ocho metodos que son nueve tipos por dos direcciones</h2>
 *
 * <p>No hay un {@code get(Object, Class)} porque el tipo del campo no es un argumento: es una
 * propiedad del accesor, fijada cuando se lo fabrica. Lo que varia es el tipo con el que el
 * <em>llamador</em> quiere el valor, y eso se elige en el nombre del metodo — que es lo que permite
 * que {@code getInt} devuelva un {@code int} de verdad y no un {@code Integer} que haya que
 * desempacar. La reflexion sin boxeo es toda la razon de que esta interfaz no sea dos metodos.
 *
 * <p>En esta biblioteca los dieciocho tienen un espejo exacto en {@link java.lang.reflect.Field}, que
 * ya los resuelve contra las costuras nativas {@code getInt0}/{@code getLong0}/{@code getReference0}
 * que mueven el slot. El accesor que devuelve {@link ReflectionFactory#newFieldAccessor} delega en
 * ese espejo: no reimplementa el acceso al campo, lo nombra.
 *
 * <p>Como en {@link MethodAccessor}, la interfaz es una declaracion pura y por eso no puede mentir:
 * no tiene cuerpos.
 */
public interface FieldAccessor {

    /** El valor del campo en {@code obj}, boxeado si el campo es primitivo. */
    Object get(Object obj) throws IllegalArgumentException;

    boolean getBoolean(Object obj) throws IllegalArgumentException;

    byte getByte(Object obj) throws IllegalArgumentException;

    char getChar(Object obj) throws IllegalArgumentException;

    short getShort(Object obj) throws IllegalArgumentException;

    int getInt(Object obj) throws IllegalArgumentException;

    long getLong(Object obj) throws IllegalArgumentException;

    float getFloat(Object obj) throws IllegalArgumentException;

    double getDouble(Object obj) throws IllegalArgumentException;

    /** Escribe {@code value} en el campo de {@code obj}, desempacandolo si el campo es primitivo. */
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
