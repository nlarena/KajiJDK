package jdk.jfr.consumer;

import java.util.List;

import jdk.jfr.ValueDescriptor;

/**
 * Un cargador de clases, tal como quedo grabado.
 *
 * <p>{@link #getType} devuelve la clase <strong>del cargador</strong>, no lo que cargo. Es la
 * distincion que hay que tener en la cabeza al leer esto: un {@code RecordedClassLoader} de una
 * aplicacion web dice que es un {@code WebAppClassLoader}, y las clases que cargo estan en los
 * eventos que lo referencian.
 *
 * @since 9
 */
public final class RecordedClassLoader extends RecordedObject {

    RecordedClassLoader(List<ValueDescriptor> descriptores, Object[] valores) {
        super(descriptores, valores);
    }

    /**
     * La clase del cargador.
     *
     * @return la clase, o {@code null} si es el cargador de arranque
     */
    public RecordedClass getType() {
        return getClass("type");
    }

    /**
     * El nombre del cargador.
     *
     * @return el nombre, o {@code null} si no tiene
     */
    public String getName() {
        return getString("name");
    }

    /**
     * El identificador que la VM que grabo le dio a este cargador.
     *
     * @return el identificador
     */
    public long getId() {
        return getLong("id");
    }
}
