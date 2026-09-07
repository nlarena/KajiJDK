package jdk.jfr.consumer;

import java.util.List;

import jdk.jfr.ValueDescriptor;

/**
 * Un hilo, tal como quedo grabado.
 *
 * <h2>Por que hay dos nombres y dos identificadores</h2>
 *
 * <p>Un hilo Java es dos cosas a la vez: un objeto {@code Thread} y un hilo del sistema operativo.
 * {@link #getJavaName} y {@link #getJavaThreadId} son del primero; {@link #getOSName} y
 * {@link #getOSThreadId} del segundo.
 *
 * <p>No coinciden y hacen falta los dos. El identificador del sistema es el que permite cruzar la
 * grabacion con lo que vio una herramienta de afuera —un perfilador nativo, {@code top}—; el de
 * Java es el que aparece en un volcado de hilos.
 *
 * <p>Un hilo virtual no tiene hilo del sistema propio: {@link #isVirtual} lo dice, y ahi los campos
 * del sistema operativo no significan nada.
 *
 * @since 9
 */
public final class RecordedThread extends RecordedObject {

    RecordedThread(List<ValueDescriptor> descriptores, Object[] valores) {
        super(descriptores, valores);
    }

    /**
     * El nombre del hilo en el sistema operativo.
     *
     * @return el nombre, o {@code null}
     */
    public String getOSName() {
        return getString("osName");
    }

    /**
     * El identificador del hilo en el sistema operativo.
     *
     * @return el identificador, o {@code -1} si es un hilo virtual
     */
    public long getOSThreadId() {
        return getLong("osThreadId");
    }

    /**
     * El grupo al que pertenece.
     *
     * @return el grupo, o {@code null}
     */
    public RecordedThreadGroup getThreadGroup() {
        return getValue("group");
    }

    /**
     * El nombre del objeto {@code Thread}.
     *
     * @return el nombre, o {@code null}
     */
    public String getJavaName() {
        return getString("javaName");
    }

    /**
     * El identificador del objeto {@code Thread}.
     *
     * @return el identificador, o {@code 0} si el hilo no es de Java
     */
    public long getJavaThreadId() {
        return getLong("javaThreadId");
    }

    /**
     * El identificador que la VM que grabo le dio a este hilo.
     *
     * @return el identificador
     */
    public long getId() {
        return getLong("javaThreadId");
    }

    /**
     * Si es un hilo virtual.
     *
     * @return si lo es
     */
    public boolean isVirtual() {
        return hasField("virtual") && getBoolean("virtual");
    }
}
