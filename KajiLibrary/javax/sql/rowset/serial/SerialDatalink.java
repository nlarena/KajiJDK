package javax.sql.rowset.serial;

import java.io.Serializable;
import java.net.URL;

/**
 * KajiLibrary's javax.sql.rowset.serial.SerialDatalink -- una copia de un valor DATALINK.
 *
 * <p>Un DATALINK de SQL es una columna que guarda un URL a un recurso <b>fuera</b> de la base. Esta
 * clase copia ese URL para que el valor sobreviva a la conexion.
 *
 * <p>Lo que copia es el <b>URL, no el recurso</b>. Es lo unico que se puede hacer --el recurso puede
 * ser un archivo de gigabytes en otra maquina-- y tiene una consecuencia que conviene tener presente:
 * un {@code SerialDatalink} serializado y leido en otro lado apunta al mismo lugar, que desde ahi
 * puede no existir.
 */
public class SerialDatalink implements Serializable, Cloneable {

    private static final long serialVersionUID = 2826907821828733626L;

    /** El URL copiado. */
    private final URL url;

    /**
     * @param url el URL del recurso
     * @throws SerialException si es null
     */
    public SerialDatalink(URL url) throws SerialException {
        if (url == null) {
            throw new SerialException("Cannot serialize empty URL instance");
        }
        this.url = url;
    }

    /** El URL. */
    public URL getDatalink() throws SerialException {
        return this.url;
    }

    /** Iguales si apuntan al mismo URL. */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SerialDatalink)) {
            return false;
        }
        return this.url.equals(((SerialDatalink) obj).url);
    }

    /** Coherente con {@link #equals}. */
    public int hashCode() {
        return 31 + this.url.hashCode();
    }

    /** Una copia; el URL es inmutable y se comparte. */
    public Object clone() {
        try {
            return new SerialDatalink(this.url);
        } catch (SerialException e) {
            return null;
        }
    }
}
