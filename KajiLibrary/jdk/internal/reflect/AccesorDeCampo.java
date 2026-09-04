package jdk.internal.reflect;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * El {@link FieldAccessor} que fabrica {@link ReflectionFactory#newFieldAccessor}.
 *
 * <p>Los dieciocho metodos delegan uno a uno en {@link Field}, que ya los resuelve contra las
 * costuras nativas que mueven el slot. No hay conversion propia: el ensanchado (un {@code getLong}
 * sobre un campo {@code int}) y el rechazo (un {@code getBoolean} sobre uno que no lo es) son los que
 * decida {@code Field}, y tienen que ser los mismos porque en el JDK esa clase le delega a este
 * accesor y aca es al reves.
 *
 * <h2>El unico comportamiento que es de este objeto y no de {@code Field}: el modo de solo lectura</h2>
 *
 * <p>{@code Field.set} no distingue un campo {@code final} de uno que no lo es —el chequeo de acceso
 * en el JDK vive del lado del accesor, no del campo—, y por eso este es el lugar donde tiene que
 * estar. El accesor nace de solo lectura cuando el campo es {@code final} y quien lo pidio
 * <strong>no</strong> declaro que ya habia suprimido el control de acceso ({@code override}); en ese
 * modo los nueve escritores tiran {@link IllegalAccessException}, que es lo que declara la interfaz y
 * lo que contesta el JDK.
 *
 * <p>Que un {@code Field} pelado si deje escribir un {@code final} en esta VM no es una contradiccion
 * con esto: es que en esta biblioteca {@code Field} nunca tuvo el chequeo, y traerselo seria cambiar
 * {@code java.lang.reflect}. Lo que este accesor promete es su propio contrato, y lo cumple.
 */
final class AccesorDeCampo implements FieldAccessor {

    private final Field campo;
    private final boolean soloLectura;

    AccesorDeCampo(Field campo, boolean override) {
        this.campo = campo;
        this.soloLectura = Modifier.isFinal(campo.getModifiers()) && !override;
    }

    // ---- lecturas: puro reenvio ----

    public Object get(Object obj) throws IllegalArgumentException {
        return this.campo.get(obj);
    }

    public boolean getBoolean(Object obj) throws IllegalArgumentException {
        return this.campo.getBoolean(obj);
    }

    public byte getByte(Object obj) throws IllegalArgumentException {
        return this.campo.getByte(obj);
    }

    public char getChar(Object obj) throws IllegalArgumentException {
        return this.campo.getChar(obj);
    }

    public short getShort(Object obj) throws IllegalArgumentException {
        return this.campo.getShort(obj);
    }

    public int getInt(Object obj) throws IllegalArgumentException {
        return this.campo.getInt(obj);
    }

    public long getLong(Object obj) throws IllegalArgumentException {
        return this.campo.getLong(obj);
    }

    public float getFloat(Object obj) throws IllegalArgumentException {
        return this.campo.getFloat(obj);
    }

    public double getDouble(Object obj) throws IllegalArgumentException {
        return this.campo.getDouble(obj);
    }

    // ---- escrituras: el chequeo primero, despues el reenvio ----

    private void exigirEscribible() throws IllegalAccessException {
        if (this.soloLectura) {
            throw new IllegalAccessException(
                    "Can not set final field " + this.campo.getDeclaringClass().getName()
                            + "." + this.campo.getName());
        }
    }

    public void set(Object obj, Object value) throws IllegalArgumentException, IllegalAccessException {
        this.exigirEscribible();
        this.campo.set(obj, value);
    }

    public void setBoolean(Object obj, boolean value)
            throws IllegalArgumentException, IllegalAccessException {
        this.exigirEscribible();
        this.campo.setBoolean(obj, value);
    }

    public void setByte(Object obj, byte value)
            throws IllegalArgumentException, IllegalAccessException {
        this.exigirEscribible();
        this.campo.setByte(obj, value);
    }

    public void setChar(Object obj, char value)
            throws IllegalArgumentException, IllegalAccessException {
        this.exigirEscribible();
        this.campo.setChar(obj, value);
    }

    public void setShort(Object obj, short value)
            throws IllegalArgumentException, IllegalAccessException {
        this.exigirEscribible();
        this.campo.setShort(obj, value);
    }

    public void setInt(Object obj, int value)
            throws IllegalArgumentException, IllegalAccessException {
        this.exigirEscribible();
        this.campo.setInt(obj, value);
    }

    public void setLong(Object obj, long value)
            throws IllegalArgumentException, IllegalAccessException {
        this.exigirEscribible();
        this.campo.setLong(obj, value);
    }

    public void setFloat(Object obj, float value)
            throws IllegalArgumentException, IllegalAccessException {
        this.exigirEscribible();
        this.campo.setFloat(obj, value);
    }

    public void setDouble(Object obj, double value)
            throws IllegalArgumentException, IllegalAccessException {
        this.exigirEscribible();
        this.campo.setDouble(obj, value);
    }
}
