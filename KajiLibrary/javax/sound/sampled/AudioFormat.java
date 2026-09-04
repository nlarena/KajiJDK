package javax.sound.sampled;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * KajiLibrary's javax.sound.sampled.AudioFormat -- como estan codificados los datos de audio.
 *
 * <p>Describe una tira de bytes de sonido: con que codificacion, a que frecuencia, con cuantos bits
 * por muestra, cuantos canales, y en que orden de bytes.
 *
 * <h2>Muestra, cuadro, y por que son dos cosas</h2>
 *
 * <p>Una <b>muestra</b> es un valor de un canal; un <b>cuadro</b> son todas las muestras de un
 * instante. En estereo de 16 bits, la muestra son 2 bytes y el cuadro son 4.
 *
 * <p>La distincion importa porque las posiciones y los largos de este paquete se miden en cuadros, no
 * en bytes ni en muestras. Confundirlos da audio a doble velocidad o con los canales cruzados.
 *
 * <h2>{@link #matches} no es simetrico</h2>
 *
 * <p>Es la parte que sorprende. {@code a.matches(b)} pregunta si <b>b</b> describe algo compatible con
 * a, tratando los {@link AudioSystem#NOT_SPECIFIED} <b>de b</b> como comodines.
 *
 * <p>Asi que un formato concreto coincide con uno subespecificado, y no al reves. Es lo correcto para
 * lo que se usa --preguntarle a una linea si acepta lo que tengo-- y hay que leerlo en el orden
 * correcto.
 *
 * <p>El orden de bytes solo se compara cuando hay mas de 8 bits por muestra: con un byte por muestra
 * no hay orden que discutir.
 *
 * <h2>Las propiedades</h2>
 *
 * <p>El mapa opcional lleva lo que no entra en los campos fijos: la tasa de bits de un formato
 * comprimido, la calidad, si es de tasa variable. Las claves estan definidas por convencion y una
 * implementacion puede agregar las suyas.
 */
public class AudioFormat {

    /** La codificacion. */
    protected AudioFormat.Encoding encoding;

    /** Muestras por segundo, o {@link AudioSystem#NOT_SPECIFIED}. */
    protected float sampleRate;

    /** Bits por muestra, o {@link AudioSystem#NOT_SPECIFIED}. */
    protected int sampleSizeInBits;

    /** Cuantos canales. */
    protected int channels;

    /** Bytes por cuadro. Ver la nota de la clase. */
    protected int frameSize;

    /** Cuadros por segundo. */
    protected float frameRate;

    /** Si el byte mas significativo va primero. */
    protected boolean bigEndian;

    /** Lo que no entra en los campos fijos; de solo lectura. */
    private HashMap<String, Object> properties;

    /**
     * El constructor completo.
     *
     * @param frameSize bytes por cuadro; ver la nota de la clase
     */
    public AudioFormat(AudioFormat.Encoding encoding, float sampleRate, int sampleSizeInBits,
                       int channels, int frameSize, float frameRate, boolean bigEndian) {
        this.encoding = encoding;
        this.sampleRate = sampleRate;
        this.sampleSizeInBits = sampleSizeInBits;
        this.channels = channels;
        this.frameSize = frameSize;
        this.frameRate = frameRate;
        this.bigEndian = bigEndian;
        this.properties = null;
    }

    /**
     * Idem, con propiedades.
     *
     * @param properties se copia; los cambios posteriores al mapa no afectan al formato
     * @throws NullPointerException si el mapa es null
     */
    public AudioFormat(AudioFormat.Encoding encoding, float sampleRate, int sampleSizeInBits,
                       int channels, int frameSize, float frameRate, boolean bigEndian,
                       Map<String, Object> properties) {
        this(encoding, sampleRate, sampleSizeInBits, channels, frameSize, frameRate, bigEndian);
        this.properties = new HashMap<String, Object>(properties);
    }

    /**
     * El atajo para PCM lineal, que es el caso normal.
     *
     * <p>Deduce la codificacion del booleano de signo, y calcula el tamano y la tasa de cuadro:
     * {@code (bits + 7) / 8 * canales} bytes por cuadro, y la tasa de cuadro igual a la de muestreo.
     * Es lo unico coherente en PCM.
     */
    public AudioFormat(float sampleRate, int sampleSizeInBits, int channels, boolean signed,
                       boolean bigEndian) {
        this(signed ? Encoding.PCM_SIGNED : Encoding.PCM_UNSIGNED,
             sampleRate, sampleSizeInBits, channels,
             (channels == AudioSystem.NOT_SPECIFIED
              || sampleSizeInBits == AudioSystem.NOT_SPECIFIED)
                 ? AudioSystem.NOT_SPECIFIED
                 : ((sampleSizeInBits + 7) / 8) * channels,
             sampleRate, bigEndian);
    }

    /** La codificacion. */
    public AudioFormat.Encoding getEncoding() {
        return this.encoding;
    }

    /** Muestras por segundo. */
    public float getSampleRate() {
        return this.sampleRate;
    }

    /** Bits por muestra. */
    public int getSampleSizeInBits() {
        return this.sampleSizeInBits;
    }

    /** Cuantos canales. */
    public int getChannels() {
        return this.channels;
    }

    /** Bytes por cuadro. Ver la nota de la clase. */
    public int getFrameSize() {
        return this.frameSize;
    }

    /** Cuadros por segundo. */
    public float getFrameRate() {
        return this.frameRate;
    }

    /** Si el byte mas significativo va primero. */
    public boolean isBigEndian() {
        return this.bigEndian;
    }

    /** Las propiedades, de solo lectura; vacio si no hay. */
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

    /**
     * Si ese formato es compatible con este.
     *
     * <p>Ver la nota de la clase: no es simetrico, y los comodines son los <b>del argumento</b>.
     */
    public boolean matches(AudioFormat format) {
        if (format.getEncoding() == null || getEncoding() == null) {
            return false;
        }
        if (!format.getEncoding().equals(getEncoding())) {
            return false;
        }
        if (format.getChannels() != AudioSystem.NOT_SPECIFIED
            && format.getChannels() != getChannels()) {
            return false;
        }
        if (format.getSampleRate() != (float) AudioSystem.NOT_SPECIFIED
            && format.getSampleRate() != getSampleRate()) {
            return false;
        }
        if (format.getSampleSizeInBits() != AudioSystem.NOT_SPECIFIED
            && format.getSampleSizeInBits() != getSampleSizeInBits()) {
            return false;
        }
        if (format.getFrameRate() != (float) AudioSystem.NOT_SPECIFIED
            && format.getFrameRate() != getFrameRate()) {
            return false;
        }
        if (format.getFrameSize() != AudioSystem.NOT_SPECIFIED
            && format.getFrameSize() != getFrameSize()) {
            return false;
        }
        // Con un byte por muestra no hay orden de bytes que discutir.
        return getSampleSizeInBits() <= 8
            || format.isBigEndian() == isBigEndian();
    }

    /**
     * Una descripcion legible.
     *
     * <p>La codificacion, y despues las partes que se sepan separadas por coma. La tasa de cuadro solo
     * aparece si difiere de la de muestreo --en PCM son iguales y repetirla seria ruido-- y el orden de
     * bytes solo si hay mas de 8 bits por muestra.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (getEncoding() != null) {
            sb.append(getEncoding().toString()).append(' ');
        }
        StringBuilder parts = new StringBuilder();
        appendPart(parts, sampleRateText());
        appendPart(parts, sampleSizeText());
        appendPart(parts, channelsText());
        appendPart(parts, frameSizeText());
        String rate = frameRateText();
        if (rate != null) {
            appendPart(parts, rate);
        }
        String endian = endianText();
        if (endian != null) {
            appendPart(parts, endian);
        }
        return sb.append(parts).toString();
    }

    /** Agrega una parte, con la coma que corresponda. */
    private static void appendPart(StringBuilder sb, String part) {
        if (sb.length() > 0) {
            sb.append(", ");
        }
        sb.append(part);
    }

    private String sampleRateText() {
        if (getSampleRate() == (float) AudioSystem.NOT_SPECIFIED) {
            return "unknown sample rate";
        }
        return getSampleRate() + " Hz";
    }

    private String sampleSizeText() {
        if (getSampleSizeInBits() == AudioSystem.NOT_SPECIFIED) {
            return "unknown bits per sample";
        }
        return getSampleSizeInBits() + " bit";
    }

    private String channelsText() {
        if (getChannels() == 1) {
            return "mono";
        }
        if (getChannels() == 2) {
            return "stereo";
        }
        if (getChannels() == AudioSystem.NOT_SPECIFIED) {
            return "unknown number of channels";
        }
        return getChannels() + " channels";
    }

    private String frameSizeText() {
        if (getFrameSize() == AudioSystem.NOT_SPECIFIED) {
            return "unknown frame size";
        }
        return getFrameSize() + " bytes/frame";
    }

    /** Solo si difiere de la tasa de muestreo; null si no hay nada que decir. */
    private String frameRateText() {
        if (Math.abs(getSampleRate() - getFrameRate()) <= 0.00001) {
            return null;
        }
        if (getFrameRate() == (float) AudioSystem.NOT_SPECIFIED) {
            return "unknown frame rate";
        }
        return getFrameRate() + " frames/second";
    }

    /** Solo para PCM de mas de 8 bits; null si no aplica. */
    private String endianText() {
        if (getEncoding() == null) {
            return null;
        }
        boolean pcm = getEncoding().equals(Encoding.PCM_SIGNED)
            || getEncoding().equals(Encoding.PCM_UNSIGNED);
        if (!pcm) {
            return null;
        }
        if (getSampleSizeInBits() <= 8 && getSampleSizeInBits() != AudioSystem.NOT_SPECIFIED) {
            return null;
        }
        if (isBigEndian()) {
            return "big-endian";
        }
        return "little-endian";
    }

    /**
     * Una codificacion de audio.
     *
     * <p>No es un enum a proposito: el constructor es publico para que un proveedor pueda declarar
     * codificaciones que la plataforma no conoce. Las cinco constantes son las que el JDK nombra.
     *
     * <p>La igualdad es por nombre, asi que una codificacion propia que se llame igual que una
     * estandar <b>es</b> la estandar.
     */
    public static class Encoding {

        /** PCM lineal con signo. Lo mas comun. */
        public static final Encoding PCM_SIGNED = new Encoding("PCM_SIGNED");

        /** PCM lineal sin signo; lo habitual en 8 bits. */
        public static final Encoding PCM_UNSIGNED = new Encoding("PCM_UNSIGNED");

        /** PCM en coma flotante. */
        public static final Encoding PCM_FLOAT = new Encoding("PCM_FLOAT");

        /** Compresion logaritmica mu-law, de telefonia. */
        public static final Encoding ULAW = new Encoding("ULAW");

        /** Compresion logaritmica A-law, de telefonia. */
        public static final Encoding ALAW = new Encoding("ALAW");

        /** El nombre, que es la identidad. */
        private final String name;

        /** @param name el nombre; es lo unico que distingue una codificacion de otra */
        public Encoding(String name) {
            this.name = name;
        }

        /** Por nombre. */
        @Override
        public final boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Encoding)) {
                return false;
            }
            Encoding other = (Encoding) obj;
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
    }
}
