package jdk.jfr.consumer;

import java.util.List;

import jdk.jfr.ValueDescriptor;

/**
 * Un metodo, tal como quedo grabado.
 *
 * <p>{@link #getDescriptor} devuelve el descriptor de la JVM, como {@code "(Ljava/lang/String;)I"}.
 * Es lo que distingue dos sobrecargas del mismo nombre, y sin el una pila de llamadas no se puede
 * resolver contra el codigo — que es lo que un perfilador necesita hacer.
 *
 * <p>{@link #isHidden} marca los metodos que la VM genera y que no estan en ningun fuente: los
 * cuerpos de las lambdas, los adaptadores de los method handles. Una herramienta los esconde por
 * omision porque ensucian la pila sin decir nada.
 *
 * @since 9
 */
public final class RecordedMethod extends RecordedObject {

    RecordedMethod(List<ValueDescriptor> descriptores, Object[] valores) {
        super(descriptores, valores);
    }

    /**
     * La clase que declara el metodo.
     *
     * @return la clase
     */
    public RecordedClass getType() {
        return getClass("type");
    }

    /**
     * El nombre del metodo.
     *
     * @return el nombre
     */
    public String getName() {
        return getString("name");
    }

    /**
     * El descriptor de la JVM.
     *
     * @return el descriptor
     */
    public String getDescriptor() {
        return getString("descriptor");
    }

    /**
     * Los modificadores del metodo.
     *
     * @return los modificadores
     */
    public int getModifiers() {
        return getInt("modifiers");
    }

    /**
     * Si es un metodo generado por la VM.
     *
     * @return si esta oculto
     */
    public boolean isHidden() {
        return getBoolean("hidden");
    }
}
