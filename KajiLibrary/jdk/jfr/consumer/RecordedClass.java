package jdk.jfr.consumer;

import java.util.List;

import jdk.jfr.ValueDescriptor;

/**
 * Una clase Java, tal como quedo grabada.
 *
 * <p>No es un {@code Class}: el proceso que lee la grabacion puede no tener esa clase cargada, ni
 * poder cargarla. Lo que queda es el nombre, los modificadores y de que cargador vino.
 *
 * <p>{@link #getId} es el identificador que la VM que grabo le dio a la clase. Sirve para saber si
 * dos eventos hablan de la misma clase sin comparar nombres, que es lo unico que funciona cuando
 * dos cargadores distintos cargaron clases del mismo nombre — el caso normal en un servidor de
 * aplicaciones.
 *
 * @since 9
 */
public final class RecordedClass extends RecordedObject {

    RecordedClass(List<ValueDescriptor> descriptores, Object[] valores) {
        super(descriptores, valores);
    }

    /**
     * Los modificadores de la clase, con el formato de {@code java.lang.reflect.Modifier}.
     *
     * @return los modificadores
     */
    public int getModifiers() {
        return getInt("modifiers");
    }

    /**
     * El cargador del que vino.
     *
     * @return el cargador, o {@code null} si es el de arranque
     */
    public RecordedClassLoader getClassLoader() {
        return getValue("classLoader");
    }

    /**
     * El nombre completo de la clase.
     *
     * @return el nombre
     */
    public String getName() {
        return getString("name");
    }

    /**
     * El identificador que la VM que grabo le dio a esta clase.
     *
     * @return el identificador
     */
    public long getId() {
        return getLong("id");
    }
}
