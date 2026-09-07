package jdk.jfr.consumer;

import java.util.List;

import jdk.jfr.ValueDescriptor;

/**
 * Un grupo de hilos, tal como quedo grabado.
 *
 * <p>{@link #getParent} arma la cadena hasta el grupo raiz. Sirve para agrupar hilos por su origen
 * —un pool, un contenedor— cuando los nombres de los hilos no lo dicen.
 *
 * @since 9
 */
public final class RecordedThreadGroup extends RecordedObject {

    RecordedThreadGroup(List<ValueDescriptor> descriptores, Object[] valores) {
        super(descriptores, valores);
    }

    /**
     * El nombre del grupo.
     *
     * @return el nombre
     */
    public String getName() {
        return getString("name");
    }

    /**
     * El grupo que lo contiene.
     *
     * @return el grupo padre, o {@code null} si este es la raiz
     */
    public RecordedThreadGroup getParent() {
        return getValue("parent");
    }
}
