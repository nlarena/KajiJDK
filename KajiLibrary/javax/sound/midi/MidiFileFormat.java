package javax.sound.midi;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * KajiLibrary's javax.sound.midi.MidiFileFormat -- que hay en un archivo MIDI.
 *
 * <p>Lo que se puede saber sin cargar la obra entera: el tipo de archivo, como se cuenta el tiempo,
 * cuanto ocupa y cuanto dura.
 *
 * <h2>Los tres tipos de archivo</h2>
 *
 * <ul>
 *   <li><b>0</b>: una sola pista con todo mezclado;
 *   <li><b>1</b>: varias pistas que suenan <b>a la vez</b>. Es el normal;
 *   <li><b>2</b>: varias pistas <b>independientes</b>, que no comparten linea de tiempo. Casi no se
 *       usa y muchos programas ni lo abren.
 * </ul>
 *
 * <p>La confusion clasica es entre el 1 y el 2: los dos tienen varias pistas, y solo en el 1 esas
 * pistas son simultaneas.
 *
 * <p>{@link #getByteLength} y {@link #getMicrosecondLength} pueden valer {@link #UNKNOWN_LENGTH}
 * --que es -1-- cuando el archivo llega por un flujo sin final conocido.
 *
 * <p>Ver {@link Sequence} sobre {@link #getDivisionType} y {@link #getResolution}.
 */
public class MidiFileFormat {

    /** No se sabe. */
    public static final int UNKNOWN_LENGTH = -1;

    /** 0, 1 o 2. Ver la nota de la clase. */
    protected int type;

    /** Como se cuenta el tiempo; ver {@link Sequence}. */
    protected float divisionType;

    /** Pulsos por negra, o por cuadro. */
    protected int resolution;

    /** Cuantos bytes ocupa, o {@link #UNKNOWN_LENGTH}. */
    protected int byteLength;

    /** Cuanto dura, o {@link #UNKNOWN_LENGTH}. */
    protected long microsecondLength;

    /** Lo que no entra en los campos fijos. */
    private HashMap<String, Object> properties;

    /** El habitual. */
    public MidiFileFormat(int type, float divisionType, int resolution, int bytes,
                          long microseconds) {
        this.type = type;
        this.divisionType = divisionType;
        this.resolution = resolution;
        this.byteLength = bytes;
        this.microsecondLength = microseconds;
        this.properties = null;
    }

    /**
     * Idem, con propiedades.
     *
     * @throws NullPointerException si el mapa es null
     */
    public MidiFileFormat(int type, float divisionType, int resolution, int bytes,
                          long microseconds, Map<String, Object> properties) {
        this(type, divisionType, resolution, bytes, microseconds);
        this.properties = new HashMap<String, Object>(properties);
    }

    /** 0, 1 o 2. Ver la nota de la clase. */
    public int getType() {
        return this.type;
    }

    /** Como se cuenta el tiempo. */
    public float getDivisionType() {
        return this.divisionType;
    }

    /** Pulsos por negra, o por cuadro. */
    public int getResolution() {
        return this.resolution;
    }

    /** Cuantos bytes, o {@link #UNKNOWN_LENGTH}. */
    public int getByteLength() {
        return this.byteLength;
    }

    /** Cuanto dura, o {@link #UNKNOWN_LENGTH}. */
    public long getMicrosecondLength() {
        return this.microsecondLength;
    }

    /** Las propiedades, de solo lectura. */
    public Map<String, Object> properties() {
        Map<String, Object> ret;
        if (this.properties == null) {
            ret = new HashMap<String, Object>(0);
        } else {
            ret = new HashMap<String, Object>(this.properties);
        }
        return Collections.unmodifiableMap(ret);
    }

    /** Una propiedad, o null. */
    public Object getProperty(String key) {
        if (this.properties == null) {
            return null;
        }
        return this.properties.get(key);
    }
}
