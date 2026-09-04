package java.rmi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * KajiLibrary's java.rmi.MarshalledObject -- un objeto guardado como bytes serializados.
 *
 * <p>Serializa en el momento de construirse y guarda los bytes; {@link #get} deserializa una copia
 * <b>nueva</b> cada vez. Es un objeto congelado en el tiempo: cambiar el original despues de meterlo
 * aca no cambia lo que sale.
 *
 * <h2>Para que sirve de verdad</h2>
 *
 * <p>Para <b>postergar</b> la deserializacion. Un objeto remoto puede recibir uno de estos y pasarlo
 * de mano en mano sin necesitar la clase adentro; solo quien llama {@code get} tiene que poder
 * cargarla. Sin esto, un servidor intermediario necesitaria en su ruta de clases todo lo que pasa por
 * el.
 *
 * <h2>{@link #equals} compara los bytes</h2>
 *
 * <p>Y no llama al {@code equals} del objeto guardado, lo cual tiene dos consecuencias:
 *
 * <ul>
 *   <li>funciona con objetos que no redefinen {@code equals}, comparando su contenido;
 *   <li>dos objetos <b>iguales</b> pueden dar false si se serializan distinto -- por ejemplo dos
 *       tablas hash con el mismo contenido en distinto orden.
 * </ul>
 *
 * <p>{@link #hashCode} arranca en 13 y va mezclando los bytes; por eso el de un objeto null es
 * exactamente 13.
 */
public final class MarshalledObject<T> implements Serializable {

    private static final long serialVersionUID = 8988374069173025854L;

    /** Los bytes del objeto, o null si el objeto era null. */
    private byte[] objBytes = null;

    /**
     * Los de las anotaciones de ubicacion de las clases.
     *
     * <p>Van aparte y <b>no</b> entran en {@link #equals}: dos objetos iguales que vinieron de
     * lugares distintos siguen siendo iguales.
     */
    private byte[] locBytes = null;

    /** El hash, calculado una sola vez sobre los bytes. */
    private int hash;

    /**
     * Serializa ese objeto.
     *
     * @throws IOException si no se pudo serializar
     */
    public MarshalledObject(T obj) throws IOException {
        if (obj == null) {
            this.hash = 13;
            return;
        }
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bout);
        out.writeObject(obj);
        out.flush();
        this.objBytes = bout.toByteArray();
        int h = 13;
        int i = 0;
        while (i < this.objBytes.length) {
            h = 37 * h + this.objBytes[i];
            i = i + 1;
        }
        this.hash = h;
    }

    /**
     * Una copia nueva del objeto guardado.
     *
     * <p>Cada llamada deserializa de vuelta, asi que devuelve objetos distintos.
     *
     * @return el objeto, o null si se guardo null
     * @throws IOException si no se pudo leer
     * @throws ClassNotFoundException si falta alguna clase
     */
    public T get() throws IOException, ClassNotFoundException {
        if (this.objBytes == null) {
            return null;
        }
        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(this.objBytes));
        @SuppressWarnings("unchecked")
        T result = (T) in.readObject();
        return result;
    }

    /** Sobre los bytes. Ver la nota de la clase: null da 13. */
    @Override
    public int hashCode() {
        return this.hash;
    }

    /** Compara los bytes, no los objetos. Ver la nota de la clase. */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof MarshalledObject)) {
            return false;
        }
        MarshalledObject<?> other = (MarshalledObject<?>) obj;
        if (this.objBytes == null || other.objBytes == null) {
            return this.objBytes == other.objBytes;
        }
        if (this.objBytes.length != other.objBytes.length) {
            return false;
        }
        int i = 0;
        while (i < this.objBytes.length) {
            if (this.objBytes[i] != other.objBytes[i]) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }
}
