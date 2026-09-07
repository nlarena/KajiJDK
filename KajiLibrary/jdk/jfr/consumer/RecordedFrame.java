package jdk.jfr.consumer;

import java.util.List;

import jdk.jfr.ValueDescriptor;

/**
 * Un marco de una pila de llamadas grabada.
 *
 * <h2>Por que hay indice de bytecode y numero de linea</h2>
 *
 * <p>El indice de bytecode siempre esta; el numero de linea solo si la clase se compilo con la
 * tabla de lineas, que es opcional. En codigo compilado sin ella, {@link #getLineNumber} devuelve
 * {@code -1} y el indice de bytecode es lo unico que ubica el punto exacto.
 *
 * <p>Ademas son distintos en precision: dentro de una linea puede haber varias llamadas, y el
 * indice de bytecode distingue cual de ellas es.
 *
 * <h2>{@link #getType} no es el tipo del metodo</h2>
 *
 * <p>Es el tipo <strong>del marco</strong>: si el codigo estaba interpretado, compilado por el JIT
 * o era nativo. Ese dato es lo que explica una pila donde el mismo metodo aparece dos veces con
 * costos completamente distintos.
 *
 * @since 9
 */
public final class RecordedFrame extends RecordedObject {

    RecordedFrame(List<ValueDescriptor> descriptores, Object[] valores) {
        super(descriptores, valores);
    }

    /**
     * Si el marco es de codigo Java y no nativo.
     *
     * @return si es de Java
     */
    public boolean isJavaFrame() {
        return hasField("method") && getValue("method") != null;
    }

    /**
     * El indice del bytecode dentro del metodo.
     *
     * @return el indice, o {@code -1} si no se sabe
     */
    public int getBytecodeIndex() {
        return getInt("bytecodeIndex");
    }

    /**
     * El numero de linea del fuente.
     *
     * @return la linea, o {@code -1} si la clase no trae la tabla de lineas
     */
    public int getLineNumber() {
        return getInt("lineNumber");
    }

    /**
     * Como estaba ejecutandose el codigo: interpretado, compilado o nativo.
     *
     * @return el tipo de marco
     */
    public String getType() {
        return getString("type");
    }

    /**
     * El metodo del marco.
     *
     * @return el metodo, o {@code null} si el marco no es de Java
     */
    public RecordedMethod getMethod() {
        return getValue("method");
    }
}
