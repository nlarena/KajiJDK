package javax.sound.sampled;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * KajiLibrary's javax.sound.sampled.AudioFileFormat -- que hay en un archivo de audio.
 *
 * <p>Junta dos cosas que se confunden: el <b>tipo de archivo</b> --WAVE, AU, AIFF-- y el
 * {@link AudioFormat} de los datos que lleva adentro. Son independientes: un WAVE puede contener PCM
 * de 16 bits o mu-law de 8.
 *
 * <p>{@link #getFrameLength} y {@link #getByteLength} pueden valer {@link AudioSystem#NOT_SPECIFIED}
 * cuando el largo no se sabe -- un flujo que llega por red, o un archivo cuyo encabezado no lo dice.
 * El de bytes ademas es -1 salvo que se use el constructor protegido, asi que no hay que contar con
 * el.
 *
 * <p>Es inmutable.
 */
public class AudioFileFormat {

    /** El tipo de archivo. */
    private final Type type;

    /** El formato de los datos. */
    private final AudioFormat format;

    /** Cuantos cuadros, o {@link AudioSystem#NOT_SPECIFIED}. */
    private final int frameLength;

    /** Cuantos bytes en total, o {@link AudioSystem#NOT_SPECIFIED}. */
    private final int byteLength;

    /** Lo que no entra en los campos fijos. */
    private HashMap<String, Object> properties;

    /**
     * El constructor con largo en bytes, para quien lea el archivo.
     *
     * <p>Protegido porque solo tiene sentido para un lector: quien arma un formato a mano no sabe
     * cuantos bytes va a ocupar.
     */
    protected AudioFileFormat(Type type, int byteLength, AudioFormat format, int frameLength) {
        this.type = type;
        this.byteLength = byteLength;
        this.format = format;
        this.frameLength = frameLength;
        this.properties = null;
    }

    /** El habitual; el largo en bytes queda sin especificar. */
    public AudioFileFormat(Type type, AudioFormat format, int frameLength) {
        this(type, AudioSystem.NOT_SPECIFIED, format, frameLength);
    }

    /**
     * Idem, con propiedades.
     *
     * @throws NullPointerException si el mapa es null
     */
    public AudioFileFormat(Type type, AudioFormat format, int frameLength,
                           Map<String, Object> properties) {
        this(type, AudioSystem.NOT_SPECIFIED, format, frameLength);
        this.properties = new HashMap<String, Object>(properties);
    }

    /** El tipo de archivo. */
    public Type getType() {
        return this.type;
    }

    /** Cuantos bytes, o {@link AudioSystem#NOT_SPECIFIED}. Ver la nota de la clase. */
    public int getByteLength() {
        return this.byteLength;
    }

    /** El formato de los datos. */
    public AudioFormat getFormat() {
        return this.format;
    }

    /** Cuantos cuadros, o {@link AudioSystem#NOT_SPECIFIED}. */
    public int getFrameLength() {
        return this.frameLength;
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

    /** El tipo con su extension, el formato de datos, y el largo si se sabe. */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (this.type != null) {
            sb.append(this.type).append(" (.").append(this.type.getExtension()).append(") file");
        } else {
            sb.append("unknown file format");
        }
        if (this.byteLength != AudioSystem.NOT_SPECIFIED) {
            sb.append(", byte length: ").append(this.byteLength);
        }
        sb.append(", data format: ").append(this.format);
        if (this.frameLength != AudioSystem.NOT_SPECIFIED) {
            sb.append(", frame length: ").append(this.frameLength);
        }
        return sb.toString();
    }

    /**
     * Un tipo de archivo de audio.
     *
     * <p>No es un enum, por lo mismo que {@link AudioFormat.Encoding}: un proveedor puede traer tipos
     * propios. Cada uno lleva ademas su extension habitual.
     *
     * <p>{@link #AIFC} tiene una particularidad que se ve en su {@code toString}: se llama
     * {@code "AIFF-C"} y no {@code "AIFC"}. Es AIFF con compresion, y el nombre lo dice.
     */
    public static class Type {

        /** WAV de Microsoft. */
        public static final Type WAVE = new Type("WAVE", "wav");

        /** AU de Sun. */
        public static final Type AU = new Type("AU", "au");

        /** AIFF de Apple. */
        public static final Type AIFF = new Type("AIFF", "aif");

        /** AIFF con compresion. Ver la nota de la clase. */
        public static final Type AIFC = new Type("AIFF-C", "aifc");

        /** El mismo formato que {@link #AU}, con otra extension. */
        public static final Type SND = new Type("SND", "snd");

        /** El nombre, que es la identidad. */
        private final String name;

        /** La extension habitual, sin punto. */
        private final String extension;

        /**
         * @param name el nombre; es lo unico que distingue un tipo de otro
         * @param extension la extension habitual, sin el punto
         */
        public Type(String name, String extension) {
            this.name = name;
            this.extension = extension;
        }

        /** Por nombre; la extension no entra. */
        @Override
        public final boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Type)) {
                return false;
            }
            Type other = (Type) obj;
            if (this.name == null) {
                return other.name == null;
            }
            return this.name.equals(other.name);
        }

        /** El del nombre. */
        @Override
        public final int hashCode() {
            if (this.name == null) {
                return 0;
            }
            return this.name.hashCode();
        }

        /** El nombre. */
        @Override
        public final String toString() {
            return this.name;
        }

        /** La extension habitual, sin punto. */
        public String getExtension() {
            return this.extension;
        }
    }
}
